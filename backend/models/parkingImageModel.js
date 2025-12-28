const mongoose = require('mongoose');

const parkingImageSchema = new mongoose.Schema({
    parkingLocationId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'parking_location',
        required: true
    },
    timestamp: {
        type: Date,
        default: Date.now
    },
    location: {
        type: {
            type: String,
            enum: ['Point'],
            default: 'Point'
        },
        coordinates: {
            type: [Number],
            required: true
        }
    },
    imageUrl: {
        type: String,
        required: true
    },
    urvrvResult: {
        totalSpots: Number,
        freeSpaces: Number,
        occupiedSpaces: Number,
        spotsCoordinates: [[Number]]
    }
});

parkingImageSchema.index({ location: '2dsphere' });

module.exports = mongoose.model('parkingImage', parkingImageSchema);
