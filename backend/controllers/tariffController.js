var TariffModel = require('../models/tariffModel.js');

module.exports = {

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

    show: function (req, res) {
        var id = req.params.id;

        TariffModel.findOne({ _id: id }, function (err, tariff) {
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

    create: function (req, res) {
        var tariff = new TariffModel({
            // ovde dodaj polja ako želiš da kreiraš odmah
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

    update: function (req, res) {
        var id = req.params.id;

        TariffModel.findOne({ _id: id }, function (err, tariff) {
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

            // update polja, na primer:
            // tariff.duration = req.body.duration || tariff.duration;
            // tariff.price = req.body.price || tariff.price;
            // ...

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
    },

    byLocation: async (req, res) => {
        try {
            const locationId = req.params.id;
            const tariffs = await TariffModel.find({ parking_location: locationId }).sort({ created: -1 });
            res.json(tariffs);
        } catch (err) {
            res.status(500).json({ message: 'Failed to fetch tariffs' });
        }
    }

};
