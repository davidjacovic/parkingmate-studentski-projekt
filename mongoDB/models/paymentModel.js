const paymentSchema = new Schema({
  date: Date,
  amount: mongoose.Types.Decimal128,
  method: String,
  payment_status: { type: String, enum: ['pending', 'completed', 'failed'] },
  hidden: Boolean,
  user: { type: Schema.Types.ObjectId, ref: 'User' },
  parking_location: { type: Schema.Types.ObjectId, ref: 'ParkingLocation' }
});

module.exports = mongoose.model('Payment', paymentSchema);
