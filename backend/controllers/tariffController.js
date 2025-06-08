const mongoose = require('mongoose');
var TariffModel = require('../models/tariffModel.js');
const UserModel = require('../models/userModel');

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
    console.log('Pristigao zahtev za kreiranje tarife:', req.body);

    let parsedPrice;
    try {
        parsedPrice = mongoose.Types.Decimal128.fromString(req.body.price.toString());
    } catch (e) {
        console.error('Neispravna vrednost za cenu:', req.body.price);
        return res.status(400).json({ message: 'Neispravna vrednost za cenu (price)', error: e });
    }

    var tariff = new TariffModel({
        tariff_type: req.body.tariff_type,
        duration: req.body.duration,
        vehicle_type: req.body.vehicle_type,
        price: parsedPrice, 
        price_unit: req.body.price_unit,
        hidden: false,
        created: new Date(),
        modified: new Date(),
        parking_location: req.body.location_id
    });

    console.log('Objekat koji će biti sačuvan:', tariff);

    tariff.save(function (err, savedTariff) {
        if (err) {
            console.error('Greška prilikom čuvanja tarife u bazi:', err);
            return res.status(500).json({
                message: 'Greška pri kreiranju tarife',
                error: err
            });
        }

        console.log('Uspešno sačuvana tarifa:', savedTariff);
        return res.status(201).json(savedTariff);
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

            // Ažuriranje polja sa podacima iz zahteva (ako nisu undefined)
            tariff.tariff_type = req.body.tariff_type !== undefined ? req.body.tariff_type : tariff.tariff_type;
            tariff.duration = req.body.duration !== undefined ? req.body.duration : tariff.duration;
            tariff.vehicle_type = req.body.vehicle_type !== undefined ? req.body.vehicle_type : tariff.vehicle_type;
            tariff.price = req.body.price !== undefined ? req.body.price : tariff.price;
            tariff.price_unit = req.body.price_unit !== undefined ? req.body.price_unit : tariff.price_unit;
            tariff.hidden = req.body.hidden !== undefined ? req.body.hidden : tariff.hidden;
            tariff.modified = new Date();

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


    remove: async function (req, res) {
        const id = req.params.id;

        if (!req.user) {
            return res.status(401).json({ message: 'Niste autentifikovani' });
        }

        try {
            const user = await UserModel.findById(req.user.userId);
            if (!user || user.user_type !== 'admin') {
                return res.status(403).json({ message: 'Samo admin može da briše tarife.' });
            }

            const tariff = await TariffModel.findById(id);
            if (!tariff) {
                return res.status(404).json({ message: 'Tarifa nije pronađena.' });
            }

            await TariffModel.findByIdAndDelete(id);

            return res.status(204).json(); // uspešno obrisano
        } catch (err) {
            return res.status(500).json({
                message: 'Greška pri brisanju tarife.',
                error: err
            });
        }
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
