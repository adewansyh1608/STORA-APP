const { ReminderSetting, User, Notifikasi } = require('../models');
const { sendPushNotification } = require('../services/firebaseAdmin');
const { validationResult } = require('express-validator');

class NotificationController {
    // Register FCM token for a user
    async registerToken(req, res) {
        try {
            const { fcm_token } = req.body;
            const userId = req.user.id;

            if (!fcm_token) {
                return res.status(400).json({
                    success: false,
                    message: 'FCM token is required',
                });
            }

            // Update all active reminders for this user with the new token
            await ReminderSetting.update(
                { fcm_token },
                { where: { ID_User: userId, is_active: true } }
            );

            console.log(`✓ FCM token registered for user ${userId}`);

            res.status(200).json({
                success: true,
                message: 'FCM token registered successfully',
            });
        } catch (error) {
            console.error('Error registering FCM token:', error);
            res.status(500).json({
                success: false,
                message: error.message,
            });
        }
    }

    // Get all reminders for current user
    async getReminders(req, res) {
        try {
            const userId = req.user.id;

            const reminders = await ReminderSetting.findAll({
                where: { ID_User: userId },
                order: [['created_at', 'DESC']],
            });

            res.status(200).json({
                success: true,
                data: reminders,
            });
        } catch (error) {
            console.error('Error getting reminders:', error);
            res.status(500).json({
                success: false,
                message: error.message,
            });
        }
    }

    // Create or update reminder (upsert for periodic)
    async createReminder(req, res) {
        try {
            const errors = validationResult(req);
            if (!errors.isEmpty()) {
                return res.status(400).json({
                    success: false,
                    message: 'Validation errors',
                    errors: errors.array(),
                });
            }

            const userId = req.user.id;
            const { reminder_type, title, periodic_months, scheduled_datetime, fcm_token } = req.body;

            // Validate based on reminder type
            if (reminder_type === 'periodic' && (!periodic_months || periodic_months < 1 || periodic_months > 12)) {
                return res.status(400).json({
                    success: false,
                    message: 'Periodic months must be between 1 and 12',
                });
            }

            if (reminder_type === 'custom' && !scheduled_datetime) {
                return res.status(400).json({
                    success: false,
                    message: 'Scheduled datetime is required for custom reminders',
                });
            }

            // For PERIODIC reminders: Check if user already has one, then UPDATE instead of CREATE
            if (reminder_type === 'periodic') {
                const existingPeriodicReminder = await ReminderSetting.findOne({
                    where: {
                        ID_User: userId,
                        reminder_type: 'periodic',
                    },
                });

                if (existingPeriodicReminder) {
                    // UPDATE existing periodic reminder
                    await existingPeriodicReminder.update({
                        title: title || 'Pengingat Pengecekan Inventory',
                        periodic_months: periodic_months || 3,
                        fcm_token: fcm_token || existingPeriodicReminder.fcm_token,
                        is_active: true,
                        last_notified: new Date(), // Reset countdown from now
                    });

                    console.log(`✓ Periodic reminder updated: ${existingPeriodicReminder.ID_Reminder} for user ${userId}, months: ${periodic_months}`);

                    return res.status(200).json({
                        success: true,
                        message: `Pengingat periodik diperbarui ke ${periodic_months} bulan`,
                        data: existingPeriodicReminder,
                    });
                }
            }

            // CREATE new reminder (for custom, or if no periodic exists)
            const reminderData = {
                ID_User: userId,
                reminder_type,
                title: title || 'Pengingat Pengecekan Inventory',
                periodic_months: reminder_type === 'periodic' ? (periodic_months || 3) : null,
                scheduled_datetime: reminder_type === 'custom' ? new Date(scheduled_datetime) : null,
                fcm_token,
                is_active: true,
                last_notified: reminder_type === 'periodic' ? new Date() : null,
            };

            const reminder = await ReminderSetting.create(reminderData);

            console.log(`✓ Reminder created: ${reminder.ID_Reminder} for user ${userId}`);

            res.status(201).json({
                success: true,
                message: 'Reminder created successfully',
                data: reminder,
            });
        } catch (error) {
            console.error('Error creating reminder:', error);
            res.status(500).json({
                success: false,
                message: error.message,
            });
        }
    }

    // Update reminder
    async updateReminder(req, res) {
        try {
            const { id } = req.params;
            const userId = req.user.id;
            const updateData = req.body;

            const reminder = await ReminderSetting.findOne({
                where: { ID_Reminder: id, ID_User: userId },
            });

            if (!reminder) {
                return res.status(404).json({
                    success: false,
                    message: 'Reminder not found',
                });
            }

            // If updating periodic_months, reset last_notified to start countdown from now
            if (updateData.periodic_months && reminder.reminder_type === 'periodic') {
                updateData.last_notified = new Date();
            }

            // Update the reminder
            await reminder.update(updateData);

            res.status(200).json({
                success: true,
                message: 'Reminder updated successfully',
                data: reminder,
            });
        } catch (error) {
            console.error('Error updating reminder:', error);
            res.status(500).json({
                success: false,
                message: error.message,
            });
        }
    }

    // Delete reminder
    async deleteReminder(req, res) {
        try {
            const { id } = req.params;
            const userId = req.user.id;

            const deletedCount = await ReminderSetting.destroy({
                where: { ID_Reminder: id, ID_User: userId },
            });

            if (deletedCount === 0) {
                return res.status(404).json({
                    success: false,
                    message: 'Reminder not found',
                });
            }

            res.status(200).json({
                success: true,
                message: 'Reminder deleted successfully',
            });
        } catch (error) {
            console.error('Error deleting reminder:', error);
            res.status(500).json({
                success: false,
                message: error.message,
            });
        }
    }

    // Toggle reminder active status
    async toggleReminder(req, res) {
        try {
            const { id } = req.params;
            const userId = req.user.id;

            const reminder = await ReminderSetting.findOne({
                where: { ID_Reminder: id, ID_User: userId },
            });

            if (!reminder) {
                return res.status(404).json({
                    success: false,
                    message: 'Reminder not found',
                });
            }

            await reminder.update({ is_active: !reminder.is_active });

            res.status(200).json({
                success: true,
                message: `Reminder ${reminder.is_active ? 'activated' : 'deactivated'} successfully`,
                data: reminder,
            });
        } catch (error) {
            console.error('Error toggling reminder:', error);
            res.status(500).json({
                success: false,
                message: error.message,
            });
        }
    }

    // Get notification history for current user
    async getNotificationHistory(req, res) {
        try {
            const userId = req.user.id;

            const notifications = await Notifikasi.findAll({
                where: { ID_User: userId },
                order: [['Tanggal', 'DESC'], ['ID_Notifikasi', 'DESC']],
                limit: 50,
            });

            res.status(200).json({
                success: true,
                data: notifications,
            });
        } catch (error) {
            console.error('Error getting notification history:', error);
            res.status(500).json({
                success: false,
                message: error.message,
            });
        }
    }
}

module.exports = new NotificationController();

