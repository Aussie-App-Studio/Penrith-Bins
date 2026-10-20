package au.com.penrithbinapp.model

import au.com.penrithbinapp.R
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalDate

enum class BinType(
    val apiEventType: String,
    val displayName: String,
    val shortName: String,
    val subtitle: String,
    val defaultFrequency: String,
    val acceptedItems: List<String>,
    val prohibitedItems: List<String>
) {
    GREEN(
        apiEventType = "organic",
        displayName = "Green Lid Bin",
        shortName = "Green (FOGO)",
        subtitle = "Food & Garden Organics (FOGO)",
        defaultFrequency = "Weekly",
        acceptedItems = listOf(
            "Food scraps, fruit & veg, meat, fish & bones",
            "Garden prunings, lawn clippings, leaves & weeds",
            "Compostable lime-green caddy liners (AS 4736)",
            "Food-soiled paper towels, tissues & pizza boxes"
        ),
        prohibitedItems = listOf(
            "Plastic bags, biodegradable plastic, or cling wrap",
            "Treated/painted timber, soil, rocks & bricks",
            "Nappies, pet droppings, or general waste"
        )
    ),
    YELLOW(
        apiEventType = "recycle",
        displayName = "Yellow Lid Bin",
        shortName = "Yellow (Recycle)",
        subtitle = "Household Mixed Recycling",
        defaultFrequency = "Fortnightly",
        acceptedItems = listOf(
            "Paper, newspapers, magazines & flattened cardboard",
            "Glass bottles & jars (rinsed, lids removed)",
            "Steel & aluminium cans, empty aerosol cans",
            "Rigid plastic bottles, jugs, tubs & containers"
        ),
        prohibitedItems = listOf(
            "Soft plastics, plastic bags & bagged recycling",
            "Polystyrene foam, bubble wrap & takeaway cups",
            "Clothing, textiles, batteries, or e-waste"
        )
    ),
    RED(
        apiEventType = "waste",
        displayName = "Red Lid Bin",
        shortName = "Red (Waste)",
        subtitle = "General Household Rubbish",
        defaultFrequency = "Fortnightly",
        acceptedItems = listOf(
            "General household rubbish & non-recyclable items",
            "Soft plastics, wrappers, chip packets & cling wrap",
            "Disposable nappies, sanitary items & bagged pet waste",
            "Polystyrene foam, broken crockery & drinking glasses"
        ),
        prohibitedItems = listOf(
            "Batteries, electronics (e-waste) & gas cylinders",
            "Paint, chemicals, motor oil, or medical sharps",
            "Hot ashes, building materials, or garden waste"
        )
    );

    companion object {
        fun fromApiEventType(eventType: String?): BinType? {
            return when (eventType?.lowercase()?.trim()) {
                "organic", "organics", "fogo", "green" -> GREEN
                "recycle", "recycling", "yellow" -> YELLOW
                "waste", "garbage", "rubbish", "red" -> RED
                else -> null
            }
        }
    }
}

@Serializable
data class LocalitiesResponseDto(
    val localities: List<LocalityDto> = emptyList()
)

@Serializable
data class LocalityDto(
    val id: Int,
    val name: String,
    val postcode: String? = null,
    val council: String? = null
)

@Serializable
data class StreetsResponseDto(
    val streets: List<StreetDto> = emptyList()
)

@Serializable
data class StreetDto(
    val id: Int,
    val name: String,
    val locality: String? = null
)

@Serializable
data class PropertiesResponseDto(
    val properties: List<PropertySummaryDto> = emptyList()
)

@Serializable
data class PropertySummaryDto(
    val id: Int,
    val name: String,
    val zone: String? = null,
    @SerialName("voucher_preferences")
    val voucherPreferences: Int? = null
)

@Serializable
data class RawScheduleEventDto(
    val property: PropertyMetadataDto? = null,
    val color: String? = null,
    val dow: List<Int>? = null,
    val daysOfWeek: List<Int>? = null,
    @SerialName("start_date")
    val startDate: String? = null,
    val start: String? = null,
    @SerialName("event_type")
    val eventType: String? = null
)

@Serializable
data class PropertyMetadataDto(
    @SerialName("collection_day")
    val collectionDay: Int? = null,
    @SerialName("collection_day_2")
    val collectionDay2: Int? = null,
    val zone: String? = null,
    val address: String? = null,
    @SerialName("service_type")
    val serviceType: String? = null
)

@Serializable
data class RecurringBinRule(
    val binType: BinType,
    /** FullCalendar / JS day-of-week numbers: 0 = Sunday, 1 = Monday, ..., 6 = Saturday */
    val daysOfWeek: List<Int>,
    val startDateIso: String? = null
)

@Serializable
data class ExplicitBinEvent(
    val binType: BinType,
    val dateIso: String
)

@Serializable
data class CachedScheduleData(
    val propertyId: Int,
    val address: String,
    val suburb: String,
    val zone: String? = null,
    val serviceType: String? = null,
    val primaryCollectionDow: Int? = null,
    val secondaryCollectionDow: Int? = null,
    val recurringRules: List<RecurringBinRule> = emptyList(),
    val explicitEvents: List<ExplicitBinEvent> = emptyList(),
    val lastSyncedEpochMillis: Long = System.currentTimeMillis()
)

