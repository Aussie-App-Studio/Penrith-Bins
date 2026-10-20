package au.com.penrithbinapp.data

import au.com.penrithbinapp.model.BinType
import au.com.penrithbinapp.model.CachedScheduleData
import au.com.penrithbinapp.model.ExplicitBinEvent
import au.com.penrithbinapp.model.RecurringBinRule
import au.com.penrithbinapp.model.ScheduledCollectionDay
import au.com.penrithbinapp.model.UserSettings
import au.com.penrithbinapp.model.toFullCalendarDow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object ScheduleCalculator {

    private val displayOrder = listOf(BinType.RED, BinType.YELLOW, BinType.GREEN)

    /**
     * Generates all scheduled collection days between [fromDate] and [toDate] (inclusive)
     * using cached Penrith Waste-Info schedule data, 14-day cycle extrapolation, and user overrides.
     */
    fun getScheduledDays(
        scheduleData: CachedScheduleData,
        settings: UserSettings,
        fromDate: LocalDate,
        toDate: LocalDate
    ): List<ScheduledCollectionDay> {
        if (toDate.isBefore(fromDate)) return emptyList()

        val explicitByBin: Map<BinType, List<LocalDate>> = scheduleData.explicitEvents
            .mapNotNull { event ->
                runCatching { LocalDate.parse(event.dateIso) }
                    .getOrNull()
                    ?.let { event.binType to it }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, dates) -> dates.distinct().sorted() }

        val explicitLookup: Map<LocalDate, Set<BinType>> = scheduleData.explicitEvents
            .mapNotNull { event ->
                runCatching { LocalDate.parse(event.dateIso) }
                    .getOrNull()
                    ?.let { it to event.binType }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.toSet() }

        val results = mutableListOf<ScheduledCollectionDay>()
        var cursor = fromDate
        while (!cursor.isAfter(toDate)) {
            val binsOnDate = mutableSetOf<BinType>()
            var isExtrapolated = false
            var redOverrideApplied = false

            val fcDow = cursor.dayOfWeek.toFullCalendarDow()

            // 1. Recurring weekly rules (e.g. Green FOGO weekly, or Multi-Unit Complex collections)
            for (rule in scheduleData.recurringRules) {
                if (fcDow in rule.daysOfWeek) {
                    val ruleStart = rule.startDateIso?.let {
                        runCatching { LocalDate.parse(it) }.getOrNull()
                    }
                    // Allow recurring rule even if start_date is within the current window
                    if (ruleStart == null || !cursor.isBefore(ruleStart.minusDays(14))) {
                        binsOnDate.add(rule.binType)
                    }
                }
            }

            // 2. Explicit collection events from the API
            explicitLookup[cursor]?.let { explicitBins ->
                binsOnDate.addAll(explicitBins)
            }

            // 3. 14-day fortnightly cycle extrapolation if cursor is outside cached explicit dates
            for (fortnightlyType in listOf(BinType.RED, BinType.YELLOW)) {
                val hasRecurringForType = scheduleData.recurringRules.any { it.binType == fortnightlyType }
                if (hasRecurringForType) continue

                val knownDates = explicitByBin[fortnightlyType].orEmpty()
                if (knownDates.isNotEmpty() && fortnightlyType !in binsOnDate) {
                    val maxKnown = knownDates.last()
                    val minKnown = knownDates.first()
                    if (cursor.isAfter(maxKnown)) {
                        val daysDiff = ChronoUnit.DAYS.between(maxKnown, cursor)
                        if (daysDiff > 0 && daysDiff % 14L == 0L) {
                            binsOnDate.add(fortnightlyType)
                            isExtrapolated = true
                        }
                    } else if (cursor.isBefore(minKnown)) {
                        val daysDiff = ChronoUnit.DAYS.between(cursor, minKnown)
                        if (daysDiff > 0 && daysDiff % 14L == 0L) {
                            binsOnDate.add(fortnightlyType)
                            isExtrapolated = true
                        }
                    }
                }
            }

            // 4. Weekly Red Bin manual override
            val isPrimaryCollectionDay = scheduleData.primaryCollectionDow == fcDow || binsOnDate.isNotEmpty()
            if (settings.weeklyRedBinOverride && isPrimaryCollectionDay && binsOnDate.isNotEmpty()) {
                if (BinType.RED !in binsOnDate) {
                    binsOnDate.add(BinType.RED)
                    redOverrideApplied = true
                }
            }

            // 5. Optional Hide Green (FOGO) Bin override
            if (settings.hideGreenBinOverride) {
                binsOnDate.remove(BinType.GREEN)
            }

            if (binsOnDate.isNotEmpty()) {
                val sortedBins = binsOnDate.sortedBy { displayOrder.indexOf(it) }
                results.add(
                    ScheduledCollectionDay(
                        date = cursor,
                        bins = sortedBins,
                        isExtrapolated = isExtrapolated,
                        hasWeeklyRedOverrideApplied = redOverrideApplied
                    )
                )
            }

            cursor = cursor.plusDays(1)
        }

        return results
    }

    /**
     * Returns the next scheduled collection on or after [referenceDate].
     */
    fun getNextCollection(
        scheduleData: CachedScheduleData,
        settings: UserSettings,
        referenceDate: LocalDate = LocalDate.now()
    ): ScheduledCollectionDay? {
        return getScheduledDays(
            scheduleData = scheduleData,
            settings = settings,
            fromDate = referenceDate,
            toDate = referenceDate.plusDays(30)
        ).firstOrNull()
    }

    /**
     * Builds a manual [CachedScheduleData] (with `propertyId = -1`) for users outside the
     * Penrith City Council LGA or anyone configuring a custom bin schedule.
     */
    fun createManualSchedule(
        addressLabel: String,
        collectionDayOfWeek: DayOfWeek,
        nextAlternatingBin: BinType,
        referenceDate: LocalDate = LocalDate.now()
    ): CachedScheduleData {
        var nextCollectionDate = referenceDate
        while (nextCollectionDate.dayOfWeek != collectionDayOfWeek) {
            nextCollectionDate = nextCollectionDate.plusDays(1)
        }

        val firstFortnightly = if (nextAlternatingBin == BinType.RED) BinType.RED else BinType.YELLOW
        val secondFortnightly = if (firstFortnightly == BinType.RED) BinType.YELLOW else BinType.RED

        val fcDow = collectionDayOfWeek.toFullCalendarDow()
        val cleanAddress = addressLabel.trim().ifEmpty { "Manual Schedule (Custom Area)" }

        return CachedScheduleData(
            propertyId = -1,
            address = cleanAddress,
            suburb = "Custom / Outside Penrith LGA",
            zone = "Manual Schedule",
            serviceType = "Manual",
            primaryCollectionDow = fcDow,
            secondaryCollectionDow = null,
            recurringRules = listOf(
                RecurringBinRule(
                    binType = BinType.GREEN,
                    daysOfWeek = listOf(fcDow),
                    startDateIso = nextCollectionDate.minusDays(14).toString()
                )
            ),
            explicitEvents = listOf(
                ExplicitBinEvent(
                    binType = firstFortnightly,
                    dateIso = nextCollectionDate.toString()
                ),
                ExplicitBinEvent(
                    binType = secondFortnightly,
                    dateIso = nextCollectionDate.plusDays(7).toString()
                )
            ),
            lastSyncedEpochMillis = System.currentTimeMillis()
        )
    }
}
