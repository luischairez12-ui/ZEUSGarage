package com.zeusgarage.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** BoreLab-style slide-out navigation: quick bottom tabs stay available. */
@Composable
fun ZeusNavigationDrawer(
    current: String,
    activeTruck: String,
    onNavigate: (String) -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = Color.White,
        modifier = Modifier.width(298.dp)
    ) {
        Column(Modifier.fillMaxHeight().verticalScroll(rememberScrollState())) {
            Column(
                Modifier.fillMaxWidth().background(RubyTint)
                    .padding(start = 20.dp, top = 28.dp, end = 12.dp, bottom = 19.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Z", color = Ruby, style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("ZEUS Garage", color = ZeusInk,
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleLarge)
                        SmallCaps("Truck command center")
                    }
                }
                Text(activeTruck, color = RubyDark, fontWeight = FontWeight.Bold)
            }

            @Composable
            fun drawerRow(title: String, route: String, icon: ImageVector) {
                NavigationDrawerItem(
                    label = { Text(title, fontWeight = if (current == route)
                        FontWeight.Bold else FontWeight.Medium) },
                    selected = current == route,
                    onClick = { onNavigate(route) },
                    icon = { Icon(icon, contentDescription = null,
                        tint = if (current == route) Ruby else ZeusMuted) },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 1.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = RubyTint,
                        selectedTextColor = RubyDark,
                        unselectedTextColor = ZeusInk
                    )
                )
            }
            Spacer(Modifier.height(12.dp))
            SmallCapsDrawer("MY TRUCK")
            drawerRow("Dashboard", "Home", Icons.Outlined.Home)
            drawerRow("Vehicle Health", "Health", Icons.Outlined.FavoriteBorder)
            drawerRow("Active Issues", "Issues", Icons.Outlined.WarningAmber)
            drawerRow("Symptom Tracker", "Symptoms", Icons.Outlined.EditNote)
            drawerRow("Diagnostics", "Diagnostics", Icons.Outlined.MonitorHeart)
            HorizontalDivider(Modifier.padding(vertical = 7.dp), color = ZeusLine)
            SmallCapsDrawer("GARAGE")
            drawerRow("My Vehicles", "Garage", Icons.Outlined.DirectionsCar)
            drawerRow("Maintenance", "Maintenance", Icons.Outlined.Build)
            drawerRow("Repairs", "Repairs", Icons.Outlined.Handyman)
            drawerRow("Upgrades", "Upgrades", Icons.Outlined.AutoAwesome)
            drawerRow("Parts & Stores", "Parts", Icons.Outlined.Storefront)
            HorizontalDivider(Modifier.padding(vertical = 7.dp), color = ZeusLine)
            SmallCapsDrawer("RECORDS")
            drawerRow("Fuel & Mileage", "Fuel", Icons.Outlined.LocalGasStation)
            drawerRow("Expenses & History", "History", Icons.Outlined.ReceiptLong)
            drawerRow("Photos & Notes", "Notes", Icons.Outlined.PhotoLibrary)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SmallCapsDrawer(label: String) {
    Box(Modifier.padding(start = 28.dp, bottom = 6.dp, top = 4.dp)) { SmallCaps(label) }
}
