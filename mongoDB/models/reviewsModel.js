const reviewSchema = new Schema({
  rating: Number,
  review_text: String,
  review_date: Date,
  hidden: Boolean,
  user: { type: Schema.Types.ObjectId, ref: 'User' },
  parking_location: { type: Schema.Types.ObjectId, ref: 'ParkingLocation' }
});

module.exports = mongoose.model('Review', reviewSchema);
