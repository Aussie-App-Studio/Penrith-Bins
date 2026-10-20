package au.com.penrithbinapp.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import au.com.penrithbinapp.model.AppIconOption

object AppIconManager {

    fun isOptionCurrentlyEnabled(context: Context, option: AppIconOption): Boolean {
        val pm = context.packageManager
        val component = ComponentName(context, option.aliasClassName)
        return when (pm.getComponentEnabledSetting(component)) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> option == AppIconOption.DEFAULT
            else -> false
        }
    }

    fun getCurrentlyEnabledIcon(context: Context): AppIconOption {
        return AppIconOption.entries.firstOrNull { isOptionCurrentlyEnabled(context, it) }
            ?: AppIconOption.DEFAULT
    }

    fun applyAppIconIfNeeded(context: Context, target: AppIconOption): Boolean {
        val pm = context.packageManager
        val alreadyActive = isOptionCurrentlyEnabled(context, target) &&
            AppIconOption.entries.filter { it != target }.none { isOptionCurrentlyEnabled(context, it) }

        if (alreadyActive) return false

        // Enable the new launcher alias first so the launcher always sees an active icon
        pm.setComponentEnabledSetting(
            ComponentName(context, target.aliasClassName),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        // Disable all other launcher aliases
        for (option in AppIconOption.entries) {
            if (option != target) {
                pm.setComponentEnabledSetting(
                    ComponentName(context, option.aliasClassName),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
        }
        return true
    }
}
