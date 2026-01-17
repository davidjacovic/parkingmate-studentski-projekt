/**
 * EPIK 3.2: Implementacija zapisa dogodkov v blockchain
 * 
 * Kontroler za blockchain worker operacije (ručno pokretanje, status, itd.)
 */

const blockchainWorker = require('../services/blockchainWorker');
const blockchainService = require('../services/blockchainService');
const Event = require('../models/eventModel');

/**
 * Ručno pokreće procesiranje svih PENDING eventov.
 * POST /api/blockchain/process
 */
exports.processPendingEvents = async (req, res) => {
    try {
        console.log('[BlockchainController] Manual processing request received');

        const result = await blockchainWorker.processAllPendingEvents();

        res.json({
            success: true,
            message: 'Processing complete',
            data: {
                processed: result.processed,
                successful: result.successful,
                failed: result.failed
            }
        });
    } catch (err) {
        console.error('Error in processPendingEvents:', err);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: err.message
        });
    }
};

/**
 * Dohvata status blockchain worker-a.
 * GET /api/blockchain/worker/status
 */
exports.getWorkerStatus = async (req, res) => {
    try {
        const healthCheck = await blockchainService.checkBlockchainServiceHealth();
        const validation = await blockchainService.validateBlockchain();
        
        // Broj PENDING eventov
        const pendingCount = await Event.countDocuments({ status: 'PENDING' });
        
        // Broj BLOCKCHAIN_RECORDED eventov
        const recordedCount = await Event.countDocuments({ status: 'BLOCKCHAIN_RECORDED' });

        res.json({
            success: true,
            data: {
                worker: {
                    enabled: blockchainWorker.WORKER_CONFIG.enabled,
                    intervalMs: blockchainWorker.WORKER_CONFIG.intervalMs,
                    batchSize: blockchainWorker.WORKER_CONFIG.batchSize,
                    blockchainServiceUrl: blockchainService.BLOCKCHAIN_SERVICE_URL
                },
                blockchainService: {
                    available: healthCheck.available,
                    message: healthCheck.message,
                    chainValid: validation.valid,
                    validationMessage: validation.message
                },
                events: {
                    pending: pendingCount,
                    blockchainRecorded: recordedCount
                }
            }
        });
    } catch (err) {
        console.error('Error in getWorkerStatus:', err);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: err.message
        });
    }
};

/**
 * Dohvata trenutno stanje blockchain lanca.
 * GET /api/blockchain/chain
 */
exports.getBlockchainChain = async (req, res) => {
    try {
        const result = await blockchainService.getBlockchainState();

        if (result.success) {
            res.json({
                success: true,
                data: result.chain
            });
        } else {
            res.status(500).json({
                success: false,
                message: 'Failed to retrieve blockchain state',
                errors: result.errors
            });
        }
    } catch (err) {
        console.error('Error in getBlockchainChain:', err);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: err.message
        });
    }
};

/**
 * Validira blockchain lanac.
 * GET /api/blockchain/validate
 */
exports.validateBlockchain = async (req, res) => {
    try {
        const result = await blockchainService.validateBlockchain();

        res.json({
            success: true,
            data: {
                valid: result.valid,
                message: result.message
            }
        });
    } catch (err) {
        console.error('Error in validateBlockchain:', err);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: err.message
        });
    }
};

