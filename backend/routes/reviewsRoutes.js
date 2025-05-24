var express = require('express');
var router = express.Router();
var reviewsController = require('../controllers/reviewsController.js');

/*
 * GET
 */
router.get('/', reviewsController.list);

/*
 * GET
 */
router.get('/:id', reviewsController.show);

/*
 * POST
 */
router.post('/', reviewsController.create);

/*
 * PUT
 */
router.put('/:id', reviewsController.update);

/*
 * DELETE
 */
router.delete('/:id', reviewsController.remove);

module.exports = router;
