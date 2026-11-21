package com.zeusgarage.mobile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate

/** Keeps the editor bound to one record; it never writes without pressing Save. */
data class EditRequest(val kind: String, val id: Long? = null, val presetTaskId: Long? = null)

@Composable
fun FormInput(value: String, onChange: (String) -> Unit, label: String,
              numeric: Boolean = false, decimal: Boolean = false,
              lines: Int = 1, required: Boolean = false) {
    OutlinedTextField(
        value = value, onValueChange = { onChange(it.take(if (lines > 1) 1000 else 140)) },
        label = { Text(label + if (required) " *" else "") },
        modifier = Modifier.fillMaxWidth(), minLines = lines, maxLines = if (lines > 1) 6 else 1,
        singleLine = lines == 1, shape = RoundedCornerShape(13.dp),
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal
            else if (numeric) KeyboardType.Number else KeyboardType.Text)
    )
}

@Composable
fun EntryEditor(
    request: EditRequest, data: GarageSnapshot,
    photoSelected: Boolean,
    onPhoto: () -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (Map<String, String>) -> Unit
) {
    val vehicle = data.vehicles.firstOrNull { it.id == request.id }
    val task = data.tasks.firstOrNull { it.id == request.id }
    val service = data.services.firstOrNull { it.id == request.id }
    val fuel = data.fuel.firstOrNull { it.id == request.id }
    val repair = data.repairs.firstOrNull { it.id == request.id }
    val upgrade = data.upgrades.firstOrNull { it.id == request.id }
    val note = data.notes.firstOrNull { it.id == request.id }
    val code = data.codes.firstOrNull { it.id == request.id }
    val symptom = data.symptoms.firstOrNull { it.id == request.id }

    val initial = remember(request) {
        mutableStateMapOf<String, String>().apply {
            when (request.kind) {
                "vehicle" -> {
                    put("nickname", vehicle?.nickname.orEmpty())
                    put("year", vehicle?.year.orEmpty()); put("make", vehicle?.make.orEmpty())
                    put("model", vehicle?.model.orEmpty()); put("engine", vehicle?.engine.orEmpty())
                    put("odometer", vehicle?.odometer?.toString().orEmpty()); put("notes", vehicle?.notes.orEmpty())
                }
                "task" -> {
                    put("title", task?.title.orEmpty()); put("interval", task?.intervalMiles?.takeIf { it > 0 }?.toString().orEmpty())
                    put("last", task?.lastDoneMiles?.toString().orEmpty()); put("notes", task?.notes.orEmpty())
                }
                "service" -> {
                    val selected = service?.taskId ?: request.presetTaskId
                    put("taskId", selected?.toString().orEmpty())
                    put("title", service?.title ?: data.tasks.firstOrNull { it.id == selected }?.title.orEmpty())
                    put("date", service?.date ?: today()); put("odometer", service?.mileage?.toString().orEmpty())
                    put("cost", service?.costCents?.takeIf { it > 0 }?.let { "%.2f".format(java.util.Locale.US, it / 100.0) }.orEmpty())
                    put("notes", service?.notes.orEmpty())
                }
                "fuel" -> {
                    put("date", fuel?.date ?: today()); put("odometer", fuel?.odometer?.toString() ?: data.active?.odometer?.toString().orEmpty())
                    put("gallons", fuel?.gallons?.toString().orEmpty())
                    put("cost", fuel?.totalCents?.let { "%.2f".format(java.util.Locale.US, it / 100.0) }.orEmpty())
                    put("full", if (fuel?.fullTank != false) "yes" else "no")
                }
                "repair" -> {
                    put("date", repair?.date ?: today()); put("title", repair?.title.orEmpty())
                    put("part", repair?.part.orEmpty()); put("odometer", repair?.mileage?.toString().orEmpty())
                    put("cost", repair?.costCents?.takeIf { it > 0 }?.let { "%.2f".format(java.util.Locale.US, it / 100.0) }.orEmpty())
                    put("notes", repair?.notes.orEmpty())
                }
                "upgrade" -> {
                    put("name", upgrade?.name.orEmpty()); put("status", upgrade?.status ?: "Planned")
                    put("cost", upgrade?.costCents?.takeIf { it > 0 }?.let { "%.2f".format(java.util.Locale.US, it / 100.0) }.orEmpty())
                    put("notes", upgrade?.notes.orEmpty())
                }
                "note" -> {
                    put("date", note?.date ?: today()); put("title", note?.title.orEmpty()); put("body", note?.body.orEmpty())
                }
                "symptom" -> {
                    put("date", symptom?.date ?: today()); put("title", symptom?.title.orEmpty())
                    put("severity", symptom?.severity ?: "Moderate")
                    put("occurrence", symptom?.occurrence ?: "Intermittent")
                    put("status", symptom?.status ?: "Open")
                    put("notes", symptom?.notes.orEmpty())
                }
                "code" -> {
                    put("date", code?.date ?: today()); put("code", code?.code.orEmpty())
                    put("status", code?.status ?: "Observed")
                    put("symptoms", code?.symptoms.orEmpty()); put("notes", code?.notes.orEmpty())
                }
                else -> Unit
            }
        }
    }
    fun value(key: String) = initial[key].orEmpty()
    val validDate = listOf("service", "fuel", "repair", "note", "code", "symptom").let { request.kind !in it ||
        runCatching { LocalDate.parse(value("date")) }.isSuccess }
    val validCost = request.kind !in listOf("service", "fuel", "repair", "upgrade") ||
        value("cost").isBlank() || moneyInputToCents(value("cost")) != null
    val validMileage = request.kind !in listOf("vehicle", "task", "service", "fuel", "repair") ||
        (if (request.kind == "task") value("last") else value("odometer"))
            .let { it.isBlank() || (it.toIntOrNull() ?: -1) in 0..10_000_000 }
    val valid = validDate && validCost && validMileage && when (request.kind) {
        "vehicle" -> listOf("nickname", "year", "make", "model").all { value(it).isNotBlank() } &&
            (value("year").toIntOrNull() ?: 0) in 1900..2100
        "task" -> value("title").isNotBlank() && (value("interval").isBlank() ||
            (value("interval").toIntOrNull() ?: -1) in 0..1_000_000)
        "service" -> value("title").isNotBlank()
        "fuel" -> value("odometer").toIntOrNull() != null &&
            (value("gallons").toDoubleOrNull() ?: 0.0) in 0.001..1000.0 && value("cost").isNotBlank()
        "repair" -> value("title").isNotBlank()
        "upgrade" -> value("name").isNotBlank()
        "note" -> value("title").isNotBlank()
        "symptom" -> value("title").isNotBlank() &&
            value("severity") in listOf("Mild", "Moderate", "Severe") &&
            value("occurrence") in listOf("Intermittent", "Constant") &&
            value("status") in listOf("Open", "Resolved")
        "code" -> value("code").matches(Regex("[A-Z][0-9A-Z]{4,6}"))
        else -> false
    }
    val title = when (request.kind) {
        "vehicle" -> "Vehicle"
        "task" -> "Service schedule"
        "service" -> "Completed service"
        "fuel" -> "Fuel-up"
        "repair" -> "Repair"
        "upgrade" -> "Upgrade"
        "note" -> "Garage note / photo"
        "symptom" -> "Symptom"
        else -> "Trouble code"
    }
    @Composable fun field(key: String, label: String, numeric: Boolean = false,
                          decimal: Boolean = false, lines: Int = 1, required: Boolean = false) {
        FormInput(value(key), { initial[key] = it }, label, numeric, decimal, lines, required)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (request.id == null) "Add $title" else "Edit $title") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 475.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (request.kind) {
                    "vehicle" -> {
                        field("nickname", "Truck nickname", required = true)
                        field("year", "Year", numeric = true, required = true)
                        field("make", "Make", required = true)
                        field("model", "Model", required = true)
                        field("engine", "Engine")
                        field("odometer", "Current odometer (miles)", numeric = true)
                        field("notes", "Truck details", lines = 3)
                    }
                    "task" -> {
                        field("title", "Service name", required = true)
                        field("interval", "Repeat every N miles (0 = unconfigured)", numeric = true)
                        field("last", "Last completed mileage", numeric = true)
                        field("notes", "Service notes", lines = 3)
                        Muted("No intervals are assumed. Use your maintenance records or OEM schedule.")
                    }
                    "service" -> {
                        var expanded by remember(request) { mutableStateOf(false) }
                        val chosen = data.tasks.firstOrNull { it.id.toString() == value("taskId") }
                        Box {
                            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("Service schedule: ${chosen?.title ?: "None / custom"} ▾")
                            }
                            DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                                DropdownMenuItem(text = { Text("None / custom") }, onClick = {
                                    initial["taskId"] = ""; expanded = false
                                })
                                data.tasks.forEach { task ->
                                    DropdownMenuItem(text = { Text(task.title) }, onClick = {
                                        initial["taskId"] = task.id.toString()
                                        initial["title"] = task.title
                                        expanded = false
                                    })
                                }
                            }
                        }
                        field("title", "Service performed", required = true)
                        field("date", "Date (YYYY-MM-DD)", required = true)
                        field("odometer", "Mileage when performed", numeric = true)
                        field("cost", "Total cost ($)", decimal = true)
                        field("notes", "Notes / filter / parts", lines = 3)
                        Muted("Saving a service with its linked schedule and mileage updates that schedule's last-done mileage.")
                    }
                    "fuel" -> {
                        field("date", "Date (YYYY-MM-DD)", required = true)
                        field("odometer", "Odometer (miles)", numeric = true, required = true)
                        field("gallons", "Gallons added", decimal = true, required = true)
                        field("cost", "Amount paid ($)", decimal = true, required = true)
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text("Filled completely", modifier = Modifier.weight(1f))
                            Switch(checked = value("full") == "yes", onCheckedChange = {
                                initial["full"] = if (it) "yes" else "no"
                            })
                        }
                        Muted("Only two full-tank readings can produce a fill-to-fill MPG estimate.")
                    }
                    "repair" -> {
                        field("title", "Repair / problem", required = true)
                        field("part", "Parts replaced")
                        field("date", "Date (YYYY-MM-DD)", required = true)
                        field("odometer", "Mileage", numeric = true)
                        field("cost", "Total spent ($)", decimal = true)
                        field("notes", "Description and results", lines = 3)
                    }
                    "upgrade" -> {
                        field("name", "Upgrade name", required = true)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Planned", "Installed").forEach { option ->
                                FilterChip(value("status") == option, onClick = { initial["status"] = option },
                                    label = { Text(option) })
                            }
                        }
                        field("cost", "Actual cost ($) when installed", decimal = true)
                        field("notes", "Part / fitment notes", lines = 3)
                        Muted("Planned upgrade amounts aren't counted as paid expenses.")
                    }
                    "note" -> {
                        field("title", "Note title", required = true)
                        field("date", "Date (YYYY-MM-DD)", required = true)
                        field("body", "Note details", lines = 4)
                        OutlinedButton(onClick = onPhoto, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Outlined.AddPhotoAlternate, null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (photoSelected) "Photo selected ✓" else if (note?.photoPath != null)
                                "Replace attached photo" else "Attach photo")
                        }
                        Muted(if (photoSelected) "Selected photo will be copied into private storage when saved."
                            else if (note?.photoPath != null) "Existing photo will be kept unless replaced."
                            else "Images are stored locally, not uploaded.")
                    }
                    "symptom" -> {
                        field("title", "What is happening?", required = true)
                        field("date", "Date (YYYY-MM-DD)", required = true)
                        Text("Severity", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Mild", "Moderate", "Severe").forEach { option ->
                                FilterChip(value("severity") == option,
                                    onClick = { initial["severity"] = option }, label = { Text(option) })
                            }
                        }
                        Text("How often?", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Intermittent", "Constant").forEach { option ->
                                FilterChip(value("occurrence") == option,
                                    onClick = { initial["occurrence"] = option }, label = { Text(option) })
                            }
                        }
                        Text("Status", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Open", "Resolved").forEach { option ->
                                FilterChip(value("status") == option,
                                    onClick = { initial["status"] = option }, label = { Text(option) })
                            }
                        }
                        field("notes", "Driving conditions, sounds and observations", lines = 4)
                        Muted("This is a manual symptom diary, not an automatic diagnosis.")
                    }
                    "code" -> {
                        FormInput(value("code"), { initial["code"] = it.uppercase().take(7) },
                            "DTC (for example P2457)", required = true)
                        field("date", "Date (YYYY-MM-DD)", required = true)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Observed", "Resolved").forEach { option ->
                                FilterChip(value("status") == option, onClick = { initial["status"] = option },
                                    label = { Text(option) })
                            }
                        }
                        field("symptoms", "Symptoms seen", lines = 3)
                        field("notes", "Repair checks and notes", lines = 3)
                        Muted("This records a code you saw elsewhere. It does not connect to an OBD scanner.")
                    }
                }
                if (!valid) Muted("Complete required fields and use valid dates and numbers to save.")
            }
        },
        confirmButton = { Button(enabled = valid, onClick = { onSubmit(initial.toMap()) }) {
            Text("Save")
        } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
