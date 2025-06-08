var express = require('express');
var router = express.Router();
var paymentController = require('../controllers/paymentController.js');
const Payment = require('../models/paymentModel.js');

const { authenticateToken } = require('../middlewares/auth');

// NOVO: Ruta za aktivna placanja prijavljenog korisnika
router.get('/active-for-user', authenticateToken, paymentController.getActivePaymentsForUser);
router.get('/addresses/list', paymentController.addresses);
router.get('/addresses/search', paymentController.searchAddresses);
router.get('/history', authenticateToken, paymentController.getUserPaymentHistory);
router.get('/payments/user-payments', authenticateToken, paymentController.listUserPayments);
router.get('/user-info', paymentController.getUserPaymentInfo);
router.get('/user/credit-card', paymentController.getUserCreditCard);
router.post('/calculate-and-pay', paymentController.createAndCalculatePayment);
router.get('/', paymentController.list);
router.get('/:id', paymentController.show);
router.post('/', paymentController.create);
router.put('/:id', paymentController.update);
router.delete('/:id', paymentController.remove);
// routes/payments.js




module.exports = router;
