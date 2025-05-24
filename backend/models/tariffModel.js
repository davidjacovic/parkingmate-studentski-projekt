var mongoose = require('mongoose');
var Schema   = mongoose.Schema;

var tariffSchema = new Schema({
    tariff_type: String,
    tariff_from: String,
    tariff_to: String,
    price_with_tax: mongoose.Types.Decimal128,
    price_unit: String,
    hidden: Boolean,
    created: Date,
    modified: Date,
    parking_location: { type: Schema.Types.ObjectId, ref: 'ParkingLocation' }
});

module.exports = mongoose.model('tariff', tariffSchema);
