const Event = require('../models/eventModel');
const ParkingLocationModel = require('../models/parkingLocationModel');

// Kreira novi dogodak
exports.create = async (req, res) => {
    try {
        const { topic, message, timestamp, location, eventType } = req.body;

        // Validacija obaveznih polja
        if (!topic || !message || !location || !eventType) {
            return res.status(400).json({
                success: false,
                message: 'Sva obavezna polja moraju biti popunjena (topic, message, location, eventType)'
            });
        }

        // Validacija eventType
        const validEventTypes = ['PARKING_FULL', 'PARKING_AVAILABLE', 'LOW_AVAILABILITY'];
        if (!validEventTypes.includes(eventType)) {
            return res.status(400).json({
                success: false,
                message: `eventType mora biti jedan od: ${validEventTypes.join(', ')}`
            });
        }

        // Validacija lokacije (format: "lat,lon")
        const locationPattern = /^-?\d+\.?\d*,-?\d+\.?\d*$/;
        if (!locationPattern.test(location)) {
            return res.status(400).json({
                success: false,
                message: 'Lokacija mora biti u formatu "latitude,longitude"'
            });
        }

        // Validacija timestamp-a
        let eventTimestamp;
        if (timestamp) {
            eventTimestamp = new Date(Number(timestamp));
            if (isNaN(eventTimestamp.getTime())) {
                return res.status(400).json({
                    success: false,
                    message: 'Timestamp mora biti validan Unix timestamp (milisekunde)'
                });
            }
        } else {
            eventTimestamp = new Date();
        }

        // Kreiraj novi dogodak
        const newEvent = new Event({
            topic,
            message,
            timestamp: eventTimestamp,
            location,
            eventType,
            status: 'PENDING'
        });

        const savedEvent = await newEvent.save();
        const parsed = parseLatLon(location);
        if (parsed) {
            await tryUpdateParkingFromEvent({ ...parsed, eventType });
        }

        res.status(201).json({
            success: true,
            message: 'Dogodak uspešno kreiran',
            data: savedEvent
        });

    } catch (err) {
        console.error('Error creating event:', err);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: err.message
        });
    }
};

// Dohvata sve dogodke sa opcionim filtriranjem
exports.getAll = async (req, res) => {
    try {
        const { eventType, status, startDate, endDate, limit = 50, skip = 0 } = req.query;

        // Kreiraj filter objekat
        const filter = {};

        if (eventType) {
            filter.eventType = eventType;
        }

        if (status) {
            filter.status = status;
        }

        if (startDate || endDate) {
            filter.timestamp = {};
            if (startDate) {
                filter.timestamp.$gte = new Date(Number(startDate));
            }
            if (endDate) {
                filter.timestamp.$lte = new Date(Number(endDate));
            }
        }

        const events = await Event.find(filter)
            .sort({ timestamp: -1 })
            .limit(parseInt(limit))
            .skip(parseInt(skip));

        const total = await Event.countDocuments(filter);

        res.json({
            success: true,
            data: events,
            pagination: {
                total,
                limit: parseInt(limit),
                skip: parseInt(skip),
                hasMore: total > parseInt(skip) + parseInt(limit)
            }
        });

    } catch (err) {
        console.error('Error fetching events:', err);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: err.message
        });
    }
};

// Dohvata jedan dogodak po ID-u
exports.getById = async (req, res) => {
    try {
        const { id } = req.params;

        const event = await Event.findById(id);

        if (!event) {
            return res.status(404).json({
                success: false,
                message: 'Dogodak nije pronađen'
            });
        }

        res.json({
            success: true,
            data: event
        });

    } catch (err) {
        console.error('Error fetching event:', err);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: err.message
        });
    }
};

// Ažurira status dogodka (npr. nakon blockchain zapisa)
exports.updateStatus = async (req, res) => {
    try {
        const { id } = req.params;
        const { status, blockchainHash, blockchainTimestamp } = req.body;

        const event = await Event.findById(id);

        if (!event) {
            return res.status(404).json({
                success: false,
                message: 'Dogodak nije pronađen'
            });
        }

        if (status) {
            const validStatuses = ['PENDING', 'PROCESSED', 'BLOCKCHAIN_RECORDED'];
            if (!validStatuses.includes(status)) {
                return res.status(400).json({
                    success: false,
                    message: `Status mora biti jedan od: ${validStatuses.join(', ')}`
                });
            }
            event.status = status;
        }

        if (blockchainHash) {
            event.blockchainHash = blockchainHash;
        }

        if (blockchainTimestamp) {
            event.blockchainTimestamp = new Date(Number(blockchainTimestamp));
        }

        const updatedEvent = await event.save();

        res.json({
            success: true,
            message: 'Status dogodka ažuriran',
            data: updatedEvent
        });

    } catch (err) {
        console.error('Error updating event status:', err);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: err.message
        });
    }
};

function parseLatLon(locationStr) {
    // očekuje "lat,lon"
    const parts = (locationStr || "").split(",");
    if (parts.length !== 2) return null;

    const lat = Number(parts[0].trim());
    const lon = Number(parts[1].trim());

    if (Number.isNaN(lat) || Number.isNaN(lon)) return null;
    return { lat, lon };
}

// procenti su "fake" procene u odnosu na total
function calcAvailableByEvent(total, eventType) {
    if (!total || total <= 0) return null;

    switch (eventType) {
        case "PARKING_FULL":
            return 0;

        case "LOW_AVAILABILITY":
            return Math.max(1, Math.ceil(total * 0.15)); // 15% slobodno

        case "PARKING_AVAILABLE":
            return Math.max(1, Math.ceil(total * 0.70)); // 70% slobodno

        default:
            return null;
    }
}

async function tryUpdateParkingFromEvent({ lat, lon, eventType }) {
    try {
        const maxDistanceMeters = 30;

        const parking = await ParkingLocationModel.findOne({
            location: {
                $near: {
                    $geometry: { type: "Point", coordinates: [lon, lat] }, // [lng, lat]
                    $maxDistance: maxDistanceMeters
                }
            }
        });

        if (!parking) return;

        const total = parking.total_regular_spots || 0;
        const newAvailable = calcAvailableByEvent(total, eventType);
        if (newAvailable == null) return;

        parking.available_regular_spots = newAvailable;
        parking.modified = new Date();
        await parking.save();

        console.log(`✅ Parking availability updated from event: ${parking._id} -> available_regular_spots=${newAvailable}`);
    } catch (e) {
        // bitno: NIKAD ne ruši event flow
        console.error("⚠️ Event->Parking update failed:", e.message);
    }
}
