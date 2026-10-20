package au.com.penrithbinapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import au.com.penrithbinapp.model.BinType
import au.com.penrithbinapp.model.ScheduledCollectionDay
import au.com.penrithbinapp.ui.BinUiState
import au.com.penrithbinapp.ui.components.BinPillBadge
import au.com.penrithbinapp.ui.components.BinUpgradesInfoCard
import au.com.penrithbinapp.ui.components.BulkyWasteInfoCard
import au.com.penrithbinapp.ui.components.DetailedBinCard
import au.com.penrithbinapp.ui.components.WheelieBinIllustration
import au.com.penrithbinapp.ui.components.heroPillLabel
import au.com.penrithbinapp.ui.components.heroRoleLabel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val fullDateFormat = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)
private val shortDateFormat = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    uiState: BinUiState,
    onOpenAddressPicker: () -> Unit,
    onQuickLoadSampleAddress: () -> Unit,
    onDismissAddressConfirmation: () -> Unit,
    onTogglePutOut: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val schedule = uiState.scheduleData
    val nextCollection = uiState.nextCollection

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (schedule == null) {
            item {
                WelcomeSetupCard(
                    isLoading = uiState.isRefreshingSchedule,
                    onFindAddress = onOpenAddressPicker,
                    onQuickLoadSample = onQuickLoadSampleAddress
                )
            }
            item {
                Text(
                    text = "Penrith 3-Bin Guide",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(BinType.entries.size) { index ->
                DetailedBinCard(binType = BinType.entries[index])
            }
        } else {
            // 1. One-time Confirmation Card after setting an address (dismissible, then only in Settings)
            if (uiState.showAddressSetConfirmation) {
                item {
                    AddressSetConfirmationCard(
                        address = schedule.address,
                        onDismiss = onDismissAddressConfirmation
                    )
                }
            }

            // 2. Hero Next Collection Card
            if (nextCollection != null) {
                val isCheckedOff =
                    uiState.settings.checkedOffCollectionDateIso == nextCollection.date.toString()

                item {
                    NextCollectionHeroCard(
                        nextCollection = nextCollection,
                        isCheckedOff = isCheckedOff,
                        onTogglePutOut = { onTogglePutOut(nextCollection.date) }
                    )
                }

                // 3. Detailed Bin Cards for Next Collection
                item {
                    Text(
                        text = "Bins Going Out (${nextCollection.bins.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(nextCollection.bins.size) { idx ->
                    val binType = nextCollection.bins[idx]
                    DetailedBinCard(
                        binType = binType,
                        hasWeeklyOverrideBadge = binType == BinType.RED &&
                            nextCollection.hasWeeklyRedOverrideApplied
                    )
                }
            }

            // 4. Following Collection Preview ("After That")
            uiState.followingCollection?.let { following ->
                item {
                    FollowingCollectionCard(following = following)
                }
            }
        }
    }
}

@Composable
private fun WelcomeSetupCard(
    isLoading: Boolean,
    onFindAddress: () -> Unit,
    onQuickLoadSample: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WheelieBinIllustration(binType = BinType.RED, size = 56.dp)
                WheelieBinIllustration(binType = BinType.GREEN, size = 64.dp)
                WheelieBinIllustration(binType = BinType.YELLOW, size = 56.dp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welcome to Penrith Bin App",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Find your exact Penrith City Council kerbside bin schedule (Red, Yellow & Green FOGO), cache it offline, and get bin night reminders.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onFindAddress,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Icon(imageVector = Icons.Default.Search, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Find My Penrith Address",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onQuickLoadSample,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Loading Schedule...")
                } else {
                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Try Example: 1 Allard St, Penrith")
                }
            }
        }
    }
}

@Composable
private fun AddressSetConfirmationCard(
    address: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Address Set: $address",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Your bin schedule is active. You can change or re-sync your address anytime in Settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Got it", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun NextCollectionHeroCard(
    nextCollection: ScheduledCollectionDay,
    isCheckedOff: Boolean,
    onTogglePutOut: () -> Unit
) {
    val today = LocalDate.now()
    val daysUntil = ChronoUnit.DAYS.between(today, nextCollection.date).toInt()

    val countdownLabel = when {
        daysUntil == 0 -> "COLLECTION TODAY"
        daysUntil == 1 -> "PUT BINS OUT TONIGHT • TOMORROW"
        else -> "IN $daysUntil DAYS"
    }

    val badgeColor = when {
        isCheckedOff -> Color(0xFF2E7D32)
        daysUntil <= 1 -> Color(0xFFE65100)
        else -> Color(0xFF1565C0)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1C262E),
                            Color(0xFF26343F)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = badgeColor,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = if (isCheckedOff) "✓ BINS ON KERB" else countdownLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }

                if (nextCollection.hasWeeklyRedOverrideApplied) {
                    Surface(
                        color = Color.White.copy(alpha = 0.14f),
                        contentColor = Color.White,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "Weekly Red Active",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Next Bin Collection",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFFB0BEC5)
            )
            Text(
                text = nextCollection.date.format(fullDateFormat),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Visual Wheelie Bins Row (scales cleanly for 1, 2, or 3 bins across all screen widths)
            val binCount = nextCollection.bins.size
            val circleSize = when {
                binCount <= 1 -> 92.dp
                binCount == 2 -> 82.dp
                else -> 68.dp
            }
            val binGraphicSize = when {
                binCount <= 1 -> 72.dp
                binCount == 2 -> 64.dp
                else -> 54.dp
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                nextCollection.bins.forEach { bin ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(circleSize)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            WheelieBinIllustration(binType = bin, size = binGraphicSize)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        BinPillBadge(
                            binType = bin,
                            labelText = bin.heroPillLabel()
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = bin.heroRoleLabel(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFB0BEC5),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Interactive "Mark Bins Put Out" Checklist Button
            FilledTonalButton(
                onClick = onTogglePutOut,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (isCheckedOff) {
                        Color(0xFF2E7D32)
                    } else {
                        Color.White.copy(alpha = 0.14f)
                    },
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = if (isCheckedOff) {
                        Icons.Default.CheckCircle
                    } else {
                        Icons.Default.RadioButtonUnchecked
                    },
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isCheckedOff) {
                        "Bins Marked as Put Out (Tap to Undo)"
                    } else {
                        "Mark Bins as Put Out on Kerb"
                    },
                    fontWeight = FontWeight.Bold
                )
            }

            if (isCheckedOff) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Reminders silenced for this collection • Resets automatically the day after pickup",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFB0BEC5),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FollowingCollectionCard(
    following: ScheduledCollectionDay
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Following Collection",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = following.date.format(shortDateFormat),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    following.bins.forEach { bin ->
                        BinPillBadge(binType = bin)
                    }
                }
            }
        }
    }
}
