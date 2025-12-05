const express = require('express');
const router = express.Router();
const userController = require('../controllers/userController');
const { body } = require('express-validator');
const authMiddleware = require('../middleware/authMiddleware');

// Validation rules for user creation
const userValidationRules = [
  body('Nama_User')
    .notEmpty()
    .withMessage('Name is required')
    .isLength({ min: 2 })
    .withMessage('Name must be at least 2 characters long'),
  body('Email')
    .isEmail()
    .withMessage('Please provide a valid email')
    .normalizeEmail(),
  body('Password')
    .isLength({ min: 6 })
    .withMessage('Password must be at least 6 characters long')
];

// Login validation rules
const loginValidationRules = [
  body('Email')
    .isEmail()
    .withMessage('Please provide a valid email')
    .normalizeEmail(),
  body('Password')
    .notEmpty()
    .withMessage('Password is required')
];

// Routes
router.get('/', userController.getAllUsers);
router.get('/:id', userController.getUserById);
router.post('/', userValidationRules, userController.createUser);
router.post('/login', loginValidationRules, userController.loginUser);
router.put('/:id', authMiddleware, userController.updateUser);
router.delete('/:id', authMiddleware, userController.deleteUser);

module.exports = router;
