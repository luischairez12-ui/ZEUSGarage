package com.zeusgarage.mobile

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class PartsStore(
    val name: String,
    val description: String,
    val url: String,
    val mapsSearch: String?
)

/** Official retail entry points; open their Android app when it owns the verified web link. */
val partsStores = listOf(
    PartsStore("NAPA Auto Parts", "OEM and aftermarket parts", "https://www.napaonline.com/", "NAPA Auto Parts"),
    PartsStore("O'Reilly Auto Parts", "Parts, tools and fluids", "https://www.oreillyauto.com/", "O'Reilly Auto Parts"),
    PartsStore("AutoZone", "Parts and maintenance supplies", "https://www.autozone.com/", "AutoZone"),
    PartsStore("Advance Auto Parts", "Parts, batteries and accessories", "https://shop.advanceautoparts.com/", "Advance Auto Parts"),
    PartsStore("Walmart Auto", "Oil, filters, batteries and accessories", "https://www.walmart.com/cp/auto-tires/91083", "Walmart Auto Care Center"),
    PartsStore("RockAuto", "Online parts catalog", "https://www.rockauto.com/", null),
    PartsStore("eBay Motors", "New, used and hard-to-find truck parts", "https://www.ebay.com/b/Auto-Parts-and-Vehicles/6000/bn_1865334", null),
    PartsStore("Amazon Automotive", "Auto parts, tools, and truck accessories", "https://www.amazon.com/s?k=automotive+parts", null)
)

private fun launchView(context: Context, intent: Intent): Boolean = try {
    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    true
} catch (_: ActivityNotFoundException) {
    false
} catch (_: SecurityException) {
    false
}

/** An HTTPS ACTION_VIEW lets Android resolve a supported installed retailer app, otherwise a browser. */
fun shopAt(context: Context, store: PartsStore) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(store.url)).addCategory(Intent.CATEGORY_BROWSABLE)
    if (!launchView(context, intent)) Toast.makeText(context, "No browser available to open " + store.name, Toast.LENGTH_LONG).show()
}

/**
 * Open Google Maps' nearby SEARCH, not an invented nearest destination.
 * Maps itself handles device location and permission; ZEUS does not need GPS access.
 */
