package com.zeusgarage.mobile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush

val Ruby = Color(0xFFB10D2D)
val RubyDark = Color(0xFF8D0A26)
val RubyTint = Color(0xFFFFE7EB)
val ZeusInk = Color(0xFF192B35)
val ZeusMuted = Color(0xFF6B7881)
val ZeusLine = Color(0xFFE0E5E9)
val ZeusBackground = Color(0xFFFAFBFC)

@Composable
fun ZeusTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(
        primary = Ruby, onPrimary = Color.White, secondary = RubyDark,
        background = ZeusBackground, onBackground = ZeusInk,
        surface = Color.White, onSurface = ZeusInk,
        surfaceVariant = RubyTint, onSurfaceVariant = ZeusMuted,
        outlineVariant = ZeusLine, error = Color(0xFFCA2446)
    ), content = content)
}

@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, ZeusLine),
        shadowElevation = 2.dp
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
fun SmallCaps(value: String) {
    Text(value.uppercase(), fontSize = 12.sp, letterSpacing = 1.5.sp,
        color = RubyDark, fontWeight = FontWeight.ExtraBold)
}

@Composable
fun Muted(value: String, modifier: Modifier = Modifier) {
    Text(value, modifier, color = ZeusMuted, style = MaterialTheme.typography.bodySmall)
}

@Composable
fun SectionTitle(title: String, description: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, fontWeight = FontWeight.ExtraBold, color = ZeusInk,
            style = MaterialTheme.typography.headlineSmall)
        if (description != null) Muted(description)
    }
}

@Composable
fun ZeusHeader() {
    Row(
        Modifier.fillMaxWidth().background(Color.White)
            .padding(start = 20.dp, end = 20.dp, top = 15.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Z", fontSize = 34.sp, fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic, color = Ruby, lineHeight = 36.sp)
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("ZEUS", fontWeight = FontWeight.ExtraBold, color = ZeusInk,
                    style = MaterialTheme.typography.titleLarge)
                Text(" Garage", fontWeight = FontWeight.ExtraBold, color = Ruby,
                    style = MaterialTheme.typography.titleLarge)
            }
            Text("TRUCK COMMAND CENTER", fontSize = 9.sp, letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold, color = ZeusMuted)
        }
    }
}

@Composable
fun TruckHero(vehicle: Vehicle?, onSwitch: () -> Unit, onEdit: () -> Unit) {
    Surface(
        color = Color.White, shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, ZeusLine), shadowElevation = 3.dp
    ) {
        Column {
            Box(Modifier.fillMaxWidth().background(Brush.horizontalGradient(
                listOf(Ruby, RubyDark))).height(5.dp))
            Column(Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    SmallCaps("Current truck")
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onSwitch, colors = ButtonDefaults.textButtonColors(contentColor = Ruby)) {
                        Text("Switch ›", fontWeight = FontWeight.Bold)
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(0.85f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(vehicle?.nickname ?: "My Garage", fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.headlineSmall, color = ZeusInk)
                        Text(vehicle?.display ?: "Add your first vehicle", color = ZeusInk,
                            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Muted(vehicle?.engine.orEmpty())
                        Text(odometerText(vehicle?.odometer), color = Ruby,
                            fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (vehicle?.id == 1L) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(R.drawable.zeus_truck),
                            contentDescription = "Ruby Red lifted F-250 hero artwork",
                            modifier = Modifier.weight(1.15f).height(150.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(Modifier.weight(1.15f).height(150.dp),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.DirectionsCar, contentDescription = "Vehicle",
                                modifier = Modifier.size(84.dp), tint = Ruby)
                        }
                    }
                }
                if (vehicle?.notes?.isNotBlank() == true) {
                    Muted(vehicle.notes, Modifier.fillMaxWidth())
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onEdit) {
                        Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Edit truck / mileage", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun DashTile(title: String, subtitle: String, icon: ImageVector, modifier: Modifier = Modifier,
             onClick: () -> Unit) {
    Surface(
        modifier = modifier.height(148.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(23.dp), color = Color.White,
        border = BorderStroke(1.dp, ZeusLine), shadowElevation = 2.dp
    ) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(shape = RoundedCornerShape(15.dp), color = RubyTint) {
                Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = Ruby, modifier = Modifier.size(26.dp))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = ZeusInk, fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.ChevronRight, null, tint = Ruby, modifier = Modifier.size(21.dp))
            }
            Muted(subtitle)
        }
    }
}

@Composable
fun RecordCard(title: String, subtitle: String, trailing: String? = null,
               icon: ImageVector = Icons.Outlined.Description,
               onClick: (() -> Unit)? = null, onDelete: (() -> Unit)? = null) {
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(color = RubyTint, shape = RoundedCornerShape(13.dp)) {
                Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = Ruby, modifier = Modifier.size(23.dp))
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = ZeusInk,
                    style = MaterialTheme.typography.bodyLarge)
                Muted(subtitle)
            }
            if (trailing != null) Text(trailing, color = Ruby, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium)
        }
        if (onClick != null || onDelete != null) {
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                if (onClick != null) TextButton(onClick = onClick) { Text("Edit / details") }
                if (onDelete != null) TextButton(onClick = onDelete) {
                    Icon(Icons.Outlined.DeleteOutline, null, tint = RubyDark, modifier = Modifier.size(17.dp))
                    Text("Delete", color = RubyDark)
                }
            }
        }
    }
}

