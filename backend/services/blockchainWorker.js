/**
 * EPIK 3.2: Implementacija zapisa dogodkov v blockchain
 * 
 * Background worker koji procesira PENDING evente i dodaje ih v blockchain.
 * 
 * @module blockchainWorker
 */

const Event = require('../models/eventModel');
const blockchainService = require('./blockchainService');

// Konfiguracija worker-a
const WORKER_CONFIG = {
    enabled: process.env.BLOCKCHAIN_WORKER_ENABLED !== 'false', // Default: enabled
    intervalMs: parseInt(process.env.BLOCKCHAIN_WORKER_INTERVAL_MS || '30000'), // Default: 30 sekundi
    batchSize: parseInt(process.env.BLOCKCHAIN_WORKER_BATCH_SIZE || '1'), // Default: 1 event (blockchain ne dozvoljava paralelno mining)
    maxRetries: parseInt(process.env.BLOCKCHAIN_WORKER_MAX_RETRIES || '5'), // Default: 5 pokušaja
    retryDelayMs: parseInt(process.env.BLOCKCHAIN_WORKER_RETRY_DELAY_MS || '5000') // Default: 5 sekundi (mining može trajati duže)
};

let workerInterval = null;
let isProcessing = false;

/**
 * Procesira jedan Event - zabeleži ga v blockchain.
 * 
 * @param {Object} event - Event dokument iz MongoDB
 * @returns {Promise<Object>} { success: boolean, event: Object, errors: string[] }
 */
async function processEvent(event) {
    try {
        // Provera: Ako event već ima blockchainHash, preskoči ga (izbegavamo duplikate)
        if (event.blockchainHash) {
            console.log(`[BlockchainWorker] ⚠ Event ${event._id} already has blockchainHash: ${event.blockchainHash.substring(0, 16)}...`);
            console.log(`[BlockchainWorker]   Updating status to BLOCKCHAIN_RECORDED...`);
            event.status = 'BLOCKCHAIN_RECORDED';
            await event.save();
            return {
                success: true,
                event: event,
                errors: []
            };
        }

        console.log(`[BlockchainWorker] Processing event ${event._id}...`);

        // Pokušaj da zabeležiš event v blockchain
        const result = await blockchainService.recordEventInBlockchain(event);

        if (result.success) {
            // Ažuriraj event sa blockchain podacima
            event.status = 'BLOCKCHAIN_RECORDED';
            event.blockchainHash = result.blockchainHash;
            event.blockchainTimestamp = result.blockchainTimestamp;

            const updatedEvent = await event.save();

            console.log(`[BlockchainWorker] ✓ Event ${event._id} recorded in blockchain at block ${result.blockIndex}`);
            console.log(`[BlockchainWorker]   Block hash: ${result.blockchainHash.substring(0, 16)}...`);

            return {
                success: true,
                event: updatedEvent,
                errors: []
            };
        } else {
            // Greška pri zapisovanju
            console.error(`[BlockchainWorker] ✗ Failed to record event ${event._id} in blockchain`);
            console.error(`[BlockchainWorker]   Errors:`, result.errors);

            return {
                success: false,
                event: event,
                errors: result.errors
            };
        }
    } catch (error) {
        console.error(`[BlockchainWorker] ✗ Error processing event ${event._id}:`, error);
        return {
            success: false,
            event: event,
            errors: [error.message]
        };
    }
}

/**
 * Procesira batch PENDING eventov.
 * 
 * @param {number} batchSize - Broj eventov za procesiranje
 * @returns {Promise<Object>} { processed: number, successful: number, failed: number }
 */
