package au.com.penrithbinapp.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import au.com.penrithbinapp.data.BinRepository
import au.com.penrithbinapp.data.ScheduleCalculator
import au.com.penrithbinapp.widget.BinAppWidgetProvider
import java.time.LocalDate

class BinReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        if (action == ACTION_MARK_BINS_PUT_OUT) {
            val dateIso = intent.getStringExtra(BinNotificationHelper.EXTRA_COLLECTION_DATE_ISO)
            if (!dateIso.isNullOrBlank()) {
                val repo = BinRepository.getInstance(context)
                repo.updateUserSettings { it.copy(checkedOffCollectionDateIso = dateIso) }
            }
            BinNotificationHelper.dismissActiveReminderNotifications(context)
            BinNotificationHelper.rescheduleReminders(context)
            BinAppWidgetProvider.updateAllWidgets(context)
            return
        }

        if (action == ACTION_MARK_BINS_BROUGHT_IN) {
            val repo = BinRepository.getInstance(context)
            repo.updateUserSettings { it.copy(checkedOffCollectionDateIso = null) }
            BinNotificationHelper.dismissActiveReminderNotifications(context)
            BinNotificationHelper.rescheduleReminders(context)
            BinAppWidgetProvider.updateAllWidgets(context)
            return
        }

        if (action == ACTION_BIN_REMINDER) {
            val reminderType = intent.getStringExtra(BinNotificationHelper.EXTRA_REMINDER_TYPE)
                ?: BinNotificationHelper.TYPE_EVENING
            val isEvening = reminderType == BinNotificationHelper.TYPE_EVENING
            val isBringIn = reminderType == BinNotificationHelper.TYPE_BRING_IN

            val repo = BinRepository.getInstance(context)
            val schedule = repo.loadCachedSchedule()
            val settings = repo.loadUserSettings()

            if (schedule != null && settings.notificationsEnabled) {
                val targetDate = if (isEvening) {
                    LocalDate.now().plusDays(1)
                } else {
                    LocalDate.now()
                }
                val nextCollection = ScheduleCalculator.getNextCollection(
                    scheduleData = schedule,
                    settings = settings,
                    referenceDate = targetDate
                )

                if (nextCollection != null) {
                    val alreadyCheckedOff =
                        settings.checkedOffCollectionDateIso == nextCollection.date.toString()
                    // For evening/morning put-out reminders, skip if already marked put out.
                    // For afternoon bring-in reminders, always notify so the user remembers to wheel them back in.
                    if (isBringIn || !alreadyCheckedOff) {
                        BinNotificationHelper.showCollectionNotification(
                            context = context,
                            collectionDay = nextCollection,
                            address = schedule.address,
                            isEveningReminder = isEvening,
                            isBringInReminder = isBringIn,
                            isTest = false
                        )
                    }
                }
            }
        }

        // Reschedule the next reminder and refresh any active Home Screen Widgets
        BinNotificationHelper.rescheduleReminders(context)
        BinAppWidgetProvider.updateAllWidgets(context)
    }

    companion object {
        const val ACTION_BIN_REMINDER = "au.com.penrithbinapp.ACTION_BIN_REMINDER"
        const val ACTION_MARK_BINS_PUT_OUT = "au.com.penrithbinapp.ACTION_MARK_BINS_PUT_OUT"
        const val ACTION_MARK_BINS_BROUGHT_IN = "au.com.penrithbinapp.ACTION_MARK_BINS_BROUGHT_IN"
    }
}
