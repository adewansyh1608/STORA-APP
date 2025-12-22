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

        cron.schedule('* * * * *', async () => {
            console.log('⏰ Running scheduled reminder check...');
            await this.checkAndSendReminders();
        });

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

            await this.checkPeriodicReminders(now);
            await this.checkCustomReminders(now);
            await this.checkLoanDeadlines(now);
        } catch (error) {
            console.error('Error in reminder scheduler:', error);
        }
    }

    async sendToAllUserDevices(userId, title, body, data = {}) {
        try {
            const devices = await UserDevice.findAll({
                where: {
                    ID_User: userId,
                    Is_Active: true,
                    FCM_Token: { [Op.ne]: null }
                }
            });

            let tokens = devices.map(d => d.FCM_Token).filter(t => t);

            if (tokens.length === 0) {
                console.log(`  ⚠ No devices in UserDevice table, checking User.FCM_Token...`);
                const user = await User.findOne({
                    where: { ID_User: userId },
                    attributes: ['FCM_Token']
                });

                if (user && user.FCM_Token) {
                    tokens = [user.FCM_Token];
                    console.log(`  ✓ Found FCM token in User table`);
                } else {
                    console.log(`  ⚠ No FCM token found for user ${userId}`);
                    return { success: false, error: 'No FCM token available' };
                }
            }

            console.log(`📱 Sending notification to ${tokens.length} devices for user ${userId}`);

            if (tokens.length === 1) {
                return await sendPushNotification(tokens[0], title, body, data);
            } else {
                return await sendMultipleNotifications(tokens, title, body, data);
            }
        } catch (error) {
            console.error(`Error sending to all user devices for user ${userId}:`, error);
            return { success: false, error: error.message };
        }
    }

    async checkPeriodicReminders(now) {
        try {
            const periodicReminders = await ReminderSetting.findAll({
                where: {
                    reminder_type: 'periodic',
                    is_active: true,
                    fcm_token: { [Op.ne]: null },
                },
            });

            console.log(`Found ${periodicReminders.length} active periodic reminders`);

            for (const reminder of periodicReminders) {
                let baseline;
                if (reminder.last_notified) {
                    baseline = new Date(reminder.last_notified);
                } else if (reminder.created_at) {
                    baseline = new Date(reminder.created_at);
                } else {
                    baseline = now;
                }

                const monthsSinceBaseline = this.getMonthsDifference(baseline, now);

                console.log(`Periodic reminder ${reminder.ID_Reminder}: baseline=${baseline.toISOString()}, months_since=${monthsSinceBaseline}, periodic_months=${reminder.periodic_months}`);

                if (monthsSinceBaseline >= reminder.periodic_months) {
                    console.log(`✓ Sending periodic reminder ${reminder.ID_Reminder} (${monthsSinceBaseline} months since baseline)`);

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
            const customReminders = await ReminderSetting.findAll({
                where: {
                    reminder_type: 'custom',
                    is_active: true,
                    fcm_token: { [Op.ne]: null },
                    scheduled_datetime: {
                        [Op.lte]: now,
                    },
                    last_notified: {
                        [Op.eq]: null,
                    },
                },
            });

            console.log(`Found ${customReminders.length} custom reminders due`);

            for (const reminder of customReminders) {
                const reminderTitle = reminder.title || 'Pengingat Pengecekan Inventory';
                const reminderMessage = `Waktu pengingat: ${reminderTitle}`;

                console.log(`Sending custom reminder ${reminder.ID_Reminder}, scheduled for ${reminder.scheduled_datetime}`);

                const result = await this.sendToAllUserDevices(
                    reminder.ID_User,
                    reminderTitle,
                    reminderMessage,
                    {
                        type: 'custom_reminder',
                        reminder_id: reminder.ID_Reminder.toString(),
                        reminder_timestamp: reminder.scheduled_datetime ?
                            new Date(reminder.scheduled_datetime).getTime().toString() :
                            now.getTime().toString(),
                    }
                );

                if (result.success || result.successCount > 0) {
                    await Notifikasi.create({
                        Judul: reminderTitle,
                        Pesan: reminderMessage,
                        Tanggal: reminder.scheduled_datetime || now,
                        Status: 'Terkirim',
                        ID_User: reminder.ID_User,
                        ID_Reminder: reminder.ID_Reminder,
                        isSynced: true,
                    });
                    console.log(`✓ Notification history saved for custom reminder ${reminder.ID_Reminder}`);

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

    async checkLoanDeadlines(now) {
        try {
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

            const currentTime = now.getTime();

            for (const loan of activeLoans) {
                const hasFcmToken = loan.user && loan.user.FCM_Token;

                if (!hasFcmToken) {
                    continue;
                }

                const deadlineDate = new Date(loan.Tanggal_Kembali);
                const deadlineTime = deadlineDate.getTime();

                const oneHourBefore = deadlineTime - (60 * 60 * 1000);
                const atDeadline = deadlineTime;
                const oneHourAfter = deadlineTime + (60 * 60 * 1000);

                const nowMinute = Math.floor(currentTime / 60000) * 60000;
                const oneMinuteWindow = 60000;

                console.log(`  Loan ${loan.ID_Peminjaman}: ${loan.Nama_Peminjam}, deadline: ${deadlineDate.toISOString()}`);

                if (nowMinute >= oneHourBefore && nowMinute < oneHourBefore + oneMinuteWindow) {
                    await this.sendLoanNotification(loan, now, 'warning',
                        `Peminjaman "${loan.Nama_Peminjam}" akan deadline dalam 1 jam`,
                        'loan_deadline_warning');
                }

                if (nowMinute >= atDeadline && nowMinute < atDeadline + oneMinuteWindow) {
                    await this.sendLoanNotification(loan, now, 'deadline',
                        `Peminjaman "${loan.Nama_Peminjam}" sudah mencapai batas waktu pengembalian`,
                        'loan_deadline');
                }

                if (nowMinute >= oneHourAfter && nowMinute < oneHourAfter + oneMinuteWindow) {
                    await this.sendLoanNotification(loan, now, 'overdue',
                        `Peminjaman "${loan.Nama_Peminjam}" sudah terlambat 1 jam dari deadline`,
                        'loan_overdue');
                }
            }
        } catch (error) {
            console.error('❌ Error checking loan deadlines:', error);
        }
    }

    async sendLoanNotification(loan, now, notifType, message, fcmType) {
        try {
            const todayStart = new Date(now);
            todayStart.setHours(0, 0, 0, 0);

            const existingNotif = await Notifikasi.findOne({
                where: {
                    ID_User: loan.ID_User,
                    ID_Peminjaman: loan.ID_Peminjaman,
                    Pesan: message,
                    Tanggal: { [Op.gte]: todayStart }
                }
            });

            if (existingNotif) {
                console.log(`  ℹ ${notifType} notification already sent for loan ${loan.ID_Peminjaman}`);
                return;
            }

            console.log(`  📤 Sending ${notifType} notification for loan ${loan.ID_Peminjaman}...`);

            const result = await this.sendToAllUserDevices(
                loan.ID_User,
                'Pengingat Peminjaman',
                message,
                {
                    type: fcmType,
                    loan_id: loan.ID_Peminjaman.toString(),
                    borrower_name: loan.Nama_Peminjam,
                    loan_timestamp: new Date(loan.Tanggal_Kembali).getTime().toString()
                }
            );

            await Notifikasi.create({
                Judul: 'Pengingat Peminjaman',
                Pesan: message,
                Tanggal: now,
                Status: (result.success || result.successCount > 0) ? 'Terkirim' : 'Gagal',
                ID_User: loan.ID_User,
                ID_Peminjaman: loan.ID_Peminjaman,
                isSynced: true
            });

            if (result.success || result.successCount > 0) {
                console.log(`  ✓ ${notifType} notification sent for loan ${loan.ID_Peminjaman}`);
            } else {
                console.log(`  ⚠ ${notifType} notification saved but FCM failed for loan ${loan.ID_Peminjaman}`);
            }
        } catch (error) {
            console.error(`  ❌ Error sending ${notifType} notification for loan ${loan.ID_Peminjaman}:`, error);
        }
    }

    async sendMorningReminder(loan, now) {
        try {
            const todayStart = new Date(now);
            todayStart.setHours(0, 0, 0, 0);

            const existingPreDeadlineNotif = await Notifikasi.findOne({
                where: {
                    ID_User: loan.ID_User,
                    ID_Peminjaman: loan.ID_Peminjaman,
                    Judul: 'Pengingat Deadline',
                    Pesan: { [Op.like]: '%1 jam lagi%' },
                    Tanggal: { [Op.gte]: todayStart }
                }
            });

            if (existingPreDeadlineNotif) {
                console.log(`  ℹ Pre-deadline reminder already sent for loan ${loan.ID_Peminjaman}`);
                return;
            }

            const deadlineTimeStr = `${deadlineHour.toString().padStart(2, '0')}:${deadlineMinute.toString().padStart(2, '0')}`;

            const preDeadlineMessage = `Peminjaman atas nama "${loan.Nama_Peminjam}" akan jatuh tempo dalam 1 jam lagi`;

            console.log(`  📤 Sending pre-deadline reminder for loan ${loan.ID_Peminjaman}...`);

            const result = await this.sendToAllUserDevices(
                loan.ID_User,
                'Pengingat Deadline',
                preDeadlineMessage,
                {
                    type: 'loan_pre_deadline_reminder',
                    loan_id: loan.ID_Peminjaman.toString(),
                    borrower_name: loan.Nama_Peminjam,
                    deadline_time: deadlineTimeStr
                }
            );

            await Notifikasi.create({
                Judul: 'Pengingat Deadline',
                Pesan: preDeadlineMessage,
                Tanggal: now,
                Status: (result.success || result.successCount > 0) ? 'Terkirim' : 'Gagal',
                ID_User: loan.ID_User,
                ID_Peminjaman: loan.ID_Peminjaman,
                isSynced: true
            });

            if (result.success || result.successCount > 0) {
                console.log(`  ✓ Pre-deadline reminder sent to all devices for loan ${loan.ID_Peminjaman} (${loan.Nama_Peminjam})`);
            } else {
                console.log(`  ⚠ Pre-deadline reminder saved to DB but FCM failed for loan ${loan.ID_Peminjaman}`);
            }
        } catch (error) {
            console.error(`  ❌ Error sending pre-deadline reminder for loan ${loan.ID_Peminjaman}:`, error);
        }
    }

    async sendDeadlineReminder(loan, now, deadlineHour, deadlineMinute) {
        try {
            const todayStart = new Date(now);
            todayStart.setHours(0, 0, 0, 0);

            const existingDeadlineNotif = await Notifikasi.findOne({
                where: {
                    ID_User: loan.ID_User,
                    ID_Peminjaman: loan.ID_Peminjaman,
                    Judul: 'Deadline Pengembalian',
                    Pesan: { [Op.like]: '%sudah jatuh tempo%' },
                    Tanggal: { [Op.gte]: todayStart }
                }
            });

            if (existingDeadlineNotif) {
                console.log(`  ℹ Deadline reminder already sent for loan ${loan.ID_Peminjaman}`);
                return;
            }

            const deadlineTimeStr = `${deadlineHour.toString().padStart(2, '0')}:${deadlineMinute.toString().padStart(2, '0')}`;

            const deadlineMessage = `Peminjaman atas nama "${loan.Nama_Peminjam}" sudah jatuh tempo`;

            console.log(`  📤 Sending deadline reminder for loan ${loan.ID_Peminjaman}...`);

            const result = await this.sendToAllUserDevices(
                loan.ID_User,
                'Deadline Pengembalian',
                deadlineMessage,
                {
                    type: 'loan_deadline',
                    loan_id: loan.ID_Peminjaman.toString(),
                    borrower_name: loan.Nama_Peminjam,
                    deadline_time: deadlineTimeStr
                }
            );

            await Notifikasi.create({
                Judul: 'Deadline Pengembalian',
                Pesan: deadlineMessage,
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

    async sendOverdueReminder(loan, now, deadlineHour, deadlineMinute) {
        try {
            const todayStart = new Date(now);
            todayStart.setHours(0, 0, 0, 0);

            const existingOverdueNotif = await Notifikasi.findOne({
                where: {
                    ID_User: loan.ID_User,
                    ID_Peminjaman: loan.ID_Peminjaman,
                    Judul: 'Peminjaman Terlambat',
                    Pesan: { [Op.like]: '%sudah melewati batas waktu%' },
                    Tanggal: { [Op.gte]: todayStart }
                }
            });

            if (existingOverdueNotif) {
                console.log(`  ℹ Overdue reminder already sent for loan ${loan.ID_Peminjaman}`);
                return;
            }

            const deadlineTimeStr = `${deadlineHour.toString().padStart(2, '0')}:${deadlineMinute.toString().padStart(2, '0')}`;

            const overdueMessage = `Peminjaman atas nama "${loan.Nama_Peminjam}" sudah melewati batas waktu pengembalian`;

            console.log(`  📤 Sending overdue reminder for loan ${loan.ID_Peminjaman}...`);

            const result = await this.sendToAllUserDevices(
                loan.ID_User,
                'Peminjaman Terlambat',
                overdueMessage,
                {
                    type: 'loan_overdue',
                    loan_id: loan.ID_Peminjaman.toString(),
                    borrower_name: loan.Nama_Peminjam,
                    deadline_time: deadlineTimeStr
                }
            );

            await Notifikasi.create({
                Judul: 'Peminjaman Terlambat',
                Pesan: overdueMessage,
                Tanggal: now,
                Status: (result.success || result.successCount > 0) ? 'Terkirim' : 'Gagal',
                ID_User: loan.ID_User,
                ID_Peminjaman: loan.ID_Peminjaman,
                isSynced: true
            });

            if (result.success || result.successCount > 0) {
                console.log(`  ✓ Overdue reminder sent to all devices for loan ${loan.ID_Peminjaman} (${loan.Nama_Peminjam})`);
            } else {
                console.log(`  ⚠ Overdue reminder saved to DB but FCM failed for loan ${loan.ID_Peminjaman}`);
            }
        } catch (error) {
            console.error(`  ❌ Error sending overdue reminder for loan ${loan.ID_Peminjaman}:`, error);
        }
    }
}

module.exports = new ReminderScheduler();
