var mongoose = require('mongoose');
var Schema   = mongoose.Schema;

var parking_locationSchema = new Schema({
});

module.exports = mongoose.model('parking_location', parking_locationSchema);
