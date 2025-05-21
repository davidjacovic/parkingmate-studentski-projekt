var express = require('express');
var router = express.Router();
var parking_locationController = require('../controllers/parkingLocationController.js');

router.get('/', parking_locationController.list);
router.get('/:id', parking_locationController.show);
router.post('/', parking_locationController.create);
router.put('/:id', parking_locationController.update);
router.delete('/:id', parking_locationController.remove);

module.exports = router;
