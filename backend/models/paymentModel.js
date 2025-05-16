var mongoose = require('mongoose');
var Schema   = mongoose.Schema;

var paymentSchema = new Schema({
});

module.exports = mongoose.model('payment', paymentSchema);
