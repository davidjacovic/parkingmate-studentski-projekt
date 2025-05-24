var mongoose = require('mongoose');
var Schema   = mongoose.Schema;

var subscribersSchema = new Schema({
    available_spots: Number,
    total_spots: Number,
    reserved_spots: Number,
    waiting_line: Number,
    created: Date,
    modified: Date,
    hidden: Boolean
});

module.exports = mongoose.model('subscribers', subscribersSchema);
