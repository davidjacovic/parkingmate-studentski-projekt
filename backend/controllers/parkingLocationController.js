var ParkinglocationModel = require('../models/parkingLocationModel.js');

/**
 * parkingLocationController.js
 *
 * @description :: Server-side logic for managing parkingLocations.
 */
module.exports = {

    /**
     * parkingLocationController.list()
     */
    list: function (req, res) {
        ParkinglocationModel.find(function (err, parkingLocations) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when getting parkingLocation.',
                    error: err
                });
            }

            return res.json(parkingLocations);
        });
    },

    /**
     * parkingLocationController.show()
     */
    show: function (req, res) {
        var id = req.params.id;

        ParkinglocationModel.findOne({_id: id}, function (err, parkingLocation) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when getting parkingLocation.',
                    error: err
                });
            }

            if (!parkingLocation) {
                return res.status(404).json({
                    message: 'No such parkingLocation'
                });
            }

            return res.json(parkingLocation);
        });
    },

    /**
     * parkingLocationController.create()
     */
    create: function (req, res) {
        var parkingLocation = new ParkinglocationModel({

        });

        parkingLocation.save(function (err, parkingLocation) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when creating parkingLocation',
                    error: err
                });
            }

            return res.status(201).json(parkingLocation);
        });
    },

    /**
     * parkingLocationController.update()
     */
    update: function (req, res) {
        var id = req.params.id;

        ParkinglocationModel.findOne({_id: id}, function (err, parkingLocation) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when getting parkingLocation',
                    error: err
                });
            }

            if (!parkingLocation) {
                return res.status(404).json({
                    message: 'No such parkingLocation'
                });
            }

            
            parkingLocation.save(function (err, parkingLocation) {
                if (err) {
                    return res.status(500).json({
                        message: 'Error when updating parkingLocation.',
                        error: err
                    });
                }

                return res.json(parkingLocation);
            });
        });
    },

    /**
     * parkingLocationController.remove()
     */
    remove: function (req, res) {
        var id = req.params.id;

        ParkinglocationModel.findByIdAndRemove(id, function (err, parkingLocation) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when deleting the parkingLocation.',
                    error: err
                });
            }

            return res.status(204).json();
        });
    }
};
