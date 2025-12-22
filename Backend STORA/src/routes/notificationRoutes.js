const express = require('express');
const router = express.Router();
const notificationController = require('../controllers/notificationController');
const authMiddleware = require('../middleware/authMiddleware');
const { body } = require('express-validator');

router.use(authMiddleware);

router.post('/register-token', notificationController.registerToken);

router.get('/history', notificationController.getNotificationHistory);

router.post('/history', notificationController.createNotificationHistory);

router.get('/reminders', notificationController.getReminders);

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
        body('scheduled_datetime').optional(),
        body('fcm_token').optional().isString(),
    ],
    notificationController.createReminder
);

router.put('/reminders/:id', notificationController.updateReminder);

router.delete('/reminders/:id', notificationController.deleteReminder);

router.patch('/reminders/:id/toggle', notificationController.toggleReminder);

module.exports = router;
