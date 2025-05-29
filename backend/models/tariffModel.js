var mongoose = require('mongoose');
var Schema   = mongoose.Schema;
/* 21.5.
* preimenovano: tariff_from: String, -> duration: String,(casovno obdobje)
*               price_with_tax: mongoose.Types.Decimal128, -> price: mongoose.Types.Decimal128,
* izbaceno: tariff_to: String,
* ubaceno: vehicle_type: String,
*/
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