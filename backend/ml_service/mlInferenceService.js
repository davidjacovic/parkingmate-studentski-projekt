/**
 * ML Inference Service - Task 3.3.1 + 3.3.2 + 3.3.3 + 3.3.4
 * Wrapper service for calling Python YOLO inference
 */

const { exec } = require('child_process');
const path = require('path');
const fsSync = require('fs');
const fs = require('fs').promises;

class MLInferenceService {
    constructor() {
        // Path to the Python inference script
        this.pythonScriptPath = path.join(__dirname, 'inference.py');
        // Default model path - can be overridden via environment variable
        this.modelPath = process.env.ML_MODEL_PATH || path.join(__dirname, '..', 'models', 'best.pt');
        // Python executable - default to 'python' on Windows, 'python3' on Unix
        this.pythonExecutable = process.env.PYTHON_EXECUTABLE || (process.platform === 'win32' ? 'python' : 'python3');
        // Default thresholds
        this.confidenceThreshold = parseFloat(process.env.ML_CONFIDENCE_THRESHOLD || '0.5');
        this.iouThreshold = parseFloat(process.env.ML_IOU_THRESHOLD || '0.3');
        // Log directory - Task 3.3.4
        this.logDirectory = path.join(__dirname, '..', 'logs');
        this.logFile = path.join(this.logDirectory, 'ml_analysis.log');
        // Ensure log directory exists (synchronous for constructor)
        this.ensureLogDirectorySync();
    }

    /**
     * Ensure log directory exists - Task 3.3.4
     * Synchronous version for constructor
     * @private
     */
    ensureLogDirectorySync() {
        try {
            if (!fsSync.existsSync(this.logDirectory)) {
                fsSync.mkdirSync(this.logDirectory, { recursive: true });
            }
        } catch (error) {
            console.error('Error creating log directory:', error);
        }
    }

