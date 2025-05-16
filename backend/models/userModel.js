var mongoose = require('mongoose');
var Schema   = mongoose.Schema;

var userSchema = new Schema({
    first_name: String,
    atribut: String,
    email: String,
    password_hash: String,
    phone_number: String,
    created_at: Date,
    updated_at: Date,
    user_type: { type: String, enum: ['admin', 'user'] },
    hidden: Boolean
});

module.exports = mongoose.model('user', userSchema);
