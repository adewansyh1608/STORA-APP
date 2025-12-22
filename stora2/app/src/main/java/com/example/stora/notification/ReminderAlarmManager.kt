package com.example.stora.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

object ReminderAlarmManager {
    private const val TAG = "ReminderAlarmManager"
    
    fun scheduleExactAlarm(
        context: Context,
        reminderId: String,
        reminderTitle: String,
        reminderType: String,
        serverReminderId: Int?,
        scheduledTimeMillis: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Cannot schedule exact alarms - permission not granted")
                return
            }
        }
        
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_TITLE, reminderTitle)
            putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_TYPE, reminderType)
            putExtra(ReminderAlarmReceiver.EXTRA_SERVER_REMINDER_ID, serverReminderId ?: -1)
            putExtra(ReminderAlarmReceiver.EXTRA_SCHEDULED_TIME, scheduledTimeMillis)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    scheduledTimeMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    scheduledTimeMillis,
                    pendingIntent
                )
            }
            
            val delay = (scheduledTimeMillis - System.currentTimeMillis()) / 1000 / 60
            Log.d(TAG, "✓ Exact alarm scheduled for $reminderTitle in $delay minutes")
            
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException scheduling alarm - falling back to WorkManager", e)
        }
    }
    
    fun cancelAlarm(context: Context, reminderId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val intent = Intent(context, ReminderAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "✓ Alarm cancelled for reminder $reminderId")
    }
    
    fun isAlarmScheduled(context: Context, reminderId: String): Boolean {
        val intent = Intent(context, ReminderAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        return pendingIntent != null
    }
    
    fun scheduleLoanAlarms(
        context: Context,
        loanId: String,
        loanServerId: Int?,
        borrowerName: String,
        deadlineTimeMillis: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Cannot schedule exact alarms for loans - permission not granted")
                return
            }
        }
        
        val currentTime = System.currentTimeMillis()
        
        val oneHourBefore = deadlineTimeMillis - (60 * 60 * 1000)
        val atDeadline = deadlineTimeMillis
        val oneHourAfter = deadlineTimeMillis + (60 * 60 * 1000)
        
        if (oneHourBefore > currentTime) {
            scheduleLoanAlarm(context, alarmManager, loanId, loanServerId, borrowerName, oneHourBefore, LoanAlarmReceiver.TYPE_WARNING)
        }
        
        if (atDeadline > currentTime) {
            scheduleLoanAlarm(context, alarmManager, loanId, loanServerId, borrowerName, atDeadline, LoanAlarmReceiver.TYPE_DEADLINE)
        }
        
        if (oneHourAfter > currentTime) {
            scheduleLoanAlarm(context, alarmManager, loanId, loanServerId, borrowerName, oneHourAfter, LoanAlarmReceiver.TYPE_OVERDUE)
        }
        
        Log.d(TAG, "✓ Scheduled loan alarms for $borrowerName (loan: $loanId)")
    }
    
    private fun scheduleLoanAlarm(
        context: Context,
        alarmManager: AlarmManager,
        loanId: String,
        loanServerId: Int?,
        borrowerName: String,
        scheduledTimeMillis: Long,
        notificationType: String
    ) {
        val intent = Intent(context, LoanAlarmReceiver::class.java).apply {
            putExtra(LoanAlarmReceiver.EXTRA_LOAN_ID, loanId)
            putExtra(LoanAlarmReceiver.EXTRA_LOAN_SERVER_ID, loanServerId ?: -1)
            putExtra(LoanAlarmReceiver.EXTRA_BORROWER_NAME, borrowerName)
            putExtra(LoanAlarmReceiver.EXTRA_NOTIFICATION_TYPE, notificationType)
            putExtra(LoanAlarmReceiver.EXTRA_SCHEDULED_TIME, scheduledTimeMillis)
        }
        
        val requestCode = (loanId.hashCode() + notificationType.hashCode()) and 0x7FFFFFFF
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    scheduledTimeMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    scheduledTimeMillis,
                    pendingIntent
                )
            }
            
            val delay = (scheduledTimeMillis - System.currentTimeMillis()) / 1000 / 60
            Log.d(TAG, "  ✓ Loan alarm ($notificationType) scheduled in $delay minutes")
            
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException scheduling loan alarm", e)
        }
    }
    
    fun cancelLoanAlarms(context: Context, loanId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val types = listOf(
            LoanAlarmReceiver.TYPE_WARNING,
            LoanAlarmReceiver.TYPE_DEADLINE,
            LoanAlarmReceiver.TYPE_OVERDUE
        )
        
        types.forEach { notificationType ->
            val intent = Intent(context, LoanAlarmReceiver::class.java)
            val requestCode = (loanId.hashCode() + notificationType.hashCode()) and 0x7FFFFFFF
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            alarmManager.cancel(pendingIntent)
        }
        
        Log.d(TAG, "✓ All loan alarms cancelled for loan $loanId")
    }
}
