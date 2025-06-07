var express = require('express');
var router = express.Router();
var reviewsController = require('../controllers/reviewsController.js');
const { authenticateToken } = require('../middlewares/auth');


router.get('/', reviewsController.list);
router.get('/parking/:parkingId', reviewsController.listByParkingLocation);
router.get('/:id', reviewsController.show);
router.get('/byId/:id', reviewsController.getById);
router.post('/', authenticateToken, reviewsController.create);
router.put('/:id', reviewsController.update);

/*
 * DELETE
 */
router.delete('/:id', reviewsController.remove);

module.exports = router;