fun navigateToStoreSearch(context: Context, store: PartsStore) {
    val search = store.mapsSearch ?: return
    val geo = Uri.parse("geo:0,0?q=" + Uri.encode(search))
    val maps = Intent(Intent.ACTION_VIEW, geo).setPackage("com.google.android.apps.maps")
    if (launchView(context, maps)) return
    val fallback = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(search))
    if (!launchView(context, Intent(Intent.ACTION_VIEW, fallback))) {
        Toast.makeText(context, "Install Google Maps or a web browser to find stores.", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun PartsStoresPage() {
    val context = androidx.compose.ui.platform.LocalContext.current
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionTitle("Parts & Stores", "Shop for ZEUS, then find a nearby pickup location.") }
        item {
            Panel {
                SmallCaps("Quick parts lookup")
                Muted("Each Shop button opens the official store website. If an installed app supports that store's links, Android may open it instead.")
                Muted("Navigate opens nearby store search in Google Maps. Choose the location and tap Directions; search results are not guaranteed to be sorted by distance.")
            }
        }
        items(partsStores, key = { it.name }) { store ->
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(13.dp), color = RubyTint) {
                        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                            Icon(if (store.mapsSearch == null) Icons.Outlined.Language else Icons.Outlined.Storefront,
                                null, tint = Ruby, modifier = Modifier.size(27.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(store.name, color = ZeusInk, fontWeight = FontWeight.ExtraBold)
                        Muted(store.description)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { shopAt(context, store) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.ShoppingBag, null, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Shop")
                    }
                    if (store.mapsSearch != null) {
                        OutlinedButton(onClick = { navigateToStoreSearch(context, store) },
                            modifier = Modifier.weight(1f)) {
                            Icon(Icons.Outlined.Navigation, null, modifier = Modifier.size(19.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Navigate")
                        }
                    }
                }
            }
        }
        item { Muted("Part fitment, price, availability, and opening hours are provided by each retailer. ZEUS does not guarantee stock or store proximity.") }
        item { Spacer(Modifier.height(75.dp)) }
    }
}

@Composable
fun HealthCenterPage(data: GarageSnapshot, onGo: (String) -> Unit) {
    val observed = data.codes.count { it.status == "Observed" }
    val reported = data.symptoms.count { it.status == "Open" }
    val odometer = data.active?.odometer
    val overdue = data.tasks.count { task ->
        (task.remaining(odometer) ?: 1) <= 0
    }
    val soon = data.tasks.count { task ->
        (task.remaining(odometer) ?: Int.MAX_VALUE) in 1..500
    }
    val issues = observed + reported
    val hasAttention = issues > 0 || overdue > 0
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionTitle("Smart Vehicle Health", "Your truck's record-based maintenance and symptom overview.") }
        item {
            TruckHero(data.active, onSwitch = { onGo("Garage") }, onEdit = { onGo("Garage") })
        }
        item {
            Panel {
                SmallCaps("Health overview")
                Text(if (hasAttention) "Needs attention" else "No logged urgent items",
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold,
                    color = if (hasAttention) Ruby else ZeusInk)
                Muted("Based only on records you've entered in ZEUS. This is not an ECU reading or a verified mechanical health score.")
                if (odometer == null) Muted("Enter your truck's mileage to calculate service reminders.")
                if (data.codes.isEmpty() && data.symptoms.isEmpty()) Muted("No issues recorded yet doesn't prove the truck is fault-free.")
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Panel(Modifier.weight(1f)) {
                    SmallCaps("Active issues")
                    Text("$issues", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    OutlinedButton(onClick = { onGo("Issues") }, modifier = Modifier.fillMaxWidth()) { Text("View") }
                }
                Panel(Modifier.weight(1f)) {
                    SmallCaps("Service due")
                    Text("$overdue", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    OutlinedButton(onClick = { onGo("Maintenance") }, modifier = Modifier.fillMaxWidth()) { Text("View") }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Panel(Modifier.weight(1f)) {
                    SmallCaps("Due soon")
                    Text("$soon", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Muted("Within 500 miles")
                }
                Panel(Modifier.weight(1f)) {
                    SmallCaps("Repair spend")
                    Text(money(data.repairs.sumOf { it.costCents }), fontWeight = FontWeight.ExtraBold)
                    TextButton(onClick = { onGo("History") }) { Text("History ›") }
                }
            }
        }
        item { SmallCaps("Health tools") }
        item { DashTile("Active Issues", "DTCs and reported symptoms", Icons.Outlined.WarningAmber,
            Modifier.fillMaxWidth()) { onGo("Issues") } }
        item { DashTile("Symptom Tracker", "Track changes and patterns", Icons.Outlined.EditNote,
            Modifier.fillMaxWidth()) { onGo("Symptoms") } }
        item { DashTile("Maintenance", "Service schedules and alerts", Icons.Outlined.Build,
            Modifier.fillMaxWidth()) { onGo("Maintenance") } }
        item { DashTile("Repair & Cost History", "Past work and spending", Icons.Outlined.ReceiptLong,
            Modifier.fillMaxWidth()) { onGo("History") } }
        item { Spacer(Modifier.height(75.dp)) }
    }
}

@Composable
fun ActiveIssuesPage(data: GarageSnapshot, onGo: (String) -> Unit) {
    val activeCodes = data.codes.filter { it.status == "Observed" }
    val activeSymptoms = data.symptoms.filter { it.status == "Open" }
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionTitle("Active Issues", "Only issues you manually recorded, not a live scan.") }
        item { SmallCaps("Recorded trouble codes") }
        if (activeCodes.isEmpty()) item { EmptyState("No observed DTCs saved.") }
        items(activeCodes, key = { "dtc_${it.id}" }) { row ->
            RecordCard(row.code, row.symptoms.ifBlank {
                dtcLibrary.firstOrNull { it.code == row.code }?.title ?: "Manual DTC record"
            }, icon = Icons.Outlined.WarningAmber, onClick = { onGo("Diagnostics") })
        }
        item { SmallCaps("Reported symptoms") }
        if (activeSymptoms.isEmpty()) item { EmptyState("No open symptoms saved.") }
        items(activeSymptoms, key = { "symptom_${it.id}" }) { row ->
            RecordCard(row.title, "${row.severity} • ${row.occurrence} • ${row.date}",
                icon = Icons.Outlined.ReportProblem, onClick = { onGo("Symptoms") })
        }
        item {
            Button(onClick = { onGo("Symptoms") }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("Track a symptom")
            }
        }
        item { Spacer(Modifier.height(75.dp)) }
    }
}

@Composable
fun SymptomsPage(
    data: GarageSnapshot,
    onAdd: () -> Unit,
    onEdit: (SymptomEntry) -> Unit,
    onDelete: (SymptomEntry) -> Unit
) {
    val sorted = data.symptoms.sortedWith(compareBy<SymptomEntry> { it.status != "Open" }
        .thenByDescending { it.date }.thenByDescending { it.id })
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionTitle("Symptom Tracker", "Log what happens and when so repairs are easier to diagnose.") }
        item { Panel {
            Muted("These observations are manually reported, not live vehicle sensor measurements.")
            Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("Add symptom")
            }
        } }
        if (sorted.isEmpty()) item { EmptyState("No symptoms recorded. Add one when something needs tracking.") }
        items(sorted, key = { it.id }) { entry ->
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Troubleshoot, null, tint = Ruby)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(entry.title, color = ZeusInk, fontWeight = FontWeight.Bold)
                        Muted("${entry.severity} • ${entry.occurrence} • ${entry.status}")
                        Muted(entry.date)
                    }
                }
                if (entry.notes.isNotBlank()) Text(entry.notes)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onEdit(entry) }) { Text("Edit / Resolve") }
                    TextButton(onClick = { onDelete(entry) }) { Text("Delete", color = RubyDark) }
                }
            }
        }
        item { Spacer(Modifier.height(75.dp)) }
    }
}
