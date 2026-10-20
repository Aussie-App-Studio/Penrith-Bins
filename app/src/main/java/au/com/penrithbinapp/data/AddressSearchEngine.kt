package au.com.penrithbinapp.data

import au.com.penrithbinapp.model.LocalityDto
import au.com.penrithbinapp.model.PropertySummaryDto
import au.com.penrithbinapp.model.StreetDto

data class ParsedAddressQuery(
    val rawQuery: String,
    val houseNumberPrefix: String?,
    val streetSuburbPart: String,
    val tokens: List<String>
)

object AddressSearchEngine {

    /**
     * All 36 official suburbs within the Penrith City Council Local Government Area (LGA)
     * with their official Waste-Info locality IDs for instant zero-latency suburb resolution.
     */
    val PENRITH_LOCALITIES: List<LocalityDto> = listOf(
        LocalityDto(101184, "Agnes Banks"),
        LocalityDto(101205, "Badgerys Creek"),
        LocalityDto(101212, "Berkshire Park"),
        LocalityDto(101198, "Caddens"),
        LocalityDto(101209, "Cambridge Gardens"),
        LocalityDto(101192, "Cambridge Park"),
        LocalityDto(101182, "Castlereagh"),
        LocalityDto(101199, "Claremont Meadows"),
        LocalityDto(101177, "Colyton"),
        LocalityDto(101204, "Cranebrook"),
        LocalityDto(101185, "Emu Heights"),
        LocalityDto(101186, "Emu Plains"),
        LocalityDto(101178, "Erskine Park"),
        LocalityDto(101187, "Glenmore Park"),
        LocalityDto(101202, "Jamisontown"),
        LocalityDto(101179, "Jordan Springs"),
        LocalityDto(101206, "Kemps Creek"),
        LocalityDto(101193, "Kingswood"),
        LocalityDto(101188, "Leonay"),
        LocalityDto(101197, "Llandilo"),
        LocalityDto(101180, "Londonderry"),
        LocalityDto(101200, "Luddenham"),
        LocalityDto(101207, "Mount Vernon"),
        LocalityDto(101190, "Mulgoa"),
        LocalityDto(101194, "North St Marys"),
        LocalityDto(101208, "Orchard Hills"),
        LocalityDto(101195, "Oxley Park"),
        LocalityDto(101203, "Penrith"),
        LocalityDto(101191, "Regentville"),
        LocalityDto(101201, "South Penrith"),
        LocalityDto(101183, "St Clair"),
        LocalityDto(101181, "St Marys"),
        LocalityDto(101189, "Wallacia"),
        LocalityDto(101196, "Werrington"),
        LocalityDto(101210, "Werrington County"),
        LocalityDto(101211, "Werrington Downs")
    )

    val PENRITH_LGA_SUBURBS: List<String> = PENRITH_LOCALITIES.map { it.name }

    private val streetSynonyms: Map<String, List<String>> = mapOf(
        "st" to listOf("st", "street", "saint"),
        "saint" to listOf("saint", "st"),
        "street" to listOf("street", "st"),
        "rd" to listOf("rd", "road"),
        "road" to listOf("road", "rd"),
        "ave" to listOf("ave", "av", "avenue"),
        "av" to listOf("av", "ave", "avenue"),
        "avenue" to listOf("avenue", "ave", "av"),
        "dr" to listOf("dr", "drive"),
        "drive" to listOf("drive", "dr"),
        "pl" to listOf("pl", "place"),
        "place" to listOf("place", "pl"),
        "ct" to listOf("ct", "court"),
        "crt" to listOf("crt", "ct", "court"),
        "court" to listOf("court", "ct", "crt"),
        "cr" to listOf("cr", "cres", "crescent"),
        "cres" to listOf("cres", "cr", "crescent"),
        "crescent" to listOf("crescent", "cres", "cr"),
        "cl" to listOf("cl", "close"),
        "close" to listOf("close", "cl"),
        "pde" to listOf("pde", "parade"),
        "parade" to listOf("parade", "pde"),
        "cct" to listOf("cct", "circuit"),
        "circuit" to listOf("circuit", "cct"),
        "ln" to listOf("ln", "lane"),
        "lane" to listOf("lane", "ln"),
        "tce" to listOf("tce", "terrace"),
        "terrace" to listOf("terrace", "tce"),
        "hwy" to listOf("hwy", "highway"),
        "highway" to listOf("highway", "hwy"),
        "blvd" to listOf("blvd", "boulevard"),
        "boulevard" to listOf("boulevard", "blvd"),
        "gr" to listOf("gr", "grove"),
        "grove" to listOf("grove", "gr"),
        "mt" to listOf("mt", "mount"),
        "mount" to listOf("mount", "mt"),
        "nth" to listOf("nth", "north"),
        "north" to listOf("north", "nth"),
        "sth" to listOf("sth", "south"),
        "south" to listOf("south", "sth")
    )

