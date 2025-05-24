var express = require('express');
var router = express.Router();
var subscribersController = require('../controllers/subscribersController.js');

/*
 * GET
 */
router.get('/', subscribersController.list);

/*
 * GET
 */
router.get('/:id', subscribersController.show);

/*
 * POST
 */
router.post('/', subscribersController.create);

/*
 * PUT
 */
router.put('/:id', subscribersController.update);

/*
 * DELETE
 */
router.delete('/:id', subscribersController.remove);

module.exports = router;
