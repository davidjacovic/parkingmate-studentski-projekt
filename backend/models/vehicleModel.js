var mongoose = require('mongoose');
var Schema   = mongoose.Schema;

var vehicleSchema = new Schema({
});

module.exports = mongoose.model('vehicle', vehicleSchema);
