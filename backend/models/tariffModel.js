var mongoose = require('mongoose');
var Schema   = mongoose.Schema;
var tariffSchema = new Schema({
    tariff_type: String,
    duration: String,
    vehicle_type: String,
    price: mongoose.Types.Decimal128,
    price_unit: String,
    hidden: Boolean,
    created: Date,
    modified: Date,
    parking_location: { type: Schema.Types.ObjectId, ref: 'ParkingLocation' }
});

module.exports = mongoose.model('tariff', tariffSchema);