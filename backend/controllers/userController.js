var UserModel = require('../models/userModel.js');
const VehicleModel = require('../models/vehicleModel');
const PaymentModel = require('../models/paymentModel');
const ParkingLocationModel = require('../models/parkingLocationModel');

const jwt = require('jsonwebtoken');
const SECRET = process.env.JWT_SECRET;
const bcrypt = require('bcrypt');
const multer = require('multer');
const path = require('path');
const fs = require('fs');

// Folder gde čuvamo avatara (kreiraj ovaj folder 'uploads/avatars')
const avatarStorage = multer.diskStorage({
    destination: function (req, file, cb) {
        const dir = './uploads/avatars';
        if (!fs.existsSync(dir)) {
            fs.mkdirSync(dir, { recursive: true });
        }
        cb(null, dir);
    },
    filename: function (req, file, cb) {
        const ext = path.extname(file.originalname);
        const uniqueName = `${Date.now()}-${Math.round(Math.random() * 1E9)}${ext}`;
        cb(null, uniqueName);
    }
});

const uploadAvatar = multer({
    storage: avatarStorage,
    limits: { fileSize: 1024 * 1024 * 5 }, // max 5MB
    fileFilter: function (req, file, cb) {
        // prihvata samo slike
        if (!file.mimetype.startsWith('image/')) {
            return cb(new Error('Samo slike su dozvoljene!'), false);
        }
        cb(null, true);
    }
});

/**
 * userController.js
 *
 * @description :: Server-side logic for managing users.
 */
module.exports = {
    uploadAvatar: async function (req, res) {
        try {
            console.log('--- Avatar upload request received ---');

            const token = req.headers.authorization?.split(' ')[1];
            if (!token) {
                console.log('No token provided');
                return res.status(401).json({ message: 'Unauthorized' });
            }

            const decoded = jwt.verify(token, SECRET);
            console.log('Token decoded:', decoded);

            if (!req.file) {
                console.log('No file uploaded in request');
                return res.status(400).json({ message: 'No file uploaded' });
            }

            console.log('File info:', req.file);

            const avatarPath = `/uploads/avatars/${req.file.filename}`;
            console.log('Avatar path set to:', avatarPath);

            const updatedUser = await UserModel.findByIdAndUpdate(
                decoded.userId,
                { avatar: avatarPath, updated_at: new Date() },
                { new: true }
            ).lean();

            console.log('User updated with new avatar:', updatedUser.avatar);

            res.json({ avatar: updatedUser.avatar });
        } catch (err) {
            console.error('Error uploading avatar:', err);
            res.status(500).json({ message: 'Upload avatara nije uspeo.' });
        }
    },
    listUsers: async function (req, res) {
        try {
            // Pretpostavimo da je ovo admin-only endpoint, pa možeš dodati validaciju tokena admina

            const users = await UserModel.find({ user_type: 'user' }).lean();
            res.json(users);
        } catch (err) {
            console.error('Error fetching users:', err);
            res.status(500).json({ message: 'Greška pri dohvatanju korisnika' });
        }
    },

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

            const allowedFields = ['email', 'phone_number', 'credit_card_number', 'username', 'avatar'];
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
            if (req.body.avatar) {
                updates.avatar = req.body.avatar;
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
    },
    deleteUser: async function (req, res) {
        try {
            const token = req.headers.authorization?.split(' ')[1];
            if (!token) {
                return res.status(401).json({ message: 'Unauthorized' });
            }

            const decoded = jwt.verify(token, SECRET);

            // Provera da li je admin
            const user = await UserModel.findById(decoded.userId);
            if (!user || user.user_type !== 'admin') {
                return res.status(403).json({ message: 'Forbidden: Only admins can delete users.' });
            }

            const userIdToDelete = req.params.id;
            if (!userIdToDelete) {
                return res.status(400).json({ message: 'User ID is required' });
            }

            // Ne dozvoli adminu da obriše samog sebe (opciono)
            if (userIdToDelete === user._id.toString()) {
                return res.status(400).json({ message: 'Admin cannot delete themselves.' });
            }

            const deletedUser = await UserModel.findByIdAndDelete(userIdToDelete);
            if (!deletedUser) {
                return res.status(404).json({ message: 'User not found' });
            }

            res.json({ message: 'User deleted successfully.' });
        } catch (err) {
            console.error('Delete user error:', err);
            res.status(500).json({ message: 'Server error' });
        }
    },
    refreshToken: function (req, res) {
        const authHeader = req.headers.authorization;
        if (!authHeader || !authHeader.startsWith('Bearer ')) {
            return res.status(401).json({ message: 'Unauthorized' });
        }

        const token = authHeader.split(' ')[1];

        jwt.verify(token, SECRET, (err, decoded) => {
            if (err) {
                return res.status(403).json({ message: 'Invalid token' });
            }

            // Opcionalno: proveri da li korisnik i dalje postoji i nije deaktiviran

            const newToken = jwt.sign(
                { userId: decoded.userId, username: decoded.username, email: decoded.email },
                SECRET,
                { expiresIn: '1h' }
            );

            res.json({ token: newToken });
        });
    },
    getUsersPerDay: async function (req, res) {
        try {
            // Grupisanje po danu na osnovu created_at
            const usersPerDay = await UserModel.aggregate([
                {
                    $match: { user_type: 'user' } // ako želiš samo obične korisnike
                },
                {
                    $group: {
                        _id: {
                            $dateToString: { format: "%Y-%m-%d", date: "$created_at" }
                        },
                        count: { $sum: 1 }
                    }
                },
                {
                    $sort: { _id: 1 } // sortiraj po datumu rastuće
                }
            ]);

            res.json(usersPerDay); // [{ _id: '2025-06-01', count: 5 }, ...]
        } catch (err) {
            console.error('Error in getUsersPerDay:', err);
            res.status(500).json({ message: 'Server error' });
        }
    }
};