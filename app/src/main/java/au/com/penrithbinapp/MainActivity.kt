package au.com.penrithbinapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.penrithbinapp.data.AppIconManager
import au.com.penrithbinapp.data.BinRepository
import au.com.penrithbinapp.model.AppIconOption
import au.com.penrithbinapp.theme.PenrithBinAppTheme
import au.com.penrithbinapp.ui.AddressLookupStep
import au.com.penrithbinapp.ui.BinViewModel
import au.com.penrithbinapp.ui.components.AddressPickerDialog
import au.com.penrithbinapp.ui.components.LocalCreatorPopupDialog
import au.com.penrithbinapp.ui.screens.DashboardScreen
import au.com.penrithbinapp.ui.screens.DonationScreen
import au.com.penrithbinapp.ui.screens.DropOffServicesScreen
import au.com.penrithbinapp.ui.screens.ScheduleCalendarScreen
import au.com.penrithbinapp.ui.screens.SettingsScreen
import au.com.penrithbinapp.widget.BinAppWidgetProvider

enum class AppTab(val label: String) {
    DASHBOARD("Next Bins"),
    CALENDAR("Schedule"),
    DROP_OFF("Drop-Off"),
    SETTINGS("Settings")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        BinAppWidgetProvider.updateAllWidgets(this)
        setContent {
            val viewModel: BinViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            PenrithBinAppTheme(darkTheme = uiState.settings.darkModeEnabled) {
                PenrithBinAppRoot(viewModel = viewModel, uiState = uiState)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            val selectedKey = BinRepository.getInstance(this).userSettings.value.appIconKey
            AppIconManager.applyAppIconIfNeeded(this, AppIconOption.fromKey(selectedKey))
        }
    }
}

