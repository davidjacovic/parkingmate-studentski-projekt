// seedParking.js
const mongoose = require('mongoose');
require('dotenv').config();
const ParkingLocation = require('../models/parkingLocationModel');

mongoose.connect(process.env.MONGO_URL, {
  useNewUrlParser: true,
  useUnifiedTopology: true
}).then(() => {
  console.log("✅ Povezano sa bazom");
  ubaciPodatke();
}).catch(err => {
  console.error("❌ Greška prilikom konekcije:", err);
});

const sampleLocations = [
  // MARIBOR
  {
    name: "Parkirišče Center Maribor",
    address: "Slomškova ulica 5, Maribor",
    location: { type: "Point", coordinates: [15.6471, 46.5532] },
    total_regular_spots: 60,
    available_regular_spots: 50,
    total_invalid_spots: 3,
    available_invalid_spots: 3,
    total_bus_spots: 2,
    available_bus_spots: 2,
    description: "Centralno parkiralište u srcu Maribora, blizu glavnih atrakcija.",
    hidden: false
  },
  {
    name: "Parkirišče Lent Maribor",
    address: "Lent, Maribor",
    location: { type: "Point", coordinates: [15.6485, 46.5580] },
    total_regular_spots: 45,
    available_regular_spots: 35,
    total_invalid_spots: 2,
    available_invalid_spots: 2,
    total_bus_spots: 1,
    available_bus_spots: 1,
    description: "Parkiralište u istorijskoj oblasti Lenta.",
    hidden: false
  },
  {
    name: "Parkirišče Tezno Maribor",
    address: "Tezno, Maribor",
    location: { type: "Point", coordinates: [15.6423, 46.5605] },
    total_regular_spots: 70,
    available_regular_spots: 60,
    total_invalid_spots: 5,
    available_invalid_spots: 5,
    total_bus_spots: 2,
    available_bus_spots: 2,
    description: "Prostrano parkiralište u blizini Teznove reke.",
    hidden: false
  },
  {
    name: "Parkirišče Pobrežje Maribor",
    address: "Pobrežje, Maribor",
    location: { type: "Point", coordinates: [15.6470, 46.5500] },
    total_regular_spots: 30,
    available_regular_spots: 25,
    total_invalid_spots: 2,
    available_invalid_spots: 2,
    total_bus_spots: 1,
    available_bus_spots: 1,
    description: "Manje parkiralište sa solidnim kapacitetom.",
    hidden: false
  },
  {
    name: "Parkirišče Pohorje Maribor",
    address: "Pohorje, Maribor",
    location: { type: "Point", coordinates: [15.6330, 46.5670] },
    total_regular_spots: 55,
    available_regular_spots: 45,
    total_invalid_spots: 3,
    available_invalid_spots: 3,
    total_bus_spots: 2,
    available_bus_spots: 2,
    description: "Parkiralište blizu raskršća na putu ka Pohorju.",
    hidden: false
  },

  // LJUBLJANA
  {
    name: "Parkirišče Tivoli",
    address: "Tivolska cesta, Ljubljana",
    location: { type: "Point", coordinates: [14.4978, 46.0576] },
    total_regular_spots: 100,
    available_regular_spots: 85,
    total_invalid_spots: 5,
    available_invalid_spots: 4,
    total_bus_spots: 3,
    available_bus_spots: 3,
    description: "Veliko parkiralište pored parka Tivoli.",
    hidden: false
  },
  {
    name: "Parkirišče Metelkova",
    address: "Metelkova ulica 10, Ljubljana",
    location: { type: "Point", coordinates: [14.5150, 46.0562] },
    total_regular_spots: 30,
    available_regular_spots: 20,
    total_invalid_spots: 2,
    available_invalid_spots: 1,
    total_bus_spots: 1,
    available_bus_spots: 1,
    description: "Parking kod Metelkove sa mestima za autobuse.",
    hidden: false
  },
  {
    name: "Parkirišče Vič",
    address: "Vič, Ljubljana",
    location: { type: "Point", coordinates: [14.4755, 46.0428] },
    total_regular_spots: 40,
    available_regular_spots: 30,
    total_invalid_spots: 2,
    available_invalid_spots: 2,
    total_bus_spots: 1,
    available_bus_spots: 1,
    description: "Parkiralište u naselju Vič.",
    hidden: false
  },
  {
    name: "Parkirišče BTC City",
    address: "Šmartinska cesta, Ljubljana",
    location: { type: "Point", coordinates: [14.5538, 46.0675] },
    total_regular_spots: 120,
    available_regular_spots: 100,
    total_invalid_spots: 6,
    available_invalid_spots: 6,
    total_bus_spots: 4,
    available_bus_spots: 4,
    description: "Veliki parking u okviru tržnog centra BTC.",
    hidden: false
  },
  {
    name: "Parkirišče Bežigrad",
    address: "Bežigrad, Ljubljana",
    location: { type: "Point", coordinates: [14.5193, 46.0712] },
    total_regular_spots: 50,
    available_regular_spots: 40,
    total_invalid_spots: 3,
    available_invalid_spots: 3,
    total_bus_spots: 2,
    available_bus_spots: 2,
    description: "Parkiralište u naselju Bežigrad.",
    hidden: false
  }
];

async function ubaciPodatke() {
  try {
    await ParkingLocation.insertMany(sampleLocations);
    console.log("✅ Testni parking podaci ubačeni.");
  } catch (err) {
    console.error("❌ Greška prilikom unosa:", err);
  } finally {
    mongoose.disconnect();
  }
}
