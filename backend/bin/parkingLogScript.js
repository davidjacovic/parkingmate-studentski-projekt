const mongoose = require('mongoose');
const ParkingLocation = require('../models/parkingLocationModel');
const ParkingLog = require('../models/parkingLogModel');

require('dotenv').config();

mongoose.connect(process.env.MONGO_URL, {
  useNewUrlParser: true,
  useUnifiedTopology: true
});

async function logParkingAvailability() {
  try {
    const locations = await ParkingLocation.find();

    for (const location of locations) {
      const logEntry = new ParkingLog({
        parkingLocationId: location._id,
        available_regular_spots: location.available_regular_spots,
        available_invalid_spots: location.available_invalid_spots,
        available_bus_spots: location.available_bus_spots,
      });

      await logEntry.save();
    }

    console.log('Logged parking availability.');
  } catch (err) {
    console.error('Error logging parking availability:', err);
  } finally {
    mongoose.connection.close();
  }
}

logParkingAvailability();
