package com.zeusgarage.mobile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun GaragePage(data: GarageSnapshot, onSelect: (Vehicle) -> Unit,
               onEdit: (Vehicle?) -> Unit, onDelete: (Vehicle) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionTitle("My Garage", "Choose a truck or add another vehicle.") }
        item { Button(onClick = { onEdit(null) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(7.dp)); Text("Add vehicle")
        } }
        items(data.vehicles, key = { it.id }) { vehicle ->
            Panel(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(14.dp), color = RubyTint) {
                        Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.DirectionsCar, null, tint = Ruby, modifier = Modifier.size(29.dp))
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(vehicle.nickname, fontWeight = FontWeight.ExtraBold, color = ZeusInk)
                        Text(vehicle.display, color = ZeusInk, style = MaterialTheme.typography.bodySmall)
                        Muted(vehicle.engine + " • " + odometerText(vehicle.odometer))
                    }
                    if (vehicle.id == data.activeId) {
                        Icon(Icons.Outlined.CheckCircle, null, tint = Ruby)
                    }
                }
                if (vehicle.notes.isNotBlank()) Muted(vehicle.notes)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (vehicle.id != data.activeId) TextButton(onClick = { onSelect(vehicle) }) {
                        Text("Use this truck")
                    }
                    TextButton(onClick = { onEdit(vehicle) }) { Text("Edit") }
                    if (data.vehicles.size > 1) TextButton(onClick = { onDelete(vehicle) }) {
                        Text("Delete", color = RubyDark)
                    }
                }
            }
        }
        item { Muted("All entries are kept separately for each vehicle. GPS, lock and remote-start controls are not connected.") }
        item { Spacer(Modifier.height(75.dp)) }
    }
}

@Composable
fun MaintenancePage(data: GarageSnapshot, onAddTask: () -> Unit,
                    onTask: (ServiceTask) -> Unit, onLog: (ServiceTask?) -> Unit,
                    onEditLog: (ServiceEvent) -> Unit,
                    onDelete: (String, Long, String) -> Unit) {
    val current = data.active?.odometer
    val tasks = data.tasks.sortedWith(compareBy<ServiceTask> { it.remaining(current) == null }
        .thenBy { it.remaining(current) ?: Int.MAX_VALUE }.thenBy { it.title })
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionTitle("Maintenance", "Schedules, reminders, and recorded service for ${data.active?.nickname ?: "your truck"}.") }
        item {
            Panel {
                SmallCaps("Your truck")
                Text(odometerText(current), fontWeight = FontWeight.ExtraBold,
                    color = ZeusInk, style = MaterialTheme.typography.headlineSmall)
                Muted("Service reminders use the mileage and intervals YOU enter. No factory schedule is assumed.")
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAddTask, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Add, null); Text("Schedule")
                }
                OutlinedButton(onClick = { onLog(null) }, modifier = Modifier.weight(1f)) {
                    Text("Log service")
                }
            }
        }
        item { SmallCaps("Service schedules") }
        if (tasks.isEmpty()) item { EmptyState("No service schedules saved.", "Add a schedule", onAddTask) }
        items(tasks, key = { it.id }) { task ->
            val left = task.remaining(current)
            val status = when {
                task.intervalMiles <= 0 -> "Set interval"
                task.lastDoneMiles == null -> "Set last completed mileage"
                current == null -> "Enter truck mileage"
                left == null -> "Unknown"
                left < 0 -> "${-left} mi overdue"
                left == 0 -> "Due now"
                else -> "${left} mi remaining"
            }
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.BuildCircle, null, tint = Ruby, modifier = Modifier.size(27.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(task.title, fontWeight = FontWeight.Bold)
                        Muted(status)
                    }
                    if (left != null && left <= 0) Icon(Icons.Outlined.WarningAmber, null, tint = Ruby)
                }
                if (task.dueMiles != null) Muted("Next at ${odometerText(task.dueMiles)}")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onTask(task) }) { Text("Edit schedule") }
                    TextButton(onClick = { onLog(task) }) { Text("Log service") }
                    TextButton(onClick = { onDelete("service_task", task.id, task.title) }) {
                        Text("Delete", color = RubyDark)
                    }
                }
            }
        }
        item { SmallCaps("Completed service history") }
        if (data.services.isEmpty()) item { EmptyState("No completed services logged.") }
        items(data.services, key = { "service_${it.id}" }) { event ->
            RecordCard(event.title, "${event.date} • ${odometerText(event.mileage)}",
                money(event.costCents), Icons.Outlined.EventAvailable,
                onClick = { onEditLog(event) },
                onDelete = { onDelete("service_event", event.id, event.title) })
        }
        item { Spacer(Modifier.height(75.dp)) }
    }
}

