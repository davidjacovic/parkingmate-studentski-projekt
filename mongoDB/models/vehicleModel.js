const vehicleSchema = new Schema({
  model: String,
  registration_number: String,
  vehicle_type: String,
  user: { type: Schema.Types.ObjectId, ref: 'User' }
});

module.exports = mongoose.model('Vehicle', vehicleSchema);
