var mongoose = require('mongoose');
var Schema   = mongoose.Schema;

var vehicleSchema = new Schema({model: String,
    registration_number: String,
    vehicle_type: String,
    created: Date,
    modified: Date,
    user: { type: Schema.Types.ObjectId, ref: 'User' }
});

module.exports = mongoose.model('vehicle', vehicleSchema);
