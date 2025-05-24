var Parking_locationModel = require('../models/parkingLocationModel.js');

/**
 * parking_locationController.js
 *
 * @description :: Server-side logic for managing parking_locations.
 */
module.exports = {

    /**
     * parking_locationController.list()
     */
    list: function (req, res) {
        Parking_locationModel.find(function (err, parking_locations) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when getting parking_location.',
                    error: err
                });
            }

            return res.json(parking_locations);
        });
    },

    /**
     * parking_locationController.show()
     */
    show: function (req, res) {
        var id = req.params.id;

        Parking_locationModel.findOne({_id: id}, function (err, parking_location) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when getting parking_location.',
                    error: err
                });
            }

            if (!parking_location) {
                return res.status(404).json({
                    message: 'No such parking_location'
                });
            }

            return res.json(parking_location);
        });
    },

    /**
     * parking_locationController.create()
     */
    create: function (req, res) {
        var parking_location = new Parking_locationModel({

        });

        parking_location.save(function (err, parking_location) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when creating parking_location',
                    error: err
                });
            }

            return res.status(201).json(parking_location);
        });
    },

    /**
     * parking_locationController.update()
     */
    update: function (req, res) {
        var id = req.params.id;

        Parking_locationModel.findOne({_id: id}, function (err, parking_location) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when getting parking_location',
                    error: err
                });
            }

            if (!parking_location) {
                return res.status(404).json({
                    message: 'No such parking_location'
                });
            }

            
            parking_location.save(function (err, parking_location) {
                if (err) {
                    return res.status(500).json({
                        message: 'Error when updating parking_location.',
                        error: err
                    });
                }

                return res.json(parking_location);
            });
        });
    },

    /**
     * parking_locationController.remove()
     */
    remove: function (req, res) {
        var id = req.params.id;

        Parking_locationModel.findByIdAndRemove(id, function (err, parking_location) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when deleting the parking_location.',
                    error: err
                });
            }

            return res.status(204).json();
        });
    },


};
