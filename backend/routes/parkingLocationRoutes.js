var express = require('express');
var router = express.Router();
var parkingLocationController = require('../controllers/parkingLocationController.js');
const { authenticateToken } = require('../middlewares/auth');

router.get('/', parkingLocationController.list);
router.get('/parking-filter', parkingLocationController.filteredParkingLocations);
router.get('/:id', parkingLocationController.show);
router.get('/nearby/search', parkingLocationController.nearby);
router.get('/occupancy/status', parkingLocationController.getOccupancyStatus);
router.get('/:id/logs', parkingLocationController.getParkingLogsByLocation);
router.post('/', authenticateToken, parkingLocationController.create);
router.put('/:id', parkingLocationController.update);
router.delete('/:id', authenticateToken,parkingLocationController.remove);

module.exports = router;
