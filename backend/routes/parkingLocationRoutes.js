var express = require('express');
var router = express.Router();
var parkingLocationController = require('../controllers/parkingLocationController.js');

/*
 * GET
 */
router.get('/', parkingLocationController.list);

/*
 * GET
 */
router.get('/:id', parkingLocationController.show);

/*
 * POST
 */
router.post('/', parkingLocationController.create);

/*
 * PUT
 */
router.put('/:id', parkingLocationController.update);

/*
 * DELETE
 */
router.delete('/:id', parkingLocationController.remove);

module.exports = router;
