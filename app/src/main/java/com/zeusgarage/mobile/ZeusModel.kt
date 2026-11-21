package com.zeusgarage.mobile

import java.time.LocalDate
import java.text.NumberFormat
import java.util.Locale

fun today(): String = LocalDate.now().toString()
fun money(cents: Long): String = NumberFormat.getCurrencyInstance(Locale.US).format(cents / 100.0)
fun moneyInputToCents(value: String): Long? = value.trim().takeIf { it.isNotEmpty() }
    ?.toBigDecimalOrNull()?.takeIf { it.signum() >= 0 && it <= java.math.BigDecimal("999999999.99") }
    ?.movePointRight(2)?.setScale(0, java.math.RoundingMode.HALF_UP)?.toLong()
fun odometerText(value: Int?): String = value?.let { "%,d mi".format(Locale.US, it) } ?: "Not entered"

data class Vehicle(
    val id: Long, val nickname: String, val year: String, val make: String,
    val model: String, val engine: String, val odometer: Int?, val notes: String
) {
    val display: String get() = "$year $make $model".trim()
}

data class ServiceTask(
    val id: Long, val vehicleId: Long, val title: String, val intervalMiles: Int,
    val lastDoneMiles: Int?, val notes: String
) {
    val dueMiles: Int? get() = if (intervalMiles > 0 && lastDoneMiles != null) {
        (lastDoneMiles.toLong() + intervalMiles).takeIf { it <= Int.MAX_VALUE }?.toInt()
    } else null
    fun remaining(currentMileage: Int?): Int? = if (currentMileage != null && dueMiles != null) dueMiles!! - currentMileage else null
}

data class ServiceEvent(
    val id: Long, val vehicleId: Long, val taskId: Long?, val title: String,
    val date: String, val mileage: Int?, val costCents: Long, val notes: String
)

data class FuelEntry(
    val id: Long, val vehicleId: Long, val date: String, val odometer: Int,
    val gallons: Double, val totalCents: Long, val fullTank: Boolean
)

data class RepairEntry(
    val id: Long, val vehicleId: Long, val date: String, val title: String,
    val part: String, val mileage: Int?, val costCents: Long, val notes: String
)

data class UpgradeEntry(
    val id: Long, val vehicleId: Long, val name: String,
    val status: String, val costCents: Long, val notes: String
)

data class NoteEntry(
    val id: Long, val vehicleId: Long, val date: String, val title: String,
    val body: String, val photoPath: String?
)

data class DtcEntry(
    val id: Long, val vehicleId: Long, val date: String, val code: String,
    val status: String, val symptoms: String, val notes: String
)

data class GarageSnapshot(
    val vehicles: List<Vehicle> = emptyList(), val activeId: Long = 0,
    val tasks: List<ServiceTask> = emptyList(), val services: List<ServiceEvent> = emptyList(),
    val fuel: List<FuelEntry> = emptyList(), val repairs: List<RepairEntry> = emptyList(),
    val upgrades: List<UpgradeEntry> = emptyList(), val notes: List<NoteEntry> = emptyList(),
    val codes: List<DtcEntry> = emptyList()
) {
    val active: Vehicle? get() = vehicles.firstOrNull { it.id == activeId } ?: vehicles.firstOrNull()
    val actualExpenses: Long get() = services.sumOf { it.costCents } + fuel.sumOf { it.totalCents } +
        repairs.sumOf { it.costCents } + upgrades.filter { it.status == "Installed" }.sumOf { it.costCents }
}

data class DtcDefinition(val code: String, val title: String, val summary: String, val checks: String)

/** A small OFFLINE reference, not a scan-tool diagnosis or a live connection. */
val dtcLibrary = listOf(
    DtcDefinition("P2457", "EGR cooling system performance", "The powertrain controller detected an EGR cooling performance issue. Manufacturer-specific diagnostics may differ.", "Check OEM service information, coolant condition and routing, related sensors and stored freeze-frame data."),
    DtcDefinition("P0401", "EGR flow insufficient", "The controller reported less exhaust-gas-recirculation flow than expected.", "Use a compatible scan tool to verify commanded versus measured EGR operation and related restrictions."),
    DtcDefinition("P0673", "Cylinder 3 glow-plug circuit", "The glow-plug circuit for cylinder 3 has an electrical fault.", "Inspect connectors and wiring and follow model-specific glow-plug test procedures."),
    DtcDefinition("P0299", "Turbocharger underboost", "Boost pressure was below the expected target in some operating conditions.", "Check charge-air pipes and couplings for leaks; compare commanded and measured boost with scan data."),
    DtcDefinition("P0087", "Fuel rail pressure low", "Fuel rail pressure was lower than expected.", "Check fuel supply and filter history and follow the high-pressure fuel safety procedures in the OEM manual."),
    DtcDefinition("P0300", "Random/multiple cylinder misfire", "Multiple or random combustion misfires were detected.", "Record conditions and inspect fuel, electrical and mechanical causes using model-specific diagnostics."),
    DtcDefinition("P0420", "Catalyst efficiency below threshold", "The controller reported catalyst conversion efficiency below the calibrated limit.", "Confirm the correct system configuration and review OEM diagnostics, exhaust leaks and sensor readings.")
)

/** Estimated MPG only for consecutive complete fill-up intervals. Partial fill-ups are included. */
fun latestFullToFullMpg(entries: List<FuelEntry>): Double? {
    val ordered = entries.sortedWith(compareBy<FuelEntry> { it.odometer }.thenBy { it.id })
    val last = ordered.indexOfLast { it.fullTank }
    if (last <= 0) return null
    val first = (last - 1 downTo 0).firstOrNull { ordered[it].fullTank } ?: return null
    val distance = ordered[last].odometer - ordered[first].odometer
    if (distance <= 0) return null
    val gallons = ordered.subList(first + 1, last + 1).sumOf { it.gallons }
    return (distance / gallons).takeIf { it.isFinite() && it in 1.0..100.0 }
}
