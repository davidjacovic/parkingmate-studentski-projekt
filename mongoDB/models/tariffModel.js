const tariffSchema = new Schema({
  tariff_from: String,
  tariff_to: String,
  price_with_tax: mongoose.Types.Decimal128,
  price_unit: String,
  hidden: Boolean
});

module.exports = mongoose.model('Tariff', tariffSchema);
