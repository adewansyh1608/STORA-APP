const express = require('express');
const router = express.Router();
const notificationController = require('../controllers/notificationController');
const authMiddleware = require('../middleware/authMiddleware');
const { body } = require('express-validator');

// All routes require authentication
router.use(authMiddleware);

// Register FCM token
router.post('/register-token', notificationController.registerToken);

// Get notification history for current user
router.get('/history', notificationController.getNotificationHistory);

// Get all reminders for current user
router.get('/reminders', notificationController.getReminders);

// Create new reminder
router.post(
    '/reminders',
    [
        body('reminder_type')
            .isIn(['periodic', 'custom'])
            .withMessage('Reminder type must be periodic or custom'),
        body('title').optional().isString().isLength({ max: 100 }),
        body('periodic_months')
            .optional()
            .isInt({ min: 1, max: 12 })
            .withMessage('Periodic months must be between 1 and 12'),
        body('scheduled_datetime').optional().isISO8601(),
        body('fcm_token').optional().isString(),
    ],
    notificationController.createReminder
);

// Update reminder
router.put('/reminders/:id', notificationController.updateReminder);

// Delete reminder
router.delete('/reminders/:id', notificationController.deleteReminder);

// Toggle reminder active status
router.patch('/reminders/:id/toggle', notificationController.toggleReminder);

module.exports = router;