    /**
     * Normalizes Geocoder suburb/street strings (e.g. "Saint Marys" -> "St Marys", "St. Marys" -> "St Marys",
     * "Mt Vernon" -> "Mount Vernon") so they match Penrith City Council's naming convention.
     */
    fun normalizeSuburbOrStreet(input: String): String {
        return input.trim()
            .replace(".", "")
            .replace(Regex("\\bSaint\\b", RegexOption.IGNORE_CASE), "St")
            .replace(Regex("\\bMt\\b", RegexOption.IGNORE_CASE), "Mount")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Checks candidate suburb strings (from Geocoder's `subLocality`, `locality`, or full `addressLine`)
     * and returns the canonical Penrith LGA suburb name if any candidate is inside Penrith City Council.
     */
    fun findMatchingPenrithSuburb(vararg candidates: String?): String? {
        for (raw in candidates) {
            if (raw.isNullOrBlank()) continue
            val normalized = normalizeSuburbOrStreet(raw)
            // Direct match against canonical Penrith LGA suburbs
            val exact = PENRITH_LGA_SUBURBS.firstOrNull { suburb ->
                suburb.equals(normalized, ignoreCase = true)
            }
            if (exact != null) return exact

            // Also check if a full address line contains a Penrith suburb (longest match first, e.g. "North St Marys" before "St Marys")
            val contained = PENRITH_LGA_SUBURBS
                .sortedByDescending { it.length }
                .firstOrNull { suburb ->
                    Regex("\\b${Regex.escape(suburb)}\\b", RegexOption.IGNORE_CASE)
                        .containsMatchIn(normalized)
                }
            if (contained != null) return contained
        }
        return null
    }

    private val houseNumberPrefixRegex =
        Regex("^\\s*((?:unit\\s+|u\\s*|lot\\s+)?\\d+[a-zA-Z]?(?:\\s*[/\\-]\\s*\\d+[a-zA-Z]?)?)\\b\\s*(.*)$", RegexOption.IGNORE_CASE)

    /**
     * Parses a user query like `"1 Allard St, Penrith"` or `"2/10 High St"` into
     * an optional leading house/unit number and normalized search tokens.
     */
    fun parseQuery(raw: String): ParsedAddressQuery {
        val cleaned = raw.trim().replace(",", " ").replace(Regex("\\s+"), " ")
        if (cleaned.isEmpty()) {
            return ParsedAddressQuery(
                rawQuery = "",
                houseNumberPrefix = null,
                streetSuburbPart = "",
                tokens = emptyList()
            )
        }

        val match = houseNumberPrefixRegex.matchEntire(cleaned)
        val housePrefix: String?
        val remainder: String
        if (match != null) {
            val candidateNum = match.groupValues[1].trim()
            val candidateRest = match.groupValues[2].trim()
            if (candidateRest.isNotEmpty()) {
                housePrefix = candidateNum
                remainder = candidateRest
            } else {
                housePrefix = candidateNum
                remainder = ""
            }
        } else {
            housePrefix = null
            remainder = cleaned
        }

        val tokens = remainder
            .lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.isNotBlank() }

        return ParsedAddressQuery(
            rawQuery = cleaned,
            houseNumberPrefix = housePrefix,
            streetSuburbPart = remainder,
            tokens = tokens
        )
    }

    /**
     * Searches and ranks Penrith LGA streets against the user's free-text query.
     * Supports queries with or without a leading house number (e.g. `"1 Allard St Penrith"` or `"Allard St"`).
     */
    fun searchStreets(
        query: String,
        allStreets: List<StreetDto>,
        limit: Int = 25
    ): List<StreetDto> {
        val parsed = parseQuery(query)
        val effectiveTokens = if (parsed.tokens.isNotEmpty()) {
            parsed.tokens
        } else {
            // User only typed a number or non-standard text; try raw tokens
            parsed.rawQuery.lowercase().split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }
        }

        if (effectiveTokens.isEmpty()) return emptyList()

        val scored = mutableListOf<Pair<StreetDto, Int>>()

        for (street in allStreets) {
            val streetLower = street.name.lowercase()
            val localityLower = street.locality?.lowercase().orEmpty()
            val combined = "$streetLower $localityLower"
            val combinedWords = combined.split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }

            val allTokensMatch = effectiveTokens.all { token ->
                val synonyms = streetSynonyms[token] ?: listOf(token)
                synonyms.any { syn ->
                    combinedWords.any { word -> word.startsWith(syn) } || combined.contains(syn)
                }
            }

            if (!allTokensMatch) continue

            val firstToken = effectiveTokens.first()
            val firstSynonyms = streetSynonyms[firstToken] ?: listOf(firstToken)
            var score = 100

            // Boost if street name starts with the first token
            if (firstSynonyms.any { streetLower.startsWith(it) }) {
                score += 60
            }
            // Boost if first word of street name equals first token
            val firstStreetWord = streetLower.substringBefore(' ')
            if (firstSynonyms.any { firstStreetWord == it }) {
                score += 50
            }
            // Boost if locality is also matched when multiple tokens are provided
            if (effectiveTokens.size >= 2 && effectiveTokens.any { localityLower.startsWith(it) }) {
                score += 35
            }
            // Prefer shorter street names when tied
            score -= (streetLower.length.coerceAtMost(30) / 3)

            scored.add(street to score)
        }

