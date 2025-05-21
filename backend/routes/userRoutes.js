var express = require('express');
var router = express.Router();
var userController = require('../controllers/userController.js');


//router.get('/', userController.list);
router.get('/register', userController.showRegister);
router.get('/login', userController.showLogin);

//router.get('/:id', userController.show);

router.post('/', userController.create);
router.post('/login', userController.login);

router.get('/me', userController.getProfile);
router.put('/update', userController.updateProfile);


//router.put('/:id', userController.update);

//router.delete('/:id', userController.remove);

module.exports = router;
