var Change_logModel = require('../models/change_logModel.js');

/**
 * change_logController.js
 *
 * @description :: Server-side logic for managing change_logs.
 */
module.exports = {

    /**
     * change_logController.list()
     */
    list: function (req, res) {
        Change_logModel.find(function (err, change_logs) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when getting change_log.',
                    error: err
                });
            }

            return res.json(change_logs);
        });
    },

    /**
     * change_logController.show()
     */
    show: function (req, res) {
        var id = req.params.id;

        Change_logModel.findOne({_id: id}, function (err, change_log) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when getting change_log.',
                    error: err
                });
            }

            if (!change_log) {
                return res.status(404).json({
                    message: 'No such change_log'
                });
            }

            return res.json(change_log);
        });
    },

    /**
     * change_logController.create()
     */
    create: function (req, res) {
        var change_log = new Change_logModel({

        });

        change_log.save(function (err, change_log) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when creating change_log',
                    error: err
                });
            }

            return res.status(201).json(change_log);
        });
    },

    /**
     * change_logController.update()
     */
    update: function (req, res) {
        var id = req.params.id;

        Change_logModel.findOne({_id: id}, function (err, change_log) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when getting change_log',
                    error: err
                });
            }

            if (!change_log) {
                return res.status(404).json({
                    message: 'No such change_log'
                });
            }

            
            change_log.save(function (err, change_log) {
                if (err) {
                    return res.status(500).json({
                        message: 'Error when updating change_log.',
                        error: err
                    });
                }

                return res.json(change_log);
            });
        });
    },

    /**
     * change_logController.remove()
     */
    remove: function (req, res) {
        var id = req.params.id;

        Change_logModel.findByIdAndRemove(id, function (err, change_log) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when deleting the change_log.',
                    error: err
                });
            }

            return res.status(204).json();
        });
    }
};
