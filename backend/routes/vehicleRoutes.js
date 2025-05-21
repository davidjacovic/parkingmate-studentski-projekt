var express = require('express');
var router = express.Router();
var vehicleController = require('../controllers/vehicleController.js');

router.get('/', vehicleController.list);
router.get('/:id', vehicleController.show);
router.post('/', vehicleController.create);
router.put('/:id', vehicleController.update);
router.delete('/:id', vehicleController.remove);

module.exports = router;
