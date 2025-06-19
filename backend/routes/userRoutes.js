var express = require('express');
const multer = require('multer');
var router = express.Router();
var userController = require('../controllers/userController.js');
const upload = multer({ dest: 'uploads/avatars/' }); // ako koristiš storage iznad, prilagodi

const { authenticateToken } = require('../middlewares/auth');


router.get('/register', userController.showRegister);
router.get('/users-per-day', authenticateToken, userController.getUsersPerDay);

router.get('/login', userController.showLogin); 
router.get('/all-users', authenticateToken, userController.listUsers);

router.post('/', userController.create);
router.post('/login', userController.login);

router.get('/me', userController.getProfile);
router.put('/update', userController.updateProfile);
router.post('/refresh-token', userController.refreshToken);
router.post('/upload-avatar', upload.single('avatar'), userController.uploadAvatar);
router.delete('/:id', authenticateToken, userController.deleteUser);

module.exports = router;
