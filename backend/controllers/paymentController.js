const mongoose = require('mongoose');
const PaymentModel = require('../models/paymentModel.js');
const ParkingLocation = require('../models/parkingLocationModel');
const User = require('../models/userModel');
const Vehicle = require('../models/vehicleModel');
const TariffModel = require('../models/tariffModel');

const jwt = require('jsonwebtoken');
const SECRET = process.env.JWT_SECRET;

function timeIntervalsOverlap(start, end, range) {
  if (!range || typeof range !== 'string' || !range.includes('-')) {
    console.warn(`[timeIntervalsOverlap] Invalid range format: ${range}`);
    return false;
  }

  const cleanedRange = range.replace(/\s/g, '');
  const [rangeStart, rangeEnd] = cleanedRange.split('-');

  if (!rangeStart || !rangeEnd) {
    console.warn(`[timeIntervalsOverlap] Invalid time range after cleaning: ${cleanedRange}`);
    return false;
  }

  let startMin = timeToMinutes(start);
  let endMin = timeToMinutes(end);
  let rangeStartMin = timeToMinutes(rangeStart);
  let rangeEndMin = timeToMinutes(rangeEnd);

  function intervalsOverlap(a1, a2, b1, b2) {
    return a1 < b2 && b1 < a2;
  }

  if (endMin <= startMin) {
    endMin += 1440;
  }

  let result;
  if (rangeEndMin <= rangeStartMin) {
    result =
      intervalsOverlap(startMin, endMin, rangeStartMin, 1440) ||
      intervalsOverlap(startMin, endMin, 0, rangeEndMin);
  } else {
    result = intervalsOverlap(startMin, endMin, rangeStartMin, rangeEndMin);
  }

  console.log(`[timeIntervalsOverlap] start: ${start} (${startMin}), end: ${end} (${endMin}), range: ${range} (${rangeStartMin}-${rangeEndMin}), overlaps: ${result}`);

  return result;
}

function calculatePrice(durationHours, tariff) {
  const price = parseFloat(tariff.price.toString());
  const unit = tariff.price_unit;

  console.log(`[calculatePrice] Duration: ${durationHours}h, Tariff ID: ${tariff._id || tariff.id}, Unit: ${unit}, Base price: ${price}`);

  switch (unit) {
    case 'dan':
    case 'noč':
    case '24 ur od nakupa':
      return price;
    case 'ura':
      return price * durationHours;
    case 'prvi dve uri':
      if (durationHours <= 2) return price;
      return price + price * (durationHours - 2);
    case 'vsaka naslednja ura':
      if (durationHours <= 1) return price;
      return price + price * (durationHours - 1);
    case 'do 1 ure':
      return durationHours <= 1 ? price : null;
    case 'od 1 do 3 ure':
      return durationHours > 1 && durationHours <= 3 ? price : null;
    case 'od 3 do 5 ur':
      return durationHours > 3 && durationHours <= 5 ? price : null;
    case 'od 5 do 8 ur':
      return durationHours > 5 && durationHours <= 8 ? price : null;
    case 'nad 8 ur':
      return durationHours > 8 ? price : null;
    case 'do 3 ure':
      return durationHours <= 3 ? price : null;
    case 'nad 3 ure':
      return durationHours > 3 ? price : null;
    default:
      console.warn(`[calculatePrice] Nepoznata jedinica cene: ${unit}`);
      return null;
  }
}

function timeToMinutes(t) {
  const [h, m] = t.split(':').map(Number);
  return h * 60 + m;
}


/**
 * Server-side logic for managing payments.
 */
