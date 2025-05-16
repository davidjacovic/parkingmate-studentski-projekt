var express = require('express');
var router = express.Router();
var parking_locationController = require('../controllers/parking_locationController.js');

/*
 * GET
 */
router.get('/', parking_locationController.list);

/*
 * GET
 */
router.get('/:id', parking_locationController.show);

/*
 * POST
 */
router.post('/', parking_locationController.create);

/*
 * PUT
 */
router.put('/:id', parking_locationController.update);

/*
 * DELETE
 */
router.delete('/:id', parking_locationController.remove);

module.exports = router;
