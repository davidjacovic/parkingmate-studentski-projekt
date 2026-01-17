/**
 * EPIK 3.2: Implementacija zapisa dogodkov v blockchain
 * 
 * Routes za blockchain worker operacije
 */

const express = require('express');
const router = express.Router();
const blockchainController = require('../controllers/blockchainController');

// POST /api/blockchain/process - Ručno pokreće procesiranje PENDING eventov
router.post('/process', blockchainController.processPendingEvents);

// GET /api/blockchain/worker/status - Dohvata status worker-a i blockchain servisa
router.get('/worker/status', blockchainController.getWorkerStatus);

// GET /api/blockchain/chain - Dohvata trenutno stanje blockchain lanca
router.get('/chain', blockchainController.getBlockchainChain);

// GET /api/blockchain/validate - Validira blockchain lanac
router.get('/validate', blockchainController.validateBlockchain);

// GET /api/blockchain/verify/event/:eventId - Verifikuje event u blockchain-u
router.get('/verify/event/:eventId', blockchainController.verifyEvent);

// GET /api/blockchain/search?data={data} - Pretražuje blokove po sadržaju
router.get('/search', blockchainController.searchBlocks);

module.exports = router;

