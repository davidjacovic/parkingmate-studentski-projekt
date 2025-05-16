var mongoose = require('mongoose');
var Schema   = mongoose.Schema;

var parking_locationSchema = new Schema({
    name: String,
    address: String,
    latitude: String,
    longitude: String,
    total_regular_spots: Number,
    total_invalid_spots: Number,
    total_electric_spots: Number,
    total_bus_spots: Number,
    available_regular_spots: Number,
    available_invalid_spots: Number,
    available_electric_spots: Number,
    available_bus_spots: Number,
    created: Date,
    modified: Date,
    description: String,
    working_hours: String,
    hidden: Boolean,
    subscriber: { type: Schema.Types.ObjectId, ref: 'Subscriber' }
});

module.exports = mongoose.model('parking_location', parking_locationSchema);
