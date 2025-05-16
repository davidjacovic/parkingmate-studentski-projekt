var mongoose = require('mongoose');
var Schema   = mongoose.Schema;

var change_logSchema = new Schema({
});

module.exports = mongoose.model('change_log', change_logSchema);
