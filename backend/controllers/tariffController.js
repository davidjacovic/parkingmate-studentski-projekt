var TariffModel = require('../models/tariffModel.js');

/**
 * tariffController.js
 *
 * @description :: Server-side logic for managing tariffs.
 */
module.exports = {

    /**
     * tariffController.list()
     */
    list: function (req, res) {
        TariffModel.find(function (err, tariffs) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when getting tariff.',
                    error: err
                });
            }

            return res.json(tariffs);
        });
    },

    /**
     * tariffController.show()
     */
    show: function (req, res) {
        var id = req.params.id;

        TariffModel.findOne({_id: id}, function (err, tariff) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when getting tariff.',
                    error: err
                });
            }

            if (!tariff) {
                return res.status(404).json({
                    message: 'No such tariff'
                });
            }

            return res.json(tariff);
        });
    },

    /**
     * tariffController.create()
     */
    create: function (req, res) {
        var tariff = new TariffModel({

        });

        tariff.save(function (err, tariff) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when creating tariff',
                    error: err
                });
            }

            return res.status(201).json(tariff);
        });
    },

    /**
     * tariffController.update()
     */
    update: function (req, res) {
        var id = req.params.id;

        TariffModel.findOne({_id: id}, function (err, tariff) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when getting tariff',
                    error: err
                });
            }

            if (!tariff) {
                return res.status(404).json({
                    message: 'No such tariff'
                });
            }

            
            tariff.save(function (err, tariff) {
                if (err) {
                    return res.status(500).json({
                        message: 'Error when updating tariff.',
                        error: err
                    });
                }

                return res.json(tariff);
            });
        });
    },

    /**
     * tariffController.remove()
     */
    remove: function (req, res) {
        var id = req.params.id;

        TariffModel.findByIdAndRemove(id, function (err, tariff) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when deleting the tariff.',
                    error: err
                });
            }

            return res.status(204).json();
        });
    }
};
