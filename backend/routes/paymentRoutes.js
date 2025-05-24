var express = require('express');
var router = express.Router();
var paymentController = require('../controllers/paymentController.js');

router.get('/addresses/list', paymentController.addresses);
router.get('/addresses/search', paymentController.searchAddresses);
router.get('/user-info', paymentController.getUserPaymentInfo);
router.get('/user/credit-card', paymentController.getUserCreditCard);
router.post('/calculate-and-pay', paymentController.createAndCalculatePayment);
router.get('/', paymentController.list);
router.get('/:id', paymentController.show);
router.post('/', paymentController.create);
router.put('/:id', paymentController.update);
router.delete('/:id', paymentController.remove);

module.exports = router;
