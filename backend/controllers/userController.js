var UserModel = require('../models/userModel.js');
const VehicleModel = require('../models/vehicleModel');
/**
 * userController.js
 *
 * @description :: Server-side logic for managing users.
 */
module.exports = {
  create: async function (req, res) {
    try {
      var user = new UserModel({
        username: req.body.username,             
        email: req.body.email,
        password_hash: req.body.password,       
        credit_card_number: req.body.credit_card_number,
        created_at: new Date(),
        updated_at: new Date(),
        user_type: 'user',
        hidden: false,
      });

      const savedUser = await user.save();

      var vehicle = new VehicleModel({
        registration_number: req.body.registration_number,
        user: savedUser._id,
        created: new Date(),
        modified: new Date(),
      });

      const savedVehicle = await vehicle.save();

      return res.status(201).json({
        user: savedUser,
        vehicle: savedVehicle,
      });
    } catch (err) {
      console.error('Error during registration:', err);
      return res.status(500).json({
        message: 'Error during registration',
        error: err.message || err,
      });
    }
  },


  showRegister: function (req, res) {
    res.render('user/register');
  },

  showLogin: function (req, res) {
    res.render('user/login');
  },

  login: function (req, res) {
  UserModel.authenticate(req.body.username, req.body.password, function (err, user) {
    if (err || !user) {
      return res.status(401).json({ message: 'Wrong username or password' });
    }
    req.session.userId = user._id;
    return res.json(user);
  });
},

};
