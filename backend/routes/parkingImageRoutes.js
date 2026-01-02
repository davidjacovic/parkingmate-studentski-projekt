const express = require('express');
const router = express.Router();
const parkingImageController = require('../controllers/parkingImageController');

router.post('/', parkingImageController.uploadImageMiddleware, parkingImageController.create);
router.post('/simulated', async (req, res) => {
    try {
        const { parkingLocationId, coordinates, timestamp, imageUrl, urvrvResult } = req.body;
        
        console.log('Received simulated data:', {
            parkingLocationId,
            coordinates,
            timestamp,
            imageUrl,
            urvrvResult
        });

        const newImage = await parkingImageController.createSimulated({
            parkingLocationId,
            coordinates,
            timestamp,
            imageUrl,
            urvrvResult
        });
        
        res.status(201).json({ message: 'Simulated image saved', data: newImage });
    } catch (err) {
        console.error('Error saving simulated image:', err);
        res.status(500).json({ message: 'Server error', error: err.message });
    }
});

module.exports = router;
