var UserModel = require('../models/userModel.js');
const VehicleModel = require('../models/vehicleModel');

const jwt = require('jsonwebtoken');
const SECRET = process.env.JWT_SECRET;
/*
* dodata validacija iz modela u funkciju create
*/

/**
 * userController.js
 *
 * @description :: Server-side logic for managing users.
 */
module.exports = {

    create: async function (req, res) {
        try {
            console.log('Register request body:', req.body);

            const { username, email, password, credit_card_number, phone_number } = req.body;

            // Validacije
            if (!username || username.trim().length < 3 || username.trim().length > 30) {
                return res.status(400).json({ message: 'Username must be between 3 and 30 characters.' });
            }

            if (!email || !/^\S+@\S+\.\S+$/.test(email)) {
                return res.status(400).json({ message: 'Invalid email format.' });
            }

            if (!password || password.length < 6) {
                return res.status(400).json({ message: 'Password must be at least 6 characters long.' });
            }

            if (credit_card_number && !/^\d{13,19}$/.test(credit_card_number)) {
                return res.status(400).json({ message: 'Credit card number must be 13 to 19 digits.' });
            }

            if (phone_number && !/^\+?[0-9]{7,15}$/.test(phone_number)) {
                return res.status(400).json({ message: 'Phone number is invalid.' });
            }

            const userData = {
                username: username.trim(),
                email: email.toLowerCase(),
                password_hash: password,
                created_at: new Date(),
                updated_at: new Date(),
                user_type: 'user',
                hidden: false,
            };

            if (credit_card_number) userData.credit_card_number = credit_card_number;
            if (phone_number) userData.phone_number = phone_number;

            const user = new UserModel(userData);
            const savedUser = await user.save();
            console.log('User saved:', savedUser._id);

            const vehicle = new VehicleModel({
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
            return res.status(500).json({ error: 'Registration failed.' });
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
                console.log('Login failed:', err || 'User not found');
                return res.status(401).json({ message: 'Wrong username or password' });
            }

            const token = jwt.sign(
                { userId: user._id, username: user.username, email: user.email },
                SECRET,
                { expiresIn: '1h' }
            );

            console.log('Login successful for user:', user.username);
            console.log('JWT token generated:', token);

            return res.json({
                user: user,
                token: token
            });
        });
    },

    getProfile: async function (req, res) {
        try {
            const authHeader = req.headers.authorization;
            if (!authHeader || !authHeader.startsWith('Bearer ')) {
                return res.status(401).json({ message: 'Unauthorized' });
            }

            const token = authHeader.split(' ')[1];
            const decoded = jwt.verify(token, SECRET);

            const user = await UserModel.findById(decoded.userId).lean();
            if (!user) {
                return res.status(404).json({ message: 'User not found' });
            }

            const vehicle = await VehicleModel.findOne({ user: user._id }).lean();

            return res.json({
                ...user,
                vehicle,
            });
        } catch (err) {
            console.error('Error getting profile:', err);
            return res.status(500).json({ message: 'Server error' });
        }
    },

    updateProfile: async function (req, res) {
        try {
            const token = req.headers.authorization?.split(' ')[1];
            const decoded = jwt.verify(token, process.env.JWT_SECRET);

            const allowedFields = ['email', 'phone_number', 'credit_card_number', 'username'];
            const updates = {};

            allowedFields.forEach((field) => {
                if (req.body[field]) {
                    updates[field] = req.body[field];
                }
            });

            const emailRegex = /^\S+@\S+\.\S+$/;
            const phoneRegex = /^(\+382\d{6,9}|06\d{6,8})$/;
            const cardRegex = /^[A-Za-z0-9]{16}$/;

            if (updates.email && !emailRegex.test(updates.email)) {
                return res.status(400).json({ message: 'Neispravan email format.' });
            }

            if (updates.phone_number && !phoneRegex.test(updates.phone_number)) {
                return res.status(400).json({ message: 'Neispravan broj telefona. Dozvoljeni formati: +382xxxxxxx ili 06xxxxxxx' });
            }

            if (updates.credit_card_number && !cardRegex.test(updates.credit_card_number)) {
                return res.status(400).json({ message: 'Broj kartice mora imati tačno 16 cifara.' });
            }

            updates.updated_at = new Date();

            const updatedUser = await UserModel.findByIdAndUpdate(
                decoded.userId,
                { $set: updates },
                { new: true }
            ).lean();

            res.json(updatedUser);
        } catch (err) {
            console.error('Update error:', err);
            res.status(500).json({ message: 'Greška pri ažuriranju profila.' });
        }
    }

};
