const express = require('express');
const router = express.Router();
const parkingImageController = require('../controllers/parkingImageController');

router.post('/', parkingImageController.uploadImageMiddleware, parkingImageController.create);

module.exports = router;
