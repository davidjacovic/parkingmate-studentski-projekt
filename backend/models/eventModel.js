const mongoose = require('mongoose');

const eventSchema = new mongoose.Schema({
    topic: {
        type: String,
        required: true,
        trim: true
    },
    message: {
        type: String,
        required: true,
        trim: true
    },
    timestamp: {
        type: Date,
        required: true,
        default: Date.now
    },
    location: {
        type: String,
        required: true,
        trim: true
    },
    eventType: {
        type: String,
        required: true,
        enum: ['PARKING_FULL', 'PARKING_AVAILABLE', 'LOW_AVAILABILITY'],
        trim: true
    },
    status: {
        type: String,
        enum: ['PENDING', 'PROCESSED', 'BLOCKCHAIN_RECORDED'],
        default: 'PENDING'
    },
    blockchainHash: {
        type: String,
        default: null
    },
    blockchainTimestamp: {
        type: Date,
        default: null
    }
}, {
    timestamps: true // Automatski dodaje createdAt i updatedAt
});

// Indeksi za brže pretraživanje
eventSchema.index({ timestamp: -1 });
eventSchema.index({ eventType: 1 });
eventSchema.index({ status: 1 });
eventSchema.index({ createdAt: -1 });

const Event = mongoose.model('Event', eventSchema);

module.exports = Event;