@Composable
fun DiagnosticsPage(data: GarageSnapshot, onAdd: () -> Unit,
                    onEdit: (DtcEntry) -> Unit, onDelete: (DtcEntry) -> Unit,
                    onDefinition: (DtcDefinition) -> Unit) {
    var query by remember { mutableStateOf("") }
    val found = dtcLibrary.filter { it.code.contains(query.trim(), true) ||
        it.title.contains(query.trim(), true) || it.summary.contains(query.trim(), true) }
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionTitle("Diagnostics", "Offline trouble-code reference and manually saved symptoms.") }
        item {
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Info, null, tint = Ruby)
                    Spacer(Modifier.width(8.dp))
                    Muted("Not connected to the vehicle. Saved codes are entered by you, NOT read from the ECU.")
                }
                Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(6.dp)); Text("Record a DTC / symptom")
                }
            }
        }
        item { SmallCaps("Your recorded codes") }
        if (data.codes.isEmpty()) item { EmptyState("No trouble codes recorded. This does not confirm that the truck has no faults.") }
        items(data.codes, key = { "dtc_${it.id}" }) { entry ->
            val def = dtcLibrary.firstOrNull { it.code == entry.code }
            RecordCard(entry.code + " • " + entry.status, def?.title ?: entry.symptoms.take(48).ifBlank { "Manual entry" },
                null, Icons.Outlined.Engineering,
                onClick = { onEdit(entry) }, onDelete = { onDelete(entry) })
        }
        item { SmallCaps("Offline code library") }
        item { OutlinedTextField(value = query, onValueChange = { query = it.take(40) },
            label = { Text("Search DTC code or description") },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            shape = RoundedCornerShape(15.dp)) }
        if (found.isEmpty()) item { EmptyState("No matching code in the built-in library. You can still save it manually.") }
        items(found, key = { it.code }) { code ->
            RecordCard(code.code, code.title, "View", Icons.Outlined.MenuBook,
                onClick = { onDefinition(code) })
        }
        item { Muted("These are generic references. Confirm definitions and test procedures against the exact truck and scanner; do not treat them as automatic diagnoses.") }
        item { Spacer(Modifier.height(75.dp)) }
    }
}

@Composable
fun FuelPage(data: GarageSnapshot, onAdd: () -> Unit,
             onEdit: (FuelEntry) -> Unit, onDelete: (FuelEntry) -> Unit) {
    val totalGallons = data.fuel.sumOf { it.gallons }
    val spent = data.fuel.sumOf { it.totalCents }
    val mpg = latestFullToFullMpg(data.fuel)
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionTitle("Fuel", "Fuel-ups and mileage for ${data.active?.nickname ?: "your truck"}.") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Panel(Modifier.weight(1f)) { SmallCaps("Fuel spent"); Text(money(spent), fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold, color = ZeusInk) }
                Panel(Modifier.weight(1f)) { SmallCaps("Fuel economy"); Text(mpg?.let { "%.1f MPG".format(Locale.US, it) }
                    ?: "Not enough data", fontWeight = FontWeight.ExtraBold, color = ZeusInk) }
            }
        }
        item {
            Panel {
                Muted("Total gallons logged: %.2f gal".format(Locale.US, totalGallons))
                Muted("MPG uses the distance between two full-tank fill-ups, including any partial fill-ups between them. Enter accurate odometer readings.")
                Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Add, null); Text("Add fuel-up")
                }
            }
        }
        item { SmallCaps("Fuel history") }
        if (data.fuel.isEmpty()) item { EmptyState("No fuel-ups recorded yet.") }
        items(data.fuel, key = { it.id }) { row ->
            RecordCard(row.date, "${odometerText(row.odometer)} • %.2f gal%s".format(Locale.US,
                row.gallons, if (row.fullTank) " • Full tank" else " • Partial"),
                money(row.totalCents), Icons.Outlined.LocalGasStation,
                onClick = { onEdit(row) }, onDelete = { onDelete(row) })
        }
        item { Spacer(Modifier.height(75.dp)) }
    }
}

