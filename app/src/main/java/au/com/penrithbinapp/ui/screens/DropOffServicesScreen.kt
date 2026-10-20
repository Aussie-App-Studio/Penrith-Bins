package au.com.penrithbinapp.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import au.com.penrithbinapp.ui.components.BinUpgradesInfoCard
import au.com.penrithbinapp.ui.components.BulkyWasteInfoCard

enum class DropOffCategory(val label: String) {
    ALL("All"),
    BOTTLES_CANS("Bottles & Cans (10¢)"),
    DUMPS("Tips & Dumps"),
    EWASTE("E-Waste"),
    OP_SHOPS("Op-Shops"),
    COUNCIL("Council Services")
}

data class DropOffLocation(
    val name: String,
    val address: String,
    val badge: String,
    val details: String,
    val hoursOrNote: String? = null
)

private val bottleCanReturnLocations = listOf(
    // ── BULK DEPOTS (Fastest for big bags, boxes & trailers) ──
    DropOffLocation(
        name = "TOMRA Return & Earn Depot & 'Takes All' – St Marys",
        address = "89 Dunheved Circuit, St Marys NSW 2760",
        badge = "10¢ Bulk Depot",
        details = "Indoor weatherproof bulk depot with 'Tip n Go' bulk counting & single-chute 'Takes All' machines (no pre-sorting required) plus staff assistance.",
        hoursOrNote = "Depot: Daily 7am–6pm • 'Takes All' RVM: Daily 7am–10pm"
    ),
    DropOffLocation(
        name = "Vinnies Return & Earn Bulk Depot – Emu Plains",
        address = "7 Pullman Place, Emu Plains NSW 2750",
        badge = "10¢ Bulk Depot",
        details = "Staffed drive-through bulk depot (relocated from Coreen Ave Penrith) with high-speed automated bulk counting (100+ containers/min) & on-the-spot EFT payouts.",
        hoursOrNote = "Mon–Sat 8:30am–3:30pm, Sun 9:00am–4:00pm"
    ),

    // ── REVERSE VENDING MACHINES (RVMs) ACROSS PENRITH LGA ──
    DropOffLocation(
        name = "Return & Earn RVM – HomeCo Penrith",
        address = "72-82 Mulgoa Road, Jamisontown / Penrith NSW 2750",
        badge = "10¢ Vending Machine",
        details = "Reverse Vending Machine in the HomeCo Penrith car park accepting eligible 150mL–3L bottles, cans & cartons for app payouts, retail vouchers, or charity donations.",
        hoursOrNote = "Daily 7:00am–10:00pm"
    ),
    DropOffLocation(
        name = "Return & Earn RVM – Penrith Paceway (Station St)",
        address = "141 Station Street, Penrith NSW 2750",
        badge = "10¢ Vending Machine",
        details = "Reverse Vending Machine at Penrith Paceway near the Penrith CBD & Panthers precinct.",
        hoursOrNote = "Daily 7:00am–9:00pm"
    ),
    DropOffLocation(
        name = "Return & Earn RVM – Southlands Shopping Centre",
        address = "2 Birmingham Road, South Penrith NSW 2750",
        badge = "10¢ Vending Machine",
        details = "Reverse Vending Machine in the Southlands car park (supports digital Return and Earn app payouts, Woolworths refund vouchers, or donations).",
        hoursOrNote = "Daily 7:00am–9:00pm"
    ),
    DropOffLocation(
        name = "Return & Earn RVM – Glenmore Park Town Centre",
        address = "1 Town Terrace, Glenmore Park NSW 2745",
        badge = "10¢ Vending Machine",
        details = "Reverse Vending Machine in the Town Centre car park supporting digital app payouts, Coles refund vouchers, or charity donations.",
        hoursOrNote = "Daily 7:00am–9:00pm"
    ),
    DropOffLocation(
        name = "Return & Earn RVM – Cranebrook Village",
        address = "Borrowdale Way, Cranebrook NSW 2749",
        badge = "10¢ Vending Machine",
        details = "Reverse Vending Machine in the Cranebrook Village Shopping Centre car park accepting eligible 10¢ refund drink containers.",
        hoursOrNote = "Daily 7:00am–9:00pm"
    ),
    DropOffLocation(
        name = "Return & Earn RVM – Jordan Springs Shopping Centre",
        address = "13-19 Lakeside Parade, Jordan Springs NSW 2747",
        badge = "10¢ Vending Machine",
        details = "Reverse Vending Machine located in the Jordan Springs Shopping Centre (Woolworths) car park.",
        hoursOrNote = "Daily 7:00am–9:00pm"
    ),
    DropOffLocation(
        name = "Return & Earn RVM – Caddens Corner (Kingswood)",
        address = "68 O'Connell Street, Caddens NSW 2747",
        badge = "10¢ Vending Machine",
        details = "Reverse Vending Machine in the Caddens Corner Shopping Centre car park servicing Kingswood, Caddens & Werrington.",
        hoursOrNote = "Daily 7:00am–10:00pm"
    ),
    DropOffLocation(
        name = "Return & Earn RVM – St Marys Village",
        address = "10 Charles Hackett Drive, St Marys NSW 2760",
        badge = "10¢ Vending Machine",
        details = "Reverse Vending Machine in the St Marys Village Shopping Centre car park for convenient local returns.",
        hoursOrNote = "Daily 7:00am–9:00pm"
    ),
    DropOffLocation(
        name = "Return & Earn RVM – St Clair Shopping Centre",
        address = "155 Bennett Road, St Clair NSW 2759",
        badge = "10¢ Vending Machine",
        details = "Reverse Vending Machine servicing St Clair and Erskine Park (keep labels & barcodes intact, uncrushed).",
        hoursOrNote = "Daily 7:00am–9:00pm"
    )
)

