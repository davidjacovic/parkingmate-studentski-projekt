var ChangelogModel = require('../models/changeLogModel.js');

/**
 * changeLogController.js
 *
 * @description :: Server-side logic for managing changeLogs.
 */
module.exports = {

    /**
     * changeLogController.list()
     */
    list: function (req, res) {
        ChangelogModel.find(function (err, changeLogs) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when getting changeLog.',
                    error: err
                });
            }

            return res.json(changeLogs);
        });
    },

    /**
     * changeLogController.show()
     */
    show: function (req, res) {
        var id = req.params.id;

        ChangelogModel.findOne({_id: id}, function (err, changeLog) {
            if (err) {
                return res.status(500).json({
                    message: 'Error while getting changeLog.',
                    error: err
                });
            }

            if (!changeLog) {
                return res.status(404).json({
                    message: 'No such changeLog'
                });
            }

            return res.json(changeLog);
        });
    },

    /**
     * changeLogController.create()
     */
    create: function (req, res) {
        var changeLog = new ChangelogModel({

        });

        changeLog.save(function (err, changeLog) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when creating changeLog',
                    error: err
                });
            }

            return res.status(201).json(changeLog);
        });
    },

    /**
     * changeLogController.update()
     */
    update: function (req, res) {
        var id = req.params.id;

        ChangelogModel.findOne({_id: id}, function (err, changeLog) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when getting changeLog',
                    error: err
                });
            }

            if (!changeLog) {
                return res.status(404).json({
                    message: 'No such changeLog'
                });
            }

            
            changeLog.save(function (err, changeLog) {
                if (err) {
                    return res.status(500).json({
                        message: 'Error when updating changeLog.',
                        error: err
                    });
                }

                return res.json(changeLog);
            });
        });
    },

    /**
     * changeLogController.remove()
     */
    remove: function (req, res) {
        var id = req.params.id;

        ChangelogModel.findByIdAndRemove(id, function (err, changeLog) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when deleting the changeLog.',
                    error: err
                });
            }

            return res.status(204).json();
        });
    }
};
