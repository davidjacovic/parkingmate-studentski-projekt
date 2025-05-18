var mongoose = require('mongoose');
var bcrypt = require('bcrypt');
var Schema   = mongoose.Schema;
var userSchema = new Schema({
    username: {
        type: String,
        required: [true, 'Username is required'],
        unique: true,
        trim: true,
        minlength: [3, 'Username must be at least 3 characters'],
        maxlength: [30, 'Username can be max 30 characters'],
    },
    atribut: {
        type: String,
        trim: true,
        maxlength: [100, 'Atribut can be max 100 characters'],
        // optional
    },
    email: {
        type: String,
        required: [true, 'Email is required'],
        unique: true,
        trim: true,
        lowercase: true,
        match: [/\S+@\S+\.\S+/, 'Email is invalid'], //email regex validation
    },
    password_hash: {
        type: String,
        required: [true, 'Password hash is required'],
    },
    phone_number: {
        type: String,
        trim: true,
        match: [/^\+?[0-9]{7,15}$/, 'Phone number is invalid'],  //phone number regex validation
        // optional
    },
    credit_card_number: {
        type: String,
        trim: true,
        match: [/^\d{13,19}$/, 'Credit card number must be between 13 and 19 digits'],
        // optional
    },
    created_at: {
        type: Date,
        default: Date.now,
    },
    updated_at: {
        type: Date,
        default: Date.now,
    },
    user_type: {
        type: String,
        enum: ['admin', 'user'],
        default: 'user',
    },
    hidden: {
        type: Boolean,
        default: false,
    },
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
