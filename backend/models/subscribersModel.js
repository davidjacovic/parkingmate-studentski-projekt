var mongoose = require('mongoose');
var Schema   = mongoose.Schema;

var subscribersSchema = new Schema({
});

module.exports = mongoose.model('subscribers', subscribersSchema);
