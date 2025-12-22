package com.example.stora.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.stora.data.AppDatabase
import com.example.stora.utils.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "=== Device boot completed, rescheduling reminders ===")
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val database = AppDatabase.getDatabase(context)
                    val tokenManager = TokenManager(context)
                    val userId = tokenManager.getUserId()
                    
                    if (userId == -1) {
                        Log.d(TAG, "No user logged in, skipping reschedule")
                        return@launch
                    }
                    
                    val reminders = database.reminderDao().getActiveReminders(userId).first()
                    Log.d(TAG, "Found ${reminders.size} active reminders to reschedule")
                    
                    reminders.forEach { reminder ->
                        if (reminder.reminderType == "custom" && reminder.scheduledDatetime != null) {
                            if (reminder.scheduledDatetime > System.currentTimeMillis()) {
                                ReminderAlarmManager.scheduleExactAlarm(
                                    context = context,
                                    reminderId = reminder.id,
                                    reminderTitle = reminder.title ?: "Pengingat",
                                    reminderType = reminder.reminderType,
                                    serverReminderId = reminder.serverId,
                                    scheduledTimeMillis = reminder.scheduledDatetime
                                )
                                Log.d(TAG, "✓ Rescheduled: ${reminder.title}")
                            } else {
                                Log.d(TAG, "⏭ Skipped (past): ${reminder.title}")
                            }
                        }
                    }
                    
                    ReminderScheduler.schedulePeriodicCheck(context)
                    
                    rescheduleLoanAlarms(context, database, userId)
                    
                    Log.d(TAG, "✓ All reminders and loan alarms rescheduled after boot")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error rescheduling reminders after boot", e)
                }
            }
        }
    }
    
    private suspend fun rescheduleLoanAlarms(
        context: Context,
        database: AppDatabase,
        userId: Int
    ) {
        try {
            val loanDao = database.loanDao()
            
            val activeLoans = loanDao.getActiveLoans(userId).first()
            Log.d(TAG, "Found ${activeLoans.size} active loans to reschedule alarms")
            
            val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
            
            activeLoans.forEach { loan ->
                try {
                    val deadlineDate = dateFormat.parse(loan.tanggalKembali)
                    val deadlineTimeMillis = deadlineDate?.time ?: return@forEach
                    
                    val oneHourAfterDeadline = deadlineTimeMillis + (60 * 60 * 1000)
                    if (oneHourAfterDeadline > System.currentTimeMillis()) {
                        ReminderAlarmManager.scheduleLoanAlarms(
                            context = context,
                            loanId = loan.id,
                            loanServerId = loan.serverId,
                            borrowerName = loan.namaPeminjam,
                            deadlineTimeMillis = deadlineTimeMillis
                        )
                        Log.d(TAG, "✓ Rescheduled loan alarms: ${loan.namaPeminjam}")
                    } else {
                        Log.d(TAG, "⏭ Skipped loan (past deadline+1hr): ${loan.namaPeminjam}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing date for loan ${loan.id}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error rescheduling loan alarms", e)
        }
    }
}
