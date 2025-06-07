const ReviewsModel = require('../models/reviewsModel.js');

/**
 * reviewsController.js
 *
 * @description :: Server-side logic for managing reviews.
 */
module.exports = {

  /**
   * List all reviews
   */
  list: function (req, res) {
    ReviewsModel.find(function (err, reviews) {
      if (err) {
        return res.status(500).json({
          message: 'Error when getting reviews.',
          error: err
        });
      }

      return res.json(reviews);
    });
  },

  /**
   * Show one review by ID
   */
  show: async function (req, res) {
    try {
      const review = await ReviewsModel.findById(req.params.id);
      if (!review) {
        return res.status(404).json({ message: 'Review not found' });
      }
      res.json(review);
    } catch (err) {
      console.error('Error fetching review:', err);
      res.status(500).json({ message: 'Server error' });
    }
  },

  /**
   * Create a new review
   */
    create: function (req, res) {
    console.log('📥 POST /reviews pozvan');
    console.log('req.headers.authorization:', req.headers.authorization);
    console.log('req.user:', req.user); // Proverava da li je `authenticateToken` middleware radio

    if (!req.user) {
      console.warn('⚠ Nema korisnika u zahtevu');
      return res.status(401).json({ message: 'Morate biti prijavljeni da biste ostavili komentar.' });
    }

    const { rating, review_text, parking_location } = req.body;
    console.log('Body:', { rating, review_text, parking_location });

    if (!rating || !parking_location) {
      console.warn('⚠ Nedostaju obavezna polja');
      return res.status(400).json({ message: 'Nedostaju obavezna polja.' });
    }

    const review = new ReviewsModel({
      rating,
      review_text,
      review_date: new Date(),
      created: new Date(),
      modified: new Date(),
      user: req.user.userId,
      parking_location,
    });

    review.save(function (err, savedReview) {
      if (err) {
        console.error('❌ Greška pri čuvanju komentara:', err);
        return res.status(500).json({ message: 'Greška pri kreiranju komentara', error: err });
      }

      console.log('✅ Komentar sačuvan:', savedReview);

      // Populacija user.username pre vraćanja odgovora
      ReviewsModel.findById(savedReview._id)
        .populate('user', 'username')
        .exec(function (err, populatedReview) {
          if (err) {
            console.error('❌ Greška pri populaciji komentara:', err);
            return res.status(500).json({ message: 'Greška pri vraćanju komentara', error: err });
          }

          console.log('✅ Komentar sačuvan i populovan:', populatedReview);
          return res.status(201).json(populatedReview);
        });
    });
  },

  /**
   * Update a review
   */
  update: function (req, res) {
    const id = req.params.id;

    ReviewsModel.findOne({ _id: id }, function (err, review) {
      if (err) {
        return res.status(500).json({
          message: 'Error when getting review',
          error: err
        });
      }

      if (!review) {
        return res.status(404).json({
          message: 'No such review'
        });
      }

      // Ažuriraj polja ako treba
      // npr: review.rating = req.body.rating || review.rating;

      review.save(function (err, updatedReview) {
        if (err) {
          return res.status(500).json({
            message: 'Error when updating review.',
            error: err
          });
        }

        return res.json(updatedReview);
      });
    });
  },

  /**
   * Delete a review
   */
  remove: function (req, res) {
    const id = req.params.id;

    ReviewsModel.findByIdAndRemove(id, function (err) {
      if (err) {
        return res.status(500).json({
          message: 'Error when deleting the review.',
          error: err
        });
      }

      return res.status(204).json();
    });
  },

  /**
   * List reviews by parking location
   */
  listByParkingLocation: function (req, res) {
    const parkingId = req.params.parkingId;
    console.log('📄 listByParkingLocation za parkingId:', parkingId);

    ReviewsModel.find({ parking_location: parkingId })
      .populate('user', 'username')
      .exec(function (err, reviews) {
        if (err) {
          console.error('❌ Greška pri dohvatanju komentara:', err);
          return res.status(500).json({ message: 'Greška pri dohvatanju komentara.', error: err });
        }

        console.log('📤 Komentari pronađeni:', reviews.map(r => ({
          id: r._id,
          user: r.user,
          username: r.user?.username || 'N/A'
        })));

        return res.json(reviews);
      });
  },

  /**
   * Get a review by ID
   */
  getById: async function (req, res) {
    try {
      const review = await ReviewsModel.findById(req.params.id);
      if (!review) {
        return res.status(404).json({ message: 'Review not found' });
      }
      res.json(review);
    } catch (err) {
      console.error('Error fetching review:', err);
      res.status(500).json({ message: 'Server error' });
    }
  }
};
