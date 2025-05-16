var mongoose = require('mongoose');
var Schema   = mongoose.Schema;

var tariffSchema = new Schema({
});

module.exports = mongoose.model('tariff', tariffSchema);
