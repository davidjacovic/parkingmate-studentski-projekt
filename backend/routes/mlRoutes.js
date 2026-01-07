/**
 * ML Analysis Routes - Task 3.3.1 + 3.3.2 + 3.3.3
 */

const express = require('express');
const router = express.Router();
const mlAnalysisController = require('../controllers/mlAnalysisController');
const multer = require('multer');
const path = require('path');

// Configure multer for image uploads
const storage = multer.diskStorage({
    destination: function (req, file, cb) {
        cb(null, 'uploads/');
    },
    filename: function (req, file, cb) {
        cb(null, Date.now() + path.extname(file.originalname));
    }
});

const upload = multer({ 
    storage: storage,
    limits: { fileSize: 10 * 1024 * 1024 }, // 10MB limit
    fileFilter: function (req, file, cb) {
        // Accept only images
        if (!file.mimetype.startsWith('image/')) {
            return cb(new Error('Only image files are allowed'), false);
        }
        cb(null, true);
    }
});

// Health check endpoint
router.get('/health', mlAnalysisController.healthCheck);

// Analyze image endpoint (with file upload)
router.post('/analyze', upload.single('image'), mlAnalysisController.analyzeImage);

// Analyze image endpoint (with image path/URL in body)
router.post('/analyze-path', mlAnalysisController.analyzeImage);

module.exports = router;

