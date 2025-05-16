var mongoose = require('mongoose');
var Schema   = mongoose.Schema;

var reviewsSchema = new Schema({
});

module.exports = mongoose.model('reviews', reviewsSchema);
