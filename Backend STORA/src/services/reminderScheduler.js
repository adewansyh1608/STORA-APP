const cron = require('node-cron');
const { ReminderSetting, User, Notifikasi, Peminjaman, UserDevice } = require('../models');
const { sendPushNotification, sendMultipleNotifications } = require('./firebaseAdmin');
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

            // Check loan return deadlines
            await this.checkLoanDeadlines(now);
        } catch (error) {
            console.error('Error in reminder scheduler:', error);
        }
    }

    /**
     * Send notification to all active devices for a user
     * @param {number} userId - The user ID
     * @param {string} title - Notification title
     * @param {string} body - Notification body
     * @param {object} data - Additional data to send
     * @returns {object} Result with success count and failure count
     */
    async sendToAllUserDevices(userId, title, body, data = {}) {
        try {
            // Get all active device tokens for this user
            const devices = await UserDevice.findAll({
                where: {
                    ID_User: userId,
                    Is_Active: true,
                    FCM_Token: { [Op.ne]: null }
                }
            });

            if (devices.length === 0) {
                console.log(`⚠ No active devices found for user ${userId}`);
                return { success: false, error: 'No active devices' };
            }

            const tokens = devices.map(d => d.FCM_Token).filter(t => t);
            console.log(`📱 Sending notification to ${tokens.length} devices for user ${userId}`);

            if (tokens.length === 1) {
                // Single device - use single notification
                return await sendPushNotification(tokens[0], title, body, data);
            } else {
                // Multiple devices - use multicast
                return await sendMultipleNotifications(tokens, title, body, data);
            }
        } catch (error) {
            console.error(`Error sending to all user devices for user ${userId}:`, error);
            return { success: false, error: error.message };
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
                // FIXED: Use last_notified if exists, otherwise use created_at as baseline
                // This prevents new reminders from immediately firing
                let baseline;
                if (reminder.last_notified) {
                    baseline = new Date(reminder.last_notified);
                } else if (reminder.created_at) {
                    baseline = new Date(reminder.created_at);
                } else {
                    // Fallback to now - should not happen but just in case
                    baseline = now;
                }

                const monthsSinceBaseline = this.getMonthsDifference(baseline, now);

                console.log(`Periodic reminder ${reminder.ID_Reminder}: baseline=${baseline.toISOString()}, months_since=${monthsSinceBaseline}, periodic_months=${reminder.periodic_months}`);

                // Only fire if enough months have passed since baseline
                if (monthsSinceBaseline >= reminder.periodic_months) {
                    console.log(`✓ Sending periodic reminder ${reminder.ID_Reminder} (${monthsSinceBaseline} months since baseline)`);

                    // Send to ALL user devices (multi-device support)
                    const result = await this.sendToAllUserDevices(
                        reminder.ID_User,
                        reminder.title || 'Pengingat Pengecekan Inventory',
                        `Sudah waktunya untuk melakukan pengecekan inventory! (Setiap ${reminder.periodic_months} bulan)`,
                        {
                            type: 'periodic_reminder',
                            reminder_id: reminder.ID_Reminder.toString(),
                        }
                    );

                    if (result.success || result.successCount > 0) {
                        // Update last_notified timestamp
                        await reminder.update({ last_notified: now });
                        console.log(`✓ Periodic reminder ${reminder.ID_Reminder} sent to all devices and updated`);
                    }
                } else {
                    console.log(`⏭ Skipping periodic reminder ${reminder.ID_Reminder} - only ${monthsSinceBaseline} months since baseline, needs ${reminder.periodic_months}`);
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

                // Send to ALL user devices (multi-device support)
                const result = await this.sendToAllUserDevices(
                    reminder.ID_User,
                    reminder.title || 'Pengingat Pengecekan Inventory',
                    'Cek Inventory Anda',
                    {
                        type: 'custom_reminder',
                        reminder_id: reminder.ID_Reminder.toString(),
                    }
                );

                if (result.success || result.successCount > 0) {
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
                    console.log(`✓ Custom reminder ${reminder.ID_Reminder} sent to all devices and deleted`);
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

    // Check loan return deadlines and send notifications
    async checkLoanDeadlines(now) {
        try {
            // Get all active loans (status = 'Dipinjam')
            const activeLoans = await Peminjaman.findAll({
                where: {
                    Status: 'Dipinjam',
                    Tanggal_Kembali: { [Op.ne]: null }
                },
                include: [{
                    model: User,
                    as: 'user',
                    attributes: ['ID_User', 'Nama_User', 'FCM_Token']
                }]
            });

            console.log(`📋 Found ${activeLoans.length} active loans to check for deadlines`);

            const today = new Date(now);
            today.setHours(0, 0, 0, 0);

            const currentHour = now.getHours();
            const currentMinute = now.getMinutes();

            console.log(`⏰ Current time: ${currentHour}:${currentMinute.toString().padStart(2, '0')}`);

            for (const loan of activeLoans) {
                const hasFcmToken = loan.user && loan.user.FCM_Token;
                console.log(`  Loan ${loan.ID_Peminjaman}: ${loan.Nama_Peminjam}, deadline: ${loan.Tanggal_Kembali}, FCM: ${hasFcmToken ? 'YES' : 'NO'}`);

                if (!hasFcmToken) {
                    console.log(`  ⚠ Skipping loan ${loan.ID_Peminjaman} - no FCM token`);
                    continue;
                }

                const deadlineDate = new Date(loan.Tanggal_Kembali);
                const deadlineDateOnly = new Date(deadlineDate);
                deadlineDateOnly.setHours(0, 0, 0, 0);

                const deadlineHour = deadlineDate.getHours();
                const deadlineMinute = deadlineDate.getMinutes();

                // Check if today is the deadline day
                const isDeadlineDay = today.getTime() === deadlineDateOnly.getTime();
                console.log(`  📅 Deadline: ${deadlineHour}:${deadlineMinute.toString().padStart(2, '0')}, isDeadlineDay: ${isDeadlineDay}`);

                if (isDeadlineDay) {
                    // Check for 07:00 AM morning reminder (trigger at 07:00)
                    if (currentHour === 7 && currentMinute === 0) {
                        await this.sendMorningReminder(loan, now);
                    }

                    // Check for exact deadline time reminder (or if past deadline time)
                    const currentTotalMinutes = currentHour * 60 + currentMinute;
                    const deadlineTotalMinutes = deadlineHour * 60 + deadlineMinute;

                    // Send if we're at or past the deadline time
                    if (currentTotalMinutes >= deadlineTotalMinutes) {
                        await this.sendDeadlineReminder(loan, now);
                    }
                }
            }
        } catch (error) {
            console.error('❌ Error checking loan deadlines:', error);
        }
    }

    async sendMorningReminder(loan, now) {
        try {
            // Check if we already sent the morning reminder today
            const todayStart = new Date(now);
            todayStart.setHours(0, 0, 0, 0);

            const existingMorningNotif = await Notifikasi.findOne({
                where: {
                    ID_User: loan.ID_User,
                    ID_Peminjaman: loan.ID_Peminjaman,
                    Judul: 'Deadline Pengembalian',
                    Pesan: { [Op.like]: 'Hari ini adalah pengembalian peminjaman%' },
                    Tanggal: { [Op.gte]: todayStart }
                }
            });

            if (existingMorningNotif) {
                console.log(`  ℹ Morning reminder already sent for loan ${loan.ID_Peminjaman}`);
                return;
            }

            // Send morning reminder
            const morningMessage = `Hari ini adalah pengembalian peminjaman atas nama "${loan.Nama_Peminjam}"`;

            console.log(`  📤 Sending morning reminder for loan ${loan.ID_Peminjaman}...`);

            // Send to ALL user devices (multi-device support)
            const result = await this.sendToAllUserDevices(
                loan.ID_User,
                'Deadline Pengembalian',
                morningMessage,
                {
                    type: 'loan_deadline_reminder',
                    loan_id: loan.ID_Peminjaman.toString(),
                    borrower_name: loan.Nama_Peminjam
                }
            );

            // Save notification to database regardless of FCM result
            await Notifikasi.create({
                Judul: 'Deadline Pengembalian',
                Pesan: morningMessage,
                Tanggal: now,
                Status: (result.success || result.successCount > 0) ? 'Terkirim' : 'Gagal',
                ID_User: loan.ID_User,
                ID_Peminjaman: loan.ID_Peminjaman,
                isSynced: true
            });

            if (result.success || result.successCount > 0) {
                console.log(`  ✓ Morning deadline reminder sent to all devices for loan ${loan.ID_Peminjaman} (${loan.Nama_Peminjam})`);
            } else {
                console.log(`  ⚠ Morning reminder saved to DB but FCM failed for loan ${loan.ID_Peminjaman}`);
            }
        } catch (error) {
            console.error(`  ❌ Error sending morning reminder for loan ${loan.ID_Peminjaman}:`, error);
        }
    }

    async sendDeadlineReminder(loan, now) {
        try {
            // Check if we already sent the deadline reminder today
            const todayStart = new Date(now);
            todayStart.setHours(0, 0, 0, 0);

            const existingDeadlineNotif = await Notifikasi.findOne({
                where: {
                    ID_User: loan.ID_User,
                    ID_Peminjaman: loan.ID_Peminjaman,
                    Judul: 'Deadline Pengembalian',
                    Pesan: { [Op.like]: '%sudah melewati batas peminjaman%' },
                    Tanggal: { [Op.gte]: todayStart }
                }
            });

            if (existingDeadlineNotif) {
                console.log(`  ℹ Deadline reminder already sent for loan ${loan.ID_Peminjaman}`);
                return;
            }

            // Send overdue notification
            const overdueMessage = `Peminjaman atas nama "${loan.Nama_Peminjam}" sudah melewati batas peminjaman`;

            console.log(`  📤 Sending deadline reminder for loan ${loan.ID_Peminjaman}...`);

            // Send to ALL user devices (multi-device support)
            const result = await this.sendToAllUserDevices(
                loan.ID_User,
                'Deadline Pengembalian',
                overdueMessage,
                {
                    type: 'loan_overdue',
                    loan_id: loan.ID_Peminjaman.toString(),
                    borrower_name: loan.Nama_Peminjam
                }
            );

            // Save notification to database regardless of FCM result
            await Notifikasi.create({
                Judul: 'Deadline Pengembalian',
                Pesan: overdueMessage,
                Tanggal: now,
                Status: (result.success || result.successCount > 0) ? 'Terkirim' : 'Gagal',
                ID_User: loan.ID_User,
                ID_Peminjaman: loan.ID_Peminjaman,
                isSynced: true
            });

            if (result.success || result.successCount > 0) {
                console.log(`  ✓ Deadline reminder sent to all devices for loan ${loan.ID_Peminjaman} (${loan.Nama_Peminjam})`);
            } else {
                console.log(`  ⚠ Deadline reminder saved to DB but FCM failed for loan ${loan.ID_Peminjaman}`);
            }
        } catch (error) {
            console.error(`  ❌ Error sending deadline reminder for loan ${loan.ID_Peminjaman}:`, error);
        }
    }
}

module.exports = new ReminderScheduler();