@Composable
fun RepairsPage(data: GarageSnapshot, onAdd: () -> Unit,
                onEdit: (RepairEntry) -> Unit, onDelete: (RepairEntry) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionTitle("Repairs", "Document fixes, parts, costs, and mileage.") }
        item { Panel {
            SmallCaps("Total repairs recorded")
            Text(money(data.repairs.sumOf { it.costCents }), color = Ruby, fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.headlineMedium)
            Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Add, null); Text("Add repair") }
        } }
        if (data.repairs.isEmpty()) item { EmptyState("Your repair history is empty.") }
        items(data.repairs, key = { it.id }) { row ->
            RecordCard(row.title, "${row.date} • ${odometerText(row.mileage)}" +
                (if (row.part.isNotBlank()) " • ${row.part}" else ""), money(row.costCents),
                Icons.Outlined.Handyman, onClick = { onEdit(row) }, onDelete = { onDelete(row) })
        }
        item { Spacer(Modifier.height(75.dp)) }
    }
}

@Composable
fun UpgradesPage(data: GarageSnapshot, onAdd: () -> Unit,
                 onEdit: (UpgradeEntry) -> Unit, onDelete: (UpgradeEntry) -> Unit) {
    val installed = data.upgrades.filter { it.status == "Installed" }
    val planned = data.upgrades.filter { it.status == "Planned" }
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionTitle("Upgrades", "Your build ideas and installed modifications.") }
        item { Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    SmallCaps("Installed spend")
                    Text(money(installed.sumOf { it.costCents }), color = Ruby,
                        style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                }
                Text("${planned.size} planned", color = ZeusMuted)
            }
            Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Add, null); Text("Add upgrade") }
        } }
        item { SmallCaps("Wishlist") }
        if (planned.isEmpty()) item { EmptyState("No planned upgrades yet.") }
        items(planned, key = { it.id }) { row ->
            RecordCard(row.name, row.notes.ifBlank { "Planned" }, null, Icons.Outlined.AutoAwesome,
                onClick = { onEdit(row) }, onDelete = { onDelete(row) })
        }
        item { SmallCaps("Installed upgrades") }
        if (installed.isEmpty()) item { EmptyState("No upgrades marked installed.") }
        items(installed, key = { it.id }) { row ->
            RecordCard(row.name, row.notes.ifBlank { "Installed" }, money(row.costCents),
                Icons.Outlined.CheckCircle, onClick = { onEdit(row) }, onDelete = { onDelete(row) })
        }
        item { Spacer(Modifier.height(75.dp)) }
    }
}

