var mongoose = require('mongoose');
var Schema   = mongoose.Schema;

var reviewsSchema = new Schema({
    rating: Number,
    review_text: String,
    review_date: Date,
    hidden: Boolean,
    created: Date,
    modified: Date,
    user: { type: Schema.Types.ObjectId, ref: 'user' },
    parking_location: { type: Schema.Types.ObjectId, ref: 'ParkingLocation' }
});

module.exports = mongoose.model('reviews', reviewsSchema);
