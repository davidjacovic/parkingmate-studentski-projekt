const express = require('express');
const router = express.Router();
const parkingImageController = require('../controllers/parkingImageController');

router.post('/', parkingImageController.uploadImageMiddleware, parkingImageController.create);
router.post('/simulated', async (req, res) => {
    try {
        const newImage = await parkingImageController.createSimulated(req.body)
        res.status(201).json({ message: 'Simulated image saved', data: newImage })
    } catch (err) {
        res.status(500).json({ message: 'Server error', error: err.message })
    }
});

module.exports = router;
