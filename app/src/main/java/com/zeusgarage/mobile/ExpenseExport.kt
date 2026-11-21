package com.zeusgarage.mobile

import android.content.Context
import android.net.Uri
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Read-only, user-directed export. No cloud dependency or automatic upload. */
object ExpenseExport {
    private fun cell(value: String): String = "\"" + value.replace("\"", "\"\"") + "\""
    fun csv(data: GarageSnapshot): String {
        val rows = buildList {
            data.fuel.forEach { add(listOf(it.date, "Fuel", "Fuel-up (%.2f gal)".format(Locale.US, it.gallons),
                it.odometer.toString(), it.totalCents)) }
            data.repairs.forEach { add(listOf(it.date, "Repair", it.title + " • " + it.part,
                it.mileage?.toString().orEmpty(), it.costCents)) }
            data.services.forEach { add(listOf(it.date, "Service", it.title,
                it.mileage?.toString().orEmpty(), it.costCents)) }
            data.upgrades.filter { it.status == "Installed" }.forEach {
                add(listOf("", "Installed upgrade", it.name, "", it.costCents))
            }
        }.sortedBy { it[0] as String }
        return buildString {
            appendLine("Date,Type,Description,Odometer (mi),Amount USD")
            for (r in rows) {
                appendLine(listOf(
                    cell(r[0] as String), cell(r[1] as String), cell(r[2] as String),
                    cell(r[3] as String),
                    "%.2f".format(Locale.US, (r[4] as Long) / 100.0)
                ).joinToString(","))
            }
        }
    }
    fun write(context: Context, uri: Uri, data: GarageSnapshot) {
        context.contentResolver.openOutputStream(uri, "w")?.bufferedWriter(Charsets.UTF_8)?.use {
            it.write(csv(data))
        } ?: error("Unable to write CSV file")
    }
}
