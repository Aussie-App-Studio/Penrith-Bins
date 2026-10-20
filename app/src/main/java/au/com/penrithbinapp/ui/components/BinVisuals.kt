package au.com.penrithbinapp.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.penrithbinapp.model.BinType

fun BinType.lidColor(): Color = when (this) {
    BinType.RED -> Color(0xFFEF3340)
    BinType.YELLOW -> Color(0xFFFFD600)
    BinType.GREEN -> Color(0xFF38B429)
}

fun BinType.accentBgColor(): Color = when (this) {
    BinType.RED -> Color(0xFFEF3340).copy(alpha = 0.14f)
    BinType.YELLOW -> Color(0xFFFFD600).copy(alpha = 0.16f)
    BinType.GREEN -> Color(0xFF38B429).copy(alpha = 0.14f)
}

fun BinType.badgeContentColor(): Color = when (this) {
    BinType.RED -> Color.White
    BinType.YELLOW -> Color(0xFF1B1B1B)
    BinType.GREEN -> Color.White
}

fun BinType.heroPillLabel(): String = when (this) {
    BinType.RED -> "Red Bin"
    BinType.YELLOW -> "Yellow Bin"
    BinType.GREEN -> "Green Bin"
}

fun BinType.heroRoleLabel(): String = when (this) {
    BinType.RED -> "General Waste"
    BinType.YELLOW -> "Recycling"
    BinType.GREEN -> "FOGO Organics"
}

/**
 * Custom Canvas-drawn Australian Kerbside Wheelie Bin with colored lid (Red, Yellow, Green)
 * and dark green/charcoal body with wheels.
 */
@Composable
fun WheelieBinIllustration(
    binType: BinType,
    modifier: Modifier = Modifier,
    size: Dp = 68.dp
) {
    val lidColor = binType.lidColor()
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Shadow at base
        drawOval(
            color = Color.Black.copy(alpha = 0.18f),
            topLeft = Offset(w * 0.16f, h * 0.89f),
            size = Size(w * 0.68f, h * 0.08f)
        )

        // Tapered bin body (trapezoid)
        val bodyPath = Path().apply {
            moveTo(w * 0.20f, h * 0.24f)
            lineTo(w * 0.80f, h * 0.24f)
            lineTo(w * 0.72f, h * 0.86f)
            lineTo(w * 0.28f, h * 0.86f)
            close()
        }
        drawPath(
            path = bodyPath,
            color = Color(0xFF283339)
        )

        // Subtle highlight on left half of bin body
        val bodyHighlightPath = Path().apply {
            moveTo(w * 0.22f, h * 0.25f)
            lineTo(w * 0.48f, h * 0.25f)
            lineTo(w * 0.45f, h * 0.85f)
            lineTo(w * 0.29f, h * 0.85f)
            close()
        }
        drawPath(
            path = bodyHighlightPath,
            color = Color(0xFF324047)
        )

        // Vertical ribs on bin body
        val ribColor = Color(0xFF1D252A)
        drawRoundRect(
            color = ribColor,
            topLeft = Offset(w * 0.36f, h * 0.32f),
            size = Size(w * 0.045f, h * 0.44f),
            cornerRadius = CornerRadius(4f, 4f)
        )
        drawRoundRect(
            color = ribColor,
            topLeft = Offset(w * 0.48f, h * 0.32f),
            size = Size(w * 0.045f, h * 0.44f),
            cornerRadius = CornerRadius(4f, 4f)
        )
        drawRoundRect(
            color = ribColor,
            topLeft = Offset(w * 0.60f, h * 0.32f),
            size = Size(w * 0.045f, h * 0.44f),
            cornerRadius = CornerRadius(4f, 4f)
        )

        // Colored Emblem Strip on Body
        drawRoundRect(
            color = lidColor,
            topLeft = Offset(w * 0.33f, h * 0.45f),
            size = Size(w * 0.34f, h * 0.12f),
            cornerRadius = CornerRadius(8f, 8f)
        )

        // Bin Lid Dome (Top)
        drawRoundRect(
            color = lidColor,
            topLeft = Offset(w * 0.18f, h * 0.10f),
            size = Size(w * 0.64f, h * 0.11f),
            cornerRadius = CornerRadius(12f, 12f)
        )

        // Bin Lid Rim / Lip (Overhanging)
        drawRoundRect(
            color = lidColor,
            topLeft = Offset(w * 0.13f, h * 0.17f),
            size = Size(w * 0.74f, h * 0.08f),
            cornerRadius = CornerRadius(8f, 8f)
        )

        // Lid Rim Bottom Shadow Line
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.22f),
            topLeft = Offset(w * 0.15f, h * 0.23f),
            size = Size(w * 0.70f, h * 0.025f),
            cornerRadius = CornerRadius(4f, 4f)
        )

        // Wheels at bottom corners
        val wheelOuter = Color(0xFF141A1D)
        val wheelHub = Color(0xFF78909C)
        drawCircle(
            color = wheelOuter,
            radius = w * 0.095f,
            center = Offset(w * 0.27f, h * 0.85f)
        )
        drawCircle(
            color = wheelHub,
            radius = w * 0.038f,
            center = Offset(w * 0.27f, h * 0.85f)
        )

        drawCircle(
            color = wheelOuter,
            radius = w * 0.095f,
            center = Offset(w * 0.73f, h * 0.85f)
        )
        drawCircle(
            color = wheelHub,
            radius = w * 0.038f,
            center = Offset(w * 0.73f, h * 0.85f)
        )
    }
}