enum class AppIconOption(
    val key: String,
    val displayName: String,
    val subtitle: String,
    val aliasClassName: String,
    val foregroundResId: Int,
    val backgroundColorHex: Long
) {
    DEFAULT(
        key = "default",
        displayName = "Default",
        subtitle = "Penrith Bins Logo",
        aliasClassName = "au.com.penrithbinapp.LauncherDefault",
        foregroundResId = R.mipmap.ic_launcher_foreground,
        backgroundColorHex = 0xFFD2D2DC
    ),
    SIMPLE_BIN(
        key = "simple_bin",
        displayName = "Simple Bin",
        subtitle = "Clean Minimal Emblem",
        aliasClassName = "au.com.penrithbinapp.LauncherSimpleBin",
        foregroundResId = R.mipmap.ic_launcher_simple_bin_foreground,
        backgroundColorHex = 0xFFD2D2DC
    ),
    BIN_TRUCK(
        key = "bin_truck",
        displayName = "Bin Truck",
        subtitle = "Yellow Garbage Truck",
        aliasClassName = "au.com.penrithbinapp.LauncherBinTruck",
        foregroundResId = R.mipmap.ic_launcher_bin_truck_foreground,
        backgroundColorHex = 0xFFFAE600
    ),
    TECH_BIN(
        key = "tech_bin",
        displayName = "Tech Bin",
        subtitle = "Neon Cyber Pulse",
        aliasClassName = "au.com.penrithbinapp.LauncherTechBin",
        foregroundResId = R.mipmap.ic_launcher_tech_bin_foreground,
        backgroundColorHex = 0xFF001432
    ),
    CLOCK(
        key = "clock",
        displayName = "Clock",
        subtitle = "Bin Day Timer Badge",
        aliasClassName = "au.com.penrithbinapp.LauncherClock",
        foregroundResId = R.mipmap.ic_launcher_clock_foreground,
        backgroundColorHex = 0xFFFFFFFF
    ),
    DARK_ALARM(
        key = "dark_alarm",
        displayName = "Dark Alarm",
        subtitle = "Sleek Dark Bell",
        aliasClassName = "au.com.penrithbinapp.LauncherDarkAlarm",
        foregroundResId = R.mipmap.ic_launcher_dark_alarm_foreground,
        backgroundColorHex = 0xFF1E2832
    ),
    LETTER_P(
        key = "letter_p",
        displayName = "Letter P",
        subtitle = "Bold Blue Monogram",
        aliasClassName = "au.com.penrithbinapp.LauncherLetterP",
        foregroundResId = R.mipmap.ic_launcher_letter_p_foreground,
        backgroundColorHex = 0xFF000000
    );

    companion object {
        fun fromKey(key: String?): AppIconOption {
            return entries.firstOrNull { it.key == key } ?: DEFAULT
        }
    }
}

@Serializable
data class UserSettings(
    val darkModeEnabled: Boolean = true,
    val appIconKey: String = AppIconOption.DEFAULT.key,
    val weeklyRedBinOverride: Boolean = false,
    val hideGreenBinOverride: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val eveningReminderEnabled: Boolean = true,
    val eveningReminderHour: Int = 19,
    val eveningReminderMinute: Int = 0,
    val morningReminderEnabled: Boolean = false,
    val morningReminderHour: Int = 6,
    val morningReminderMinute: Int = 30,
    val bringInReminderEnabled: Boolean = false,
    val bringInReminderHour: Int = 17,
    val bringInReminderMinute: Int = 0,
    val checkedOffCollectionDateIso: String? = null,
    val hasClickedSupportHeart: Boolean = false,
    val hasEngagedWithWelcomePopup: Boolean = false,
    val lastWelcomePopupShownEpochMillis: Long = 0L
)

data class ScheduledCollectionDay(
    val date: LocalDate,
    val bins: List<BinType>,
    val isExtrapolated: Boolean = false,
    val hasWeeklyRedOverrideApplied: Boolean = false
)

/**
 * Converts FullCalendar day-of-week integer (0 = Sunday, 1 = Monday ... 6 = Saturday)
 * into java.time.DayOfWeek.
 */
fun fullCalendarDowToJavaDayOfWeek(dow: Int): DayOfWeek? {
    return when (dow) {
        0 -> DayOfWeek.SUNDAY
        1 -> DayOfWeek.MONDAY
        2 -> DayOfWeek.TUESDAY
        3 -> DayOfWeek.WEDNESDAY
        4 -> DayOfWeek.THURSDAY
        5 -> DayOfWeek.FRIDAY
        6 -> DayOfWeek.SATURDAY
        7 -> DayOfWeek.SUNDAY
        else -> null
    }
}

fun DayOfWeek.toFullCalendarDow(): Int {
    return when (this) {
        DayOfWeek.SUNDAY -> 0
        DayOfWeek.MONDAY -> 1
        DayOfWeek.TUESDAY -> 2
        DayOfWeek.WEDNESDAY -> 3
        DayOfWeek.THURSDAY -> 4
        DayOfWeek.FRIDAY -> 5
        DayOfWeek.SATURDAY -> 6
    }
}
