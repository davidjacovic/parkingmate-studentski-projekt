const PaymentModel = require('../models/paymentModel.js');
const ParkingLocation = require('../models/parkingLocationModel');
const User = require('../models/userModel');
const Vehicle = require('../models/vehicleModel');
const Tariff = require('../models/tariffModel');

/**
 * Server-side logic for managing payments.
 */
module.exports = {
    /**
     * List all payments
     */
    list: function (req, res) {
        PaymentModel.find(function (err, payments) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when getting payments',
                    error: err,
                });
            }
            return res.json(payments);
        });
    },

    /**
     * Get one payment by ID
     */
    show: function (req, res) {
        const id = req.params.id;

        PaymentModel.findOne({ _id: id }, function (err, payment) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when getting payment',
                    error: err,
                });
            }

            if (!payment) {
                return res.status(404).json({
                    message: 'No such payment',
                });
            }

            return res.json(payment);
        });
    },

    /**
     * Create a new payment
     */
    create: function (req, res) {
        const payment = new PaymentModel({
        });

        payment.save(function (err, savedPayment) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when creating payment',
                    error: err,
                });
            }

            return res.status(201).json(savedPayment);
        });
    },

    /**
     * Update an existing payment
     */
    update: function (req, res) {
        const id = req.params.id;

        PaymentModel.findOne({ _id: id }, function (err, payment) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when finding payment',
                    error: err,
                });
            }

            if (!payment) {
                return res.status(404).json({
                    message: 'No such payment',
                });
            }
            payment.save(function (err, updatedPayment) {
                if (err) {
                    return res.status(500).json({
                        message: 'Error when updating payment',
                        error: err,
                    });
                }

                return res.json(updatedPayment);
            });
        });
    },

    /**
     * Delete a payment
     */
    remove: function (req, res) {
        const id = req.params.id;

        PaymentModel.findByIdAndRemove(id, function (err) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when deleting the payment',
                    error: err,
                });
            }

            return res.status(204).json();
        });
    },

    /**
     * Get list of all addresses
     */
    addresses: function (req, res) {
        ParkingLocation.find({}, 'name', function (err, locations) {
            if (err) {
                return res.status(500).json({
                    message: 'Error fetching names',
                    error: err
                });
            }
            res.json(locations);
        });
    },

    /**
     * Search addresses
     */
    searchAddresses: async (req, res) => {
        const searchQuery = req.query.q || '';
        console.log('Search query:', searchQuery);

        try {
            const results = await ParkingLocation.find({
                name: { $regex: searchQuery, $options: 'i' }
            }).limit(10).select('name');

            console.log('Search results:', results);
            res.status(200).json(results);
        } catch (error) {
            console.error('Search error:', error.message);
            res.status(500).json({ message: 'Search failed', error: error.message });
        }
    },

    /**
     * getUserPaymentInfo
     */
    getUserPaymentInfo: async (req, res) => {
        try {
            const userId = req.user && (req.user._id || req.user.userId);
            if (!userId) {
                return res.status(401).json({ message: 'Unauthorized' });
            }

            const user = await User.findById(userId).lean();
            const vehicles = await Vehicle.find({ user: userId }).lean();

            res.status(200).json({
                credit_card_number: user?.credit_card_number || '',
                registration_number: vehicles.length > 0 ? vehicles[0].registration_number : ''
            });
        } catch (err) {
            console.error('Error fetching user payment info:', err.message);
            res.status(500).json({ message: 'Failed to fetch user info' });
        }
    },

    /**
     * Get current user's credit card number
     */ 
    getUserCreditCard: async (req, res) => {
        try {
            const userId = req.user && (req.user._id || req.user.userId);
            if (!userId) {
                return res.status(401).json({ message: 'Unauthorized' });
            }

            const user = await User.findById(userId).select('credit_card_number').lean();

            if (!user) {
                return res.status(404).json({ message: 'User not found' });
            }

            res.status(200).json({ credit_card_number: user.credit_card_number || '' });
        } catch (err) {
            console.error('Error fetching credit card:', err.message);
            res.status(500).json({ message: 'Failed to fetch credit card info' });
        }
    },

    /**
     * createAndCalculatePayment
     */ 
    createAndCalculatePayment: async (req, res) => {
        try {
            const userId = req.user && (req.user._id || req.user.userId);
            const { locationId, duration, method = 'card' } = req.body;

            if (!userId || !locationId || !duration) {
                return res.status(400).json({ message: 'Missing required fields' });
            }

            const tariffs = await Tariff.find({ parking_location: locationId }).sort({ created: -1 });

            if (!tariffs || tariffs.length === 0) {
                return res.status(404).json({ message: 'No tariffs for selected location' });
            }

            const pricePerHour = parseFloat(tariffs[0].price_with_tax.toString());
            const amount = (pricePerHour * duration).toFixed(2);

            const payment = new PaymentModel({
                date: new Date(),
                amount,
                method,
                payment_status: 'completed',
                duration,
                hidden: false,
                created: new Date(),
                modified: new Date(),
                user: userId,
                parking_location: locationId
            });

            await payment.save();

            res.status(201).json({
                message: 'Payment successful',
                payment,
                calculated_price: amount,
                unit: tariffs[0].price_unit
            });
        } catch (err) {
            console.error('Error processing payment:', err);
            res.status(500).json({ message: 'Internal server error' });
        }
    }
};
