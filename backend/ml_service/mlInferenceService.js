/**
 * ML Inference Service - Task 3.3.1 + 3.3.2
 * Wrapper service for calling Python YOLO inference
 */

const { exec } = require('child_process');
const path = require('path');
const fsSync = require('fs');

class MLInferenceService {
    constructor() {
        // Path to the Python inference script
        this.pythonScriptPath = path.join(__dirname, 'inference.py');
        // Default model path - can be overridden via environment variable
        this.modelPath = process.env.ML_MODEL_PATH || path.join(__dirname, '..', 'models', 'best.pt');
        // Python executable - try python3 first, then python
        this.pythonExecutable = process.env.PYTHON_EXECUTABLE || 'python3';
        // Default thresholds
        this.confidenceThreshold = parseFloat(process.env.ML_CONFIDENCE_THRESHOLD || '0.5');
        this.iouThreshold = parseFloat(process.env.ML_IOU_THRESHOLD || '0.3');
    }

    /**
     * Check if Python and required packages are available
     */
    async checkDependencies() {
        return new Promise((resolve, reject) => {
            exec(`${this.pythonExecutable} -c "import ultralytics; import numpy"`, (error) => {
                if (error) {
                    reject(new Error('Python dependencies not installed. Run: pip install -r ml_service/requirements.txt'));
                } else {
                    resolve(true);
                }
            });
        });
    }

    /**
     * Analyze parking image using ML model
     * @param {string} imagePath - Path to the image file
     * @param {Object} options - Optional parameters
     * @returns {Promise<Object>} Analysis results
     */
    async analyzeImage(imagePath, options = {}) {
        try {
            // Validate image path
            if (!fsSync.existsSync(imagePath)) {
                throw new Error(`Image file not found: ${imagePath}`);
            }

            // Validate model path
            if (!fsSync.existsSync(this.modelPath)) {
                throw new Error(`Model file not found: ${this.modelPath}. Please set ML_MODEL_PATH environment variable.`);
            }

            // Use provided options or defaults
            const confidenceThreshold = options.confidenceThreshold || this.confidenceThreshold;
            const iouThreshold = options.iouThreshold || this.iouThreshold;

            // Build command
            const command = `${this.pythonExecutable} "${this.pythonScriptPath}" "${imagePath}" "${this.modelPath}" ${confidenceThreshold} ${iouThreshold}`;

            // Execute Python script
            const result = await this.executeCommand(command);

            // Parse JSON result
            const analysisResult = JSON.parse(result);

            if (!analysisResult.success) {
                throw new Error(analysisResult.error || 'ML inference failed');
            }

            return analysisResult;

        } catch (error) {
            console.error('ML Inference Error:', error);
            throw error;
        }
    }

    /**
     * Execute shell command and return output
     * @private
     */
    executeCommand(command) {
        return new Promise((resolve, reject) => {
            exec(command, { maxBuffer: 10 * 1024 * 1024 }, (error, stdout, stderr) => {
                if (error) {
                    console.error('Command execution error:', error);
                    console.error('Stderr:', stderr);
                    reject(new Error(`Command failed: ${error.message}\n${stderr}`));
                    return;
                }

                if (stderr && !stdout) {
                    reject(new Error(`Python script error: ${stderr}`));
                    return;
                }

                resolve(stdout);
            });
        });
    }

    /**
     * Format analysis result for API response - Task 3.3.2
     * Returns number of free and occupied spaces
     * @param {Object} analysisResult - Raw analysis result from Python script
     * @returns {Object} Formatted result with freeSpaces and occupiedSpaces
     */
    formatResult(analysisResult) {
        return {
            totalSpots: analysisResult.total_spots || 0,
            freeSpaces: analysisResult.free_spaces || 0,
            occupiedSpaces: analysisResult.occupied_spaces || 0,
            totalCars: analysisResult.total_cars || 0,
            allDetections: analysisResult.all_detections || [],
            metadata: analysisResult.analysis_metadata || {}
        };
    }
}

module.exports = new MLInferenceService();