private val localDumpLocations = listOf(
    DropOffLocation(
        name = "Penrith Community Recycling Centre (CRC)",
        address = "Gate 3, 96 Dunheved Circuit, St Marys NSW 2760",
        badge = "FREE (Household)",
        details = "Self-serve drop-off for problem waste: paint (up to 100L), gas bottles, fire extinguishers, motor oil, car & household batteries, fluoro globes/tubes, smoke detectors & e-waste.",
        hoursOrNote = "Mon–Fri 8:30am–2:00pm (Closed weekends & public holidays)"
    ),
    DropOffLocation(
        name = "BINGO Recycling Centre – St Marys",
        address = "25 Dunheved Circuit, St Marys NSW 2760",
        badge = "Paid Tip",
        details = "Public waste transfer station accepting building & renovation waste, timber, bricks, concrete, green waste, and mixed household/general waste loads.",
        hoursOrNote = "Mon–Fri 6:00am–4:00pm, Sat 6:00am–1:00pm"
    ),
    DropOffLocation(
        name = "Cleanaway Kemps Creek Resource Recovery",
        address = "1725 Elizabeth Drive, Kemps Creek NSW 2178",
        badge = "Paid Tip",
        details = "Accepts general residential and commercial landfill waste loads (minimum tonnage charges apply).",
        hoursOrNote = "Mon–Fri 6:00am–5:00pm, Sat 7:00am–2:00pm"
    ),
    DropOffLocation(
        name = "Blaxland Waste Management Facility",
        address = "Attunga Road, Blaxland NSW 2774",
        badge = "Paid Tip",
        details = "Sorting bays for general waste, green garden waste, scrap metal, timber, and cardboard.",
        hoursOrNote = "Open 7 days, 8:00am–4:45pm"
    ),
    DropOffLocation(
        name = "Sims Metal Recycling – St Marys",
        address = "76 Christie Street, St Marys NSW 2760",
        badge = "FREE Metal Drop-Off",
        details = "Drop off scrap steel, aluminium, copper, whitegoods, and metal appliances.",
        hoursOrNote = "Mon–Fri 7:00am–4:30pm"
    )
)