    /**
     * Ensure log directory exists - Task 3.3.4
     * Async version for runtime checks
     * @private
     */
    async ensureLogDirectory() {
        try {
            if (!fsSync.existsSync(this.logDirectory)) {
                await fs.mkdir(this.logDirectory, { recursive: true });
            }
        } catch (error) {
            console.error('Error creating log directory:', error);
        }
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
            const carThreshold = options.carThreshold || null;
            const parkingThreshold = options.parkingThreshold || null;

            // Build command - use absolute paths to avoid path issues
            const absImagePath = path.isAbsolute(imagePath) ? imagePath : path.resolve(imagePath);
            const absModelPath = path.isAbsolute(this.modelPath) ? this.modelPath : path.resolve(this.modelPath);
            const absScriptPath = path.isAbsolute(this.pythonScriptPath) ? this.pythonScriptPath : path.resolve(this.pythonScriptPath);
            
            // Build command with optional class-specific thresholds
            let command = `${this.pythonExecutable} "${absScriptPath}" "${absImagePath}" "${absModelPath}" ${confidenceThreshold} ${iouThreshold}`;
            if (carThreshold !== null) {
                command += ` ${carThreshold}`;
                if (parkingThreshold !== null) {
                    command += ` ${parkingThreshold}`;
                }
            } else if (parkingThreshold !== null) {
                command += ` null ${parkingThreshold}`;
            }

            // Execute Python script
            const result = await this.executeCommand(command);

            // Debug: log raw output if needed (uncomment for debugging)
            // console.log('Raw Python output length:', result.length);
            // console.log('Raw Python output (first 1000):', result.substring(0, 1000));

            // Parse JSON result - extract JSON from output (YOLO may print warnings to stdout)
            // YOLO might print progress messages before JSON, so we need to extract the JSON part
            let jsonString = result.trim();
            
            // Try to parse directly first (in case output is clean JSON)
            let analysisResult;
            try {
                analysisResult = JSON.parse(jsonString);
                // If parsing succeeds and we have success field, we're done
                if (analysisResult.hasOwnProperty('success')) {
                    // Success! Use this result
                } else {
                    // Parsed but missing success field - try to extract JSON object
                    throw new Error('Parsed but missing success field');
                }
            } catch (directParseError) {
                // Direct parse failed or missing success - try to extract JSON object
                // Find the last occurrence of '{' which should be the start of our JSON
                const lastBraceIndex = jsonString.lastIndexOf('{');
                if (lastBraceIndex === -1) {
                    throw new Error(`No JSON object found in Python output. Output: ${result.substring(0, 500)}`);
                }
                
                // Extract from the last '{' to the end
                jsonString = jsonString.substring(lastBraceIndex);
                
                // Find the matching closing brace to get complete JSON
                let braceCount = 0;
                let jsonEndIndex = -1;
                for (let i = 0; i < jsonString.length; i++) {
                    if (jsonString[i] === '{') braceCount++;
                    if (jsonString[i] === '}') {
                        braceCount--;
                        if (braceCount === 0) {
                            jsonEndIndex = i + 1;
                            break;
                        }
                    }
                }
                
                if (jsonEndIndex === -1) {
                    throw new Error(`Incomplete JSON in Python output. Output: ${result.substring(0, 500)}`);
                }
                
                // Extract complete JSON
                jsonString = jsonString.substring(0, jsonEndIndex);
                
                // Parse the extracted JSON
                try {
                    analysisResult = JSON.parse(jsonString);
                } catch (parseError) {
                    // Debug: show what we're trying to parse
                    console.error('Failed to parse extracted JSON. Attempted to parse:', jsonString.substring(0, 500));
                    throw new Error(`Failed to parse JSON from Python output. Parse error: ${parseError.message}. JSON string (first 500 chars): ${jsonString.substring(0, 500)}`);
                }
            }
            
            // Debug: log parsed result if success is false
            if (!analysisResult.success) {
                console.error('Python returned success=false. Full result:', JSON.stringify(analysisResult, null, 2));
                const errorMsg = analysisResult.error || 'ML inference failed';
                const errorType = analysisResult.error_type || 'Unknown';
                throw new Error(`ML inference failed: ${errorMsg} (Type: ${errorType})`);
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

                // YOLO may print warnings to stderr, but that's OK
                // We only care about stdout which should contain JSON
                if (!stdout || stdout.trim().length === 0) {
                    reject(new Error(`Python script produced no output. Stderr: ${stderr}`));
                    return;
                }

                resolve(stdout);
            });
        });
    }

    /**
     * Format analysis result for API response - Task 3.3.2 + 3.3.3
     * Returns number of free and occupied spaces, and coordinates of parking spots
     * @param {Object} analysisResult - Raw analysis result from Python script
     * @returns {Object} Formatted result with freeSpaces, occupiedSpaces, and spotsCoordinates
     */
    formatResult(analysisResult) {
        // Task 3.3.3: Extract coordinates for all parking spots
        // Format: [[x_center, y_center, width, height], ...]
        const spotsCoordinates = [];
        
        if (analysisResult.spots_with_coordinates) {
            analysisResult.spots_with_coordinates.forEach(spot => {
                // Coordinates are already in format [x_center, y_center, width, height]
                spotsCoordinates.push(spot.coordinates);
            });
        } else if (analysisResult.parking_spots) {
            // Fallback: extract from parking_spots if spots_with_coordinates not available
            analysisResult.parking_spots.forEach(spot => {
                spotsCoordinates.push(spot.bbox);
            });
        }
        
        return {
            totalSpots: analysisResult.total_spots || 0,
            freeSpaces: analysisResult.free_spaces || 0,
            occupiedSpaces: analysisResult.occupied_spaces || 0,
            totalCars: analysisResult.total_cars || 0,
            spotsCoordinates: spotsCoordinates,  // Task 3.3.3: Coordinates of parking spots
            allDetections: analysisResult.all_detections || [],
            metadata: analysisResult.analysis_metadata || {}
        };
    }

    /**
     * Log analysis results - Task 3.3.4
     * Logs to console and optionally to file
     * @param {string} imagePath - Path to analyzed image
     * @param {Object} result - Analysis result (formatted)
     * @param {Object} rawResult - Raw analysis result from Python (optional)
     */
    async logAnalysis(imagePath, result, rawResult = null) {
        const logEntry = {
            timestamp: new Date().toISOString(),
            imagePath: imagePath,
            result: {
                totalSpots: result.totalSpots,
                freeSpaces: result.freeSpaces,
                occupiedSpaces: result.occupiedSpaces,
                totalCars: result.totalCars,
                spotsCoordinatesCount: result.spotsCoordinates ? result.spotsCoordinates.length : 0
            },
            metadata: result.metadata || {}
        };

        // Console logging
        console.log('=== ML Analysis Log ===');
        console.log(JSON.stringify(logEntry, null, 2));
        console.log('======================');

        // File logging - Task 3.3.4
        try {
            await this.ensureLogDirectory();
            const logLine = JSON.stringify(logEntry) + '\n';
            await fs.appendFile(this.logFile, logLine);
        } catch (error) {
            console.error('Error writing to log file:', error);
            // Don't throw - logging failure shouldn't break the service
        }
    }
}

module.exports = new MLInferenceService();
