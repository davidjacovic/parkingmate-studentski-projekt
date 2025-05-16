var ReviewsModel = require('../models/reviewsModel.js');

/**
 * reviewsController.js
 *
 * @description :: Server-side logic for managing reviewss.
 */
module.exports = {

    /**
     * reviewsController.list()
     */
    list: function (req, res) {
        ReviewsModel.find(function (err, reviewss) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when getting reviews.',
                    error: err
                });
            }

            return res.json(reviewss);
        });
    },

    /**
     * reviewsController.show()
     */
    show: function (req, res) {
        var id = req.params.id;

        ReviewsModel.findOne({_id: id}, function (err, reviews) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when getting reviews.',
                    error: err
                });
            }

            if (!reviews) {
                return res.status(404).json({
                    message: 'No such reviews'
                });
            }

            return res.json(reviews);
        });
    },

    /**
     * reviewsController.create()
     */
    create: function (req, res) {
        var reviews = new ReviewsModel({

        });

        reviews.save(function (err, reviews) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when creating reviews',
                    error: err
                });
            }

            return res.status(201).json(reviews);
        });
    },

    /**
     * reviewsController.update()
     */
    update: function (req, res) {
        var id = req.params.id;

        ReviewsModel.findOne({_id: id}, function (err, reviews) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when getting reviews',
                    error: err
                });
            }

            if (!reviews) {
                return res.status(404).json({
                    message: 'No such reviews'
                });
            }

            
            reviews.save(function (err, reviews) {
                if (err) {
                    return res.status(500).json({
                        message: 'Error when updating reviews.',
                        error: err
                    });
                }

                return res.json(reviews);
            });
        });
    },

    /**
     * reviewsController.remove()
     */
    remove: function (req, res) {
        var id = req.params.id;

        ReviewsModel.findByIdAndRemove(id, function (err, reviews) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when deleting the reviews.',
                    error: err
                });
            }

            return res.status(204).json();
        });
    }
};
