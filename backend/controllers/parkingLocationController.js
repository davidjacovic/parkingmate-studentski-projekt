var Parking_locationModel = require('../models/parkingLocationModel.js');
const ParkingLog = require('../models/parkingLogModel.js');
const UserModel = require('../models/userModel');
const ParkingLocationModel = require('../models/parkingLocationModel');

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
    const id = req.params.id;

    console.log('🔎 Pozvana funkcija show sa ID:', id);

    // Provera validnosti MongoDB ObjectId formata (opciono, ali korisno)
    if (!id || !id.match(/^[0-9a-fA-F]{24}$/)) {
        console.warn('⚠️ Nevalidan MongoDB ID:', id);
        return res.status(400).json({ message: 'Nevalidan ID formata.' });
    }

    Parking_locationModel.findById(id, function (err, parking_location) {
        if (err) {
            console.error('❌ Greška pri dohvaćanju parking lokacije:', err);
            return res.status(500).json({ message: 'Error when getting parking_location.', error: err });
        }

        if (!parking_location) {
            console.warn('❓ Nema parking lokacije sa tim ID:', id);
            return res.status(404).json({ message: 'No such parking_location' });
        }

        console.log('✅ Parking lokacija pronađena:', parking_location);

        convertDecimalCoordinates(parking_location);

        console.log('📦 Parking lokacija nakon konverzije koordinata:', parking_location);

        return res.json(parking_location);
    });
},

    create: function (req, res) {
        try {
            console.log('create parkingLocation called');
            console.log('req.user:', req.user);       // log korisnika (token)
            console.log('req.body:', req.body);       // log body requesta
            // pretpostavka da je user tu, ili zameni po potrebi
            var data = req.body;

            var parking_location = new Parking_locationModel({
                name: data.name,
                address: data.address,
                location: {
                    type: 'Point',
                    coordinates: data.location.coordinates // očekujem [lng, lat]
                },
                total_regular_spots: data.total_regular_spots,
                total_invalid_spots: data.total_invalid_spots,
                total_bus_spots: data.total_bus_spots,
                available_regular_spots: data.available_regular_spots,
                available_invalid_spots: data.available_invalid_spots,
                available_bus_spots: data.available_bus_spots,
                created: new Date(),
                modified: new Date(),
                description: data.description,
                hidden: data.hidden
            });

            parking_location.save(function (err, parking_location) {
                if (err) {
                    console.error('Error when creating parking_location:', err);
                    return res.status(500).json({
                        message: 'Error when creating parking_location',
                        error: err.message || err
                    });
                }

                console.log('Parking location saved:', parking_location);

                // ako treba formatirati koordinate, pozovi funkciju
                if (typeof convertDecimalCoordinates === 'function') {
                    convertDecimalCoordinates(parking_location);
                }

                // Kreiraj inicijalni log zapisa
                const log = new ParkingLog({
                    parkingLocationId: parking_location._id,
                    available_regular_spots: parking_location.available_regular_spots,
                    available_invalid_spots: parking_location.available_invalid_spots,
                    available_bus_spots: parking_location.available_bus_spots
                });

                log.save(err => {
                    if (err) {
                        console.error('Error saving parking log:', err);
                    } else {
                        console.log('Initial parking log saved.');
                    }
                });

                return res.status(201).json(parking_location);
            });

        } catch (error) {
            console.error('Unexpected error in create:', error);
            return res.status(500).json({ message: 'Server error', error: error.message });
        }
    },


    update: async function (req, res) {
        const id = req.params.id;
        const data = req.body;

        console.log('🛠 UPDATE pozvan za ID:', id);
        console.log('📥 Podaci za update:', data);

        try {
            const parking_location = await Parking_locationModel.findById(id);
            if (!parking_location) {
                console.warn('⚠️ Parking lokacija nije pronađena za ID:', id);
                return res.status(404).json({ message: 'No such parking_location' });
            }

            console.log('✅ Lokacija pronađena:', parking_location.name);

            // Log promena
            if (data.name !== undefined) console.log(`🔄 Menjam name: ${parking_location.name} -> ${data.name}`);
            if (data.address !== undefined) console.log(`🔄 Menjam address: ${parking_location.address} -> ${data.address}`);
            if (data.location !== undefined) console.log(`🔄 Menjam location:`, data.location);

            // Update polja ako postoje u data
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
            parking_location.modified = new Date();

            await parking_location.save();

            console.log('✅ Uspesno sacuvana izmenjena lokacija:', parking_location._id);
            convertDecimalCoordinates(parking_location);
            return res.json(parking_location);

        } catch (err) {
            console.error('❌ Greška prilikom updejta:', err);
            return res.status(500).json({
                message: 'Error when updating parking_location.',
                error: err
            });
        }
    },
    remove: async function (req, res) {
        const id = req.params.id;
        console.log('🗑 DELETE pozvan za parkingLocation:', id);

        try {
            const location = await ParkingLocationModel.findById(id);
            if (!location) {
                console.warn('❓ Lokacija nije pronađena');
                return res.status(404).json({ message: 'Lokacija nije pronađena.' });
            }

            await ParkingLocationModel.findByIdAndDelete(id);
            console.log('✅ Lokacija obrisana:', id);
            return res.status(204).json();
        } catch (err) {
            console.error('❌ Greška pri brisanju lokacije:', err);
            return res.status(500).json({ message: 'Greška pri brisanju lokacije.', error: err });
        }

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

    filteredParkingLocations: async function (req, res) {
        const filterRegular = req.query.regular === 'true';
        const filterInvalid = req.query.invalid === 'true';
        const filterElectric = req.query.electric === 'true';
        const filterBus = req.query.bus === 'true';

        // Build MongoDB query filters
        const orFilters = [];

        if (filterRegular) orFilters.push({ available_regular_spots: { $gt: 0 } });
        if (filterInvalid) orFilters.push({ available_invalid_spots: { $gt: 0 } });
        if (filterElectric) orFilters.push({ available_electric_spots: { $gt: 0 } });
        if (filterBus) orFilters.push({ available_bus_spots: { $gt: 0 } });

        try {
            let query = {};
            if (orFilters.length > 0) {
                query = { $or: orFilters };
            }

            const locations = await Parking_locationModel.find(query);
            res.json(locations);
        } catch (err) {
            console.error(err);
            res.status(500).json({ message: 'Error fetching filtered parking locations' });
        }
    }


};
