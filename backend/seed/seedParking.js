/* ----------------UBACIVANJE TESTNIH PRIMERA----------------

const mongoose = require('mongoose');
require('dotenv').config();
const ParkingLocation = require('../models/parkingLocationModel');

// Konektovanje na Atlas
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
  {
    name: "Parkirišče Center Maribor",
    address: "Slomškova ulica 5, Maribor",
    latitude: "46.5532",
    longitude: "15.6471",
    total_regular_spots: 60,
    available_regular_spots: 50,
    total_invalid_spots: 3,
    available_invalid_spots: 3,
    total_bus_spots: 2,
    available_bus_spots: 2,
    created: new Date("2024-05-04T08:00:00Z"),
    modified: new Date("2024-05-04T08:00:00Z"),
    description: "Centralno parkiralište u srcu Maribora, blizu glavnih atrakcija.",
    hidden: false
  },
  {
    name: "Parkirišče Lent Maribor",
    address: "Lent, Maribor",
    latitude: "46.5580",
    longitude: "15.6485",
    total_regular_spots: 45,
    available_regular_spots: 35,
    total_invalid_spots: 2,
    available_invalid_spots: 2,
    total_bus_spots: 1,
    available_bus_spots: 1,
    created: new Date("2024-05-05T09:00:00Z"),
    modified: new Date("2024-05-05T09:00:00Z"),
    description: "Parkiralište u istorijskoj oblasti Lenta.",
    hidden: false
  },
  {
    name: "Parkirišče Tezno Maribor",
    address: "Tezno, Maribor",
    latitude: "46.5605",
    longitude: "15.6423",
    total_regular_spots: 70,
    available_regular_spots: 60,
    total_invalid_spots: 5,
    available_invalid_spots: 5,
    total_bus_spots: 2,
    available_bus_spots: 2,
    created: new Date("2024-05-06T10:00:00Z"),
    modified: new Date("2024-05-06T10:00:00Z"),
    description: "Prostrano parkiralište u blizini Teznove reke.",
    hidden: false
  },
  {
    name: "Parkirišče Pobrežje Maribor",
    address: "Pobrežje, Maribor",
    latitude: "46.5500",
    longitude: "15.6470",
    total_regular_spots: 30,
    available_regular_spots: 25,
    total_invalid_spots: 2,
    available_invalid_spots: 2,
    total_bus_spots: 1,
    available_bus_spots: 1,
    created: new Date("2024-05-07T08:30:00Z"),
    modified: new Date("2024-05-07T08:30:00Z"),
    description: "Manje parkiralište sa solidnim kapacitetom.",
    hidden: false
  },
  {
    name: "Parkirišče Pohorje Maribor",
    address: "Pohorje, Maribor",
    latitude: "46.5670",
    longitude: "15.6330",
    total_regular_spots: 55,
    available_regular_spots: 45,
    total_invalid_spots: 3,
    available_invalid_spots: 3,
    total_bus_spots: 2,
    available_bus_spots: 2,
    created: new Date("2024-05-08T07:45:00Z"),
    modified: new Date("2024-05-08T07:45:00Z"),
    description: "Parkiralište blizu raskršća na putu ka Pohorju.",
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
*/
