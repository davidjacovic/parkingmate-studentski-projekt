var mongoose = require('mongoose');
var Schema   = mongoose.Schema;

var paymentSchema = new Schema({
    date: Date,
    amount: mongoose.Types.Decimal128,
    method: String,
    payment_status: { type: String, enum: ['pending', 'completed', 'failed'] },
    duration: Number,
    hidden: Boolean,
    created: Date,
    modified: Date,
    user: { type: Schema.Types.ObjectId, ref: 'user' },
    parking_location: { type: Schema.Types.ObjectId, ref: 'ParkingLocation' },
     vehicle_plate: String
});

module.exports = mongoose.model('payment', paymentSchema);
