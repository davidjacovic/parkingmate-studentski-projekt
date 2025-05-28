var express = require('express');
var router = express.Router();
var parkingLocationController = require('../controllers/parkingLocationController.js');

/* GET all parking locations */
router.get('/', parkingLocationController.list);

/* GET nearby parking locations */
router.get('/nearby/search', parkingLocationController.nearby);

/* GET a single parking location by ID */
router.get('/:id', parkingLocationController.show);

/* POST create new parking location */
router.post('/', parkingLocationController.create);

/* PUT update parking location by ID */
router.put('/:id', parkingLocationController.update);

/* DELETE remove parking location by ID */
router.delete('/:id', parkingLocationController.remove);

module.exports = router;
