/**
 * ML Inference Service - Task 3.3.1
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
        // Default confidence threshold
        this.confidenceThreshold = parseFloat(process.env.ML_CONFIDENCE_THRESHOLD || '0.5');
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

            // Build command
            const command = `${this.pythonExecutable} "${this.pythonScriptPath}" "${imagePath}" "${this.modelPath}" ${confidenceThreshold}`;

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
}

module.exports = new MLInferenceService();