@Composable
fun NotesPage(data: GarageSnapshot, onAdd: () -> Unit,
              onEdit: (NoteEntry) -> Unit, onDelete: (NoteEntry) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionTitle("Photos / Notes", "Garage notes and privately saved truck pictures.") }
        item { Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Add, null); Text("Add note / photo") } }
        if (data.notes.isEmpty()) item { EmptyState("No notes or pictures saved yet.") }
        items(data.notes, key = { it.id }) { row ->
            Panel {
                Text(row.title, fontWeight = FontWeight.Bold, color = ZeusInk)
                Muted(row.date)
                if (row.body.isNotBlank()) Text(row.body, color = ZeusInk)
                if (row.photoPath != null) {
                    val bitmap = remember(row.photoPath) {
                        android.graphics.BitmapFactory.Options().apply { inSampleSize = 2 }.let { options ->
                            runCatching { android.graphics.BitmapFactory.decodeFile(row.photoPath, options) }.getOrNull()
                        }
                    }
                    if (bitmap != null) androidx.compose.foundation.Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Attached garage photo",
                        modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    ) else Muted("Attached photo unavailable")
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onEdit(row) }) { Text("Edit") }
                    TextButton(onClick = { onDelete(row) }) { Text("Delete", color = RubyDark) }
                }
            }
        }
        item { Spacer(Modifier.height(75.dp)) }
    }
}

private data class JournalItem(val id: String, val date: String, val title: String, val subtitle: String,
                               val costCents: Long, val kind: String)

@Composable
fun HistoryPage(data: GarageSnapshot, onGo: (String) -> Unit, onExport: () -> Unit) {
    val entries = buildList {
        data.services.forEach { add(JournalItem("s${it.id}", it.date, it.title, "Maintenance", it.costCents, "Maintenance")) }
        data.repairs.forEach { add(JournalItem("r${it.id}", it.date, it.title, "Repair", it.costCents, "Repairs")) }
        data.fuel.forEach { add(JournalItem("f${it.id}", it.date, "Fuel-up", "${it.gallons} gal", it.totalCents, "Fuel")) }
        data.notes.forEach { add(JournalItem("n${it.id}", it.date, it.title, "Garage note", 0, "Notes")) }
        data.codes.forEach { add(JournalItem("c${it.id}", it.date, it.code, "Diagnostic entry", 0, "Diagnostics")) }
    }.sortedWith(compareByDescending<JournalItem> { it.date }.thenByDescending { it.id })
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionTitle("History", "One timeline of everything you've recorded.") }
        item { Panel {
            SmallCaps("Recorded expenses")
            Text(money(data.actualExpenses), color = Ruby,
                style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Muted("Totals include fuel, completed service and repairs, and installed upgrades. Wishlist estimates are excluded.")
            OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.FileDownload, null)
                Spacer(Modifier.width(6.dp))
                Text("Export expense CSV")
            }
        } }
        if (entries.isEmpty()) item { EmptyState("No activity yet. Add a maintenance, fuel, repair, or note entry.") }
        items(entries, key = { it.id }) { row ->
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(row.title, fontWeight = FontWeight.Bold)
                        Muted("${row.date} • ${row.subtitle}")
                    }
                    if (row.costCents > 0) Text(money(row.costCents), color = Ruby, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = { onGo(row.kind) }) { Text("Open ${row.kind} ›") }
            }
        }
        item { Spacer(Modifier.height(75.dp)) }
    }
}

@Composable
fun MorePage(onGo: (String) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionTitle("More", "Tools and records for your garage.") }
        item { DashTile("Diagnostics", "Offline DTC library", Icons.Outlined.MonitorHeart, Modifier.fillMaxWidth()) { onGo("Diagnostics") } }
        item { DashTile("Fuel & expenses", "Fill-ups and costs", Icons.Outlined.LocalGasStation, Modifier.fillMaxWidth()) { onGo("Fuel") } }
        item { DashTile("Repairs", "Parts and invoices", Icons.Outlined.Build, Modifier.fillMaxWidth()) { onGo("Repairs") } }
        item { DashTile("Upgrades", "Wishlist and installed", Icons.Outlined.AutoAwesome, Modifier.fillMaxWidth()) { onGo("Upgrades") } }
        item { DashTile("Photos / Notes", "Truck memories", Icons.Outlined.PhotoLibrary, Modifier.fillMaxWidth()) { onGo("Notes") } }
        item { Muted("ZEUS Garage v0.1.0 • Offline-only. No cloud login, vehicle remote controls or live OBD reader.") }
        item { Spacer(Modifier.height(75.dp)) }
    }
}