module.exports = {
  // controllers/paymentController.js (dopuni existing module.exports):
  getUserPaymentHistory: async (req, res) => {
    try {
      console.log('🔍 getUserPaymentHistory req.user:', req.user);

      const userId = req.user.userId;
      if (!userId) {
        console.error('❌ No userId found in token');
        return res.status(401).json({ message: 'Unauthorized' });
      }

      const payments = await PaymentModel.find({ user: userId }).sort({ date: -1 });
      console.log(`Found ${payments.length} payments`);
      res.json(payments);

    } catch (err) {
      console.error('Error in getUserPaymentHistory:', err);
      res.status(500).json({ message: 'Server error', error: err.message });
    }
  },
getPaymentsForUserByAdmin: async (req, res) => {
  try {
    if (!req.user) {
      return res.status(401).json({ message: 'Niste autentifikovani' });
    }

    const loggedUser = await User.findById(req.user.userId);
    if (!loggedUser || loggedUser.user_type !== 'admin') {
      return res.status(403).json({ message: 'Nema pristup' });
    }

    const userId = req.params.userId;
    if (!userId) {
      return res.status(400).json({ message: 'UserId je obavezan' });
    }

    const payments = await PaymentModel.find({ user: userId })
      .sort({ date: -1 })
    return res.json(payments);
  } catch (error) {
    return res.status(500).json({ message: 'Greška na serveru', error: error.message });
  }
},
  createAndCalculatePayment: async (req, res) => {
    try {
      const userId = req.user && (req.user._id || req.user.userId);
      const { locationId, duration, method = 'card', credit_card, vehicle_plate } = req.body;

      console.log(`[createAndCalculatePayment] User: ${userId}, Location: ${locationId}, Duration: ${duration}, Method: ${method}, Card: ${credit_card}, Plate: ${vehicle_plate}`);

      if (!userId || !locationId || !duration || !credit_card || !vehicle_plate) {
        console.warn('[createAndCalculatePayment] Nedostaju potrebni podaci');
        return res.status(400).json({ message: 'Nedostaju potrebni podaci' });
      }

      const tariffs = await TariffModel.find({
        parking_location: locationId,
        hidden: false,
      }).sort({ created: -1 });

      console.log(`[createAndCalculatePayment] Dohvaceno tarifa: ${tariffs.length}`);

      if (tariffs.length === 0) {
        console.warn('[createAndCalculatePayment] Nema tarifa za odabranu lokaciju');
        return res.status(404).json({ message: 'Nema tarifa za odabranu lokaciju' });
      }

      const now = new Date();
      const startTimeStr = now.toTimeString().slice(0, 5);
      const endDate = new Date(now.getTime() + duration * 60 * 60 * 1000);
      const endTimeStr = endDate.toTimeString().slice(0, 5);

      console.log(`[createAndCalculatePayment] Vreme pocetka: ${startTimeStr}, vreme kraja: ${endTimeStr}`);

      // Filtriraj samo tarife koje važe u trenutku početka
      const applicableTariffs = tariffs.filter((tariff) => {
        return timeIntervalsOverlap(startTimeStr, startTimeStr, tariff.duration);
      });


      console.log(`[createAndCalculatePayment] Vazeci tarifa: ${applicableTariffs.length}`);

      if (applicableTariffs.length === 0) {
        console.warn('[createAndCalculatePayment] Nema važećih tarifa za ovo vreme');
        return res.status(404).json({ message: 'Nema važećih tarifa za ovo vreme' });
      }

      let finalPrice = null;
      let selectedTariff = null;

      for (const tariff of applicableTariffs) {
        const price = calculatePrice(duration, tariff);
        console.log(`[createAndCalculatePayment] Provera tarife ID ${tariff._id || tariff.id} - cena: ${price}`);
        if (price !== null && (finalPrice === null || price < finalPrice)) {
          finalPrice = price;
          selectedTariff = tariff;
        }
      }

      if (finalPrice === null) {
        console.warn('[createAndCalculatePayment] Nema odgovarajuće tarife za odabrano trajanje');
        return res.status(400).json({ message: 'Nema odgovarajuće tarife za odabrano trajanje' });
      }

      console.log(`[createAndCalculatePayment] Izabrana tarifa ID: ${selectedTariff._id || selectedTariff.id}, cena: ${finalPrice}`);

      const payment = new PaymentModel({
        date: now,
        amount: mongoose.Types.Decimal128.fromString(finalPrice.toFixed(2)),
        method,
        payment_status: 'completed',
        duration,
        hidden: false,
        created: now,
        modified: now,
        user: userId,
        parking_location: locationId,
        vehicle_plate: vehicle_plate,
      });

      await payment.save();

      console.log('[createAndCalculatePayment] Placanje uspesno sacuvano');

      res.status(200).json({
        price: finalPrice.toFixed(2),
        unit: selectedTariff.price_unit,
        message: 'Plaćanje uspešno',
      });
    } catch (error) {
      console.error('[createAndCalculatePayment] Greška pri plaćanju:', error.message);
      res.status(500).json({ message: 'Greška pri plaćanju', error: error.message });
    }
  },
  getTariffsByLocation: async (req, res) => {
    try {
      const locationId = req.params.locationId;
      const tariffs = await TariffModel.find({ parking_location: locationId, hidden: false });
      res.status(200).json(tariffs);
    } catch (error) {
      res.status(500).json({ message: 'Neuspelo dohvatanje tarifa' });
    }
  },
  checkActivePayment: async (req, res) => {
    try {
      const userId = req.user && (req.user._id || req.user.userId);
      if (!userId) {
        return res.status(401).json({ message: 'Unauthorized' });
      }

      const now = new Date();

      // Nadji aktivna placanja gde (date + duration) > now
      // Pretpostavljam da 'date' je početak plaćanja, duration u satima
      const activePayment = await PaymentModel.findOne({
        user: userId,
        payment_status: 'completed',
        $expr: {
          $gt: [
            { $add: ["$date", { $multiply: ["$duration", 3600 * 1000] }] },
            now,
          ]
        }
      }).sort({ date: -1 });

      if (!activePayment) {
        return res.status(200).json({ active: false });
      }

      // Izračunaj vreme do isteka u ms
      const endTime = new Date(activePayment.date.getTime() + activePayment.duration * 3600 * 1000);
      const remainingMs = endTime.getTime() - now.getTime();

      return res.status(200).json({
        active: true,
        expiresAt: endTime,
        remainingMs,
      });
    } catch (err) {
      console.error('Error checking active payment:', err);
      res.status(500).json({ message: 'Server error' });
    }
  },
  listUserPayments: function (req, res) {
    try {
      console.log('User iz tokena:', req.user);

      const userIdRaw = req.user && (req.user.id || req.user.userId);
      console.log('Raw User ID:', userIdRaw);

      if (!userIdRaw) {
        console.log('Nema userId u tokenu!');
        return res.status(401).json({ message: 'Unauthorized' });
      }

      const mongoose = require('mongoose');
      let userId;
      try {
        userId = mongoose.Types.ObjectId(userIdRaw);
      } catch (e) {
        console.log('User ID nije validan ObjectId:', userIdRaw);
        return res.status(400).json({ message: 'Invalid user ID format' });
      }

      PaymentModel.find({ user: userId }, function (err, payments) {
        if (err) {
          console.error('Greška pri dohvatanju uplata:', err);
          return res.status(500).json({
            message: 'Error when getting user payments',
            error: err.message,
          });
        }
        console.log('Uplate za korisnika:', payments);
        return res.json(payments);
      });
    } catch (err) {
      console.error('Nešto nije u redu u listUserPayments:', err);
      return res.status(500).json({ message: 'Server error', error: err.message });
    }
  },

  getActivePaymentsForUser: async (req, res) => {
    try {
      const authHeader = req.headers.authorization;
      if (!authHeader || !authHeader.startsWith('Bearer ')) {
        return res.status(401).json({ message: 'Unauthorized: token missing' });
      }

      const token = authHeader.split(' ')[1];
      let decoded;
      try {
        decoded = jwt.verify(token, SECRET);
      } catch {
        return res.status(401).json({ message: 'Unauthorized: invalid token' });
      }

      const userId = decoded.id || decoded._id || decoded.userId;
      if (!mongoose.Types.ObjectId.isValid(userId)) {
        return res.status(400).json({ message: 'Invalid user ID' });
      }

      const now = new Date();

      const payments = await PaymentModel.aggregate([
        {
          $match: {
            user: mongoose.Types.ObjectId(userId),
            payment_status: 'completed',
            hidden: { $ne: true },
          }
        },
        {
          $addFields: {
            expiresAt: {
              $add: [
                "$date",
                { $multiply: ["$duration", 60 * 60 * 1000] }
              ]
            }
          }
        },
        {
          $match: {
            expiresAt: { $gt: now }
          }
        },
        {
          $project: {
            vehicle_plate: 1,
            date: 1,
            duration: 1,
            amount: 1,
            expiresAt: 1,
          }
        }
      ]);

      const formatted = payments.map(p => ({
        vehiclePlate: p.vehicle_plate || '',
        date: p.date,
        duration: p.duration,
        amount: p.amount ? parseFloat(p.amount.toString()) : null,
        expiresAt: new Date(p.expiresAt).getTime()
      }));

      return res.json(formatted);

    } catch (err) {
      console.error('Error in getActivePaymentsForUser:', err);
      return res.status(500).json({ message: 'Server error', error: err.message });
    }
  },
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
// Dobavljanje top parking lokacija po broju uplata
getTopParkingLocations: async (req, res) => {
  try {
    // Opcionalno možeš primiti query parametre za filtriranje po vremenu, npr:
    // ?from=2025-01-01&to=2025-06-01
    const { from, to } = req.query;

    const matchStage = { payment_status: 'completed' };

    if (from || to) {
      matchStage.date = {};
      if (from) matchStage.date.$gte = new Date(from);
      if (to) matchStage.date.$lte = new Date(to);
    }

    const topLocations = await PaymentModel.aggregate([
      { $match: matchStage },
      {
        $group: {
          _id: '$parking_location',
          usageCount: { $sum: 1 }
        }
      },
      {
        $lookup: {
          from: 'parking_locations', // ime kolekcije u Mongo
          localField: '_id',
          foreignField: '_id',
          as: 'locationDetails'
        }
      },
      { $unwind: '$locationDetails' },
      {
        $project: {
          _id: 0,
          locationId: '$_id',
          name: '$locationDetails.name',
          address: '$locationDetails.address',
          usageCount: 1
        }
      },
      { $sort: { usageCount: -1 } },
      { $limit: 10 } // npr top 10
    ]);

    res.json(topLocations);

  } catch (error) {
    console.error('Error fetching top parking locations:', error);
    res.status(500).json({ message: 'Greška prilikom dohvatanja podataka' });
  }
}

};