@Composable
fun BinPillBadge(
    binType: BinType,
    modifier: Modifier = Modifier,
    labelText: String = binType.shortName
) {
    Surface(
        modifier = modifier,
        color = binType.lidColor(),
        contentColor = binType.badgeContentColor(),
        shape = RoundedCornerShape(10.dp),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(binType.badgeContentColor().copy(alpha = 0.85f))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = labelText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun DetailedBinCard(
    binType: BinType,
    hasWeeklyOverrideBadge: Boolean = false,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                color = binType.lidColor().copy(alpha = 0.55f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(binType.accentBgColor()),
                    contentAlignment = Alignment.Center
                ) {
                    WheelieBinIllustration(binType = binType, size = 60.dp)
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = binType.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            color = binType.lidColor(),
                            contentColor = binType.badgeContentColor(),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.widthIn(min = 82.dp)
                        ) {
                            Text(
                                text = if (hasWeeklyOverrideBadge) "Weekly (Override)" else binType.defaultFrequency,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = binType.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (expanded) "Tap to hide sorting guide" else "Tap for what goes in this bin",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "What goes IN:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    binType.acceptedItems.forEach { item ->
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Keep OUT:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC62828)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    binType.prohibitedItems.forEach { item ->
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = null,
                                tint = Color(0xFFC62828),
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val BULKY_WASTE_URL =
    "https://www.penrithcity.nsw.gov.au/waste-environment/waste/bulky-waste-collection"
const val BIN_SERVICES_UPGRADE_URL =
    "https://www.penrithcity.nsw.gov.au/waste-environment/waste/bin-services"
private const val COUNCIL_PHONE_URI = "tel:0247327777"

private val bulkyKeyRules = listOf(
    "4 free collections per calendar year for eligible domestic households",
    "Up to 2 cubic metres (2m³) per collection — roughly 1 standard box trailer",
    "Must be booked in advance (scheduled on your normal bin collection day)",
    "Place items neatly on the kerb no earlier than the evening before collection",
    "Changes or cancellations require at least 5 business days' notice"
)

private val bulkyAcceptedItems = listOf(
    "Furniture, couches, tables, chairs, cabinets, rugs & mattresses",
    "Whitegoods & appliances (fridge/freezer doors must be removed or taped shut)",
    "Small appliances, electronics, toys & loose items packed in sturdy boxes/bags",
    "Bundled garden branches (< 1.2m long & < 15cm thick, tied with natural twine)"
)

private val bulkyProhibitedItems = listOf(
    "Building, renovation & demolition materials (bricks, concrete, tiles, plasterboard)",
    "Hazardous waste, asbestos, paint, chemicals, gas bottles & fire extinguishers",
    "Car parts, tyres, batteries, commercial waste & unboxed loose litter"
)

@Composable
fun BulkyWasteInfoCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    val accentColor = Color(0xFF29B6F6)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                color = accentColor.copy(alpha = 0.55f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Chair,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Bulky Waste",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            color = accentColor,
                            contentColor = Color(0xFF002538),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.widthIn(min = 82.dp)
                        ) {
                            Text(
                                text = "4 Free / Year",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Kerbside Household Clean-Up (Up to 2m³)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (expanded) "Tap to hide bulky waste rules & booking" else "Tap for rules, accepted items & booking",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Booking & Key Rules
                    Text(
                        text = "Penrith Council Rules & Entitlement:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    bulkyKeyRules.forEach { rule ->
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = rule,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "What goes OUT (Accepted):",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    bulkyAcceptedItems.forEach { item ->
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "NOT Accepted (Keep Out):",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC62828)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    bulkyProhibitedItems.forEach { item ->
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = null,
                                tint = Color(0xFFC62828),
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Buttons: Book Online & Call Council
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(BULKY_WASTE_URL))
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Book Online", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_DIAL, Uri.parse(COUNCIL_PHONE_URI))
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("(02) 4732 7777", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private val binUpgradeOptions = listOf(
    "Larger 240L Red Bin: Upgrade from the standard 140L Red bin to a larger 240L size",
    "Weekly Red Bin Service: Upgrade your Red bin from fortnightly to weekly collection (ideal for larger families, nappies, or medical needs)",
    "Larger 360L Yellow Bin: Upsize your fortnightly Yellow recycling bin from 240L to 360L",
    "Additional Bins: Request an extra Red, Yellow, or Green FOGO bin for your property",
    "Damaged or Stolen Bins: Request a free bin lid/wheel repair or replacement"
)

private val binUpgradeHowToApply = listOf(
    "Property Owner / Agent Required: Because annual service fees are added to Council rates, only the property owner or managing real estate agent can authorise bin upgrades (tenants should ask their landlord/agent)",
    "Apply Online or by Phone: Submit a request via Council's Bin Services portal (my.penrith.city) or call Waste Services on (02) 4732 7777",
    "Update This App: Once your weekly Red bin is active, turn on \"Weekly Red Bin Service\" in the Settings tab!"
)

@Composable
fun BinUpgradesInfoCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    val accentColor = Color(0xFFFF7043)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                color = accentColor.copy(alpha = 0.55f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircleOutline,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Bin Upgrades",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            color = accentColor,
                            contentColor = Color.White,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.widthIn(min = 82.dp)
                        ) {
                            Text(
                                text = "240L & Weekly",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Bigger Red Bin, Weekly Collection & Extra Bins",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (expanded) "Tap to hide upgrade options" else "Tap for how to get a bigger or weekly bin",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Available Bin Upgrades & Services:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    binUpgradeOptions.forEach { item ->
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "How to Request an Upgrade:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    binUpgradeHowToApply.forEach { item ->
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(BIN_SERVICES_UPGRADE_URL))
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Council Bin Options", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_DIAL, Uri.parse(COUNCIL_PHONE_URI))
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("(02) 4732 7777", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
