const subscriberSchema = new Schema({
  available_spots: Number,
  total_spots: Number,
  reserved_spots: Number,
  waiting_line: Number,
  hidden: Boolean
});

module.exports = mongoose.model('Subscriber', subscriberSchema);
