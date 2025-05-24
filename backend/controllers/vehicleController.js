var VehicleModel = require('../models/vehicleModel.js');

/**
 * vehicleController.js
 *
 * @description :: Server-side logic for managing vehicles.
 */
module.exports = {

    /**
     * vehicleController.list()
     */
    list: function (req, res) {
        VehicleModel.find(function (err, vehicles) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when getting vehicle.',
                    error: err
                });
            }

            return res.json(vehicles);
        });
    },

    /**
     * vehicleController.show()
     */
    show: function (req, res) {
        var id = req.params.id;

        VehicleModel.findOne({_id: id}, function (err, vehicle) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when getting vehicle.',
                    error: err
                });
            }

            if (!vehicle) {
                return res.status(404).json({
                    message: 'No such vehicle'
                });
            }

            return res.json(vehicle);
        });
    },

    /**
     * vehicleController.create()
     */
    create: function (req, res) {
        var vehicle = new VehicleModel({

        });

        vehicle.save(function (err, vehicle) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when creating vehicle',
                    error: err
                });
            }

            return res.status(201).json(vehicle);
        });
    },

    /**
     * vehicleController.update()
     */
    update: function (req, res) {
        var id = req.params.id;

        VehicleModel.findOne({_id: id}, function (err, vehicle) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when getting vehicle',
                    error: err
                });
            }

            if (!vehicle) {
                return res.status(404).json({
                    message: 'No such vehicle'
                });
            }

            
            vehicle.save(function (err, vehicle) {
                if (err) {
                    return res.status(500).json({
                        message: 'Error when updating vehicle.',
                        error: err
                    });
                }

                return res.json(vehicle);
            });
        });
    },

    /**
     * vehicleController.remove()
     */
    remove: function (req, res) {
        var id = req.params.id;

        VehicleModel.findByIdAndRemove(id, function (err, vehicle) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when deleting the vehicle.',
                    error: err
                });
            }

            return res.status(204).json();
        });
    }
};
