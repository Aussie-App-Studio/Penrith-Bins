package au.com.penrithbinapp.ui

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import au.com.penrithbinapp.data.AddressSearchEngine
import au.com.penrithbinapp.data.AppIconManager
import au.com.penrithbinapp.data.BinRepository
import au.com.penrithbinapp.data.PenrithWasteApi
import au.com.penrithbinapp.data.ScheduleCalculator
import au.com.penrithbinapp.model.AppIconOption
import au.com.penrithbinapp.model.BinType
import au.com.penrithbinapp.model.CachedScheduleData
import au.com.penrithbinapp.model.LocalityDto
import au.com.penrithbinapp.model.PropertySummaryDto
import au.com.penrithbinapp.model.ScheduledCollectionDay
import au.com.penrithbinapp.model.StreetDto
import au.com.penrithbinapp.model.UserSettings
import au.com.penrithbinapp.notifications.BinNotificationHelper
import au.com.penrithbinapp.widget.BinAppWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale
import kotlin.coroutines.resume

enum class AddressPickerMode {
    QUICK_SEARCH,
    BROWSE_SUBURBS,
    MANUAL_SCHEDULE
}

enum class AddressLookupStep {
    SUBURB,
    STREET,
    PROPERTY
}

data class QuickPropertyMatch(
    val property: PropertySummaryDto,
    val street: StreetDto,
    val suburb: String
)

data class AddressPickerState(
    val isOpen: Boolean = false,
    val mode: AddressPickerMode = AddressPickerMode.QUICK_SEARCH,
    val step: AddressLookupStep = AddressLookupStep.SUBURB,
    val isLoading: Boolean = false,
    val isSearchingProperties: Boolean = false,
    val isLocatingGps: Boolean = false,
    val errorMessage: String? = null,
    val gpsNoticeMessage: String? = null,
    val isGpsOutsidePenrith: Boolean = false,
    val searchQuery: String = "",
    // Quick Search state
    val allStreets: List<StreetDto> = emptyList(),
    val matchedStreets: List<StreetDto> = emptyList(),
    val quickPropertyMatches: List<QuickPropertyMatch> = emptyList(),
    val focusedStreetInQuickSearch: StreetDto? = null,
    val focusedStreetProperties: List<PropertySummaryDto> = emptyList(),
    // Browse by Suburb fallback state
    val localities: List<LocalityDto> = AddressSearchEngine.PENRITH_LOCALITIES,
    val selectedLocality: LocalityDto? = null,
    val streets: List<StreetDto> = emptyList(),
    val selectedStreet: StreetDto? = null,
    val properties: List<PropertySummaryDto> = emptyList(),
    // Manual Schedule (Out-of-Area) state
    val manualAddressInput: String = "",
    val manualDayOfWeek: DayOfWeek = DayOfWeek.TUESDAY,
    val manualNextAlternatingBin: BinType = BinType.YELLOW
)

data class BinUiState(
    val scheduleData: CachedScheduleData? = null,
    val settings: UserSettings = UserSettings(),
    val upcomingCollections: List<ScheduledCollectionDay> = emptyList(),
    val nextCollection: ScheduledCollectionDay? = null,
    val followingCollection: ScheduledCollectionDay? = null,
    val isRefreshingSchedule: Boolean = false,
    val showAddressSetConfirmation: Boolean = false,
    val statusBannerMessage: String? = null
)

class BinViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val repository = BinRepository.getInstance(appContext)
    private val api = PenrithWasteApi()

    private val _isRefreshing = MutableStateFlow(false)
    private val _showAddressConfirmation = MutableStateFlow(false)
    private val _statusBannerMessage = MutableStateFlow<String?>(null)

    private val _addressPickerState = MutableStateFlow(AddressPickerState())
    val addressPickerState: StateFlow<AddressPickerState> = _addressPickerState.asStateFlow()

    // In-memory cache of streetId -> properties so autocomplete is instant
    private val streetPropertiesCache = mutableMapOf<Int, List<PropertySummaryDto>>()
    private var quickSearchJob: Job? = null

    val uiState: StateFlow<BinUiState> = combine(
        repository.scheduleData,
        repository.userSettings,
        _isRefreshing,
        _showAddressConfirmation,
        _statusBannerMessage
    ) { schedule, settings, refreshing, showConfirmed, banner ->
        val today = LocalDate.now()
        val upcoming = if (schedule != null) {
            ScheduleCalculator.getScheduledDays(
                scheduleData = schedule,
                settings = settings,
                fromDate = today,
                toDate = today.plusWeeks(10)
            )
        } else {
            emptyList()
        }
        BinUiState(
            scheduleData = schedule,
            settings = settings,
            upcomingCollections = upcoming,
            nextCollection = upcoming.firstOrNull(),
            followingCollection = upcoming.getOrNull(1),
            isRefreshingSchedule = refreshing,
            showAddressSetConfirmation = showConfirmed,
            statusBannerMessage = banner
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BinUiState(
            scheduleData = repository.loadCachedSchedule(),
            settings = repository.loadUserSettings()
        )
    )

    init {
        BinNotificationHelper.ensureChannelCreated(appContext)
        // Auto-sync in background if cache is older than 30 days (and not a manual schedule)
        val cached = repository.loadCachedSchedule()
        if (cached != null && cached.propertyId > 0) {
            val ageMillis = System.currentTimeMillis() - cached.lastSyncedEpochMillis
            if (ageMillis > 30L * 24 * 60 * 60 * 1000) {
                refreshCurrentSchedule(silent = true)
            }
        }
    }

    fun clearStatusBanner() {
        _statusBannerMessage.value = null
    }

    fun dismissAddressConfirmation() {
        _showAddressConfirmation.value = false
    }

    fun openAddressPicker(initialMode: AddressPickerMode = AddressPickerMode.QUICK_SEARCH) {
        val current = _addressPickerState.value
        _addressPickerState.update {
            it.copy(
                isOpen = true,
                mode = initialMode,
                step = AddressLookupStep.SUBURB,
                searchQuery = "",
                focusedStreetInQuickSearch = null,
                focusedStreetProperties = emptyList(),
                matchedStreets = emptyList(),
                quickPropertyMatches = emptyList(),
                errorMessage = null,
                gpsNoticeMessage = null,
                isGpsOutsidePenrith = false
            )
        }
        if (current.allStreets.isEmpty() || current.localities.isEmpty()) {
            preloadAddressCatalogue()
        }
    }

    fun closeAddressPicker() {
        quickSearchJob?.cancel()
        _addressPickerState.update {
            it.copy(
                isOpen = false,
                searchQuery = "",
                focusedStreetInQuickSearch = null,
                errorMessage = null,
                gpsNoticeMessage = null
            )
        }
    }

    fun setAddressPickerMode(mode: AddressPickerMode) {
        _addressPickerState.update { state ->
            val prefillManual = if (mode == AddressPickerMode.MANUAL_SCHEDULE &&
                state.manualAddressInput.isBlank() &&
                state.searchQuery.isNotBlank()
            ) {
                state.searchQuery
            } else {
                state.manualAddressInput
            }
            state.copy(
                mode = mode,
                step = AddressLookupStep.SUBURB,
                focusedStreetInQuickSearch = null,
                errorMessage = null,
                manualAddressInput = prefillManual
            )
        }
        val current = _addressPickerState.value
        if (mode == AddressPickerMode.BROWSE_SUBURBS && current.localities.isEmpty()) {
            loadLocalities()
        } else if (mode == AddressPickerMode.QUICK_SEARCH && current.allStreets.isEmpty()) {
            preloadAddressCatalogue()
        }
    }

    fun preloadAddressCatalogue() {
        viewModelScope.launch {
            _addressPickerState.update { it.copy(isLoading = true, errorMessage = null) }
            val streetsResult = runCatching { api.fetchAllStreets() }
            val localitiesResult = runCatching { api.fetchLocalities() }

            streetsResult.onSuccess { allStreets ->
                val localities = localitiesResult.getOrDefault(emptyList())
                _addressPickerState.update { state ->
                    state.copy(
                        isLoading = false,
                        allStreets = allStreets,
                        localities = localities.ifEmpty { state.localities },
                        errorMessage = null
                    )
                }
                val existingQuery = _addressPickerState.value.searchQuery
                if (existingQuery.isNotBlank()) {
                    runQuickSearch(existingQuery)
                }
            }.onFailure { err ->
                _addressPickerState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Could not load Penrith address catalogue: ${err.localizedMessage ?: "Network error"}"
                    )
                }
            }
        }
    }

    fun updatePickerSearchQuery(query: String) {
        val currentMode = _addressPickerState.value.mode
        _addressPickerState.update {
            it.copy(
                searchQuery = query,
                focusedStreetInQuickSearch = null,
                focusedStreetProperties = emptyList()
            )
        }
        if (currentMode == AddressPickerMode.QUICK_SEARCH) {
            runQuickSearch(query)
        }
    }

    private fun runQuickSearch(query: String) {
        quickSearchJob?.cancel()
        val trimmed = query.trim()
        val allStreets = _addressPickerState.value.allStreets
        if (trimmed.isEmpty() || allStreets.isEmpty()) {
            _addressPickerState.update {
                it.copy(
                    matchedStreets = emptyList(),
                    quickPropertyMatches = emptyList(),
                    isSearchingProperties = false
                )
            }
            return
        }

        val parsed = AddressSearchEngine.parseQuery(trimmed)
        val matchedStreets = AddressSearchEngine.searchStreets(trimmed, allStreets, limit = 15)

        _addressPickerState.update {
            it.copy(
                matchedStreets = matchedStreets,
                isSearchingProperties = matchedStreets.isNotEmpty()
            )
        }

        if (matchedStreets.isEmpty()) return

        quickSearchJob = viewModelScope.launch {
            // Small debounce so typing is smooth
            delay(200)
            val streetsToInspect = if (parsed.houseNumberPrefix != null) {
                matchedStreets.take(3)
            } else {
                matchedStreets.take(1)
            }

            val propertyMatches = mutableListOf<QuickPropertyMatch>()
            for (street in streetsToInspect) {
                val props = streetPropertiesCache[street.id] ?: runCatching {
                    api.fetchProperties(street.id)
                }.getOrNull()?.also { fetched ->
                    streetPropertiesCache[street.id] = fetched
                }.orEmpty()

                val filtered = AddressSearchEngine.filterAndRankProperties(
                    properties = props,
                    houseNumberPrefix = parsed.houseNumberPrefix
                )
                val takeCount = if (parsed.houseNumberPrefix != null) 8 else 5
                val suburb = street.locality ?: "Penrith"
                filtered.take(takeCount).forEach { prop ->
                    propertyMatches.add(
                        QuickPropertyMatch(
                            property = prop,
                            street = street,
                            suburb = suburb
                        )
                    )
                }
            }

            _addressPickerState.update {
                it.copy(
                    quickPropertyMatches = propertyMatches,
                    isSearchingProperties = false
                )
            }
        }
    }

    fun selectStreetInQuickSearch(street: StreetDto) {
        viewModelScope.launch {
            _addressPickerState.update {
                it.copy(
                    focusedStreetInQuickSearch = street,
                    isSearchingProperties = true,
                    errorMessage = null
                )
            }
            val cached = streetPropertiesCache[street.id]
            if (cached != null) {
                _addressPickerState.update {
                    it.copy(
                        isSearchingProperties = false,
                        focusedStreetProperties = cached
                    )
                }
            } else {
                runCatching { api.fetchProperties(street.id) }
                    .onSuccess { list ->
                        streetPropertiesCache[street.id] = list
                        _addressPickerState.update {
                            it.copy(
                                isSearchingProperties = false,
                                focusedStreetProperties = list
                            )
                        }
                    }
                    .onFailure { err ->
                        _addressPickerState.update {
                            it.copy(
                                isSearchingProperties = false,
                                errorMessage = "Could not load house numbers for ${street.name}: ${err.localizedMessage ?: "Network error"}"
                            )
                        }
                    }
            }
        }
    }

    fun clearFocusedStreetInQuickSearch() {
        _addressPickerState.update {
            it.copy(
                focusedStreetInQuickSearch = null,
                focusedStreetProperties = emptyList()
            )
        }
    }

    fun selectQuickPropertyMatch(match: QuickPropertyMatch) {
        fetchAndSavePropertySchedule(
            propertyId = match.property.id,
            fallbackAddress = match.property.name,
            suburb = match.suburb,
            fallbackZone = match.property.zone
        )
    }

    @SuppressLint("MissingPermission")
    fun useCurrentGpsLocation() {
        viewModelScope.launch {
            _addressPickerState.update {
                it.copy(
                    isLocatingGps = true,
                    gpsNoticeMessage = null,
                    isGpsOutsidePenrith = false,
                    errorMessage = null
                )
            }

            if (_addressPickerState.value.allStreets.isEmpty()) {
                runCatching { api.fetchAllStreets() }.getOrNull()?.let { streets ->
                    _addressPickerState.update { it.copy(allStreets = streets) }
                }
            }

            val location = getBestDeviceLocation()
            if (location == null) {
                _addressPickerState.update {
                    it.copy(
                        isLocatingGps = false,
                        gpsNoticeMessage = "Could not read GPS location. Make sure Location Services are enabled on your phone.",
                        isGpsOutsidePenrith = false
                    )
                }
                return@launch
            }

            val addressResult = reverseGeocodeLocation(location.latitude, location.longitude)
            if (addressResult == null) {
                _addressPickerState.update {
                    it.copy(
                        isLocatingGps = false,
                        gpsNoticeMessage = "Located coordinates (${String.format(Locale.US, "%.4f, %.4f", location.latitude, location.longitude)}), but could not resolve street name.",
                        isGpsOutsidePenrith = false
                    )
                }
                return@launch
            }

            val rawLine = runCatching { addressResult.getAddressLine(0) }.getOrNull().orEmpty()
            val firstSegment = rawLine.substringBefore(",").trim()

            var houseNum = addressResult.subThoroughfare?.trim().orEmpty()
            var streetName = AddressSearchEngine.normalizeSuburbOrStreet(
                addressResult.thoroughfare?.trim().orEmpty()
            )
            if (streetName.isEmpty() && firstSegment.isNotEmpty()) {
                val parsedFirst = AddressSearchEngine.parseQuery(firstSegment)
                if (houseNum.isEmpty() && !parsedFirst.houseNumberPrefix.isNullOrBlank()) {
                    houseNum = parsedFirst.houseNumberPrefix
                }
                streetName = AddressSearchEngine.normalizeSuburbOrStreet(parsedFirst.streetSuburbPart)
            }

            // Check all Geocoder locality fields against Penrith LGA's 36 suburbs (normalizing "Saint Marys" -> "St Marys")
            val matchedPenrithSuburb = AddressSearchEngine.findMatchingPenrithSuburb(
                addressResult.subLocality,
                addressResult.locality,
                rawLine
            )

            val displaySuburb = matchedPenrithSuburb
                ?: AddressSearchEngine.normalizeSuburbOrStreet(
                    (addressResult.subLocality ?: addressResult.locality)?.trim().orEmpty()
                )

            val formattedDetected = listOf(
                listOf(houseNum, streetName).filter { it.isNotEmpty() }.joinToString(" "),
                displaySuburb
            ).filter { it.isNotEmpty() }.joinToString(", ")

            var allStreets = _addressPickerState.value.allStreets
            if (allStreets.isEmpty() && matchedPenrithSuburb != null) {
                val localityId = AddressSearchEngine.PENRITH_LOCALITIES
                    .firstOrNull { it.name.equals(matchedPenrithSuburb, ignoreCase = true) }
                    ?.id
                if (localityId != null) {
                    runCatching { api.fetchStreets(localityId) }.getOrNull()?.let { suburbStreets ->
                        val tagged = suburbStreets.map {
                            if (it.locality.isNullOrBlank()) it.copy(locality = matchedPenrithSuburb) else it
                        }
                        allStreets = tagged
                        _addressPickerState.update { it.copy(allStreets = tagged) }
                    }
                }
            }

            val streetQuery = listOf(houseNum, streetName, displaySuburb)
                .filter { it.isNotEmpty() }
                .joinToString(" ")

            val matchedInPenrith = if (allStreets.isNotEmpty() && streetQuery.isNotBlank()) {
                AddressSearchEngine.searchStreets(streetQuery, allStreets, limit = 5).ifEmpty {
                    // Fallback to searching by street name alone in case of border suburb naming
                    if (streetName.isNotBlank()) {
                        AddressSearchEngine.searchStreets("$houseNum $streetName", allStreets, limit = 5)
                    } else {
                        emptyList()
                    }
                }
            } else {
                emptyList()
            }

            val isInsidePenrith = matchedPenrithSuburb != null || matchedInPenrith.isNotEmpty()

            if (isInsidePenrith) {
                _addressPickerState.update {
                    it.copy(
                        isLocatingGps = false,
                        mode = AddressPickerMode.QUICK_SEARCH,
                        searchQuery = streetQuery,
                        gpsNoticeMessage = "Located: $formattedDetected",
                        isGpsOutsidePenrith = false
                    )
                }
                runQuickSearch(streetQuery)
            } else {
                _addressPickerState.update {
                    it.copy(
                        isLocatingGps = false,
                        gpsNoticeMessage = "Your current location ($formattedDetected) is outside the Penrith City Council area.",
                        isGpsOutsidePenrith = true,
                        manualAddressInput = formattedDetected.ifEmpty { "My Home ($displaySuburb)" }
                    )
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getBestDeviceLocation(): Location? = withContext(Dispatchers.IO) {
        val locationManager =
            appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return@withContext null

        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        ).filter {
            runCatching { locationManager.isProviderEnabled(it) }.getOrDefault(false)
        }

        // First check recent last-known location (< 10 minutes old)
        val now = System.currentTimeMillis()
        val lastKnown = providers.mapNotNull { provider ->
            runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
        }.maxByOrNull { it.time }

        if (lastKnown != null && (now - lastKnown.time) < 10 * 60 * 1000L) {
            return@withContext lastKnown
        }

        // Request fresh current location on API 30+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val activeProvider = providers.firstOrNull() ?: return@withContext lastKnown
            val fresh = suspendCancellableCoroutine<Location?> { cont ->
                val signal = CancellationSignal()
                cont.invokeOnCancellation { signal.cancel() }
                runCatching {
                    locationManager.getCurrentLocation(
                        activeProvider,
                        signal,
                        appContext.mainExecutor
                    ) { loc ->
                        if (cont.isActive) cont.resume(loc)
                    }
                }.onFailure {
                    if (cont.isActive) cont.resume(null)
                }
            }
            if (fresh != null) return@withContext fresh
        }

        lastKnown
    }

    @Suppress("DEPRECATION")
    private suspend fun reverseGeocodeLocation(lat: Double, lng: Double): Address? =
        withContext(Dispatchers.IO) {
            runCatching {
                val geocoder = Geocoder(appContext, Locale.getDefault())
                geocoder.getFromLocation(lat, lng, 1)?.firstOrNull()
            }.getOrNull()
        }

    fun updateManualScheduleInputs(
        address: String? = null,
        dayOfWeek: DayOfWeek? = null,
        nextAlternatingBin: BinType? = null
    ) {
        _addressPickerState.update {
            it.copy(
                manualAddressInput = address ?: it.manualAddressInput,
                manualDayOfWeek = dayOfWeek ?: it.manualDayOfWeek,
                manualNextAlternatingBin = nextAlternatingBin ?: it.manualNextAlternatingBin
            )
        }
    }

    fun saveManualSchedule() {
        val state = _addressPickerState.value
        val addressLabel = state.manualAddressInput.trim()
            .ifEmpty { state.searchQuery.trim() }
            .ifEmpty { "Manual Schedule (Outside Penrith)" }

        val manualData = ScheduleCalculator.createManualSchedule(
            addressLabel = addressLabel,
            collectionDayOfWeek = state.manualDayOfWeek,
            nextAlternatingBin = state.manualNextAlternatingBin
        )
        repository.saveCachedSchedule(manualData)
        _showAddressConfirmation.value = true
        _addressPickerState.update {
            it.copy(
                isOpen = false,
                searchQuery = "",
                errorMessage = null,
                gpsNoticeMessage = null
            )
        }
        _statusBannerMessage.value = "Address set to ${manualData.address} (changeable in Settings)"
        syncSystemIntegrations()
    }

    fun navigateBackInPicker() {
        val current = _addressPickerState.value
        if (current.mode == AddressPickerMode.QUICK_SEARCH && current.focusedStreetInQuickSearch != null) {
            clearFocusedStreetInQuickSearch()
            return
        }
        if (current.mode != AddressPickerMode.BROWSE_SUBURBS) {
            closeAddressPicker()
            return
        }
        when (current.step) {
            AddressLookupStep.PROPERTY -> {
                _addressPickerState.update {
                    it.copy(
                        step = AddressLookupStep.STREET,
                        searchQuery = "",
                        errorMessage = null
                    )
                }
            }
            AddressLookupStep.STREET -> {
                _addressPickerState.update {
                    it.copy(
                        step = AddressLookupStep.SUBURB,
                        searchQuery = "",
                        errorMessage = null
                    )
                }
            }
            AddressLookupStep.SUBURB -> closeAddressPicker()
        }
    }

    fun loadLocalities() {
        viewModelScope.launch {
            _addressPickerState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { api.fetchLocalities() }
                .onSuccess { list ->
                    _addressPickerState.update {
                        it.copy(
                            isLoading = false,
                            localities = list,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { err ->
                    _addressPickerState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Could not load Penrith suburbs: ${err.localizedMessage ?: "Network error"}"
                        )
                    }
                }
        }
    }

    fun selectLocality(locality: LocalityDto) {
        viewModelScope.launch {
            _addressPickerState.update {
                it.copy(
                    selectedLocality = locality,
                    step = AddressLookupStep.STREET,
                    searchQuery = "",
                    isLoading = true,
                    errorMessage = null,
                    streets = emptyList()
                )
            }
            runCatching { api.fetchStreets(locality.id) }
                .onSuccess { list ->
                    _addressPickerState.update {
                        it.copy(
                            isLoading = false,
                            streets = list,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { err ->
                    _addressPickerState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Could not load streets in ${locality.name}: ${err.localizedMessage ?: "Network error"}"
                        )
                    }
                }
        }
    }

    fun selectStreet(street: StreetDto) {
        viewModelScope.launch {
            _addressPickerState.update {
                it.copy(
                    selectedStreet = street,
                    step = AddressLookupStep.PROPERTY,
                    searchQuery = "",
                    isLoading = true,
                    errorMessage = null,
                    properties = emptyList()
                )
            }
            runCatching { api.fetchProperties(street.id) }
                .onSuccess { list ->
                    streetPropertiesCache[street.id] = list
                    _addressPickerState.update {
                        it.copy(
                            isLoading = false,
                            properties = list,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { err ->
                    _addressPickerState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Could not load street numbers for ${street.name}: ${err.localizedMessage ?: "Network error"}"
                        )
                    }
                }
        }
    }

    fun selectProperty(property: PropertySummaryDto) {
        val state = _addressPickerState.value
        val suburb = state.focusedStreetInQuickSearch?.locality
            ?: state.selectedLocality?.name
            ?: "Penrith"
        fetchAndSavePropertySchedule(
            propertyId = property.id,
            fallbackAddress = property.name,
            suburb = suburb,
            fallbackZone = property.zone
        )
    }

    fun fetchAndSavePropertySchedule(
        propertyId: Int,
        fallbackAddress: String,
        suburb: String,
        fallbackZone: String? = null
    ) {
        viewModelScope.launch {
            _addressPickerState.update { it.copy(isLoading = true, errorMessage = null) }
            _isRefreshing.value = true
            runCatching {
                api.fetchPropertySchedule(
                    propertyId = propertyId,
                    fallbackAddress = fallbackAddress,
                    suburb = suburb,
                    fallbackZone = fallbackZone
                )
            }.onSuccess { scheduleData ->
                repository.saveCachedSchedule(scheduleData)
                _showAddressConfirmation.value = true
                _isRefreshing.value = false
                _addressPickerState.update {
                    it.copy(
                        isOpen = false,
                        isLoading = false,
                        searchQuery = "",
                        focusedStreetInQuickSearch = null,
                        errorMessage = null
                    )
                }
                _statusBannerMessage.value = "Address set to ${scheduleData.address} (changeable in Settings)"
                syncSystemIntegrations()
            }.onFailure { err ->
                _isRefreshing.value = false
                _addressPickerState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to fetch bin schedule: ${err.localizedMessage ?: "Check connection"}"
                    )
                }
            }
        }
    }

    fun refreshCurrentSchedule(silent: Boolean = false) {
        val current = repository.loadCachedSchedule() ?: return
        if (current.propertyId <= 0) {
            if (!silent) {
                _statusBannerMessage.value = "Using custom Manual Bin Schedule"
            }
            return
        }
        viewModelScope.launch {
            if (!silent) _isRefreshing.value = true
            runCatching {
                api.fetchPropertySchedule(
                    propertyId = current.propertyId,
                    fallbackAddress = current.address,
                    suburb = current.suburb,
                    fallbackZone = current.zone
                )
            }.onSuccess { updated ->
                repository.saveCachedSchedule(updated)
                _isRefreshing.value = false
                if (!silent) {
                    _statusBannerMessage.value = "Schedule synced with Penrith Waste-Info"
                }
                syncSystemIntegrations()
            }.onFailure { err ->
                _isRefreshing.value = false
                if (!silent) {
                    _statusBannerMessage.value =
                        "Offline: Using cached schedule (${err.localizedMessage ?: "network unavailable"})"
                }
            }
        }
    }

    fun toggleNextCollectionPutOut(collectionDate: LocalDate) {
        val iso = collectionDate.toString()
        var markedOut = false
        repository.updateUserSettings { current ->
            val nextVal = if (current.checkedOffCollectionDateIso == iso) null else iso
            markedOut = nextVal != null
            current.copy(checkedOffCollectionDateIso = nextVal)
        }
        if (markedOut) {
            BinNotificationHelper.dismissActiveReminderNotifications(appContext)
        }
        syncSystemIntegrations()
    }

    fun setDarkModeEnabled(enabled: Boolean) {
        repository.updateUserSettings { it.copy(darkModeEnabled = enabled) }
    }

    fun markSupportHeartClicked() {
        if (!repository.userSettings.value.hasClickedSupportHeart) {
            repository.updateUserSettings { it.copy(hasClickedSupportHeart = true) }
        }
    }

    fun dismissWelcomePopupForSixMonths() {
        repository.updateUserSettings {
            it.copy(lastWelcomePopupShownEpochMillis = System.currentTimeMillis())
        }
    }

    fun markWelcomePopupEngaged() {
        repository.updateUserSettings {
            it.copy(
                hasEngagedWithWelcomePopup = true,
                lastWelcomePopupShownEpochMillis = System.currentTimeMillis()
            )
        }
    }

    fun setAppIcon(option: AppIconOption, applyImmediately: Boolean = false) {
        repository.updateUserSettings { it.copy(appIconKey = option.key) }
        if (applyImmediately) {
            AppIconManager.applyAppIconIfNeeded(appContext, option)
            _statusBannerMessage.value = "Home screen icon updated to ${option.displayName}!"
        } else {
            _statusBannerMessage.value = "${option.displayName} icon selected (updates when returning to Home Screen)"
        }
    }

    fun setWeeklyRedBinOverride(enabled: Boolean) {
        repository.updateUserSettings { it.copy(weeklyRedBinOverride = enabled) }
        syncSystemIntegrations()
    }

    fun setHideGreenBinOverride(enabled: Boolean) {
        repository.updateUserSettings { it.copy(hideGreenBinOverride = enabled) }
        syncSystemIntegrations()
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        repository.updateUserSettings { it.copy(notificationsEnabled = enabled) }
        syncSystemIntegrations()
    }

    fun setEveningReminder(enabled: Boolean, hour: Int? = null, minute: Int? = null) {
        repository.updateUserSettings {
            it.copy(
                eveningReminderEnabled = enabled,
                eveningReminderHour = hour ?: it.eveningReminderHour,
                eveningReminderMinute = minute ?: it.eveningReminderMinute
            )
        }
        syncSystemIntegrations()
    }

    fun setMorningReminder(enabled: Boolean, hour: Int? = null, minute: Int? = null) {
        repository.updateUserSettings {
            it.copy(
                morningReminderEnabled = enabled,
                morningReminderHour = hour ?: it.morningReminderHour,
                morningReminderMinute = minute ?: it.morningReminderMinute
            )
        }
        syncSystemIntegrations()
    }

    fun setBringInReminder(enabled: Boolean, hour: Int? = null, minute: Int? = null) {
        repository.updateUserSettings {
            it.copy(
                bringInReminderEnabled = enabled,
                bringInReminderHour = hour ?: it.bringInReminderHour,
                bringInReminderMinute = minute ?: it.bringInReminderMinute
            )
        }
        syncSystemIntegrations()
    }

    fun sendTestNotification(): Boolean {
        val state = uiState.value
        val sampleDay = state.nextCollection ?: ScheduledCollectionDay(
            date = LocalDate.now().plusDays(1),
            bins = listOf(BinType.YELLOW, BinType.GREEN)
        )
        val address = state.scheduleData?.address ?: "Penrith LGA Property"
        val sent = BinNotificationHelper.showCollectionNotification(
            context = appContext,
            collectionDay = sampleDay,
            address = address,
            isEveningReminder = true,
            isTest = true
        )
        _statusBannerMessage.value = if (sent) {
            "Test notification sent! Check your notification shade."
        } else {
            "Please grant Notification permission first."
        }
        return sent
    }

    private fun syncSystemIntegrations() {
        BinNotificationHelper.rescheduleReminders(appContext)
        BinAppWidgetProvider.updateAllWidgets(appContext)
    }
}
