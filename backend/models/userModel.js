var mongoose = require('mongoose');
var bcrypt = require('bcrypt');
var Schema   = mongoose.Schema;

var userSchema = new Schema({
    username: String,
    atribut: String,
    email: String,
    password_hash: String,
    phone_number: String,
     credit_card_number: String, 
    created_at: Date,
    updated_at: Date,
    user_type: { type: String, enum: ['admin', 'user'] },
    hidden: Boolean
});

userSchema.pre('save', async function(next) {
  if (this.isModified('password_hash')) {
    try {
      const hash = await bcrypt.hash(this.password_hash, 10);
      this.password_hash = hash;
      next();
    } catch (err) {
      next(err);
    }
  } else {
    next();
  }
});
userSchema.statics.authenticate = function(username, password, callback) {
  this.findOne({ username: username })
    .exec(function(err, user) {
      if (err) return callback(err);
      if (!user) {
        const err = new Error('User not found.');
        err.status = 401;
        return callback(err);
      }
      bcrypt.compare(password, user.password_hash, function(err, result) {
        if (err) return callback(err);
        if (result === true) {
          return callback(null, user);
        } else {
          const err = new Error('Wrong password');
          err.status = 401;
          return callback(err);
        }
      });
    });
};



module.exports = mongoose.model('user', userSchema);
