package com.zeusgarage.mobile

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        setContent { ZeusTheme { ZeusGarageApp() } }
    }
}

private data class DeleteRequest(val table: String, val id: Long, val label: String)

@Composable
fun ZeusGarageApp() {
    val context = LocalContext.current
    val store = remember(context) { ZeusStore(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var snapshot by remember { mutableStateOf(GarageSnapshot()) }
    var busy by remember { mutableStateOf(true) }
    var revision by remember { mutableIntStateOf(0) }
    var route by remember { mutableStateOf("Home") }
    var launcherOpen by remember { mutableStateOf(false) }
    fun navigate(destination: String) {
        route = destination
        launcherOpen = false
    }
    var editing by remember { mutableStateOf<EditRequest?>(null) }
    var pendingDelete by remember { mutableStateOf<DeleteRequest?>(null) }
    var selectedDefinition by remember { mutableStateOf<DtcDefinition?>(null) }
    var quickAdd by remember { mutableStateOf(false) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    fun notify(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
    fun modify(action: ZeusStore.() -> Unit, onSuccess: () -> Unit = {}) {
        scope.launch {
            busy = true
            try {
                withContext(Dispatchers.IO) { store.action() }
                onSuccess()
                revision++
            } catch (error: Exception) {
                notify("Not saved: " + (error.message ?: "Unexpected error"))
            } finally {
                busy = false
            }
        }
    }
    LaunchedEffect(revision) {
        busy = true
        try {
            snapshot = withContext(Dispatchers.IO) { store.snapshot() }
        } catch (error: Exception) {
            notify("Could not open garage: " + error.message)
        } finally { busy = false }
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingPhotoUri = uri
    }
    val exportPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            val dataToExport = snapshot
            scope.launch {
                try {
                    withContext(Dispatchers.IO) { ExpenseExport.write(context, uri, dataToExport) }
                    notify("Expense CSV exported")
                } catch (error: Exception) { notify("Export failed: " + error.message) }
            }
        }
    }
    fun edit(kind: String, id: Long? = null, preset: Long? = null) {
        pendingPhotoUri = null
        editing = EditRequest(kind, id, preset)
    }
    fun askDelete(table: String, id: Long, label: String) {
        pendingDelete = DeleteRequest(table, id, label)
    }
    BackHandler(enabled = launcherOpen || editing != null || pendingDelete != null || quickAdd || route != "Home") {
        when {
            editing != null -> { editing = null; pendingPhotoUri = null }
            pendingDelete != null -> pendingDelete = null
            quickAdd -> quickAdd = false
            launcherOpen -> launcherOpen = false
            else -> route = "Home"
        }
    }

    // Only the navigation surface changes in Batch 4; the truck hero and dashboard stay untouched.
    Box(Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = ZeusBackground,
        topBar = { Box(Modifier.statusBarsPadding()) { ZeusHeader(onMenu = {
            launcherOpen = true
        }) } },
        bottomBar = { Column(Modifier.navigationBarsPadding()) {
            ZeusBottomNav(route) { destination ->
                if (destination == "More") launcherOpen = true else navigate(destination)
            }
        } },
        floatingActionButton = {
            if (route != "Garage") FloatingActionButton(
                onClick = { quickAdd = true }, containerColor = Ruby, contentColor = Color.White
            ) { Icon(Icons.Outlined.Add, contentDescription = "Quick add record") }
        }
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            when (route) {
                "Home" -> HomePage(snapshot, onGo = { route = it },
                    onSwitch = { route = "Garage" },
                    onEditTruck = { edit("vehicle", snapshot.active?.id) },
                    onQuickAdd = { quickAdd = true })
                "Garage" -> GaragePage(snapshot,
                    onSelect = { v -> modify({ selectVehicle(v.id) }) { route = "Home" } },
                    onEdit = { v -> edit("vehicle", v?.id) },
                    onDelete = { v -> askDelete("vehicle", v.id, v.nickname) })
                "Health" -> HealthCenterPage(snapshot, onGo = ::navigate)
                "Issues" -> ActiveIssuesPage(snapshot, onGo = ::navigate)
                "Symptoms" -> SymptomsPage(snapshot,
                    onAdd = { edit("symptom") }, onEdit = { edit("symptom", it.id) },
                    onDelete = { askDelete("symptom", it.id, it.title) })
                "Parts" -> PartsStoresPage()
                "Maintenance" -> MaintenancePage(snapshot,
                    onAddTask = { edit("task") }, onTask = { edit("task", it.id) },
                    onLog = { edit("service", null, it?.id) }, onEditLog = { edit("service", it.id) },
                    onDelete = ::askDelete)
                "Diagnostics" -> DiagnosticsPage(snapshot,
                    onAdd = { edit("code") }, onEdit = { edit("code", it.id) },
                    onDelete = { askDelete("dtc", it.id, it.code) },
                    onDefinition = { selectedDefinition = it })
                "Fuel" -> FuelPage(snapshot,
                    onAdd = { edit("fuel") }, onEdit = { edit("fuel", it.id) },
                    onDelete = { askDelete("fuel", it.id, "fuel-up on ${it.date}") })
                "Repairs" -> RepairsPage(snapshot,
                    onAdd = { edit("repair") }, onEdit = { edit("repair", it.id) },
                    onDelete = { askDelete("repair", it.id, it.title) })
                "Upgrades" -> UpgradesPage(snapshot,
                    onAdd = { edit("upgrade") }, onEdit = { edit("upgrade", it.id) },
                    onDelete = { askDelete("upgrade", it.id, it.name) })
                "Notes" -> NotesPage(snapshot,
                    onAdd = { edit("note") }, onEdit = { edit("note", it.id) },
                    onDelete = { askDelete("garage_note", it.id, it.title) })
                "History" -> HistoryPage(snapshot, onGo = { route = it },
                    onExport = { exportPicker.launch("ZEUS-Expenses.csv") })
                else -> MorePage { route = it }
            }
            if (busy && snapshot.vehicles.isEmpty()) CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center), color = Ruby)
            else if (busy) LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter), color = Ruby)
        }
    }

    // BoreLab-style right-side launcher; dismiss by tapping outside, Close, Back, or choosing a page.
    AnimatedVisibility(
        visible = launcherOpen,
        modifier = Modifier.matchParentSize(),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(Modifier.fillMaxSize()
            .background(Color.Black.copy(alpha = 0.26f))
            .clickable { launcherOpen = false })
    }
    AnimatedVisibility(
        visible = launcherOpen,
        modifier = Modifier.align(Alignment.CenterEnd)
            .fillMaxWidth(0.94f).fillMaxHeight()
            .statusBarsPadding().navigationBarsPadding(),
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it })
    ) {
        ZeusLauncherPanel(
            current = route,
            activeTruck = snapshot.active?.nickname ?: "My garage",
            onNavigate = ::navigate,
            onClose = { launcherOpen = false },
            onQuickAdd = { launcherOpen = false; quickAdd = true }
        )
    }
    }

    if (quickAdd) {
        AlertDialog(
            onDismissRequest = { quickAdd = false },
            title = { Text("Quick add") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf(
                        "fuel" to "Fuel-up", "service" to "Completed service",
                        "repair" to "Repair", "code" to "DTC",
                        "symptom" to "Symptom",
                        "upgrade" to "Upgrade", "note" to "Photo / note",
                        "task" to "Maintenance schedule"
                    ).forEach { (kind, name) ->
                        OutlinedButton(onClick = { quickAdd = false; edit(kind) },
                            modifier = Modifier.fillMaxWidth()) { Text(name) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { quickAdd = false }) { Text("Close") } }
        )
    }

    if (editing != null) {
        val request = editing!!
        val vid = snapshot.active?.id ?: 1L
        key(request) {
            EntryEditor(request, snapshot, pendingPhotoUri != null,
                onPhoto = { photoPicker.launch(arrayOf("image/*")) },
                onDismiss = { editing = null; pendingPhotoUri = null },
                onSubmit = { values ->
                    fun v(name: String) = values[name].orEmpty().trim()
                    fun intOrNull(name: String) = v(name).toIntOrNull()
                    fun cents(name: String) = moneyInputToCents(v(name)) ?: 0L
                    val photo = pendingPhotoUri
                    modify(action = {
                        when (request.kind) {
                            "vehicle" -> saveVehicle(request.id, v("nickname"), v("year"), v("make"),
                                v("model"), v("engine"), intOrNull("odometer"), v("notes"))
                            "task" -> saveTask(vid, request.id, v("title"), intOrNull("interval") ?: 0,
                                intOrNull("last"), v("notes"))
                            "service" -> saveService(vid, request.id, v("taskId").toLongOrNull(),
                                v("title"), v("date"), intOrNull("odometer"), cents("cost"), v("notes"))
                            "fuel" -> saveFuel(vid, request.id, v("date"), intOrNull("odometer")!!,
                                v("gallons").toDouble(), cents("cost"), v("full") == "yes")
                            "repair" -> saveRepair(vid, request.id, v("date"), v("title"), v("part"),
                                intOrNull("odometer"), cents("cost"), v("notes"))
                            "upgrade" -> saveUpgrade(vid, request.id, v("name"), v("status"), cents("cost"), v("notes"))
                            "note" -> {
                                val oldPath = snapshot.notes.firstOrNull { it.id == request.id }?.photoPath
                                val newPath = photo?.let { importPhoto(it) } ?: oldPath
                                saveNote(vid, request.id, v("date"), v("title"), v("body"), newPath)
                            }
                            "code" -> saveCode(vid, request.id, v("date"), v("code").uppercase(),
                                v("status"), v("symptoms"), v("notes"))
                            "symptom" -> saveSymptom(vid, request.id, v("date"), v("title"),
                                v("severity"), v("occurrence"), v("notes"), v("status"))
                            else -> error("Unknown editor type")
                        }
                    }, onSuccess = {
                        editing = null
                        pendingPhotoUri = null
                        notify("Saved to ZEUS Garage")
                    })
                })
        }
    }

    if (pendingDelete != null) {
        val item = pendingDelete!!
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete this record?") },
            text = { Text("Delete '${item.label}' from ZEUS Garage? This cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    val vid = snapshot.active?.id ?: 0L
                    modify(action = {
                        if (item.table == "vehicle") deleteVehicle(item.id)
                        else deleteRecord(item.table, item.id, vid)
                    }, onSuccess = { pendingDelete = null; notify("Record deleted") })
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } }
        )
    }

    if (selectedDefinition != null) {
        val def = selectedDefinition!!
        AlertDialog(onDismissRequest = { selectedDefinition = null },
            title = { Text("${def.code} • ${def.title}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(def.summary)
                    SmallCaps("Things to verify")
                    Text(def.checks)
                    Muted("General informational reference, not a live diagnosis. For vehicle-specific repair procedures, consult the OEM service manual.")
                }
            },
            confirmButton = { TextButton(onClick = { selectedDefinition = null }) { Text("Close") } },
            dismissButton = { TextButton(onClick = { selectedDefinition = null; edit("code", null) }) {
                Text("Record this code")
            } }
        )
    }
}
