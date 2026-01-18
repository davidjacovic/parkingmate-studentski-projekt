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

        let total = Number(urvrvResult?.totalSpots);
        let free = Number(urvrvResult?.freeSpaces);
        let occ = Number(urvrvResult?.occupiedSpaces);

        if (!Number.isFinite(total) || total <= 0) {
            return res.status(400).json({ message: "totalSpots must be > 0" });
        }
        if (!Number.isFinite(free) || free < 0) free = 0;
        if (!Number.isFinite(occ) || occ < 0) occ = 0;

        // clamp
        free = Math.min(free, total);
        occ = Math.min(occ, total);

        // make consistent
        if (free + occ !== total) {
            // Prioritet: free je istina, occ = total - free
            occ = total - free;
            if (occ < 0) {
                occ = 0;
                free = total;
            }
        }

        req.body.urvrvResult = {
            totalSpots: total,
            freeSpaces: free,
            occupiedSpaces: occ,
            spotsCoordinates: urvrvResult?.spotsCoordinates || []
        };


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