private val eWasteLocations = listOf(
    // ── FULL E-WASTE (TVs, Computers, Appliances & Tech) ──
    DropOffLocation(
        name = "Penrith Community Recycling Centre (CRC)",
        address = "Gate 3, 96 Dunheved Circuit, St Marys NSW 2760",
        badge = "FREE • All E-Waste",
        details = "Council's year-round self-serve hub accepting TVs, computers, laptops, monitors, tablets, phones, cords, household & car batteries, fluoro globes & smoke detectors.",
        hoursOrNote = "Mon–Fri 8:30am–2:00pm (Closed weekends & public holidays)"
    ),
    DropOffLocation(
        name = "Council Electronic Waste Drop-Off Days",
        address = "The Kingsway Playing Fields, Werrington NSW 2747",
        badge = "FREE • Drive-Thru Event",
        details = "Periodic drive-through boot/trailer drop-off events hosted by Penrith City Council for any item with a cord or battery (TVs, computers, kitchen appliances, stereos, power tools).",
        hoursOrNote = "Held quarterly (9:00am–3:30pm) • Check Council website for next date"
    ),
    DropOffLocation(
        name = "The Recycle Project (TechCollect) – Penrith",
        address = "Unit 14, 42 Jack Williams Drive, Penrith NSW 2750",
        badge = "FREE • IT & TVs",
        details = "Dedicated local e-waste recycler accepting computers, laptops, monitors, TVs, printers, servers, mobile phones, tablets & computer accessories.",
        hoursOrNote = "Mon–Fri"
    ),
    DropOffLocation(
        name = "Officeworks – Penrith",
        address = "76 Henry Street, Penrith NSW 2750",
        badge = "FREE • Tech & Ink",
        details = "In-store Bring IT Back station accepting laptops, desktops, monitors, printers, keyboards, mice, cables, hard drives, ink/toner cartridges, phones & household batteries.",
        hoursOrNote = "Open 7 days"
    ),
    DropOffLocation(
        name = "The Good Guys – Penrith (HomeCo)",
        address = "88-90 Mulgoa Road, Jamisontown / Penrith NSW 2750",
        badge = "FREE • E-Waste",
        details = "Accepts small e-waste, computers, laptops, tablets, mobile phones, cables, and household batteries in-store.",
        hoursOrNote = "Open 7 days"
    ),
    DropOffLocation(
        name = "Harvey Norman & JB Hi-Fi – Penrith Homemaker Centre",
        address = "13-23 Pattys Place (off Mulgoa Rd), Jamisontown NSW 2750",
        badge = "FREE • E-Waste",
        details = "Accepts TVs, computers, laptops, small electronic appliances, mobile phones (MobileMuster), and household batteries.",
        hoursOrNote = "Open 7 days"
    ),

    // ── BATTERIES, PHONES & SMALL ITEMS ──
    DropOffLocation(
        name = "Battery World – Penrith",
        address = "Shop 2, 31-33 Henry Street, Penrith NSW 2750",
        badge = "FREE • All Batteries",
        details = "Accepts all household batteries, button batteries, power tool battery packs, and car/marine/motorbike lead-acid batteries.",
        hoursOrNote = "Mon–Sat"
    ),
    DropOffLocation(
        name = "Bunnings Warehouse – Penrith & North Penrith",
        address = "Wolseley St, Jamisontown & 2265 Castlereagh Rd, Penrith NSW 2750",
        badge = "FREE • Tool & AA/AAA",
        details = "Front-of-store battery recycling bins for power tool batteries (Ryobi, Makita, DeWalt, Ozito, etc.) and standard household batteries.",
        hoursOrNote = "Open 7 days (Both Penrith & North Penrith stores)"
    ),
    DropOffLocation(
        name = "Penrith Library – Council Recycling Station",
        address = "601 High Street, Penrith NSW 2750",
        badge = "FREE • Small Items",
        details = "Council foyer collection station for household batteries (tape terminals), mobile phones & accessories, and light globes.",
        hoursOrNote = "Mon–Sun during library opening hours"
    ),
    DropOffLocation(
        name = "St Marys Library – Council Recycling Station",
        address = "207-209 Queen Street, St Marys NSW 2760",
        badge = "FREE • Small Items",
        details = "Council foyer collection station for household batteries (tape terminals), mobile phones & accessories, and light globes.",
        hoursOrNote = "Mon–Sat during library opening hours"
    ),
    DropOffLocation(
        name = "St Clair Library – Council Recycling Station",
        address = "Shop 12, St Clair Shopping Centre, 155 Bennett Road, St Clair NSW 2759",
        badge = "FREE • Small Items",
        details = "Council foyer collection station for household batteries (tape terminals), mobile phones & accessories, and light globes.",
        hoursOrNote = "Mon–Sat during library opening hours"
    ),
    DropOffLocation(
        name = "Coles, Woolworths & ALDI Supermarket Battery Bins",
        address = "All Penrith LGA Supermarkets (Penrith, St Marys, Glenmore Park, Cranebrook, St Clair, Jordan Springs)",
        badge = "FREE • B-cycle Bins",
        details = "Accredited B-cycle battery drop-off bins near the store entrance or service desk for AA, AAA, C, D, 9V & button batteries (never put batteries in household bins).",
        hoursOrNote = "Open 7 days • Tape battery terminals with clear sticky tape"
    )
)

