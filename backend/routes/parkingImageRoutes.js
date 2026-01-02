const express = require('express');
const router = express.Router();
const parkingImageController = require('../controllers/parkingImageController');

// Ruta za upload realne slike parkinga
router.post('/', parkingImageController.uploadImageMiddleware, parkingImageController.create);

// Ruta za simulirane podatke parkinga
router.post('/simulated', async (req, res) => {
    try {
        // Izvlači podatke iz tela zahteva
        const { parkingLocationId, coordinates, timestamp, imageUrl, urvrvResult } = req.body;
        
        // Log-uje primljene podatke za debagovanje
        console.log('Received simulated data:', {
            parkingLocationId,
            coordinates,
            timestamp,
            imageUrl,
            urvrvResult
        });

        // Poziva kontroler za kreiranje simuliranog zapisa
        const newImage = await parkingImageController.createSimulated({
            parkingLocationId,
            coordinates,
            timestamp,
            imageUrl,
            urvrvResult
        });
        
        // Vraća uspešan odgovor sa kreiranim podacima
        res.status(201).json({ message: 'Simulated image saved', data: newImage });
    } catch (err) {
        console.error('Error saving simulated image:', err);
        res.status(500).json({ message: 'Server error', error: err.message });
    }
});

module.exports = router;