const { ReminderSetting, User, Notifikasi, UserDevice } = require('../models');
const { sendPushNotification } = require('../services/firebaseAdmin');
const { validationResult } = require('express-validator');

class NotificationController {
    async registerToken(req, res) {
        try {
            const { fcm_token, device_name } = req.body;
            const userId = req.user.id;

            if (!fcm_token) {
                return res.status(400).json({
                    success: false,
                    message: 'FCM token is required',
                });
            }

            const existingDevice = await UserDevice.findOne({
                where: { ID_User: userId, FCM_Token: fcm_token }
            });

            if (existingDevice) {
                await existingDevice.update({
                    Device_Name: device_name || existingDevice.Device_Name || 'Unknown Device',
                    Last_Active: new Date(),
                    Is_Active: true,
                });
            } else {
                await UserDevice.create({
                    ID_User: userId,
                    FCM_Token: fcm_token,
                    Device_Name: device_name || 'Unknown Device',
                    Last_Active: new Date(),
                    Is_Active: true,
                });
            }

            await User.update(
                { FCM_Token: fcm_token },
                { where: { ID_User: userId } }
            );

            await ReminderSetting.update(
                { fcm_token },
                { where: { ID_User: userId, is_active: true } }
            );

            console.log(`✓ FCM token registered for user ${userId} (device: ${device_name || 'Unknown'})`);

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

    async getReminders(req, res) {
        try {
            const userId = req.user.id;

            const reminders = await ReminderSetting.findAll({
                where: { ID_User: userId },
                order: [['created_at', 'DESC']],
            });

            const transformedReminders = reminders.map(reminder => {
                const plainReminder = reminder.toJSON();

                if (plainReminder.scheduled_datetime) {
                    const dateObj = new Date(plainReminder.scheduled_datetime);
                    plainReminder.scheduled_datetime = dateObj.getTime().toString();
                }

                if (plainReminder.last_notified) {
                    const dateObj = new Date(plainReminder.last_notified);
                    plainReminder.last_notified = dateObj.getTime().toString();
                }

                return plainReminder;
            });

            res.status(200).json({
                success: true,
                data: transformedReminders,
            });
        } catch (error) {
            console.error('Error getting reminders:', error);
            res.status(500).json({
                success: false,
                message: error.message,
            });
        }
    }

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

            if (reminder_type === 'periodic') {
                const existingPeriodicReminder = await ReminderSetting.findOne({
                    where: {
                        ID_User: userId,
                        reminder_type: 'periodic',
                    },
                });

                if (existingPeriodicReminder) {
                    await existingPeriodicReminder.update({
                        title: title || 'Pengingat Pengecekan Inventory',
                        periodic_months: periodic_months || 3,
                        fcm_token: fcm_token || existingPeriodicReminder.fcm_token,
                        is_active: true,
                        last_notified: new Date(),
                    });

                    console.log(`✓ Periodic reminder updated: ${existingPeriodicReminder.ID_Reminder} for user ${userId}, months: ${periodic_months}`);

                    return res.status(200).json({
                        success: true,
                        message: `Pengingat periodik diperbarui ke ${periodic_months} bulan`,
                        data: existingPeriodicReminder,
                    });
                }
            }

            let parsedDatetime = null;
            if (reminder_type === 'custom' && scheduled_datetime) {
                const numericValue = parseInt(scheduled_datetime, 10);
                if (!isNaN(numericValue) && numericValue > 1000000000000) {
                    parsedDatetime = new Date(numericValue);
                    console.log(`Parsed timestamp (ms): ${scheduled_datetime} -> ${parsedDatetime.toISOString()}`);
                } else if (!isNaN(numericValue) && numericValue > 1000000000) {
                    parsedDatetime = new Date(numericValue * 1000);
                    console.log(`Parsed timestamp (s): ${scheduled_datetime} -> ${parsedDatetime.toISOString()}`);
                } else {
                    parsedDatetime = new Date(scheduled_datetime);
                    console.log(`Parsed date string: ${scheduled_datetime} -> ${parsedDatetime.toISOString()}`);
                }
            }

            const reminderData = {
                ID_User: userId,
                reminder_type,
                title: title || 'Pengingat Pengecekan Inventory',
                periodic_months: reminder_type === 'periodic' ? (periodic_months || 3) : null,
                scheduled_datetime: parsedDatetime,
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

            if (updateData.periodic_months && reminder.reminder_type === 'periodic') {
                updateData.last_notified = new Date();
            }

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

    async createNotificationHistory(req, res) {
        try {
            const userId = req.user.id;
            const { Judul, Pesan, Tanggal, Status, ID_Reminder, timestamp } = req.body;

            let parsedDate;
            if (timestamp) {
                const numericValue = parseInt(timestamp, 10);
                if (!isNaN(numericValue) && numericValue > 1000000000000) {
                    parsedDate = new Date(numericValue);
                } else if (!isNaN(numericValue) && numericValue > 1000000000) {
                    parsedDate = new Date(numericValue * 1000);
                } else {
                    parsedDate = new Date(timestamp);
                }
            } else if (Tanggal) {
                parsedDate = new Date(Tanggal);
            } else {
                parsedDate = new Date();
            }

            console.log(`Creating notification: Judul=${Judul}, timestamp=${timestamp}, parsedDate=${parsedDate.toISOString()}`);

            if (ID_Reminder) {
                const startOfDay = new Date(parsedDate);
                startOfDay.setHours(0, 0, 0, 0);
                const endOfDay = new Date(parsedDate);
                endOfDay.setHours(23, 59, 59, 999);

                const existingNotification = await Notifikasi.findOne({
                    where: {
                        ID_User: userId,
                        ID_Reminder: ID_Reminder,
                        Tanggal: {
                            [require('sequelize').Op.between]: [startOfDay, endOfDay]
                        }
                    }
                });

                if (existingNotification) {
                    console.log(`✓ Duplicate notification found for reminder ${ID_Reminder}, returning existing`);
                    return res.status(200).json({
                        success: true,
                        message: 'Notification already exists',
                        data: existingNotification,
                        isDuplicate: true,
                    });
                }
            }

            const notification = await Notifikasi.create({
                ID_User: userId,
                Judul: Judul || 'Notifikasi',
                Pesan: Pesan || '',
                Tanggal: parsedDate,
                Status: 'Terkirim',
                ID_Reminder: ID_Reminder || null,
                isSynced: true,
            });

            console.log(`✓ Notification history created for user ${userId}: ${Judul} at ${parsedDate.toISOString()}`);

            res.status(201).json({
                success: true,
                message: 'Notification history created successfully',
                data: notification,
            });
        } catch (error) {
            console.error('Error creating notification history:', error);
            res.status(500).json({
                success: false,
                message: error.message,
            });
        }
    }
}

module.exports = new NotificationController();
