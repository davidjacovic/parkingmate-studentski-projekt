var mongoose = require('mongoose');
var Schema   = mongoose.Schema;

var paymentSchema = new Schema({
     date: Date,
    amount: mongoose.Types.Decimal128,
    method: String,
    payment_status: { type: String, enum: ['pending', 'completed', 'failed'] },
    hidden: Boolean,
    created: Date,
    modified: Date,
    user: { type: Schema.Types.ObjectId, ref: 'User' },
    parking_location: { type: Schema.Types.ObjectId, ref: 'ParkingLocation' }
});

module.exports = mongoose.model('payment', paymentSchema);
