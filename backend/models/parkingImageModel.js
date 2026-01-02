const mongoose = require('mongoose');

// Mongoose šema za ParkingImage kolekciju
const parkingImageSchema = new mongoose.Schema({
    parkingLocationId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'parking_location', // Referenca na parking_location kolekciju
        required: true
    },
    timestamp: {
        type: Date,
        default: Date.now // Podrazumevano trenutno vreme
    },
    location: {
        type: {
            type: String,
            enum: ['Point'], // Dozvoljen samo 'Point' tip
            default: 'Point'
        },
        coordinates: {
            type: [Number], // Niz [longitude, latitude]
            required: true
        }
    },
    imageUrl: {
        type: String,
        required: true // Putanja do slike
    },
    urvrvResult: {
        totalSpots: Number, // Ukupan broj parking mesta
        freeSpaces: Number, // Broj slobodnih mesta
        occupiedSpaces: Number, // Broj zauzetih mesta
        spotsCoordinates: [[Number]] // Dvodimenzionalni niz koordinata pojedinačnih mesta
    }
});

// Geo indeks za geografsko pretraživanje (2dsphere za sferne koordinate)
parkingImageSchema.index({ location: '2dsphere' });

// Export modela za korišćenje u drugim fajlovima
module.exports = mongoose.model('parkingImage', parkingImageSchema);
