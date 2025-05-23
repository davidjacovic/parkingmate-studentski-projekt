const parkingLocationSchema = new Schema({
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
  created_at: Date,
  updated_at: Date,
  description: String,
  working_hours: String,
  hidden: Boolean,
  tariff: { type: Schema.Types.ObjectId, ref: 'Tariff' },
  subscriber: { type: Schema.Types.ObjectId, ref: 'Subscriber' }
});

module.exports = mongoose.model('ParkingLocation', parkingLocationSchema);
