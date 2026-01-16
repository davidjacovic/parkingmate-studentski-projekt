const express = require('express');
const router = express.Router();
const eventController = require('../controllers/eventController');

// POST /api/events - Kreira novi dogodak
router.post('/', eventController.create);

// GET /api/events - Dohvata sve dogodke (sa opcionim filtriranjem)
router.get('/', eventController.getAll);

// GET /api/events/:id - Dohvata jedan dogodak po ID-u
router.get('/:id', eventController.getById);

// PATCH /api/events/:id/status - Ažurira status dogodka
router.patch('/:id/status', eventController.updateStatus);

module.exports = router;

