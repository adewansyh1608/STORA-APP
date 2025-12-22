const express = require('express');
const router = express.Router();
const authController = require('../controllers/authController');
const { body } = require('express-validator');
const authMiddleware = require('../middleware/authMiddleware');
const { profileUpload } = require('../middleware/upload');

const signupValidationRules = [
  body('name')
    .notEmpty()
    .withMessage('Name is required')
    .isLength({ min: 2 })
    .withMessage('Name must be at least 2 characters long'),
  body('email')
    .isEmail()
    .withMessage('Please provide a valid email')
    .normalizeEmail(),
  body('password')
    .isLength({ min: 6 })
    .withMessage('Password must be at least 6 characters long'),
  body('password_confirmation')
    .notEmpty()
    .withMessage('Password confirmation is required')
];

const loginValidationRules = [
  body('email')
    .isEmail()
    .withMessage('Please provide a valid email')
    .normalizeEmail(),
  body('password')
    .notEmpty()
    .withMessage('Password is required')
];

const profileUpdateValidationRules = [
  body('name')
    .optional()
    .isLength({ min: 2 })
    .withMessage('Name must be at least 2 characters long'),
  body('email')
    .optional()
    .isEmail()
    .withMessage('Please provide a valid email')
    .normalizeEmail()
];

const resetPasswordValidationRules = [
  body('email')
    .isEmail()
    .withMessage('Please provide a valid email')
    .normalizeEmail(),
  body('new_password')
    .isLength({ min: 6 })
    .withMessage('Password must be at least 6 characters long'),
  body('confirm_password')
    .notEmpty()
    .withMessage('Confirm password is required')
];

router.post('/signup', signupValidationRules, authController.signup);
router.post('/login', loginValidationRules, authController.login);
router.post('/logout', authMiddleware, authController.logout);
router.post('/reset-password', resetPasswordValidationRules, authController.resetPassword);
router.get('/profile', authMiddleware, authController.getProfile);
router.put('/profile', authMiddleware, profileUpdateValidationRules, authController.updateProfile);

router.post('/profile/photo', authMiddleware, profileUpload.single('photo'), authController.uploadProfilePhoto);

module.exports = router;
