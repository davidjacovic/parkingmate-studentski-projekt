var UserModel = require('../models/userModel.js');
var VehicleModel = require('../models/vehicleModel.js');


/**
 * userController.js
 *
 * @description :: Server-side logic for managing users.
 */
module.exports = {

    create: async function (req, res) {
  try {
    console.log('Register request body:', req.body);

    // Create base user data
    const userData = {
      username: req.body.username,
      email: req.body.email,
      password_hash: req.body.password,
      created_at: new Date(),
      updated_at: new Date(),
      user_type: 'user',
      hidden: false,
    };

    // Add credit_card_number only if provided
    if (req.body.credit_card_number) {
      userData.credit_card_number = req.body.credit_card_number;
    }

    var user = new UserModel(userData);

    const savedUser = await user.save();
    console.log('User saved:', savedUser._id);

    var vehicle = new VehicleModel({
      registration_number: req.body.registration_number,
      user: savedUser._id,
      created: new Date(),
      modified: new Date(),
    });

    const savedVehicle = await vehicle.save();
    console.log('Vehicle saved:', savedVehicle._id);

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

    /**
     * userController.update()
     */
    update: function (req, res) {
        var id = req.params.id;

        UserModel.findOne({_id: id}, function (err, user) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when getting user',
                    error: err
                });
            }

            if (!user) {
                return res.status(404).json({
                    message: 'No such user'
                });
            }

            
            user.save(function (err, user) {
                if (err) {
                    return res.status(500).json({
                        message: 'Error when updating user.',
                        error: err
                    });
                }

                return res.json(user);
            });
        });
    },

    /**
     * userController.remove()
     */
    remove: function (req, res) {
        var id = req.params.id;

        UserModel.findByIdAndRemove(id, function (err, user) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when deleting the user.',
                    error: err
                });
            }

            return res.status(204).json();
        });
    },

    login: function (req, res) {
  UserModel.authenticate(req.body.username, req.body.password, function (err, user) {
    if (err || !user) {
      console.log('Login failed:', err || 'User not found');
      return res.status(401).json({ message: 'Wrong username or password' });
    }

    console.log('Login successful for user:', user.username);

    return res.json({
      user: user
    });
  });
}

};
