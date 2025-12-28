const ParkingImage = require('../models/parkingImageModel');
const multer = require('multer');
const path = require('path');
const fs = require('fs');

const storage = multer.diskStorage({
  destination: function (req, file, cb) {
    const uploadPath = path.join(__dirname, '../uploads');
    if (!fs.existsSync(uploadPath)) fs.mkdirSync(uploadPath);
    cb(null, uploadPath);
  },
  filename: function (req, file, cb) {
    cb(null, `${Date.now()}_${file.originalname}`);
  }
});

const upload = multer({ storage });

exports.uploadImageMiddleware = upload.single('file');

exports.create = async (req, res) => {
  try {
    const { parkingLocationId } = req.body;
    let coordinates = req.body['coordinates[]'];
    const file = req.file;

    if (!parkingLocationId || !file) return res.status(400).json({ message: 'Missing required fields.' });

    let coordsArray = [0, 0];
    if (coordinates) {
      if (Array.isArray(coordinates)) coordsArray = coordinates.map(Number);
      else coordsArray = [Number(coordinates), 0];
    }

    const newImage = new ParkingImage({
      parkingLocationId,
      location: { type: 'Point', coordinates: coordsArray },
      imageUrl: file.filename
    });

    await newImage.save();
    res.status(201).json({ message: 'Image saved successfully', data: newImage });
  } catch (err) {
    console.error('Error saving parking image:', err);
    res.status(500).json({ message: 'Server error', error: err.message });
  }
};