@Composable
fun EmptyState(message: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Panel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = ZeusMuted)
            Spacer(Modifier.width(8.dp))
            Muted(message, Modifier.weight(1f))
        }
        if (action != null && onAction != null) OutlinedButton(onClick = onAction) { Text(action) }
    }
}

@Composable
fun ZeusBottomNav(current: String, onGo: (String) -> Unit) {
    val rootTab = when (current) {
        "Home" -> "Home"
        "Garage" -> "Garage"
        "Maintenance" -> "Service"
        "History" -> "History"
        else -> "More"
    }
    NavigationBar(containerColor = Color.White, tonalElevation = 3.dp) {
        listOf(
            Triple("Home", Icons.Outlined.Home, "Home"),
            Triple("Garage", Icons.Outlined.DirectionsCar, "Garage"),
            Triple("Service", Icons.Outlined.Build, "Maintenance"),
            Triple("History", Icons.Outlined.History, "History"),
            Triple("More", Icons.Outlined.MoreHoriz, "More")
        ).forEach { (tab, vector, route) ->
            NavigationBarItem(
                selected = rootTab == tab,
                onClick = { onGo(route) },
                icon = { Icon(vector, contentDescription = null) },
                label = { Text(tab, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Ruby, selectedTextColor = Ruby,
                    indicatorColor = RubyTint, unselectedIconColor = ZeusMuted,
                    unselectedTextColor = ZeusMuted
                )
            )
        }
    }
}

@Composable
fun HomePage(data: GarageSnapshot, onGo: (String) -> Unit,
             onSwitch: () -> Unit, onEditTruck: () -> Unit, onQuickAdd: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { TruckHero(data.active, onSwitch, onEditTruck) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SmallCaps("Garage dashboard")
                Text("Ready for today", fontWeight = FontWeight.ExtraBold,
                    fontSize = 29.sp, color = ZeusInk)
                Muted("Manage maintenance, performance, and your build.")
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashTile("Maintenance", "Service and reminders", Icons.Outlined.Build,
                    Modifier.weight(1f)) { onGo("Maintenance") }
                DashTile("Diagnostics", "Codes and health", Icons.Outlined.MonitorHeart,
                    Modifier.weight(1f)) { onGo("Diagnostics") }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashTile("Fuel", "Mileage and fill-ups", Icons.Outlined.LocalGasStation,
                    Modifier.weight(1f)) { onGo("Fuel") }
                DashTile("Repairs", "Parts and fixes", Icons.Outlined.Settings,
                    Modifier.weight(1f)) { onGo("Repairs") }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashTile("Upgrades", "Mods and wishlist", Icons.Outlined.Layers,
                    Modifier.weight(1f)) { onGo("Upgrades") }
                DashTile("Photos / Notes", "Truck records", Icons.Outlined.PhotoLibrary,
                    Modifier.weight(1f)) { onGo("Notes") }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashTile("Vehicle Health", "Issues and symptoms", Icons.Outlined.FavoriteBorder,
                    Modifier.weight(1f)) { onGo("Health") }
                DashTile("Parts & Stores", "Shop and navigate", Icons.Outlined.Storefront,
                    Modifier.weight(1f)) { onGo("Parts") }
            }
        }
        item {
            val needAttention = data.tasks.mapNotNull { task ->
                task.remaining(data.active?.odometer)?.let { left -> task.title to left }
            }.sortedBy { it.second }.firstOrNull()
            Panel {
                SmallCaps("At a glance")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Total recorded spending", fontWeight = FontWeight.SemiBold)
                        Muted("Fuel + completed repairs/services/upgrades")
                    }
                    Text(money(data.actualExpenses), color = Ruby, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(color = ZeusLine)
                Text("Next maintenance", fontWeight = FontWeight.SemiBold)
                Muted(when {
                    needAttention == null -> "Set mileage and service intervals to see due reminders."
                    needAttention.second <= 0 -> "${needAttention.first} • Due / overdue by ${-needAttention.second} mi"
                    else -> "${needAttention.first} • ${needAttention.second} mi remaining"
                })
                OutlinedButton(onClick = onQuickAdd, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Add, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Quick add a record")
                }
            }
        }
        item { Spacer(Modifier.height(65.dp)) }
    }
}
