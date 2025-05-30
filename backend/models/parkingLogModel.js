const mongoose = require('mongoose');
const parkingLogSchema = new mongoose.Schema({
  parkingLocationId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'parking_location',
    required: true,
  },
  timestamp: {
    type: Date,
    default: Date.now,
  },
  available_regular_spots: Number,
  available_invalid_spots: Number,
  available_bus_spots: Number,
});

module.exports = mongoose.model('parkingLog', parkingLogSchema);