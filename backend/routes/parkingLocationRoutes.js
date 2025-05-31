var express = require('express');
var router = express.Router();
var parkingLocationController = require('../controllers/parkingLocationController.js');

/* GET all parking locations */
router.get('/', parkingLocationController.list);

router.get('/parking-filter', parkingLocationController.filteredParkingLocations);


/* GET nearby parking locations */
router.get('/nearby/search', parkingLocationController.nearby);

/* GET occupancy status */
router.get('/occupancy/status', parkingLocationController.getOccupancyStatus);

/* GET a single parking location by ID */
router.get('/:id', parkingLocationController.show);

// Route to get parking logs for that location
router.get('/:id/logs', parkingLocationController.getParkingLogsByLocation);

/* POST create new parking location */
router.post('/', parkingLocationController.create);

/* PUT update parking location by ID */
router.put('/:id', parkingLocationController.update);

/* DELETE remove parking location by ID */
router.delete('/:id', parkingLocationController.remove);

module.exports = router;