private val opShopLocations = listOf(
    // ── PENRITH CBD (High St, Henry St & Station St) ──
    DropOffLocation(
        name = "Vinnies – Penrith (Henry St)",
        address = "Shop 1, 87-93 Henry Street, Penrith NSW 2750",
        badge = "Penrith CBD",
        details = "Large Vinnies store accepting quality pre-loved clothing, shoes, books, toys, bric-a-brac & homewares.",
        hoursOrNote = "Mon–Sat • Donate during opening hours"
    ),
    DropOffLocation(
        name = "Salvos Stores – Penrith",
        address = "499-505 High Street, Penrith NSW 2750",
        badge = "Penrith CBD",
        details = "Accepts clean wearable clothing, accessories, books, manchester, homewares & small electrical items.",
        hoursOrNote = "Mon–Sat 9am–5pm"
    ),
    DropOffLocation(
        name = "Lifeline Shop – Penrith (High St)",
        address = "505-507 High Street, Penrith NSW 2750",
        badge = "Penrith CBD",
        details = "Pre-loved clothing, shoes, handbags, books, media, and homewares supporting Lifeline crisis services.",
        hoursOrNote = "Mon–Sat"
    ),
    DropOffLocation(
        name = "Lifeline Shop – Penrith (Henry St)",
        address = "109 Henry Street, Penrith NSW 2750",
        badge = "Penrith CBD",
        details = "Clothing, shoes, books, puzzles, and household goods supporting local crisis support services.",
        hoursOrNote = "Mon–Sat"
    ),
    DropOffLocation(
        name = "Anglicare Op Shop – Penrith",
        address = "Shop 1B, 458-470 High Street, Penrith NSW 2750",
        badge = "Penrith CBD",
        details = "Quality pre-loved clothing, linen, shoes, books, and household items.",
        hoursOrNote = "Mon–Fri 9am–5pm, Sat 9:30am–4pm"
    ),
    DropOffLocation(
        name = "Australian Red Cross Shop – Penrith",
        address = "Shop 1-2, 442 High Street, Penrith NSW 2750",
        badge = "Penrith CBD",
        details = "Accepts good-quality clothing, accessories, books, and homewares supporting humanitarian programs.",
        hoursOrNote = "Mon–Fri 9am–5pm, Sat 10am–4pm"
    ),
    DropOffLocation(
        name = "Save the Children Op Shop – Penrith",
        address = "82 Station Street, Penrith NSW 2750",
        badge = "Penrith CBD",
        details = "Charity op-shop accepting clean clothing, children's goods, books, toys & bric-a-brac.",
        hoursOrNote = "Mon–Fri 9am–4pm, Sat 9am–2pm"
    ),
    DropOffLocation(
        name = "Cancer Wellness Support Op Shop – Penrith",
        address = "211 High Street, Penrith NSW 2750",
        badge = "Penrith CBD",
        details = "Located next to Aldi; accepts clothing, accessories, books & homewares supporting local families affected by cancer.",
        hoursOrNote = "Mon–Fri 9am–4:30pm, Sat 9am–3pm"
    ),
    DropOffLocation(
        name = "Finders Keepers Boutique (FKB) – Penrith",
        address = "415 High Street, Penrith NSW 2750",
        badge = "Penrith CBD",
        details = "Curated pre-loved fashion, vintage clothing, labels & accessories.",
        hoursOrNote = "Mon–Sat"
    ),
    DropOffLocation(
        name = "Dress for Success Western Sydney – Penrith",
        address = "Shop 4, 444 High Street, Penrith NSW 2750",
        badge = "Penrith CBD",
        details = "Accepts gently used, nearly-new women's corporate & interview attire, work shoes, and handbags.",
        hoursOrNote = "Check donation days before dropping off"
    ),

    // ── JAMISONTOWN ──
    DropOffLocation(
        name = "Vinnies Mega Centre – Jamisontown",
        address = "Unit 2, 120-124 Batt Street, Jamisontown NSW 2750",
        badge = "Jamisontown",
        details = "Major warehouse & donation drop-off hub accepting clothing, shoes, books, toys, bric-a-brac, homewares & furniture.",
        hoursOrNote = "7 days • Dedicated loading/donation bay"
    ),
    DropOffLocation(
        name = "Jamisontown Community Op Shop (Uniting Church)",
        address = "3 Stuart Street, Jamisontown NSW 2750",
        badge = "Jamisontown",
        details = "Local church community op-shop accepting clean clothing, books, kitchenware & bric-a-brac.",
        hoursOrNote = "Check local church op-shop hours"
    ),

    // ── KINGSWOOD ──
    DropOffLocation(
        name = "Vinnies – Kingswood",
        address = "74 Cox Avenue, Kingswood NSW 2747",
        badge = "Kingswood",
        details = "Local store accepting clean clothing, shoes, books, and household goods.",
        hoursOrNote = "Mon–Sat"
    ),
    DropOffLocation(
        name = "WestCare Community Op Shop – Kingswood",
        address = "Shop 2, 10 Bringelly Road, Kingswood NSW 2747",
        badge = "Kingswood",
        details = "Opposite Kingswood Post Office; supports WestCare local Penrith community relief services.",
        hoursOrNote = "Mon–Fri 9am–4pm"
    ),
    DropOffLocation(
        name = "Kingswood Anglican Church Op Shop",
        address = "36 Bringelly Road, Kingswood NSW 2747",
        badge = "Kingswood",
        details = "Community church op-shop accepting clothing, books, toys, and small household goods.",
        hoursOrNote = "Selected weekday mornings"
    ),

    // ── ST MARYS (Queen St & Great Western Hwy) ──
    DropOffLocation(
        name = "Vinnies – St Marys",
        address = "4 Crana Street, St Marys NSW 2760",
        badge = "St Marys",
        details = "Pre-loved clothing, books, toys, bric-a-brac, and household donations.",
        hoursOrNote = "Mon–Sat 9am–4pm"
    ),
    DropOffLocation(
        name = "Salvos Stores – St Marys",
        address = "504 Great Western Highway, St Marys NSW 2760",
        badge = "St Marys",
        details = "Large store with drop-off access accepting clothing, shoes, homewares, books & small furniture.",
        hoursOrNote = "Mon–Sat 9am–5pm"
    ),
    DropOffLocation(
        name = "Anglicare Op Shop – St Marys",
        address = "120 Queen Street, St Marys NSW 2760",
        badge = "St Marys",
        details = "Quality clothing, shoes, accessories, linen, and household items.",
        hoursOrNote = "Mon–Fri 9am–5pm, Sat 9:30am–4pm"
    ),
    DropOffLocation(
        name = "Cancer Wellness Support Op Shop – St Marys",
        address = "Shop 1, 170 Queen Street, St Marys NSW 2760",
        badge = "St Marys",
        details = "Accepts pre-loved clothing, homewares, and lifestyle goods supporting local cancer wellness services.",
        hoursOrNote = "Mon–Fri 9am–4:30pm, Sat 9am–2pm"
    ),
    DropOffLocation(
        name = "Finders Keepers Boutique (FKB) – St Marys",
        address = "99 Queen Street, St Marys NSW 2760",
        badge = "St Marys",
        details = "Pre-loved clothing, vintage fashion, and accessories along Queen Street.",
        hoursOrNote = "Mon–Sat"
    ),
    DropOffLocation(
        name = "Lavender Cottage Preloved Boutique – St Marys",
        address = "253 Great Western Highway, St Marys NSW 2760",
        badge = "St Marys",
        details = "Pre-loved clothing, bric-a-brac, collectibles, and accessories.",
        hoursOrNote = "Tue–Sat"
    )
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DropOffServicesScreen(
    modifier: Modifier = Modifier
) {
    var selectedCategory by rememberSaveable { mutableStateOf(DropOffCategory.ALL) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Category Filter Chips
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DropOffCategory.entries.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category.label) }
                    )
                }
            }
        }

        // 1. Bottles & Cans (10c Return and Earn)
        if (selectedCategory == DropOffCategory.ALL || selectedCategory == DropOffCategory.BOTTLES_CANS) {
            item {
                ExpandableDirectoryCard(
                    title = "Bottles & Cans",
                    badgeText = "${bottleCanReturnLocations.size} Locations",
                    subtitle = "10¢ Return and Earn Bulk Depots & Vending Machines",
                    icon = Icons.Default.LocalDrink,
                    accentColor = Color(0xFF42A5F5),
                    initiallyExpanded = selectedCategory == DropOffCategory.BOTTLES_CANS,
                    locations = bottleCanReturnLocations,
                    externalLinkLabel = "Live Machine Status & Return and Earn Map",
                    externalLinkUrl = "https://returnandearn.org.au/map/"
                )
            }
        }

        // 2. Local Dumps, Tips & Recycling Centres
        if (selectedCategory == DropOffCategory.ALL || selectedCategory == DropOffCategory.DUMPS) {
            item {
                ExpandableDirectoryCard(
                    title = "Dumps & Tips",
                    badgeText = "${localDumpLocations.size} Locations",
                    subtitle = "Waste Transfer Stations & Free Problem Waste CRC",
                    icon = Icons.Default.LocalShipping,
                    accentColor = Color(0xFFAB47BC),
                    initiallyExpanded = selectedCategory == DropOffCategory.DUMPS,
                    locations = localDumpLocations
                )
            }
        }

        // 3. E-Waste & Battery Drop-Off Locations
        if (selectedCategory == DropOffCategory.ALL || selectedCategory == DropOffCategory.EWASTE) {
            item {
                ExpandableDirectoryCard(
                    title = "E-Waste",
                    badgeText = "${eWasteLocations.size} Locations",
                    subtitle = "TVs, Computers, Phones, Batteries & Appliances",
                    icon = Icons.Default.Devices,
                    accentColor = Color(0xFF26A69A),
                    initiallyExpanded = selectedCategory == DropOffCategory.EWASTE,
                    locations = eWasteLocations,
                    externalLinkLabel = "Penrith Council E-Waste & CRC Info",
                    externalLinkUrl = "https://www.penrithcity.nsw.gov.au/waste-environment/waste/community-recycling-centre"
                )
            }
        }

        // 4. Local Op-Shops & Charity Donations
        if (selectedCategory == DropOffCategory.ALL || selectedCategory == DropOffCategory.OP_SHOPS) {
            item {
                ExpandableDirectoryCard(
                    title = "Local Op-Shops",
                    badgeText = "${opShopLocations.size} Stores",
                    subtitle = "Donate Good-Quality Clothes, Books & Homewares",
                    icon = Icons.Default.Checkroom,
                    accentColor = Color(0xFFEC407A),
                    initiallyExpanded = selectedCategory == DropOffCategory.OP_SHOPS,
                    locations = opShopLocations
                )
            }
        }

        // 4. Council Clean-Up & Bin Upgrades
        if (selectedCategory == DropOffCategory.ALL || selectedCategory == DropOffCategory.COUNCIL) {
            item {
                Text(
                    text = "Penrith Council Household Services",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            item {
                BulkyWasteInfoCard()
            }
            item {
                BinUpgradesInfoCard()
            }
        }
    }
}

