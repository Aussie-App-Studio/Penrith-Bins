package au.com.penrithbinapp.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import au.com.penrithbinapp.MainActivity
import au.com.penrithbinapp.R
import au.com.penrithbinapp.data.BinRepository
import au.com.penrithbinapp.data.ScheduleCalculator
import au.com.penrithbinapp.model.BinType
import au.com.penrithbinapp.model.ScheduledCollectionDay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object BinNotificationHelper {

    const val CHANNEL_ID = "penrith_bin_reminders"
    private const val NOTIFICATION_ID_REMINDER = 1001
    private const val NOTIFICATION_ID_TEST = 1002
    private const val REQUEST_CODE_EVENING = 2001
    private const val REQUEST_CODE_MORNING = 2002
    private const val REQUEST_CODE_MARK_PUT_OUT = 2003
    private const val REQUEST_CODE_BRING_IN = 2004

    const val EXTRA_REMINDER_TYPE = "extra_reminder_type"
    const val EXTRA_COLLECTION_DATE_ISO = "extra_collection_date_iso"
    const val TYPE_EVENING = "evening"
    const val TYPE_MORNING = "morning"
    const val TYPE_BRING_IN = "bring_in"

    private val shortDateFormatter = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)

    fun ensureChannelCreated(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Penrith Bin Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Evening and morning reminders for which Penrith Council bins to put out"
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showCollectionNotification(
        context: Context,
        collectionDay: ScheduledCollectionDay,
        address: String,
        isEveningReminder: Boolean,
        isBringInReminder: Boolean = false,
        isTest: Boolean = false
    ): Boolean {
        ensureChannelCreated(context)
        if (!hasNotificationPermission(context)) {
            return false
        }

        val binsListText = collectionDay.bins.joinToString(" + ") { binEmojiAndName(it) }
        val formattedDate = collectionDay.date.format(shortDateFormatter)

        val title = when {
            isTest -> "Test Reminder: ${collectionDay.bins.joinToString(" & ") { it.shortName }}"
            isBringInReminder -> "Bring Bins Back In! ($formattedDate)"
            isEveningReminder -> "Bin Night! Put out bins for $formattedDate"
            else -> "Bin Collection Today ($formattedDate)"
        }

        val detailText = buildString {
            if (isBringInReminder) {
                append("Remember to wheel your bins back in from the kerb: $binsListText\n")
            } else {
                append("Bins to put out: $binsListText\n")
            }
            append("Collection Date: $formattedDate\n")
            append("Property: $address")
            if (isTest) {
                append("\n(This is a test notification from Penrith Bin App)")
            }
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val actionIntent = Intent(context, BinReminderReceiver::class.java).apply {
            action = if (isBringInReminder) {
                BinReminderReceiver.ACTION_MARK_BINS_BROUGHT_IN
            } else {
                BinReminderReceiver.ACTION_MARK_BINS_PUT_OUT
            }
            putExtra(EXTRA_COLLECTION_DATE_ISO, collectionDay.date.toString())
        }
        val actionPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_MARK_PUT_OUT,
            actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_bin)
            .setContentTitle(title)
            .setContentText(
                if (isBringInReminder) {
                    "Time to wheel in: $binsListText"
                } else {
                    "$binsListText • $address"
                }
            )
            .setStyle(NotificationCompat.BigTextStyle().bigText(detailText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_notification_bin,
                if (isBringInReminder) "✓ Bins Brought In" else "✓ Mark Bins Put Out",
                actionPendingIntent
            )
            .setAutoCancel(true)
            .build()

        return runCatching {
            NotificationManagerCompat.from(context).notify(
                if (isTest) NOTIFICATION_ID_TEST else NOTIFICATION_ID_REMINDER,
                notification
            )
            true
        }.getOrDefault(false)
    }

    private fun binEmojiAndName(binType: BinType): String {
        return when (binType) {
            BinType.RED -> "🟥 Red (Waste)"
            BinType.YELLOW -> "🟨 Yellow (Recycling)"
            BinType.GREEN -> "🟩 Green (FOGO)"
        }
    }

    fun dismissActiveReminderNotifications(context: Context) {
        runCatching {
            val nm = NotificationManagerCompat.from(context.applicationContext)
            nm.cancel(NOTIFICATION_ID_REMINDER)
            nm.cancel(NOTIFICATION_ID_TEST)
        }
    }

    /**
     * Schedules (or cancels) the next Evening and Morning bin reminders via AlarmManager
     * based on the user's current schedule and settings.
     * Any collection date already marked as put out (checkedOffCollectionDateIso) is skipped.
     */
    fun rescheduleReminders(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val repo = BinRepository.getInstance(appContext)
        val schedule = repo.loadCachedSchedule()
        val settings = repo.loadUserSettings()

        // Cancel existing pending alarms first
        cancelAlarm(appContext, alarmManager, REQUEST_CODE_EVENING, TYPE_EVENING)
        cancelAlarm(appContext, alarmManager, REQUEST_CODE_MORNING, TYPE_MORNING)
        cancelAlarm(appContext, alarmManager, REQUEST_CODE_BRING_IN, TYPE_BRING_IN)

        if (schedule == null || !settings.notificationsEnabled) {
            return
        }

        val now = LocalDateTime.now()
        val allUpcomingDays = ScheduleCalculator.getScheduledDays(
            scheduleData = schedule,
            settings = settings,
            fromDate = now.toLocalDate(),
            toDate = now.toLocalDate().plusDays(35)
        )
        val unPutOutDays = allUpcomingDays.filter {
            it.date.toString() != settings.checkedOffCollectionDateIso
        }

        if (settings.eveningReminderEnabled) {
            val eveningTime = LocalTime.of(
                settings.eveningReminderHour.coerceIn(0, 23),
                settings.eveningReminderMinute.coerceIn(0, 59)
            )
            val nextEveningTrigger = unPutOutDays
                .map { LocalDateTime.of(it.date.minusDays(1), eveningTime) }
                .firstOrNull { it.isAfter(now.plusSeconds(5)) }

            if (nextEveningTrigger != null) {
                scheduleAlarmAt(
                    context = appContext,
                    alarmManager = alarmManager,
                    triggerDateTime = nextEveningTrigger,
                    requestCode = REQUEST_CODE_EVENING,
                    reminderType = TYPE_EVENING
                )
            }
        }

        if (settings.morningReminderEnabled) {
            val morningTime = LocalTime.of(
                settings.morningReminderHour.coerceIn(0, 23),
                settings.morningReminderMinute.coerceIn(0, 59)
            )
            val nextMorningTrigger = unPutOutDays
                .map { LocalDateTime.of(it.date, morningTime) }
                .firstOrNull { it.isAfter(now.plusSeconds(5)) }

            if (nextMorningTrigger != null) {
                scheduleAlarmAt(
                    context = appContext,
                    alarmManager = alarmManager,
                    triggerDateTime = nextMorningTrigger,
                    requestCode = REQUEST_CODE_MORNING,
                    reminderType = TYPE_MORNING
                )
            }
        }

        if (settings.bringInReminderEnabled) {
            val bringInTime = LocalTime.of(
                settings.bringInReminderHour.coerceIn(0, 23),
                settings.bringInReminderMinute.coerceIn(0, 59)
            )
            val nextBringInTrigger = allUpcomingDays
                .map { LocalDateTime.of(it.date, bringInTime) }
                .firstOrNull { it.isAfter(now.plusSeconds(5)) }

            if (nextBringInTrigger != null) {
                scheduleAlarmAt(
                    context = appContext,
                    alarmManager = alarmManager,
                    triggerDateTime = nextBringInTrigger,
                    requestCode = REQUEST_CODE_BRING_IN,
                    reminderType = TYPE_BRING_IN
                )
            }
        }
    }

    private fun scheduleAlarmAt(
        context: Context,
        alarmManager: AlarmManager,
        triggerDateTime: LocalDateTime,
        requestCode: Int,
        reminderType: String
    ) {
        val intent = Intent(context, BinReminderReceiver::class.java).apply {
            action = BinReminderReceiver.ACTION_BIN_REMINDER
            putExtra(EXTRA_REMINDER_TYPE, reminderType)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val epochMillis = triggerDateTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            epochMillis,
            pendingIntent
        )
    }

    private fun cancelAlarm(
        context: Context,
        alarmManager: AlarmManager,
        requestCode: Int,
        reminderType: String
    ) {
        val intent = Intent(context, BinReminderReceiver::class.java).apply {
            action = BinReminderReceiver.ACTION_BIN_REMINDER
            putExtra(EXTRA_REMINDER_TYPE, reminderType)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
