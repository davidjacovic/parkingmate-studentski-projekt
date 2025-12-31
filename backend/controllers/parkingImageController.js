const ParkingImage = require('../models/parkingImageModel');
const multer = require('multer');
const path = require('path');

const storage = multer.diskStorage({
    destination: function (req, file, cb) {
        cb(null, 'uploads/');
    },
    filename: function (req, file, cb) {
        cb(null, Date.now() + path.extname(file.originalname));
    }
});

exports.uploadImageMiddleware = multer({ storage: storage }).single('file');
exports.create = async (req, res) => {
    try {
        const { parkingLocationId, coordinates, timestamp } = req.body;
        const file = req.file;

        if (!file) {
            return res.status(400).json({ message: 'Nije poslata nijedna slika.' });
        }
        let coords = [0, 0];
        if (coordinates) {
            if (Array.isArray(coordinates)) {
                coords = coordinates.map(Number);
            } else {
                const parts = coordinates.split(',');
                coords = parts.map(Number);
            }
        }
        const imageTimestamp = timestamp ? new Date(Number(timestamp)) : Date.now();

        const newImage = new ParkingImage({
            parkingLocationId,
            location: {
                type: 'Point',
                coordinates: coords
            },
            imageUrl: file.filename,
            timestamp: imageTimestamp
        });

        await newImage.save();
        res.status(201).json({ message: 'Slika i metapodaci sačuvani', data: newImage });

    } catch (err) {
        console.error('Error saving parking image:', err);
        res.status(500).json({ message: 'Server error', error: err.message });
    }
};

exports.createSimulated = async ({ parkingLocationId, coordinates, timestamp, value }) => {
    try {
        let coords = [0, 0];
        if (coordinates) {
            if (Array.isArray(coordinates)) {
                coords = coordinates.map(Number);
            } else {
                const parts = coordinates.split(',');
                coords = parts.map(Number);
            }
        }

        const imageTimestamp = timestamp ? new Date(Number(timestamp)) : Date.now();

        const newImage = new ParkingImage({
            parkingLocationId,
            location: {
                type: 'Point',
                coordinates: coords
            },
            imageUrl: 'simulated.jpg',
            timestamp: imageTimestamp,
            urvrvResult: whenSimulation(value) 
        });

        await newImage.save();
        console.log('Simulated ParkingImage saved:', newImage._id);
        return newImage;
    } catch (err) {
        console.error('Error saving simulated parking image:', err);
        throw err;
    }
};

function whenSimulation(value) {
    const num = Number(value) || 0;
    return {
        totalSpots: 100,
        freeSpaces: 100 - num,
        occupiedSpaces: num,
        spotsCoordinates: []
    };
}