@Composable
private fun ExpandableDirectoryCard(
    title: String,
    badgeText: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    initiallyExpanded: Boolean,
    locations: List<DropOffLocation>,
    externalLinkLabel: String? = null,
    externalLinkUrl: String? = null
) {
    val context = LocalContext.current
    var expanded by rememberSaveable(initiallyExpanded) { mutableStateOf(initiallyExpanded) }

    Card(
        modifier = Modifier
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
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(36.dp)
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
                            text = title,
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
                                text = badgeText,
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
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (expanded) "Tap to collapse locations" else "Tap to view locations & open in Maps",
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
                Column(
                    modifier = Modifier.padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    locations.forEach { loc ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = loc.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = accentColor.copy(alpha = 0.2f),
                                        contentColor = accentColor,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = loc.badge,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = loc.address,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = loc.details,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (!loc.hoursOrNote.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = loc.hoursOrNote,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                FilledTonalButton(
                                    onClick = {
                                        val query = Uri.encode("${loc.name}, ${loc.address}")
                                        val mapUri = Uri.parse("geo:0,0?q=$query")
                                        val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
                                        runCatching {
                                            context.startActivity(mapIntent)
                                        }.onFailure {
                                            val webUri =
                                                Uri.parse("https://www.google.com/maps/search/?api=1&query=$query")
                                            runCatching {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Open in Maps",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    if (!externalLinkLabel.isNullOrBlank() && !externalLinkUrl.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(externalLinkUrl))
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(externalLinkLabel, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
