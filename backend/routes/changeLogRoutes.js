var express = require('express');
var router = express.Router();
var change_logController = require('../controllers/changeLogController.js');

/*
 * GET
 */
router.get('/', change_logController.list);

/*
 * GET
 */
router.get('/:id', change_logController.show);

/*
 * POST
 */
router.post('/', change_logController.create);

/*
 * PUT
 */
router.put('/:id', change_logController.update);

/*
 * DELETE
 */
router.delete('/:id', change_logController.remove);

module.exports = router;
