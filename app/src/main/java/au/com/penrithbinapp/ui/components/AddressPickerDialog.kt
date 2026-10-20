package au.com.penrithbinapp.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import au.com.penrithbinapp.data.AddressSearchEngine
import au.com.penrithbinapp.model.BinType
import au.com.penrithbinapp.model.LocalityDto
import au.com.penrithbinapp.model.PropertySummaryDto
import au.com.penrithbinapp.model.StreetDto
import au.com.penrithbinapp.ui.AddressLookupStep
import au.com.penrithbinapp.ui.AddressPickerMode
import au.com.penrithbinapp.ui.AddressPickerState
import au.com.penrithbinapp.ui.QuickPropertyMatch
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddressPickerDialog(
    state: AddressPickerState,
    onModeChange: (AddressPickerMode) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onUseGpsLocation: () -> Unit,
    onSelectQuickProperty: (QuickPropertyMatch) -> Unit,
    onSelectQuickStreet: (StreetDto) -> Unit,
    onClearFocusedQuickStreet: () -> Unit,
    onSelectLocality: (LocalityDto) -> Unit,
    onSelectStreet: (StreetDto) -> Unit,
    onSelectProperty: (PropertySummaryDto) -> Unit,
    onUpdateManualInputs: (String?, DayOfWeek?, BinType?) -> Unit,
    onSaveManualSchedule: () -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!state.isOpen) return

    val context = LocalContext.current
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            onUseGpsLocation()
        }
    }

    val triggerGpsCheck = {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            onUseGpsLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (state.mode) {
                                AddressPickerMode.QUICK_SEARCH -> "Find Your Address"
                                AddressPickerMode.BROWSE_SUBURBS -> when (state.step) {
                                    AddressLookupStep.SUBURB -> "1. Choose Penrith Suburb"
                                    AddressLookupStep.STREET -> "2. Choose Street"
                                    AddressLookupStep.PROPERTY -> "3. Select House Number"
                                }
                                AddressPickerMode.MANUAL_SCHEDULE -> "Manual Bin Schedule"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val subtitle = when (state.mode) {
                            AddressPickerMode.QUICK_SEARCH -> {
                                state.focusedStreetInQuickSearch?.let {
                                    "${it.name}, ${it.locality.orEmpty()} • Pick house number"
                                } ?: "Type street & number or use GPS"
                            }
                            AddressPickerMode.BROWSE_SUBURBS -> when (state.step) {
                                AddressLookupStep.SUBURB -> "Penrith City Council (39 Suburbs)"
                                AddressLookupStep.STREET -> "Suburb: ${state.selectedLocality?.name.orEmpty()}"
                                AddressLookupStep.PROPERTY ->
                                    "${state.selectedStreet?.name.orEmpty()}, ${state.selectedLocality?.name.orEmpty()}"
                            }
                            AddressPickerMode.MANUAL_SCHEDULE -> "For addresses outside Penrith Council or custom setups"
                        }
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }

                // Mode Switcher Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.mode == AddressPickerMode.QUICK_SEARCH,
                        onClick = { onModeChange(AddressPickerMode.QUICK_SEARCH) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("Quick Search", style = MaterialTheme.typography.labelMedium) }
                    )
                    FilterChip(
                        selected = state.mode == AddressPickerMode.BROWSE_SUBURBS,
                        onClick = { onModeChange(AddressPickerMode.BROWSE_SUBURBS) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.LocationCity,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("Suburbs", style = MaterialTheme.typography.labelMedium) }
                    )
                    FilterChip(
                        selected = state.mode == AddressPickerMode.MANUAL_SCHEDULE,
                        onClick = { onModeChange(AddressPickerMode.MANUAL_SCHEDULE) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.EditCalendar,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("Manual", style = MaterialTheme.typography.labelMedium) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                when (state.mode) {
                    AddressPickerMode.QUICK_SEARCH -> {
                        QuickSearchContent(
                            state = state,
                            onSearchQueryChange = onSearchQueryChange,
                            onTriggerGps = triggerGpsCheck,
                            onSelectQuickProperty = onSelectQuickProperty,
                            onSelectQuickStreet = onSelectQuickStreet,
                            onClearFocusedStreet = onClearFocusedQuickStreet,
                            onSelectPropertyFromFocusedStreet = onSelectProperty,
                            onSwitchToBrowseSuburbs = { onModeChange(AddressPickerMode.BROWSE_SUBURBS) },
                            onSwitchToManualSchedule = { prefill ->
                                if (!prefill.isNullOrBlank()) {
                                    onUpdateManualInputs(prefill, null, null)
                                }
                                onModeChange(AddressPickerMode.MANUAL_SCHEDULE)
                            },
                            onRetry = onRetry,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    AddressPickerMode.BROWSE_SUBURBS -> {
                        BrowseSuburbsContent(
                            state = state,
                            onSearchQueryChange = onSearchQueryChange,
                            onSelectLocality = onSelectLocality,
                            onSelectStreet = onSelectStreet,
                            onSelectProperty = onSelectProperty,
                            onSwitchToManualSchedule = {
                                onModeChange(AddressPickerMode.MANUAL_SCHEDULE)
                            },
                            onRetry = onRetry,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    AddressPickerMode.MANUAL_SCHEDULE -> {
                        ManualScheduleContent(
                            state = state,
                            onUpdateInputs = onUpdateManualInputs,
                            onSaveManualSchedule = onSaveManualSchedule,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickSearchContent(
    state: AddressPickerState,
    onSearchQueryChange: (String) -> Unit,
    onTriggerGps: () -> Unit,
    onSelectQuickProperty: (QuickPropertyMatch) -> Unit,
    onSelectQuickStreet: (StreetDto) -> Unit,
    onClearFocusedStreet: () -> Unit,
    onSelectPropertyFromFocusedStreet: (PropertySummaryDto) -> Unit,
    onSwitchToBrowseSuburbs: () -> Unit,
    onSwitchToManualSchedule: (String?) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Unified Address Search Box
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = {
                Text("Type address (e.g. 1 Allard St, Penrith)...")
            },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = null)
            },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // GPS Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                onClick = onTriggerGps,
                enabled = !state.isLocatingGps,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                if (state.isLocatingGps) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Locating via GPS...", style = MaterialTheme.typography.labelLarge)
                } else {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Use My Current Location (GPS)", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // GPS Notice or Out-of-Area Alert
        state.gpsNoticeMessage?.let { notice ->
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.isGpsOutsidePenrith) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    }
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (state.isGpsOutsidePenrith) {
                                Icons.Default.WarningAmber
                            } else {
                                Icons.Default.MyLocation
                            },
                            contentDescription = null,
                            tint = if (state.isGpsOutsidePenrith) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            },
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = notice,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (state.isGpsOutsidePenrith) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            }
                        )
                    }
                    if (state.isGpsOutsidePenrith) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { onSwitchToManualSchedule(state.manualAddressInput) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.EditCalendar, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Set Manual Bin Schedule for My Location")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()

        // Results Box
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                state.isLoading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Loading Penrith address directory...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                state.errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onRetry) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry")
                        }
                    }
                }

                // User tapped a specific street in Quick Search to view all numbers on that street
                state.focusedStreetInQuickSearch != null -> {
                    val street = state.focusedStreetInQuickSearch
                    val parsed = AddressSearchEngine.parseQuery(state.searchQuery)
                    val filteredProps = AddressSearchEngine.filterAndRankProperties(
                        properties = state.focusedStreetProperties,
                        houseNumberPrefix = parsed.houseNumberPrefix
                    )
                    Column(modifier = Modifier.fillMaxSize()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${street.name}, ${street.locality.orEmpty()}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Select your house or unit number below",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(onClick = onClearFocusedStreet) {
                                    Text("Change Street")
                                }
                            }
                        }

                        if (state.isSearchingProperties) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else if (filteredProps.isEmpty()) {
                            OutsidePenrithOrEmptyCard(
                                query = state.searchQuery,
                                onSetupManualSchedule = { onSwitchToManualSchedule(state.searchQuery) }
                            )
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(filteredProps, key = { it.id }) { prop ->
                                    PropertyRowItem(
                                        title = prop.name,
                                        subtitle = prop.zone?.let { "Zone: $it" },
                                        onClick = { onSelectPropertyFromFocusedStreet(prop) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Empty query -> Show helpful tips + quick actions
                state.searchQuery.isBlank() -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Instant Single-Box Search",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Type your house number and street directly (e.g. \"1 Allard St\" or \"14 High St Penrith\") and tap your property directly.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        item {
                            OutlinedButton(
                                onClick = onSwitchToBrowseSuburbs,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Explore, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Or Browse by Penrith Suburb (${state.localities.size.takeIf { it > 0 } ?: 39} Suburbs)")
                            }
                        }

                        item {
                            OutlinedButton(
                                onClick = { onSwitchToManualSchedule(null) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.EditCalendar, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Outside Penrith Area? Set Manual Schedule")
                            }
                        }
                    }
                }

                // Active search with no matches in Penrith LGA -> Show Outside Penrith Area helper
                state.matchedStreets.isEmpty() -> {
                    OutsidePenrithOrEmptyCard(
                        query = state.searchQuery,
                        onSetupManualSchedule = { onSwitchToManualSchedule(state.searchQuery) }
                    )
                }

                // Active search with Instant Property Matches + Matching Streets
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        if (state.quickPropertyMatches.isNotEmpty()) {
                            item {
                                SectionHeaderRow(
                                    title = "Instant Property Matches (1-Tap)",
                                    isLoading = state.isSearchingProperties
                                )
                            }
                            items(
                                items = state.quickPropertyMatches,
                                key = { "prop_${it.property.id}" }
                            ) { match ->
                                PropertyRowItem(
                                    title = match.property.name,
                                    subtitle = buildString {
                                        append(match.suburb)
                                        if (!match.property.zone.isNullOrBlank()) {
                                            append(" • Zone: ${match.property.zone}")
                                        }
                                    },
                                    onClick = { onSelectQuickProperty(match) }
                                )
                            }
                        } else if (state.isSearchingProperties) {
                            item {
                                SectionHeaderRow(
                                    title = "Matching House Numbers...",
                                    isLoading = true
                                )
                            }
                        }

                        item {
                            SectionHeaderRow(
                                title = "Matching Penrith Streets (Tap to see all numbers)",
                                isLoading = false
                            )
                        }

                        items(
                            items = state.matchedStreets,
                            key = { "street_${it.id}" }
                        ) { street ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectQuickStreet(street) }
                                    .padding(horizontal = 20.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Route,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = street.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (!street.locality.isNullOrBlank()) {
                                        Text(
                                            text = street.locality,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 56.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowseSuburbsContent(
    state: AddressPickerState,
    onSearchQueryChange: (String) -> Unit,
    onSelectLocality: (LocalityDto) -> Unit,
    onSelectStreet: (StreetDto) -> Unit,
    onSelectProperty: (PropertySummaryDto) -> Unit,
    onSwitchToManualSchedule: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = {
                Text(
                    when (state.step) {
                        AddressLookupStep.SUBURB -> "Filter Penrith suburbs (e.g. Glenmore Park, St Marys)..."
                        AddressLookupStep.STREET -> "Filter streets in ${state.selectedLocality?.name.orEmpty()}..."
                        AddressLookupStep.PROPERTY -> "Filter by house or unit number..."
                    }
                )
            },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = null)
            },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider()

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                state.isLoading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Fetching from Penrith Waste-Info...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                state.errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onRetry) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry")
                        }
                    }
                }

                else -> {
                    val q = state.searchQuery.trim().lowercase()
                    when (state.step) {
                        AddressLookupStep.SUBURB -> {
                            val filtered = state.localities.filter {
                                q.isEmpty() || it.name.lowercase().contains(q)
                            }
                            if (filtered.isEmpty()) {
                                OutsidePenrithOrEmptyCard(
                                    query = state.searchQuery,
                                    onSetupManualSchedule = onSwitchToManualSchedule
                                )
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(filtered, key = { it.id }) { locality ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onSelectLocality(locality) }
                                                .padding(horizontal = 20.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocationCity,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Text(
                                                text = locality.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        HorizontalDivider(
                                            modifier = Modifier.padding(start = 56.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                        )
                                    }
                                }
                            }
                        }

                        AddressLookupStep.STREET -> {
                            val filtered = state.streets.filter {
                                q.isEmpty() || it.name.lowercase().contains(q)
                            }
                            if (filtered.isEmpty()) {
                                OutsidePenrithOrEmptyCard(
                                    query = state.searchQuery,
                                    onSetupManualSchedule = onSwitchToManualSchedule
                                )
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(filtered, key = { it.id }) { street ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onSelectStreet(street) }
                                                .padding(horizontal = 20.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Route,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Text(
                                                text = street.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        HorizontalDivider(
                                            modifier = Modifier.padding(start = 56.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                        )
                                    }
                                }
                            }
                        }

                        AddressLookupStep.PROPERTY -> {
                            val filtered = state.properties.filter {
                                q.isEmpty() || it.name.lowercase().contains(q)
                            }
                            if (filtered.isEmpty()) {
                                OutsidePenrithOrEmptyCard(
                                    query = state.searchQuery,
                                    onSetupManualSchedule = onSwitchToManualSchedule
                                )
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(filtered, key = { it.id }) { prop ->
                                        PropertyRowItem(
                                            title = prop.name,
                                            subtitle = prop.zone?.let { "Zone: $it" },
                                            onClick = { onSelectProperty(prop) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ManualScheduleContent(
    state: AddressPickerState,
    onUpdateInputs: (String?, DayOfWeek?, BinType?) -> Unit,
    onSaveManualSchedule: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Outside Penrith City Council Area?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Penrith Council's online lookup only covers properties inside the Penrith LGA. If you live in a neighbouring council (e.g. Blacktown, Blue Mountains, Hawkesbury, Parramatta) or a new estate not yet listed, set your collection day and alternating week below to get full calendar tracking, widgets, and reminders!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f)
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = state.manualAddressInput,
                onValueChange = { onUpdateInputs(it, null, null) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Address or Label (Optional)") },
                placeholder = { Text("e.g. 12 Main St, Blacktown") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )
        }

        item {
            Text(
                text = "1. Which day of the week are your bins collected?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val days = listOf(
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY,
                    DayOfWeek.SATURDAY,
                    DayOfWeek.SUNDAY
                )
                days.forEach { day ->
                    FilterChip(
                        selected = state.manualDayOfWeek == day,
                        onClick = { onUpdateInputs(null, day, null) },
                        label = {
                            Text(day.getDisplayName(TextStyle.FULL, Locale.ENGLISH))
                        }
                    )
                }
            }
        }

        item {
            Text(
                text = "2. Which fortnightly bin goes out on your NEXT collection?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ManualCycleOptionCard(
                    title = "Yellow (Recycling) + Green",
                    subtitle = "Next collection is Recycling week (then Red Waste the week after)",
                    binType = BinType.YELLOW,
                    selected = state.manualNextAlternatingBin == BinType.YELLOW,
                    onClick = { onUpdateInputs(null, null, BinType.YELLOW) }
                )
                ManualCycleOptionCard(
                    title = "Red (General Waste) + Green",
                    subtitle = "Next collection is General Waste week (then Yellow Recycling the week after)",
                    binType = BinType.RED,
                    selected = state.manualNextAlternatingBin == BinType.RED,
                    onClick = { onUpdateInputs(null, null, BinType.RED) }
                )
            }
        }

        item {
            Button(
                onClick = onSaveManualSchedule,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save Manual Bin Schedule",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ManualCycleOptionCard(
    title: String,
    subtitle: String,
    binType: BinType,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WheelieBinIllustration(binType = binType, size = 46.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SectionHeaderRow(
    title: String,
    isLoading: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(horizontal = 16.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp
            )
        }
    }
}

@Composable
private fun PropertyRowItem(
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Home,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    )
}

@Composable
private fun OutsidePenrithOrEmptyCard(
    query: String,
    onSetupManualSchedule: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (query.isNotBlank()) {
                        "\"$query\" not found in Penrith Council"
                    } else {
                        "No matching Penrith address found"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "This database only includes the 39 suburbs within Penrith City Council LGA. If your home is outside the Penrith area (or in a new estate), you can set a Manual Bin Schedule in seconds!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onSetupManualSchedule,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.EditCalendar, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Set Manual Bin Schedule")
                }
            }
        }
    }
}