        return scored
            .sortedWith(
                compareByDescending<Pair<StreetDto, Int>> { it.second }
                    .thenBy { it.first.name.lowercase() }
                    .thenBy { it.first.locality?.lowercase().orEmpty() }
            )
            .take(limit)
            .map { it.first }
    }

    /**
     * Filters and ranks properties on a street using an optional house/unit number prefix.
     */
    fun filterAndRankProperties(
        properties: List<PropertySummaryDto>,
        houseNumberPrefix: String?,
        rawFilterQuery: String = ""
    ): List<PropertySummaryDto> {
        val cleanHouse = houseNumberPrefix
            ?.lowercase()
            ?.replace(Regex("^(unit|lot|u)\\s*"), "")
            ?.replace(" ", "")
            ?.trim()
            .orEmpty()

        val extraFilter = rawFilterQuery.trim().lowercase()

        if (cleanHouse.isEmpty() && extraFilter.isEmpty()) {
            return properties
        }

        val scored = mutableListOf<Pair<PropertySummaryDto, Int>>()
        for (prop in properties) {
            val nameLower = prop.name.lowercase()
            if (extraFilter.isNotEmpty() && !nameLower.contains(extraFilter)) {
                continue
            }

            if (cleanHouse.isNotEmpty()) {
                // Extract the leading address number token before the street name
                val leadingNumMatch = Regex("^\\s*(\\d+[a-z]?(?:[/\\-]\\d+[a-z]?)?)\\b").find(nameLower)
                val leadingNum = leadingNumMatch?.groupValues?.get(1).orEmpty()

                val matchesHouse = when {
                    leadingNum == cleanHouse -> 300 // Exact number match (e.g. "1 Allard Street")
                    leadingNum.endsWith("/$cleanHouse") || leadingNum.startsWith("$cleanHouse/") -> 220 // Unit match
                    leadingNum.startsWith(cleanHouse) -> 150 // Prefix match (e.g. "10", "12" when typing "1")
                    nameLower.contains(cleanHouse) -> 60
                    else -> 0
                }
                if (matchesHouse > 0) {
                    scored.add(prop to matchesHouse)
                }
            } else {
                scored.add(prop to 100)
            }
        }

        return scored
            .sortedByDescending { it.second }
            .map { it.first }
    }
}
