var express = require('express');
var router = express.Router();
var paymentController = require('../controllers/paymentController.js');

/*
 * GET
 */
router.get('/', paymentController.list);

/*
 * GET
 */
router.get('/:id', paymentController.show);

/*
 * POST
 */
router.post('/', paymentController.create);

/*
 * PUT
 */
router.put('/:id', paymentController.update);

/*
 * DELETE
 */
router.delete('/:id', paymentController.remove);

module.exports = router;
