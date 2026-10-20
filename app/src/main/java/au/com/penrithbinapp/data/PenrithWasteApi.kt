package au.com.penrithbinapp.data

import au.com.penrithbinapp.model.BinType
import au.com.penrithbinapp.model.CachedScheduleData
import au.com.penrithbinapp.model.ExplicitBinEvent
import au.com.penrithbinapp.model.LocalitiesResponseDto
import au.com.penrithbinapp.model.LocalityDto
import au.com.penrithbinapp.model.PropertiesResponseDto
import au.com.penrithbinapp.model.PropertySummaryDto
import au.com.penrithbinapp.model.RawScheduleEventDto
import au.com.penrithbinapp.model.RecurringBinRule
import au.com.penrithbinapp.model.StreetDto
import au.com.penrithbinapp.model.StreetsResponseDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PenrithWasteApi {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    companion object {
        private const val BASE_URL = "https://penrith.waste-info.com.au/api/v1"
        private const val CONNECT_TIMEOUT_MS = 12_000
        private const val READ_TIMEOUT_MS = 15_000
    }

    suspend fun fetchLocalities(): List<LocalityDto> = withContext(Dispatchers.IO) {
        val raw = getJsonString("$BASE_URL/localities.json")
        val response = json.decodeFromString<LocalitiesResponseDto>(raw)
        response.localities
            .filter { !it.name.equals("Test", ignoreCase = true) && it.name.isNotBlank() }
            .sortedBy { it.name.lowercase() }
    }

    /**
     * Fetches the entire Penrith LGA street catalogue in a single call (~170 KB).
     * Each [StreetDto] includes `id`, `name`, and `locality` (suburb).
     */
    suspend fun fetchAllStreets(): List<StreetDto> = withContext(Dispatchers.IO) {
        val raw = getJsonString("$BASE_URL/streets.json")
        val response = json.decodeFromString<StreetsResponseDto>(raw)
        response.streets
            .filter {
                it.name.isNotBlank() &&
                    !it.locality.equals("Test", ignoreCase = true)
            }
            .sortedWith(compareBy({ it.name.lowercase() }, { it.locality?.lowercase().orEmpty() }))
    }

    suspend fun fetchStreets(localityId: Int): List<StreetDto> = withContext(Dispatchers.IO) {
        val raw = getJsonString("$BASE_URL/streets.json?locality=$localityId")
        val response = json.decodeFromString<StreetsResponseDto>(raw)
        response.streets
            .filter { it.name.isNotBlank() }
            .sortedBy { it.name.lowercase() }
    }

    suspend fun fetchProperties(streetId: Int): List<PropertySummaryDto> = withContext(Dispatchers.IO) {
        val raw = getJsonString("$BASE_URL/properties.json?street=$streetId")
        val response = json.decodeFromString<PropertiesResponseDto>(raw)
        response.properties
            .filter { it.name.isNotBlank() }
            .sortedWith(naturalAddressComparator)
    }

    suspend fun fetchPropertySchedule(
        propertyId: Int,
        fallbackAddress: String,
        suburb: String,
        fallbackZone: String? = null,
        referenceDate: LocalDate = LocalDate.now()
    ): CachedScheduleData = withContext(Dispatchers.IO) {
        val start = referenceDate.minusDays(14).format(DateTimeFormatter.ISO_LOCAL_DATE)
        val end = referenceDate.plusDays(180).format(DateTimeFormatter.ISO_LOCAL_DATE)
        val url = "$BASE_URL/properties/$propertyId.json?start=$start&end=$end"
        val raw = getJsonString(url)
        val events = json.decodeFromString<List<RawScheduleEventDto>>(raw)

        val metadata = events.firstNotNullOfOrNull { it.property }
        val resolvedAddress = metadata?.address?.takeIf { it.isNotBlank() } ?: fallbackAddress
        val resolvedZone = metadata?.zone?.takeIf { it.isNotBlank() } ?: fallbackZone
        val serviceType = metadata?.serviceType

        val recurringRules = mutableListOf<RecurringBinRule>()
        val explicitEvents = mutableListOf<ExplicitBinEvent>()

        for (event in events) {
            val binType = BinType.fromApiEventType(event.eventType) ?: continue
            val dows = (event.daysOfWeek ?: event.dow).orEmpty().distinct()
            if (dows.isNotEmpty()) {
                recurringRules.add(
                    RecurringBinRule(
                        binType = binType,
                        daysOfWeek = dows,
                        startDateIso = event.startDate
                    )
                )
            } else if (!event.start.isNullOrBlank()) {
                explicitEvents.add(
                    ExplicitBinEvent(
                        binType = binType,
                        dateIso = event.start.trim()
                    )
                )
            }
        }

        CachedScheduleData(
            propertyId = propertyId,
            address = resolvedAddress,
            suburb = suburb,
            zone = resolvedZone,
            serviceType = serviceType,
            primaryCollectionDow = metadata?.collectionDay,
            secondaryCollectionDow = metadata?.collectionDay2,
            recurringRules = recurringRules,
            explicitEvents = explicitEvents.distinctBy { "${it.binType.name}_${it.dateIso}" },
            lastSyncedEpochMillis = System.currentTimeMillis()
        )
    }

    private fun getJsonString(urlString: String): String {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "PenrithBinApp-Android/1.0")
        }
        try {
            val statusCode = connection.responseCode
            val stream = if (statusCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val body = stream?.let {
                BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            }.orEmpty()

            if (statusCode !in 200..299) {
                throw IllegalStateException("HTTP $statusCode from Penrith Waste API: $body")
            }
            return body
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Sorts addresses like "1 High St", "2 High St", "1/10 High St", "2/10 High St", "10 High St"
     * in natural numeric order instead of ASCII lexicographical order.
     */
    private val naturalAddressComparator = Comparator<PropertySummaryDto> { a, b ->
        val numsA = Regex("\\d+").findAll(a.name).mapNotNull { it.value.toIntOrNull() }.toList()
        val numsB = Regex("\\d+").findAll(b.name).mapNotNull { it.value.toIntOrNull() }.toList()
        val mainA = numsA.lastOrNull() ?: Int.MAX_VALUE
        val mainB = numsB.lastOrNull() ?: Int.MAX_VALUE
        if (mainA != mainB) {
            mainA.compareTo(mainB)
        } else {
            val unitA = if (numsA.size > 1) numsA.first() else 0
            val unitB = if (numsB.size > 1) numsB.first() else 0
            if (unitA != unitB) {
                unitA.compareTo(unitB)
            } else {
                a.name.compareTo(b.name, ignoreCase = true)
            }
        }
    }
}
