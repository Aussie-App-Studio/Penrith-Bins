package au.com.penrithbinapp

import au.com.penrithbinapp.data.AddressSearchEngine
import au.com.penrithbinapp.data.ScheduleCalculator
import au.com.penrithbinapp.model.BinType
import au.com.penrithbinapp.model.CachedScheduleData
import au.com.penrithbinapp.model.ExplicitBinEvent
import au.com.penrithbinapp.model.PropertySummaryDto
import au.com.penrithbinapp.model.RecurringBinRule
import au.com.penrithbinapp.model.StreetDto
import au.com.penrithbinapp.model.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class ScheduleCalculatorTest {

    private val sampleSchedule = CachedScheduleData(
        propertyId = 2861017,
        address = "1 Allard Street, Penrith",
        suburb = "Penrith",
        zone = "week 2",
        serviceType = "all_collections",
        primaryCollectionDow = 2, // Tuesday
        recurringRules = listOf(
            RecurringBinRule(
                binType = BinType.GREEN,
                daysOfWeek = listOf(2),
                startDateIso = "2026-09-29"
            )
        ),
        explicitEvents = listOf(
            ExplicitBinEvent(BinType.RED, "2026-10-20"),
            ExplicitBinEvent(BinType.YELLOW, "2026-10-27")
        )
    )

    @Test
    fun calculatesStandardAlternatingFortnightlySchedule() {
        val days = ScheduleCalculator.getScheduledDays(
            scheduleData = sampleSchedule,
            settings = UserSettings(),
            fromDate = LocalDate.of(2026, 10, 20),
            toDate = LocalDate.of(2026, 10, 27)
        )
        assertEquals(2, days.size)

        // Oct 20: Red + Green
        assertEquals(LocalDate.of(2026, 10, 20), days[0].date)
        assertEquals(listOf(BinType.RED, BinType.GREEN), days[0].bins)

        // Oct 27: Yellow + Green
        assertEquals(LocalDate.of(2026, 10, 27), days[1].date)
        assertEquals(listOf(BinType.YELLOW, BinType.GREEN), days[1].bins)
    }

    @Test
    fun extrapolates14DayCycleBeyondCachedDates() {
        // Nov 3 is 14 days after Oct 20 (Red)
        // Nov 10 is 14 days after Oct 27 (Yellow)
        val days = ScheduleCalculator.getScheduledDays(
            scheduleData = sampleSchedule,
            settings = UserSettings(),
            fromDate = LocalDate.of(2026, 11, 3),
            toDate = LocalDate.of(2026, 11, 10)
        )
        assertEquals(2, days.size)

        assertEquals(LocalDate.of(2026, 11, 3), days[0].date)
        assertEquals(listOf(BinType.RED, BinType.GREEN), days[0].bins)
        assertTrue(days[0].isExtrapolated)

        assertEquals(LocalDate.of(2026, 11, 10), days[1].date)
        assertEquals(listOf(BinType.YELLOW, BinType.GREEN), days[1].bins)
        assertTrue(days[1].isExtrapolated)
    }

    @Test
    fun weeklyRedBinOverrideAddsRedBinOnYellowWeeks() {
        val settings = UserSettings(weeklyRedBinOverride = true)
        val days = ScheduleCalculator.getScheduledDays(
            scheduleData = sampleSchedule,
            settings = settings,
            fromDate = LocalDate.of(2026, 10, 27),
            toDate = LocalDate.of(2026, 10, 27)
        )
        assertEquals(1, days.size)
        assertEquals(listOf(BinType.RED, BinType.YELLOW, BinType.GREEN), days[0].bins)
        assertTrue(days[0].hasWeeklyRedOverrideApplied)
    }

    @Test
    fun hideGreenBinOverrideRemovesGreenBin() {
        val settings = UserSettings(hideGreenBinOverride = true)
        val days = ScheduleCalculator.getScheduledDays(
            scheduleData = sampleSchedule,
            settings = settings,
            fromDate = LocalDate.of(2026, 10, 20),
            toDate = LocalDate.of(2026, 10, 20)
        )
        assertEquals(1, days.size)
        assertFalse(BinType.GREEN in days[0].bins)
        assertEquals(listOf(BinType.RED), days[0].bins)
    }

    @Test
    fun singleBoxAddressSearchMatchesHouseNumberAndAbbreviations() {
        val streets = listOf(
            StreetDto(id = 157983, name = "Allard Street", locality = "Penrith"),
            StreetDto(id = 158001, name = "Queen Street", locality = "St Marys"),
            StreetDto(id = 158002, name = "Glenmore Parkway", locality = "Glenmore Park")
        )

        val parsed = AddressSearchEngine.parseQuery("1 Allard St Penrith")
        assertEquals("1", parsed.houseNumberPrefix)

        val matched = AddressSearchEngine.searchStreets("1 Allard St Penrith", streets)
        assertEquals(1, matched.size)
        assertEquals(157983, matched.first().id)

        // Ensure "St" works for both "Street" and "St Marys"
        val stMarysMatches = AddressSearchEngine.searchStreets("10 Queen St St Marys", streets)
        assertEquals(1, stMarysMatches.size)
        assertEquals(158001, stMarysMatches.first().id)

        // Ensure exact house number ranks first
        val properties = listOf(
            PropertySummaryDto(id = 10, name = "10 Allard Street Penrith"),
            PropertySummaryDto(id = 1, name = "1 Allard Street Penrith"),
            PropertySummaryDto(id = 12, name = "12 Allard Street Penrith")
        )
        val rankedProps = AddressSearchEngine.filterAndRankProperties(properties, parsed.houseNumberPrefix)
        assertEquals(1, rankedProps.first().id)
    }

    @Test
    fun gpsReverseGeocodeResolves72MarsdenRdStMarysVariants() {
        val streets = listOf(
            StreetDto(id = 154634, name = "Marsden Road", locality = "Colyton"),
            StreetDto(id = 155031, name = "Marsden Road", locality = "St Marys"),
            StreetDto(id = 159826, name = "Samuel Marsden Road", locality = "Orchard Hills")
        )

        // Geocoder often returns "Saint Marys", "St. Marys", or locality="Sydney" + subLocality="St Marys"
        assertEquals("St Marys", AddressSearchEngine.findMatchingPenrithSuburb("Saint Marys"))
        assertEquals("St Marys", AddressSearchEngine.findMatchingPenrithSuburb("St. Marys"))
        assertEquals(
            "St Marys",
            AddressSearchEngine.findMatchingPenrithSuburb(null, "Sydney", "72 Marsden Rd, St Marys NSW 2760, Australia")
        )

        val matched = AddressSearchEngine.searchStreets("72 Marsden Rd Saint Marys", streets)
        assertTrue(matched.isNotEmpty())
        assertEquals(155031, matched.first().id)
    }

    @Test
    fun manualScheduleOutsidePenrithCalculatesAlternatingWeeks() {
        val manual = ScheduleCalculator.createManualSchedule(
            addressLabel = "15 Main St, Blacktown",
            collectionDayOfWeek = DayOfWeek.WEDNESDAY,
            nextAlternatingBin = BinType.YELLOW,
            referenceDate = LocalDate.of(2026, 10, 20) // Tuesday -> next Wednesday is Oct 21
        )
        assertEquals(-1, manual.propertyId)

        val days = ScheduleCalculator.getScheduledDays(
            scheduleData = manual,
            settings = UserSettings(),
            fromDate = LocalDate.of(2026, 10, 20),
            toDate = LocalDate.of(2026, 11, 5)
        )
        assertEquals(3, days.size)
        // Oct 21: Yellow + Green
        assertEquals(LocalDate.of(2026, 10, 21), days[0].date)
        assertEquals(listOf(BinType.YELLOW, BinType.GREEN), days[0].bins)
        // Oct 28: Red + Green
        assertEquals(LocalDate.of(2026, 10, 28), days[1].date)
        assertEquals(listOf(BinType.RED, BinType.GREEN), days[1].bins)
        // Nov 4 (extrapolated 14 days after Oct 21): Yellow + Green
        assertEquals(LocalDate.of(2026, 11, 4), days[2].date)
        assertEquals(listOf(BinType.YELLOW, BinType.GREEN), days[2].bins)
    }
}
