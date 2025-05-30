var Parking_locationModel = require('../models/parkingLocationModel.js');
const ParkingLog = require('../models/parkingLogModel.js');
// or the correct relative path to your model


// Helper funkcija koja konvertuje Decimal128 u number za coordinates
function convertDecimalCoordinates(parkingLocation) {
    if (parkingLocation.location && Array.isArray(parkingLocation.location.coordinates)) {
        parkingLocation.location.coordinates = parkingLocation.location.coordinates.map(coord => {
            let num;
            if (coord && typeof coord.toDouble === 'function') {
                num = coord.toDouble();
            } else {
                num = Number(coord);
            }
            if (isNaN(num)) {
                console.error('Invalid coordinate value:', coord);
                throw new Error('Invalid coordinate value');
            }
            return num;
        });
    }
    return parkingLocation;
}


module.exports = {

    list: function (req, res) {
        Parking_locationModel.find(function (err, parking_locations) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when getting parking_locations.',
                    error: err
                });
            }
            const converted = parking_locations.map(convertDecimalCoordinates);
            return res.json(converted);
        });
    },

    show: function (req, res) {
        var id = req.params.id;
        Parking_locationModel.findOne({ _id: id }, function (err, parking_location) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when getting parking_location.',
                    error: err
                });
            }
            if (!parking_location) {
                return res.status(404).json({
                    message: 'No such parking_location'
                });
            }
            convertDecimalCoordinates(parking_location);
            return res.json(parking_location);
        });
    },

    create: function (req, res) {
        var data = req.body;

        var parking_location = new Parking_locationModel({
            name: data.name,
            address: data.address,
            location: data.location,
            total_regular_spots: data.total_regular_spots,
            total_invalid_spots: data.total_invalid_spots,
            total_bus_spots: data.total_bus_spots,
            available_regular_spots: data.available_regular_spots,
            available_invalid_spots: data.available_invalid_spots,
            available_bus_spots: data.available_bus_spots,
            created: new Date(),
            modified: new Date(),
            description: data.description,
            hidden: data.hidden,
            subscriber: data.subscriber
        });

        parking_location.save(function (err, parking_location) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when creating parking_location',
                    error: err
                });
            }
            convertDecimalCoordinates(parking_location);
            return res.status(201).json(parking_location);
        });
    },

    update: function (req, res) {
        var id = req.params.id;
        var data = req.body;

        Parking_locationModel.findOne({ _id: id }, function (err, parking_location) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when getting parking_location',
                    error: err
                });
            }
            if (!parking_location) {
                return res.status(404).json({
                    message: 'No such parking_location'
                });
            }

            parking_location.name = data.name !== undefined ? data.name : parking_location.name;
            parking_location.address = data.address !== undefined ? data.address : parking_location.address;
            parking_location.location = data.location !== undefined ? data.location : parking_location.location;
            parking_location.total_regular_spots = data.total_regular_spots !== undefined ? data.total_regular_spots : parking_location.total_regular_spots;
            parking_location.total_invalid_spots = data.total_invalid_spots !== undefined ? data.total_invalid_spots : parking_location.total_invalid_spots;
            parking_location.total_bus_spots = data.total_bus_spots !== undefined ? data.total_bus_spots : parking_location.total_bus_spots;
            parking_location.available_regular_spots = data.available_regular_spots !== undefined ? data.available_regular_spots : parking_location.available_regular_spots;
            parking_location.available_invalid_spots = data.available_invalid_spots !== undefined ? data.available_invalid_spots : parking_location.available_invalid_spots;
            parking_location.available_bus_spots = data.available_bus_spots !== undefined ? data.available_bus_spots : parking_location.available_bus_spots;
            parking_location.description = data.description !== undefined ? data.description : parking_location.description;
            parking_location.hidden = data.hidden !== undefined ? data.hidden : parking_location.hidden;
            parking_location.subscriber = data.subscriber !== undefined ? data.subscriber : parking_location.subscriber;
            parking_location.modified = new Date();

            parking_location.save(function (err, parking_location) {
                if (err) {
                    return res.status(500).json({
                        message: 'Error when updating parking_location.',
                        error: err
                    });
                }
                convertDecimalCoordinates(parking_location);
                return res.json(parking_location);
            });
        });
    },

    remove: function (req, res) {
        var id = req.params.id;
        Parking_locationModel.findByIdAndRemove(id, function (err, parking_location) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when deleting the parking_location.',
                    error: err
                });
            }
            return res.status(204).json();
        });
    },

    nearby: async function (req, res) {
        const { lat, lng, radius } = req.query;

        if (!lat || !lng || !radius) {
            return res.status(400).json({
                message: 'Missing required query parameters: lat, lng, radius'
            });
        }

        try {
            const locations = await Parking_locationModel.find({
                location: {
                    $near: {
                        $geometry: {
                            type: "Point",
                            coordinates: [parseFloat(lng), parseFloat(lat)]
                        },
                        $maxDistance: parseInt(radius) // in meters
                    }
                }
            });

            const convertedLocations = locations.map(convertDecimalCoordinates);
            return res.json(convertedLocations);
        } catch (err) {
            return res.status(500).json({
                message: 'Error during geospatial search',
                error: err
            });
        }
    },

    getOccupancyStatus: async function (req, res) {
        try {
            const locations = await Parking_locationModel.find({});
            const processed = locations.map(loc => {
                const total = loc.total_regular_spots || 0;
                const available = loc.available_regular_spots || 0;
                const occupied = total - available;
                const occupancy = total > 0 ? (occupied / total) * 100 : 0;

                return {
                    _id: loc._id,
                    name: loc.name,
                    address: loc.address,
                    location: loc.location,
                    occupancy: Math.round(occupancy),
                };
            });
            res.json(processed);
        } catch (err) {
            res.status(500).json({
                message: 'Error while fetching occupancy data',
                error: err
            });
        }
    },

    getParkingLogsByLocation: async function (req, res) {
        try {
            const locationId = req.params.id;
            const fromDate = req.query.from ? new Date(req.query.from) : null;
            const toDate = req.query.to ? new Date(req.query.to) : null;

            const filter = { parkingLocationId: locationId };
            if (fromDate || toDate) {
                filter.timestamp = {};
                if (fromDate) filter.timestamp.$gte = fromDate;
                if (toDate) filter.timestamp.$lte = toDate;
            }

            const logs = await ParkingLog.find(filter)
                .sort({ timestamp: -1 })
                .limit(1000);

            res.json(logs.reverse());
        } catch (err) {
            res.status(500).json({ error: err.message });
        }
    },
};
