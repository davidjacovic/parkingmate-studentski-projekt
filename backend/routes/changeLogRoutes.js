var express = require('express');
var router = express.Router();
var changeLogController = require('../controllers/changeLogController.js');

/*
 * GET
 */
router.get('/', changeLogController.list);

/*
 * GET
 */
router.get('/:id', changeLogController.show);

/*
 * POST
 */
router.post('/', changeLogController.create);

/*
 * PUT
 */
router.put('/:id', changeLogController.update);

/*
 * DELETE
 */
router.delete('/:id', changeLogController.remove);

module.exports = router;
