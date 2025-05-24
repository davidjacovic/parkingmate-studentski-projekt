var express = require('express');
var router = express.Router();
var userController = require('../controllers/userController.js');
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
module.exports = router;
