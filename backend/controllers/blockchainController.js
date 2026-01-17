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

/**
 * Verifikuje da li je event zapisan u blockchain-u i da li je validan.
 * GET /api/blockchain/verify/event/:eventId
 */
exports.verifyEvent = async (req, res) => {
    try {
        const { eventId } = req.params;

        if (!eventId) {
            return res.status(400).json({
                success: false,
                message: 'Event ID is required'
            });
        }

        // Pronađi event u bazi
        const event = await Event.findById(eventId);
        
        if (!event) {
            return res.status(404).json({
                success: false,
                message: `Event with ID '${eventId}' not found`
            });
        }

        // Proveri da li event ima blockchainHash
        if (!event.blockchainHash) {
            return res.json({
                success: true,
                data: {
                    eventId: event._id.toString(),
                    verified: false,
                    found: false,
                    integrityValid: false,
                    chainValid: false,
                    message: 'Event is not recorded in blockchain (no blockchainHash)'
                }
            });
        }

        // Verifikuj blok u blockchain-u
        const verification = await blockchainService.verifyBlock(event.blockchainHash);

        res.json({
            success: true,
            data: {
                eventId: event._id.toString(),
                eventStatus: event.status,
                blockchainHash: event.blockchainHash,
                verified: verification.verified,
                found: verification.found,
                integrityValid: verification.integrityValid,
                chainValid: verification.chainValid,
                message: verification.message,
                block: verification.block
            }
        });
    } catch (err) {
        console.error('Error in verifyEvent:', err);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: err.message
        });
    }
};

/**
 * Pretražuje blokove po sadržaju (event ID ili drugi podaci).
 * GET /api/blockchain/search?data={data}
 */
exports.searchBlocks = async (req, res) => {
    try {
        const { data } = req.query;

        if (!data || typeof data !== 'string' || data.trim().length === 0) {
            return res.status(400).json({
                success: false,
                message: 'Search data parameter is required'
            });
        }

        const result = await blockchainService.searchBlocks(data);

        if (result.success) {
            res.json({
                success: true,
                data: {
                    blocks: result.blocks,
                    count: result.blocks.length
                }
            });
        } else {
            res.status(500).json({
                success: false,
                message: 'Failed to search blocks',
                errors: result.errors
            });
        }
    } catch (err) {
        console.error('Error in searchBlocks:', err);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: err.message
        });
    }
};

