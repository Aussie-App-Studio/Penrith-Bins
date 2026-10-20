package au.com.penrithbinapp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import au.com.penrithbinapp.MainActivity
import au.com.penrithbinapp.R
import au.com.penrithbinapp.data.BinRepository
import au.com.penrithbinapp.data.ScheduleCalculator
import au.com.penrithbinapp.model.BinType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class BinAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        private val dayOfWeekFormatter = DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH)

        fun updateAllWidgets(context: Context) {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val componentName = ComponentName(appContext, BinAppWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(componentName)
            if (ids.isNotEmpty()) {
                for (id in ids) {
                    updateWidget(appContext, manager, id)
                }
            }
            // Also update the Visual Wheelie Bins widget whenever schedules/settings change
            BinIconsWidgetProvider.updateAllWidgets(appContext)
        }

        private fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.bin_app_widget)

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            val repo = BinRepository.getInstance(context)
            val schedule = repo.loadCachedSchedule()
            val settings = repo.loadUserSettings()
            val today = LocalDate.now()

            if (schedule == null) {
                views.setTextViewText(R.id.widget_countdown_badge, "Setup")
                views.setTextViewText(R.id.widget_date_title, "Next Bins")
                views.setViewVisibility(R.id.widget_pill_red, View.GONE)
                views.setViewVisibility(R.id.widget_pill_yellow, View.GONE)
                views.setViewVisibility(R.id.widget_pill_green, View.GONE)
                views.setViewVisibility(R.id.widget_empty_hint, View.VISIBLE)
            } else {
                val nextCollection = ScheduleCalculator.getNextCollection(schedule, settings, today)
                if (nextCollection == null) {
                    views.setTextViewText(R.id.widget_countdown_badge, "Sync")
                    views.setTextViewText(R.id.widget_date_title, "Next Bins")
                    views.setViewVisibility(R.id.widget_pill_red, View.GONE)
                    views.setViewVisibility(R.id.widget_pill_yellow, View.GONE)
                    views.setViewVisibility(R.id.widget_pill_green, View.GONE)
                    views.setViewVisibility(R.id.widget_empty_hint, View.VISIBLE)
                } else {
                    val daysUntil = ChronoUnit.DAYS.between(today, nextCollection.date).toInt()
                    val isCheckedOff =
                        settings.checkedOffCollectionDateIso == nextCollection.date.toString()

                    val countdownText = when {
                        isCheckedOff -> "✓ Bins Out"
                        daysUntil == 0 -> "TODAY!"
                        daysUntil == 1 -> "TOMORROW"
                        else -> "In $daysUntil days"
                    }
                    views.setTextViewText(R.id.widget_countdown_badge, countdownText)
                    views.setTextViewText(
                        R.id.widget_date_title,
                        nextCollection.date.format(dayOfWeekFormatter)
                    )
                    views.setViewVisibility(R.id.widget_empty_hint, View.GONE)
                    views.setViewVisibility(
                        R.id.widget_pill_red,
                        if (BinType.RED in nextCollection.bins) View.VISIBLE else View.GONE
                    )
                    views.setViewVisibility(
                        R.id.widget_pill_yellow,
                        if (BinType.YELLOW in nextCollection.bins) View.VISIBLE else View.GONE
                    )
                    views.setViewVisibility(
                        R.id.widget_pill_green,
                        if (BinType.GREEN in nextCollection.bins) View.VISIBLE else View.GONE
                    )
                }
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