private const val SIX_MONTHS_MILLIS = 180L * 24L * 60L * 60L * 1000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PenrithBinAppRoot(
    viewModel: BinViewModel = viewModel(),
    uiState: au.com.penrithbinapp.ui.BinUiState = viewModel.uiState.collectAsStateWithLifecycle().value
) {
    val pickerState by viewModel.addressPickerState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.DASHBOARD) }
    var showDonationScreen by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val shouldShowWelcomePopup = !uiState.settings.hasEngagedWithWelcomePopup && (
        uiState.settings.lastWelcomePopupShownEpochMillis == 0L ||
            (System.currentTimeMillis() - uiState.settings.lastWelcomePopupShownEpochMillis) >= SIX_MONTHS_MILLIS
        )

    if (showDonationScreen) {
        BackHandler { showDonationScreen = false }
        DonationScreen(
            onNavigateBack = { showDonationScreen = false }
        )
        return
    }

    LaunchedEffect(uiState.statusBannerMessage) {
        val msg = uiState.statusBannerMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusBanner()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Penrith Bins",
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                actions = {
                    val shouldPulse = !uiState.settings.hasClickedSupportHeart
                    val heartScale = if (shouldPulse) {
                        val infiniteTransition = rememberInfiniteTransition(label = "SupportHeartPulse")
                        val animatedScale by infiniteTransition.animateFloat(
                            initialValue = 1.0f,
                            targetValue = 1.24f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "HeartPulseScale"
                        )
                        animatedScale
                    } else {
                        1.0f
                    }
                    IconButton(
                        onClick = {
                            viewModel.markSupportHeartClicked()
                            showDonationScreen = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Support the Developer",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.scale(heartScale)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == AppTab.DASHBOARD,
                    onClick = { selectedTab = AppTab.DASHBOARD },
                    icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                    label = { Text(AppTab.DASHBOARD.label) }
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.CALENDAR,
                    onClick = { selectedTab = AppTab.CALENDAR },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                    label = { Text(AppTab.CALENDAR.label) }
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.DROP_OFF,
                    onClick = { selectedTab = AppTab.DROP_OFF },
                    icon = { Icon(Icons.Default.Recycling, contentDescription = null) },
                    label = { Text(AppTab.DROP_OFF.label) }
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.SETTINGS,
                    onClick = { selectedTab = AppTab.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(AppTab.SETTINGS.label) }
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            AppTab.DASHBOARD -> {
                DashboardScreen(
                    uiState = uiState,
                    onOpenAddressPicker = { viewModel.openAddressPicker() },
                    onQuickLoadSampleAddress = {
                        viewModel.fetchAndSavePropertySchedule(
                            propertyId = 2861017,
                            fallbackAddress = "1 Allard Street, Penrith",
                            suburb = "Penrith",
                            fallbackZone = "week 2"
                        )
                    },
                    onDismissAddressConfirmation = { viewModel.dismissAddressConfirmation() },
                    onTogglePutOut = { date -> viewModel.toggleNextCollectionPutOut(date) },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            AppTab.CALENDAR -> {
                ScheduleCalendarScreen(
                    uiState = uiState,
                    onOpenAddressPicker = { viewModel.openAddressPicker() },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            AppTab.DROP_OFF -> {
                DropOffServicesScreen(
                    modifier = Modifier.padding(innerPadding)
                )
            }

            AppTab.SETTINGS -> {
                SettingsScreen(
                    uiState = uiState,
                    onOpenAddressPicker = { viewModel.openAddressPicker() },
                    onRefreshSchedule = { viewModel.refreshCurrentSchedule(silent = false) },
                    onDarkModeChange = { viewModel.setDarkModeEnabled(it) },
                    onSelectAppIcon = { option, applyNow -> viewModel.setAppIcon(option, applyNow) },
                    onWeeklyRedOverrideChange = { viewModel.setWeeklyRedBinOverride(it) },
                    onHideGreenOverrideChange = { viewModel.setHideGreenBinOverride(it) },
                    onNotificationsEnabledChange = { viewModel.setNotificationsEnabled(it) },
                    onEveningReminderChange = { enabled, h, m ->
                        viewModel.setEveningReminder(enabled, h, m)
                    },
                    onMorningReminderChange = { enabled, h, m ->
                        viewModel.setMorningReminder(enabled, h, m)
                    },
                    onBringInReminderChange = { enabled, h, m ->
                        viewModel.setBringInReminder(enabled, h, m)
                    },
                    onSendTestNotification = { viewModel.sendTestNotification() },
                    onNavigateToDonation = { showDonationScreen = true },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }

    AddressPickerDialog(
        state = pickerState,
        onModeChange = { viewModel.setAddressPickerMode(it) },
        onSearchQueryChange = { viewModel.updatePickerSearchQuery(it) },
        onUseGpsLocation = { viewModel.useCurrentGpsLocation() },
        onSelectQuickProperty = { viewModel.selectQuickPropertyMatch(it) },
        onSelectQuickStreet = { viewModel.selectStreetInQuickSearch(it) },
        onClearFocusedQuickStreet = { viewModel.clearFocusedStreetInQuickSearch() },
        onSelectLocality = { viewModel.selectLocality(it) },
        onSelectStreet = { viewModel.selectStreet(it) },
        onSelectProperty = { viewModel.selectProperty(it) },
        onUpdateManualInputs = { addr, dow, bin ->
            viewModel.updateManualScheduleInputs(addr, dow, bin)
        },
        onSaveManualSchedule = { viewModel.saveManualSchedule() },
        onBack = { viewModel.navigateBackInPicker() },
        onRetry = {
            if (pickerState.mode == au.com.penrithbinapp.ui.AddressPickerMode.QUICK_SEARCH) {
                viewModel.preloadAddressCatalogue()
            } else {
                when (pickerState.step) {
                    AddressLookupStep.SUBURB -> viewModel.loadLocalities()
                    AddressLookupStep.STREET -> pickerState.selectedLocality?.let { viewModel.selectLocality(it) }
                    AddressLookupStep.PROPERTY -> pickerState.selectedStreet?.let { viewModel.selectStreet(it) }
                }
            }
        },
        onDismiss = { viewModel.closeAddressPicker() }
    )

    if (shouldShowWelcomePopup && !pickerState.isOpen && !showDonationScreen) {
        LocalCreatorPopupDialog(
            onDismissForSixMonths = { viewModel.dismissWelcomePopupForSixMonths() },
            onEngaged = { viewModel.markWelcomePopupEngaged() },
            onOpenDonationScreen = {
                viewModel.markSupportHeartClicked()
                showDonationScreen = true
            }
        )
    }
}
