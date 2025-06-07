var express = require('express');
var router = express.Router();
var tariffController = require('../controllers/tariffController.js');

router.get('/by-location/:id', tariffController.byLocation); // tarife po parking lokaciji
router.get('/', tariffController.list);
router.get('/:id', tariffController.show);
router.post('/', tariffController.create);
router.put('/:id', tariffController.update);
router.delete('/:id', tariffController.remove);

module.exports = router;
