const cron = require('node-cron');
const { ReminderSetting, User, Notifikasi } = require('../models');
const { sendPushNotification } = require('./firebaseAdmin');
const { Op } = require('sequelize');

class ReminderScheduler {
    constructor() {
        this.isRunning = false;
    }

    start() {
        if (this.isRunning) {
            console.log('Reminder scheduler is already running');
            return;
        }

        // Run every minute to check custom reminders more frequently
        cron.schedule('* * * * *', async () => {
            console.log('⏰ Running scheduled reminder check...');
            await this.checkAndSendReminders();
        });

        // Also run immediately on startup after a short delay
        setTimeout(async () => {
            console.log('⏰ Running initial reminder check...');
            await this.checkAndSendReminders();
        }, 5000);

        this.isRunning = true;
        console.log('✓ Reminder scheduler started (runs every minute)');
    }

    async checkAndSendReminders() {
        try {
            const now = new Date();
            console.log(`Checking reminders at ${now.toISOString()}`);

            // Check periodic reminders
            await this.checkPeriodicReminders(now);

            // Check custom scheduled reminders
            await this.checkCustomReminders(now);
        } catch (error) {
            console.error('Error in reminder scheduler:', error);
        }
    }

    async checkPeriodicReminders(now) {
        try {
            // Find all active periodic reminders
            const periodicReminders = await ReminderSetting.findAll({
                where: {
                    reminder_type: 'periodic',
                    is_active: true,
                    fcm_token: { [Op.ne]: null },
                },
            });

            console.log(`Found ${periodicReminders.length} active periodic reminders`);

            for (const reminder of periodicReminders) {
                const lastNotified = reminder.last_notified ? new Date(reminder.last_notified) : new Date(0);
                const monthsSinceLastNotified = this.getMonthsDifference(lastNotified, now);

                console.log(`Periodic reminder ${reminder.ID_Reminder}: last_notified=${lastNotified.toISOString()}, months_since=${monthsSinceLastNotified}, periodic_months=${reminder.periodic_months}`);

                if (monthsSinceLastNotified >= reminder.periodic_months) {
                    console.log(`Sending periodic reminder ${reminder.ID_Reminder} (${monthsSinceLastNotified} months since last)`);

                    const result = await sendPushNotification(
                        reminder.fcm_token,
                        reminder.title || 'Pengingat Pengecekan Inventory',
                        `Sudah waktunya untuk melakukan pengecekan inventory! (Setiap ${reminder.periodic_months} bulan)`,
                        {
                            type: 'periodic_reminder',
                            reminder_id: reminder.ID_Reminder.toString(),
                        }
                    );

                    if (result.success) {
                        // Update last_notified timestamp
                        await reminder.update({ last_notified: now });
                        console.log(`✓ Periodic reminder ${reminder.ID_Reminder} sent and updated`);
                    }
                }
            }
        } catch (error) {
            console.error('Error checking periodic reminders:', error);
        }
    }

    async checkCustomReminders(now) {
        try {
            // Find custom reminders where scheduled_datetime has passed and not yet notified
            const customReminders = await ReminderSetting.findAll({
                where: {
                    reminder_type: 'custom',
                    is_active: true,
                    fcm_token: { [Op.ne]: null },
                    scheduled_datetime: {
                        [Op.lte]: now, // Scheduled time has passed or is now
                    },
                    last_notified: {
                        [Op.eq]: null, // Not yet notified
                    },
                },
            });

            console.log(`Found ${customReminders.length} custom reminders due`);

            for (const reminder of customReminders) {
                console.log(`Sending custom reminder ${reminder.ID_Reminder}, scheduled for ${reminder.scheduled_datetime}`);

                const result = await sendPushNotification(
                    reminder.fcm_token,
                    reminder.title || 'Pengingat Pengecekan Inventory',
                    'Cek Inventory Anda',
                    {
                        type: 'custom_reminder',
                        reminder_id: reminder.ID_Reminder.toString(),
                    }
                );

                if (result.success) {
                    // Save notification history to database
                    await Notifikasi.create({
                        Judul: reminder.title || 'Pengingat Pengecekan Inventory',
                        Pesan: 'Cek Inventory Anda',
                        Tanggal: now,
                        Status: 'Terkirim',
                        ID_User: reminder.ID_User,
                        isSynced: true,
                    });
                    console.log(`✓ Notification history saved for custom reminder ${reminder.ID_Reminder}`);

                    // Delete the custom reminder after successful notification (one-time)
                    await reminder.destroy();
                    console.log(`✓ Custom reminder ${reminder.ID_Reminder} sent and deleted`);
                }
            }
        } catch (error) {
            console.error('Error checking custom reminders:', error);
        }
    }

    getMonthsDifference(date1, date2) {
        const months =
            (date2.getFullYear() - date1.getFullYear()) * 12 +
            (date2.getMonth() - date1.getMonth());
        return months;
    }
}

module.exports = new ReminderScheduler();

