var express = require('express');
var router = express.Router();
var tariffController = require('../controllers/tariffController.js');

/*
 * GET
 */
router.get('/', tariffController.list);

/*
 * GET
 */
router.get('/:id', tariffController.show);

/*
 * POST
 */
router.post('/', tariffController.create);

/*
 * PUT
 */
router.put('/:id', tariffController.update);

/*
 * DELETE
 */
router.delete('/:id', tariffController.remove);

module.exports = router;
