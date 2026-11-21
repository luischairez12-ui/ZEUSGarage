package com.zeusgarage.mobile

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import java.io.File
import java.util.UUID

/** All data stays in this app's private SQLite database and internal photo folder. */
class ZeusStore(private val context: Context) : SQLiteOpenHelper(context, "zeus_garage.db", null, 1) {
    override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE vehicle (
            id INTEGER PRIMARY KEY AUTOINCREMENT, nickname TEXT NOT NULL, year TEXT NOT NULL,
            make TEXT NOT NULL, model TEXT NOT NULL, engine TEXT NOT NULL,
            odometer INTEGER, notes TEXT NOT NULL DEFAULT ''
        )""")
        db.execSQL("""CREATE TABLE service_task (
            id INTEGER PRIMARY KEY AUTOINCREMENT, vehicle_id INTEGER NOT NULL REFERENCES vehicle(id) ON DELETE CASCADE,
            title TEXT NOT NULL, interval_miles INTEGER NOT NULL DEFAULT 0,
            last_done_miles INTEGER, notes TEXT NOT NULL DEFAULT ''
        )""")
        db.execSQL("""CREATE TABLE service_event (
            id INTEGER PRIMARY KEY AUTOINCREMENT, vehicle_id INTEGER NOT NULL REFERENCES vehicle(id) ON DELETE CASCADE,
            task_id INTEGER REFERENCES service_task(id) ON DELETE SET NULL,
            title TEXT NOT NULL, date TEXT NOT NULL, mileage INTEGER,
            cost_cents INTEGER NOT NULL DEFAULT 0, notes TEXT NOT NULL DEFAULT ''
        )""")
        db.execSQL("""CREATE TABLE fuel (
            id INTEGER PRIMARY KEY AUTOINCREMENT, vehicle_id INTEGER NOT NULL REFERENCES vehicle(id) ON DELETE CASCADE,
            date TEXT NOT NULL, odometer INTEGER NOT NULL, gallons REAL NOT NULL,
            total_cents INTEGER NOT NULL, full_tank INTEGER NOT NULL DEFAULT 1
        )""")
        db.execSQL("""CREATE TABLE repair (
            id INTEGER PRIMARY KEY AUTOINCREMENT, vehicle_id INTEGER NOT NULL REFERENCES vehicle(id) ON DELETE CASCADE,
            date TEXT NOT NULL, title TEXT NOT NULL, part TEXT NOT NULL DEFAULT '', mileage INTEGER,
            cost_cents INTEGER NOT NULL DEFAULT 0, notes TEXT NOT NULL DEFAULT ''
        )""")
        db.execSQL("""CREATE TABLE upgrade (
            id INTEGER PRIMARY KEY AUTOINCREMENT, vehicle_id INTEGER NOT NULL REFERENCES vehicle(id) ON DELETE CASCADE,
            name TEXT NOT NULL, status TEXT NOT NULL, cost_cents INTEGER NOT NULL DEFAULT 0,
            notes TEXT NOT NULL DEFAULT ''
        )""")
        db.execSQL("""CREATE TABLE garage_note (
            id INTEGER PRIMARY KEY AUTOINCREMENT, vehicle_id INTEGER NOT NULL REFERENCES vehicle(id) ON DELETE CASCADE,
            date TEXT NOT NULL, title TEXT NOT NULL, body TEXT NOT NULL DEFAULT '', photo_path TEXT
        )""")
        db.execSQL("""CREATE TABLE dtc (
            id INTEGER PRIMARY KEY AUTOINCREMENT, vehicle_id INTEGER NOT NULL REFERENCES vehicle(id) ON DELETE CASCADE,
            date TEXT NOT NULL, code TEXT NOT NULL, status TEXT NOT NULL,
            symptoms TEXT NOT NULL DEFAULT '', notes TEXT NOT NULL DEFAULT ''
        )""")
        // Only known descriptive truck facts are seeded. No fabricated odometer, fuel level or codes.
        val truck = ContentValues().apply {
            put("nickname", "ZEUS"); put("year", "2019"); put("make", "Ford")
            put("model", "F-250 Super Duty"); put("engine", "6.7L Power Stroke")
            put("notes", "Ruby Red • 6-inch lift • 37-inch tires")
        }
        val id = db.insertOrThrow("vehicle", null, truck)
        for (task in listOf("Oil & oil filter", "Fuel filters", "Transmission service",
            "Tire rotation", "Brake inspection", "Coolant check")) {
            db.insertOrThrow("service_task", null, ContentValues().apply {
                put("vehicle_id", id); put("title", task); put("interval_miles", 0)
            })
        }
        for (item in listOf("Raptor-style steering wheel", "Larger touchscreen radio")) {
            db.insertOrThrow("upgrade", null, ContentValues().apply {
                put("vehicle_id", id); put("name", item); put("status", "Planned")
            })
        }
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Future migrations must preserve all user-created records.
        if (oldVersion != newVersion) error("Database migration required")
    }

    private fun Cursor.str(name: String) = getString(getColumnIndexOrThrow(name))
    private fun Cursor.num(name: String) = getLong(getColumnIndexOrThrow(name))
    private fun Cursor.int(name: String) = getInt(getColumnIndexOrThrow(name))
    private fun Cursor.optionalInt(name: String): Int? = getColumnIndexOrThrow(name).let {
        if (isNull(it)) null else getInt(it)
    }
    private fun Cursor.optionalLong(name: String): Long? = getColumnIndexOrThrow(name).let {
        if (isNull(it)) null else getLong(it)
    }
    private fun Cursor.optionalString(name: String): String? = getColumnIndexOrThrow(name).let {
        if (isNull(it)) null else getString(it)
    }
    private inline fun <T> rows(sql: String, args: Array<String> = emptyArray(), map: Cursor.() -> T): List<T> =
        readableDatabase.rawQuery(sql, args).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.map()) }
        }

    fun vehicles(): List<Vehicle> = rows("SELECT * FROM vehicle ORDER BY id") {
        Vehicle(num("id"), str("nickname"), str("year"), str("make"), str("model"),
            str("engine"), optionalInt("odometer"), str("notes"))
    }

    private fun chosenId(): Long = context.getSharedPreferences("zeus_prefs", Context.MODE_PRIVATE)
        .getLong("active_vehicle", 0L)

    fun selectVehicle(id: Long) {
        require(vehicles().any { it.id == id }) { "Unknown vehicle" }
        context.getSharedPreferences("zeus_prefs", Context.MODE_PRIVATE).edit().putLong("active_vehicle", id).apply()
    }

    fun snapshot(): GarageSnapshot {
        val cars = vehicles()
        val id = chosenId().takeIf { it != 0L && cars.any { car -> car.id == it } }
            ?: cars.firstOrNull()?.id ?: return GarageSnapshot()
        val args = arrayOf(id.toString())
        return GarageSnapshot(
            vehicles = cars, activeId = id,
            tasks = rows("SELECT * FROM service_task WHERE vehicle_id=? ORDER BY id", args) {
                ServiceTask(num("id"), num("vehicle_id"), str("title"), int("interval_miles"),
                    optionalInt("last_done_miles"), str("notes"))
            },
            services = rows("SELECT * FROM service_event WHERE vehicle_id=? ORDER BY date DESC,id DESC", args) {
                ServiceEvent(num("id"), num("vehicle_id"), optionalLong("task_id"), str("title"),
                    str("date"), optionalInt("mileage"), num("cost_cents"), str("notes"))
            },
            fuel = rows("SELECT * FROM fuel WHERE vehicle_id=? ORDER BY date DESC,id DESC", args) {
                FuelEntry(num("id"), num("vehicle_id"), str("date"), int("odometer"),
                    getDouble(getColumnIndexOrThrow("gallons")), num("total_cents"), int("full_tank") == 1)
            },
            repairs = rows("SELECT * FROM repair WHERE vehicle_id=? ORDER BY date DESC,id DESC", args) {
                RepairEntry(num("id"), num("vehicle_id"), str("date"), str("title"), str("part"),
                    optionalInt("mileage"), num("cost_cents"), str("notes"))
            },
            upgrades = rows("SELECT * FROM upgrade WHERE vehicle_id=? ORDER BY id DESC", args) {
                UpgradeEntry(num("id"), num("vehicle_id"), str("name"), str("status"),
                    num("cost_cents"), str("notes"))
            },
            notes = rows("SELECT * FROM garage_note WHERE vehicle_id=? ORDER BY date DESC,id DESC", args) {
                NoteEntry(num("id"), num("vehicle_id"), str("date"), str("title"), str("body"), optionalString("photo_path"))
            },
            codes = rows("SELECT * FROM dtc WHERE vehicle_id=? ORDER BY date DESC,id DESC", args) {
                DtcEntry(num("id"), num("vehicle_id"), str("date"), str("code"), str("status"),
                    str("symptoms"), str("notes"))
            }
        )
    }

    fun saveVehicle(id: Long?, nickname: String, year: String, make: String, model: String,
                    engine: String, odometer: Int?, notes: String): Long {
        require(nickname.isNotBlank() && year.isNotBlank() && make.isNotBlank() && model.isNotBlank())
        require(odometer == null || odometer >= 0)
        val values = ContentValues().apply {
            put("nickname", nickname.trim()); put("year", year.trim()); put("make", make.trim())
            put("model", model.trim()); put("engine", engine.trim()); put("notes", notes.trim())
            if (odometer == null) putNull("odometer") else put("odometer", odometer)
        }
        val db = writableDatabase
        if (id == null) return db.insertOrThrow("vehicle", null, values)
        check(db.update("vehicle", values, "id=?", arrayOf(id.toString())) > 0) { "Vehicle missing" }
        return id
    }

    private fun deletePrivatePhoto(path: String?) {
        if (path == null) return
        val folder = File(context.filesDir, "garage_photos").canonicalFile
        val file = File(path).canonicalFile
        if (file.parentFile == folder) file.delete()
    }
    fun deleteVehicle(id: Long) {
        require(vehicles().size > 1) { "Keep at least one vehicle in your garage" }
        val photos = rows("SELECT photo_path FROM garage_note WHERE vehicle_id=?", arrayOf(id.toString())) {
            optionalString("photo_path")
        }
        writableDatabase.delete("vehicle", "id=?", arrayOf(id.toString()))
        photos.forEach(::deletePrivatePhoto)
        if (chosenId() == id) selectVehicle(vehicles().first().id)
    }

    private fun updateOdometerIfHigher(db: SQLiteDatabase, vehicleId: Long, miles: Int?) {
        if (miles == null) return
        db.execSQL("UPDATE vehicle SET odometer=? WHERE id=? AND (odometer IS NULL OR odometer < ?)",
            arrayOf(miles, vehicleId, miles))
    }

    fun saveTask(vehicleId: Long, id: Long?, title: String, interval: Int,
                 lastDone: Int?, notes: String) {
        require(title.isNotBlank() && interval in 0..1_000_000 && (lastDone == null || lastDone >= 0))
        val values = ContentValues().apply {
            put("vehicle_id", vehicleId); put("title", title.trim()); put("interval_miles", interval)
            if (lastDone == null) putNull("last_done_miles") else put("last_done_miles", lastDone)
            put("notes", notes.trim())
        }
        save("service_task", vehicleId, id, values)
    }
    fun saveService(vehicleId: Long, id: Long?, taskId: Long?, title: String, date: String,
                    mileage: Int?, costCents: Long, notes: String) {
        require(title.isNotBlank() && costCents >= 0 && (mileage == null || mileage >= 0))
        val values = ContentValues().apply {
            put("vehicle_id", vehicleId); if (taskId == null) putNull("task_id") else put("task_id", taskId)
            put("title", title.trim()); put("date", date); if (mileage == null) putNull("mileage") else put("mileage", mileage)
            put("cost_cents", costCents); put("notes", notes.trim())
        }
        val db = writableDatabase
        db.beginTransaction()
        try {
            saveWithDb(db, "service_event", vehicleId, id, values)
            updateOdometerIfHigher(db, vehicleId, mileage)
            if (taskId != null && mileage != null) {
                val old = db.rawQuery("SELECT last_done_miles FROM service_task WHERE id=? AND vehicle_id=?",
                    arrayOf(taskId.toString(), vehicleId.toString())).use {
                    if (it.moveToFirst() && !it.isNull(0)) it.getInt(0) else null
                }
                // Don't roll an existing maintenance baseline backwards.
                if (old == null || mileage > old) {
                    db.update("service_task", ContentValues().apply { put("last_done_miles", mileage) },
                        "id=? AND vehicle_id=?", arrayOf(taskId.toString(), vehicleId.toString()))
                }
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }
    fun saveFuel(vehicleId: Long, id: Long?, date: String, mileage: Int, gallons: Double,
                 totalCents: Long, fullTank: Boolean) {
        require(mileage >= 0 && gallons.isFinite() && gallons > 0 && gallons <= 1000 && totalCents >= 0)
        save("fuel", vehicleId, id, ContentValues().apply {
            put("vehicle_id", vehicleId); put("date", date); put("odometer", mileage)
            put("gallons", gallons); put("total_cents", totalCents); put("full_tank", if (fullTank) 1 else 0)
        })
        updateOdometerIfHigher(writableDatabase, vehicleId, mileage)
    }
    fun saveRepair(vehicleId: Long, id: Long?, date: String, title: String, part: String,
                   mileage: Int?, costCents: Long, notes: String) {
        require(title.isNotBlank() && costCents >= 0 && (mileage == null || mileage >= 0))
        save("repair", vehicleId, id, ContentValues().apply {
            put("vehicle_id", vehicleId); put("date", date); put("title", title.trim()); put("part", part.trim())
            if (mileage == null) putNull("mileage") else put("mileage", mileage)
            put("cost_cents", costCents); put("notes", notes.trim())
        })
        updateOdometerIfHigher(writableDatabase, vehicleId, mileage)
    }
    fun saveUpgrade(vehicleId: Long, id: Long?, name: String, status: String,
                    costCents: Long, notes: String) {
        require(name.isNotBlank() && status in listOf("Planned", "Installed") && costCents >= 0)
        save("upgrade", vehicleId, id, ContentValues().apply {
            put("vehicle_id", vehicleId); put("name", name.trim()); put("status", status)
            put("cost_cents", costCents); put("notes", notes.trim())
        })
    }
    fun saveNote(vehicleId: Long, id: Long?, date: String, title: String, body: String,
                 photoPath: String?) {
        require(title.isNotBlank())
        val oldPhoto = id?.let { key ->
            rows("SELECT photo_path FROM garage_note WHERE id=? AND vehicle_id=?",
                arrayOf(key.toString(), vehicleId.toString())) { optionalString("photo_path") }.firstOrNull()
        }
        save("garage_note", vehicleId, id, ContentValues().apply {
            put("vehicle_id", vehicleId); put("date", date); put("title", title.trim()); put("body", body.trim())
            if (photoPath == null) putNull("photo_path") else put("photo_path", photoPath)
        })
        if (oldPhoto != photoPath) deletePrivatePhoto(oldPhoto)
    }
    fun saveCode(vehicleId: Long, id: Long?, date: String, code: String, status: String,
                 symptoms: String, notes: String) {
        require(code.matches(Regex("[A-Z][0-9A-Z]{4,6}")) && status in listOf("Observed", "Resolved"))
        save("dtc", vehicleId, id, ContentValues().apply {
            put("vehicle_id", vehicleId); put("date", date); put("code", code)
            put("status", status); put("symptoms", symptoms.trim()); put("notes", notes.trim())
        })
    }
    private fun save(table: String, vehicleId: Long, id: Long?, values: ContentValues) {
        saveWithDb(writableDatabase, table, vehicleId, id, values)
    }
    private fun saveWithDb(db: SQLiteDatabase, table: String, vehicleId: Long, id: Long?, values: ContentValues) {
        if (id == null) db.insertOrThrow(table, null, values)
        else check(db.update(table, values, "id=? AND vehicle_id=?", arrayOf(id.toString(), vehicleId.toString())) > 0) {
            "Record missing or belongs to another vehicle"
        }
    }
    fun deleteRecord(table: String, id: Long, vehicleId: Long) {
        require(table in setOf("service_task", "service_event", "fuel", "repair", "upgrade", "garage_note", "dtc"))
        // Only delete precisely the requested record for the current vehicle.
        if (table == "garage_note") {
            val photo = snapshot().notes.firstOrNull { it.id == id && it.vehicleId == vehicleId }?.photoPath
            val deleted = writableDatabase.delete(table, "id=? AND vehicle_id=?", arrayOf(id.toString(), vehicleId.toString()))
            if (deleted > 0) deletePrivatePhoto(photo)
        } else writableDatabase.delete(table, "id=? AND vehicle_id=?", arrayOf(id.toString(), vehicleId.toString()))
    }

    /** User-selected photo is copied privately; no shared-storage permissions or networking. */
    fun importPhoto(uri: Uri): String {
        val type = context.contentResolver.getType(uri)
        require(type?.startsWith("image/") == true) { "Select an image file" }
        val folder = File(context.filesDir, "garage_photos").apply { mkdirs() }
        val dest = File(folder, "${UUID.randomUUID()}.jpg")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { out ->
                    val buf = ByteArray(16384)
                    var total = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        total += n
                        require(total <= 20L * 1024 * 1024) { "Image exceeds 20 MB" }
                        out.write(buf, 0, n)
                    }
                }
            } ?: error("Cannot open the photo")
            require(dest.length() > 100) { "Empty image" }
            // Don't trust arbitrary MIME; verify the bytes can be decoded as an image.
            val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            android.graphics.BitmapFactory.decodeFile(dest.absolutePath, bounds)
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Unrecognized image format" }
            return dest.absolutePath
        } catch (error: Exception) { dest.delete(); throw error }
    }
}
