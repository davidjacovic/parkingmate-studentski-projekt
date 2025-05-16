var PaymentModel = require('../models/paymentModel.js');

/**
 * paymentController.js
 *
 * @description :: Server-side logic for managing payments.
 */
module.exports = {

    /**
     * paymentController.list()
     */
    list: function (req, res) {
        PaymentModel.find(function (err, payments) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when getting payment.',
                    error: err
                });
            }

            return res.json(payments);
        });
    },

    /**
     * paymentController.show()
     */
    show: function (req, res) {
        var id = req.params.id;

        PaymentModel.findOne({_id: id}, function (err, payment) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when getting payment.',
                    error: err
                });
            }

            if (!payment) {
                return res.status(404).json({
                    message: 'No such payment'
                });
            }

            return res.json(payment);
        });
    },

    /**
     * paymentController.create()
     */
    create: function (req, res) {
        var payment = new PaymentModel({

        });

        payment.save(function (err, payment) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when creating payment',
                    error: err
                });
            }

            return res.status(201).json(payment);
        });
    },

    /**
     * paymentController.update()
     */
    update: function (req, res) {
        var id = req.params.id;

        PaymentModel.findOne({_id: id}, function (err, payment) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when getting payment',
                    error: err
                });
            }

            if (!payment) {
                return res.status(404).json({
                    message: 'No such payment'
                });
            }

            
            payment.save(function (err, payment) {
                if (err) {
                    return res.status(500).json({
                        message: 'Error when updating payment.',
                        error: err
                    });
                }

                return res.json(payment);
            });
        });
    },

    /**
     * paymentController.remove()
     */
    remove: function (req, res) {
        var id = req.params.id;

        PaymentModel.findByIdAndRemove(id, function (err, payment) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when deleting the payment.',
                    error: err
                });
            }

            return res.status(204).json();
        });
    }
};
