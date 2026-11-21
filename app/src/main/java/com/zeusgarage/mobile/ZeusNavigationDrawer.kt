package com.zeusgarage.mobile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ZEUS Batch 4: a BoreLab Field-style launcher panel instead of a stock navigation drawer.
 * This only navigates to existing pages. It does not edit the Home dashboard or truck hero.
 */
private data class ZeusLauncherShortcut(
    val group: String,
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

private val launcherShortcuts = listOf(
    ZeusLauncherShortcut("MY TRUCK", "Home", "Home", "Your truck dashboard", Icons.Outlined.Home),
    ZeusLauncherShortcut("MY TRUCK", "Health", "Vehicle Health", "Health overview and reminders", Icons.Outlined.FavoriteBorder),
    ZeusLauncherShortcut("MY TRUCK", "Issues", "Active Issues", "Problems that need attention", Icons.Outlined.WarningAmber),
    ZeusLauncherShortcut("MY TRUCK", "Symptoms", "Symptom Tracker", "Record sounds, faults and patterns", Icons.Outlined.EditNote),
    ZeusLauncherShortcut("MY TRUCK", "Diagnostics", "Diagnostics", "Saved codes and DTC reference", Icons.Outlined.MonitorHeart),
    ZeusLauncherShortcut("GARAGE", "Garage", "My Vehicles", "Select or edit a vehicle", Icons.Outlined.DirectionsCar),
    ZeusLauncherShortcut("GARAGE", "Maintenance", "Maintenance", "Services and reminders", Icons.Outlined.Build),
    ZeusLauncherShortcut("GARAGE", "Repairs", "Repairs", "Parts, work and costs", Icons.Outlined.Handyman),
    ZeusLauncherShortcut("GARAGE", "Upgrades", "Upgrades", "Your modifications and wishlist", Icons.Outlined.AutoAwesome),
    ZeusLauncherShortcut("GARAGE", "Parts", "Parts & Stores", "Shop parts and find stores", Icons.Outlined.Storefront),
    ZeusLauncherShortcut("RECORDS", "Fuel", "Fuel & Mileage", "Fill-ups and MPG history", Icons.Outlined.LocalGasStation),
    ZeusLauncherShortcut("RECORDS", "History", "Expenses & History", "Your spending and repair timeline", Icons.Outlined.ReceiptLong),
    ZeusLauncherShortcut("RECORDS", "Notes", "Photos & Notes", "Pictures and garage observations", Icons.Outlined.PhotoLibrary)
)

@Composable
fun ZeusLauncherPanel(
    current: String,
    activeTruck: String,
    onNavigate: (String) -> Unit,
    onClose: () -> Unit,
    onQuickAdd: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        val term = query.trim()
        if (term.isEmpty()) launcherShortcuts
        else launcherShortcuts.filter { item ->
            listOf(item.title, item.subtitle, item.group, item.route)
                .any { it.contains(term, ignoreCase = true) }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp),
        color = Color.White,
        shadowElevation = 14.dp
    ) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(Color.White, Color(0xFFFFF0F2))))
                    .padding(start = 22.dp, end = 16.dp, top = 24.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        SmallCaps("ZEUS GARAGE")
                        Text("Garage Menu", fontSize = 30.sp, fontWeight = FontWeight.Black, color = ZeusInk)
                        Muted("Everything one tap away")
                    }
                    Surface(
                        modifier = Modifier.size(50.dp).clickable(onClick = onClose),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, ZeusLine)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Close, contentDescription = "Close garage menu",
                                tint = ZeusInk, modifier = Modifier.size(28.dp))
                        }
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text("ACTIVE TRUCK  •  $activeTruck", fontSize = 12.sp,
                    fontWeight = FontWeight.Bold, color = RubyDark)
            }

            HorizontalDivider(color = ZeusLine)

            Column(
                Modifier.fillMaxWidth().padding(start = 17.dp, end = 17.dp, top = 14.dp, bottom = 5.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                SmallCaps("SEARCH SHORTCUTS")
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(80) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Find a page, e.g. parts or fuel") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Ruby) },
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                            Icon(Icons.Outlined.Clear, contentDescription = "Clear search")
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp)
                )
                Muted("Finds ZEUS pages. Your saved records stay inside each section.")
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 17.dp, end = 17.dp, top = 12.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                if (filtered.isEmpty()) {
                    item {
                        Panel { Muted("No matching pages. Try maintenance, parts, fuel, or health.") }
                    }
                }
                listOf("MY TRUCK", "GARAGE", "RECORDS").forEach { section ->
                    val entries = filtered.filter { it.group == section }
                    if (entries.isNotEmpty()) {
                        item(key = "section_$section") {
                            Spacer(Modifier.height(7.dp))
                            SmallCaps(section)
                        }
                        items(entries, key = { it.route }) { shortcut ->
                            ZeusLauncherCard(shortcut, selected = current == shortcut.route) {
                                onNavigate(shortcut.route)
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = ZeusLine)
            Box(
                Modifier.fillMaxWidth()
                    .background(Color.White)
                    .padding(start = 17.dp, end = 17.dp, top = 13.dp, bottom = 14.dp)
            ) {
                Button(
                    onClick = onQuickAdd,
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(19.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Ruby)
                ) {
                    Icon(Icons.Outlined.AddCircleOutline, contentDescription = null,
                        modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(11.dp))
                    Text("Quick Add Record", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun ZeusLauncherCard(
    shortcut: ZeusLauncherShortcut,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(21.dp),
        color = if (selected) Color(0xFFFFF1F3) else Color.White,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp,
            if (selected) Color(0xFFE0A3AF) else ZeusLine),
        shadowElevation = if (selected) 2.dp else 1.dp
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 13.dp, top = 11.dp, bottom = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = RubyTint,
                shape = RoundedCornerShape(15.dp)
            ) {
                Box(Modifier.size(54.dp), contentAlignment = Alignment.Center) {
                    Icon(shortcut.icon, contentDescription = null,
                        tint = Ruby, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(shortcut.title, fontWeight = FontWeight.ExtraBold, color = ZeusInk,
                    style = MaterialTheme.typography.titleMedium)
                Text(shortcut.subtitle, color = ZeusMuted,
                    style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null,
                tint = RubyDark, modifier = Modifier.size(23.dp))
        }
    }
}
