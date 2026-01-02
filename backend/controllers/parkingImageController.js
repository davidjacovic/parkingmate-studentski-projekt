const ParkingImage = require('../models/parkingImageModel');
const multer = require('multer');
const path = require('path');

// Konfiguracija multer storage za čuvanje upload-ovanih fajlova
const storage = multer.diskStorage({
    destination: function (req, file, cb) {
        cb(null, 'uploads/'); // Čuva fajlove u 'uploads' direktorijum
    },
    filename: function (req, file, cb) {
        // Generiše jedinstveno ime fajla: timestamp + originalna ekstenzija
        cb(null, Date.now() + path.extname(file.originalname));
    }
});

// Middleware za obradu upload-a slika (jedna slika po zahtevu)
exports.uploadImageMiddleware = multer({ storage: storage }).single('file');

// Kontroler za kreiranje novog parking image-a sa upload-ovanom slikom
exports.create = async (req, res) => {
    try {
        const { parkingLocationId, coordinates, timestamp } = req.body;
        const file = req.file; // Upload-ovani fajl

        // Validacija - mora biti upload-ovana slika
        if (!file) {
            return res.status(400).json({ message: 'No image has been sent.' });
        }
        
        // Obrada koordinata - podržava niz ili string format
        let coords = [0, 0];
        if (coordinates) {
            if (Array.isArray(coordinates)) {
                coords = coordinates.map(Number); // Konvertuje niz u brojeve
            } else {
                const parts = coordinates.split(',');
                coords = parts.map(Number); // Parsira string "lon,lat" u niz brojeva
            }
        }
        
        // Obrada timestamp-a - ako je poslat, koristi ga, inače trenutno vreme
        const imageTimestamp = timestamp ? new Date(Number(timestamp)) : Date.now();

        // Kreiranje novog ParkingImage dokumenta
        const newImage = new ParkingImage({
            parkingLocationId,
            location: {
                type: 'Point', // GeoJSON Point tip za geografske koordinate
                coordinates: coords // [longitude, latitude]
            },
            imageUrl: file.filename, // Ime sačuvanog fajla
            timestamp: imageTimestamp
        });

        await newImage.save(); // Čuva u bazu
        res.status(201).json({ message: 'Image and metadata saved', data: newImage });

    } catch (err) {
        console.error('Error saving parking image:', err);
        res.status(500).json({ message: 'Server error', error: err.message });
    }
};

// Kontroler za kreiranje simuliranih parking podataka (bez stvarne slike)
exports.createSimulated = async ({ parkingLocationId, coordinates, timestamp, imageUrl, urvrvResult }) => {
    try {
        // Ista logika obrade koordinata kao u create funkciji
        let coords = [0, 0];
        if (coordinates) {
            if (Array.isArray(coordinates)) {
                coords = coordinates.map(Number);
            } else {
                const parts = coordinates.split(',');
                coords = parts.map(Number);
            }
        }

        // Obrada timestamp-a
        const imageTimestamp = timestamp ? new Date(Number(timestamp)) : Date.now();

        // Kreiranje simuliranog ParkingImage dokumenta
        const newImage = new ParkingImage({
            parkingLocationId,
            location: {
                type: 'Point',
                coordinates: coords
            },
            imageUrl: imageUrl || 'simulated.jpg', // Podrazumevana slika za simulacije
            timestamp: imageTimestamp,
            urvrvResult: urvrvResult || whenSimulation(value) // Koristi poslate podatke ili generiše
        });

        await newImage.save();
        console.log('Simulated ParkingImage saved:', newImage._id);
        return newImage;
    } catch (err) {
        console.error('Error saving simulated parking image:', err);
        throw err;
    }
};

// Pomoćna funkcija za generisanje simuliranih parking podataka (fallback)
function whenSimulation(value) {
    const num = Number(value) || 0; // Konvertuje vrednost u broj ili 0
    const totalSpots = 100; // Fiksan broj ukupnih parking mesta
    const occupiedSpaces = Math.min(num, totalSpots); // Zauzeto do maksimalno 100
    const freeSpaces = totalSpots - occupiedSpaces; // Slobodna mesta

    return {
        totalSpots,
        freeSpaces,
        occupiedSpaces,
        spotsCoordinates: [] // Prazna lista koordinata pojedinačnih mesta
    };
}
