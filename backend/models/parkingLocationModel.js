var mongoose = require('mongoose');
var Schema   = mongoose.Schema;

/*
* izbrisano: total_electric_spots: Number, available_electric_spots: Number, working_hours: String, 
* zamenjeno: latitude i longitude sa location
*
*/

var parking_locationSchema = new Schema({
    name: String,
    address: String,
    location: {
        type: {
            type: String,
            enum: ['Point'],
            required: true,
            default: 'Point'
        },
        coordinates: {
            type: [Number], // [longitude, latitude]
            required: true
        }
    },
    total_regular_spots: Number,
    total_invalid_spots: Number,
    total_bus_spots: Number,
    available_regular_spots: Number,
    available_invalid_spots: Number,
    available_bus_spots: Number,
    created: { type: Date, default: Date.now },
    modified: { type: Date, default: Date.now },
    description: String,
    hidden: Boolean,
    subscriber: { type: Schema.Types.ObjectId, ref: 'Subscriber' }
});

// 👇 Dodaj indeks za geospatial upite
parking_locationSchema.index({ location: '2dsphere' });

module.exports = mongoose.model('parking_location', parking_locationSchema);