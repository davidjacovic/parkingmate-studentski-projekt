var SubscribersModel = require('../models/subscribersModel.js');

/**
 * subscribersController.js
 *
 * @description :: Server-side logic for managing subscriberss.
 */
module.exports = {

    /**
     * subscribersController.list()
     */
    list: function (req, res) {
        SubscribersModel.find(function (err, subscriberss) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when getting subscribers.',
                    error: err
                });
            }

            return res.json(subscriberss);
        });
    },

    /**
     * subscribersController.show()
     */
    show: function (req, res) {
        var id = req.params.id;

        SubscribersModel.findOne({_id: id}, function (err, subscribers) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when getting subscribers.',
                    error: err
                });
            }

            if (!subscribers) {
                return res.status(404).json({
                    message: 'No such subscribers'
                });
            }

            return res.json(subscribers);
        });
    },

    /**
     * subscribersController.create()
     */
    create: function (req, res) {
        var subscribers = new SubscribersModel({

        });

        subscribers.save(function (err, subscribers) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when creating subscribers',
                    error: err
                });
            }

            return res.status(201).json(subscribers);
        });
    },

    /**
     * subscribersController.update()
     */
    update: function (req, res) {
        var id = req.params.id;

        SubscribersModel.findOne({_id: id}, function (err, subscribers) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when getting subscribers',
                    error: err
                });
            }

            if (!subscribers) {
                return res.status(404).json({
                    message: 'No such subscribers'
                });
            }

            
            subscribers.save(function (err, subscribers) {
                if (err) {
                    return res.status(500).json({
                        message: 'Error when updating subscribers.',
                        error: err
                    });
                }

                return res.json(subscribers);
            });
        });
    },

    /**
     * subscribersController.remove()
     */
    remove: function (req, res) {
        var id = req.params.id;

        SubscribersModel.findByIdAndRemove(id, function (err, subscribers) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when deleting the subscribers.',
                    error: err
                });
            }

            return res.status(204).json();
        });
    }
};
