const { body, param, query } = require('express-validator');

class Validator {
  // User validation rules
  static userRegistration() {
    return [
      body('name')
        .notEmpty()
        .withMessage('Name is required')
        .isLength({ min: 2, max: 50 })
        .withMessage('Name must be between 2 and 50 characters')
        .trim(),
      body('email')
        .isEmail()
        .withMessage('Please provide a valid email')
        .normalizeEmail()
        .toLowerCase(),
      body('password')
        .isLength({ min: 6 })
        .withMessage('Password must be at least 6 characters long')
        .matches(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/)
        .withMessage('Password must contain at least one lowercase letter, one uppercase letter, and one number')
    ];
  }

  static userLogin() {
    return [
      body('email')
        .isEmail()
        .withMessage('Please provide a valid email')
        .normalizeEmail()
        .toLowerCase(),
      body('password')
        .notEmpty()
        .withMessage('Password is required')
    ];
  }

  static userUpdate() {
    return [
      body('name')
        .optional()
        .isLength({ min: 2, max: 50 })
        .withMessage('Name must be between 2 and 50 characters')
        .trim(),
      body('email')
        .optional()
        .isEmail()
        .withMessage('Please provide a valid email')
        .normalizeEmail()
        .toLowerCase()
    ];
  }

  static passwordUpdate() {
    return [
      body('currentPassword')
        .notEmpty()
        .withMessage('Current password is required'),
      body('newPassword')
        .isLength({ min: 6 })
        .withMessage('New password must be at least 6 characters long')
        .matches(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/)
        .withMessage('New password must contain at least one lowercase letter, one uppercase letter, and one number'),
      body('confirmPassword')
        .custom((value, { req }) => {
          if (value !== req.body.newPassword) {
            throw new Error('Password confirmation does not match new password');
          }
          return true;
        })
    ];
  }

  // Common parameter validations
  static mongoId() {
    return [
      param('id')
        .isMongoId()
        .withMessage('Invalid ID format')
    ];
  }

  // Query parameter validations
  static pagination() {
    return [
      query('page')
        .optional()
        .isInt({ min: 1 })
        .withMessage('Page must be a positive integer'),
      query('limit')
        .optional()
        .isInt({ min: 1, max: 100 })
        .withMessage('Limit must be between 1 and 100'),
      query('search')
        .optional()
        .isLength({ max: 100 })
        .withMessage('Search term cannot exceed 100 characters')
        .trim()
    ];
  }
}

module.exports = Validator;
