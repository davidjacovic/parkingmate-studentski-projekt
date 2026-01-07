/**
 * ML Analysis Controller - Task 3.3.1 + 3.3.2 + 3.3.3
 * Handles ML inference requests for parking image analysis
 */

const mlInferenceService = require('../ml_service/mlInferenceService');
const path = require('path');
const fs = require('fs');

/**
 * Analyze parking image using ML model
 * POST /api/ml/analyze
 */
exports.analyzeImage = async (req, res) => {
    try {
        // Get image path from request
        let imagePath;
        
        // Check if image file is uploaded
        if (req.file) {
            imagePath = req.file.path;
        } else if (req.body.imagePath) {
            // Or use provided image path
            imagePath = path.join(__dirname, '..', 'uploads', req.body.imagePath);
        } else if (req.body.imageUrl) {
            // Or use imageUrl from database
            imagePath = path.join(__dirname, '..', 'uploads', req.body.imageUrl);
        } else {
            return res.status(400).json({
                success: false,
                message: 'No image provided. Send image file, imagePath, or imageUrl'
            });
        }

        // Validate image exists
        if (!fs.existsSync(imagePath)) {
            return res.status(404).json({
                success: false,
                message: `Image file not found: ${imagePath}`
            });
        }

        // Optional parameters
        const options = {
            confidenceThreshold: req.body.confidenceThreshold ? parseFloat(req.body.confidenceThreshold) : undefined,
            iouThreshold: req.body.iouThreshold ? parseFloat(req.body.iouThreshold) : undefined
        };

        // Run ML analysis
        const analysisResult = await mlInferenceService.analyzeImage(imagePath, options);

        // Task 3.3.2: Format result to return free and occupied spaces
        const formattedResult = mlInferenceService.formatResult(analysisResult);

        // Return result
        res.json({
            success: true,
            data: formattedResult
        });

    } catch (error) {
        console.error('ML Analysis Controller Error:', error);
        res.status(500).json({
            success: false,
            message: 'ML analysis failed',
            error: error.message
        });
    }
};

/**
 * Health check endpoint for ML service
 * GET /api/ml/health
 */
exports.healthCheck = async (req, res) => {
    try {
        await mlInferenceService.checkDependencies();
        res.json({
            success: true,
            message: 'ML service is ready',
            modelPath: mlInferenceService.modelPath,
            pythonExecutable: mlInferenceService.pythonExecutable
        });
    } catch (error) {
        res.status(503).json({
            success: false,
            message: 'ML service is not ready',
            error: error.message
        });
    }
};
