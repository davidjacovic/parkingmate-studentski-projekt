var express = require('express');
var router = express.Router();
var vehicleController = require('../controllers/vehicleController.js');

/*
 * GET
 */
router.get('/', vehicleController.list);

/*
 * GET
 */
router.get('/:id', vehicleController.show);

/*
 * POST
 */
router.post('/', vehicleController.create);

/*
 * PUT
 */
router.put('/:id', vehicleController.update);

/*
 * DELETE
 */
router.delete('/:id', vehicleController.remove);

module.exports = router;
