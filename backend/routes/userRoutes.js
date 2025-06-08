var express = require('express');
const multer = require('multer');
var router = express.Router();
var userController = require('../controllers/userController.js');
const upload = multer({ dest: 'uploads/avatars/' }); // ako koristiš storage iznad, prilagodi


/*
router.get('/', userController.list);
router.get('/:id', userController.show);
router.put('/:id', userController.update);
router.delete('/:id', userController.remove);
*/

router.get('/register', userController.showRegister);
router.get('/login', userController.showLogin); 

router.post('/', userController.create);
router.post('/login', userController.login);

router.get('/me', userController.getProfile);
router.put('/update', userController.updateProfile);
router.post('/refresh-token', userController.refreshToken);
router.post('/upload-avatar', upload.single('avatar'), userController.uploadAvatar);

module.exports = router;