async function processPendingEvents(batchSize = WORKER_CONFIG.batchSize) {
    if (isProcessing) {
        console.log('[BlockchainWorker] Already processing, skipping this cycle...');
        return { processed: 0, successful: 0, failed: 0 };
    }

    isProcessing = true;

    try {
        // Proveri da li je blockchain servis dostupan
        const healthCheck = await blockchainService.checkBlockchainServiceHealth();
        if (!healthCheck.available) {
            console.warn(`[BlockchainWorker] Blockchain service unavailable: ${healthCheck.message}`);
            return { processed: 0, successful: 0, failed: 0 };
        }

        // Pronađi sve PENDING evente koji još nemaju blockchainHash
        // (izbegavamo duplikate - event koji već ima blockchainHash ne treba ponovo obrađivati)
        const pendingEvents = await Event.find({ 
            status: 'PENDING',
            blockchainHash: null  // Samo eventi koji još nisu u blockchain-u
        })
            .sort({ timestamp: 1 }) // Najstariji prvi (FIFO)
            .limit(batchSize);

        if (pendingEvents.length === 0) {
            console.log('[BlockchainWorker] No pending events to process');
            return { processed: 0, successful: 0, failed: 0 };
        }

        console.log(`[BlockchainWorker] Found ${pendingEvents.length} pending event(s) to process`);

        let successful = 0;
        let failed = 0;

        // Procesiraj svaki event SERIJSKI (jedan po jedan)
        // Blockchain servis ne dozvoljava paralelno mining (409 Conflict)
        for (const event of pendingEvents) {
            let result = await processEvent(event);
            
            // Retry logika za 409 Conflict (mining već u toku)
            let retryCount = 0;
            const maxRetries = WORKER_CONFIG.maxRetries;
            const retryDelay = WORKER_CONFIG.retryDelayMs;
            
            while (!result.success && retryCount < maxRetries && 
                   result.errors.some(err => err.includes('409 Conflict'))) {
                retryCount++;
                console.log(`[BlockchainWorker] Mining in progress for event ${event._id}, retry ${retryCount}/${maxRetries} after ${retryDelay}ms...`);
                await new Promise(resolve => setTimeout(resolve, retryDelay));
                result = await processEvent(event);
            }
            
            if (!result.success && result.errors.some(err => err.includes('409 Conflict'))) {
                console.log(`[BlockchainWorker] Event ${event._id} failed after ${maxRetries} retries - mining still in progress. Will retry in next cycle.`);
            }
            
            if (result.success) {
                successful++;
                // Pauza između uspešnih eventova da se osigura da je mining potpuno završen
                await new Promise(resolve => setTimeout(resolve, 1000));
            } else {
                failed++;
                // Ako nije 409 Conflict, ne čekaj previše
                if (!result.errors.some(err => err.includes('409 Conflict'))) {
                    await new Promise(resolve => setTimeout(resolve, 500));
                }
            }
        }

        console.log(`[BlockchainWorker] Batch processing complete: ${successful} successful, ${failed} failed`);

        return {
            processed: pendingEvents.length,
            successful: successful,
            failed: failed
        };
    } catch (error) {
        console.error('[BlockchainWorker] Error in processPendingEvents:', error);
        return { processed: 0, successful: 0, failed: 0 };
    } finally {
        isProcessing = false;
    }
}

/**
 * Pokreće background worker (periodično procesiranje).
 */
function startWorker() {
    if (!WORKER_CONFIG.enabled) {
        console.log('[BlockchainWorker] Worker is disabled (set BLOCKCHAIN_WORKER_ENABLED=true to enable)');
        return;
    }

    if (workerInterval !== null) {
        console.log('[BlockchainWorker] Worker is already running');
        return;
    }

    console.log(`[BlockchainWorker] Starting background worker...`);
    console.log(`[BlockchainWorker] Configuration:`, {
        intervalMs: WORKER_CONFIG.intervalMs,
        batchSize: WORKER_CONFIG.batchSize,
        blockchainServiceUrl: blockchainService.BLOCKCHAIN_SERVICE_URL
    });

    // Prvo pokreni jednom odmah
    processPendingEvents().catch(err => {
        console.error('[BlockchainWorker] Error in initial processing:', err);
    });

    // Zatim pokreni na intervalu
    workerInterval = setInterval(() => {
        processPendingEvents().catch(err => {
            console.error('[BlockchainWorker] Error in periodic processing:', err);
        });
    }, WORKER_CONFIG.intervalMs);

    console.log(`[BlockchainWorker] ✓ Worker started (checking every ${WORKER_CONFIG.intervalMs}ms)`);
}

/**
 * Zaustavlja background worker.
 */
function stopWorker() {
    if (workerInterval === null) {
        console.log('[BlockchainWorker] Worker is not running');
        return;
    }

    clearInterval(workerInterval);
    workerInterval = null;
    console.log('[BlockchainWorker] Worker stopped');
}

/**
 * Procesira sve PENDING evente (ručno pokretanje).
 * 
 * @returns {Promise<Object>} { processed: number, successful: number, failed: number }
 */
async function processAllPendingEvents() {
    console.log('[BlockchainWorker] Manual processing of all pending events...');
    
    let totalProcessed = 0;
    let totalSuccessful = 0;
    let totalFailed = 0;

    while (true) {
        const result = await processPendingEvents(WORKER_CONFIG.batchSize);
        
        totalProcessed += result.processed;
        totalSuccessful += result.successful;
        totalFailed += result.failed;

        // Ako nema više eventov za procesiranje, završi
        if (result.processed === 0) {
            break;
        }

        // Kratka pauza između batch-eva
        await new Promise(resolve => setTimeout(resolve, 1000));
    }

    console.log(`[BlockchainWorker] Manual processing complete: ${totalSuccessful} successful, ${totalFailed} failed out of ${totalProcessed} total`);

    return {
        processed: totalProcessed,
        successful: totalSuccessful,
        failed: totalFailed
    };
}

module.exports = {
    startWorker,
    stopWorker,
    processPendingEvents,
    processAllPendingEvents,
    processEvent,
    WORKER_CONFIG
};

