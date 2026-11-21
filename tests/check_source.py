#!/usr/bin/env python3
"""Zero-dependency source package checks; Android build still requires Gradle/SDK."""
from pathlib import Path
from xml.etree import ElementTree as ET
import re
import sqlite3
import zipfile

root = Path(__file__).resolve().parents[1]
for name in ['settings.gradle.kts', 'build.gradle.kts', 'app/build.gradle.kts',
             'app/src/main/AndroidManifest.xml', 'app/src/main/res/values/styles.xml',
             'app/src/main/res/drawable/zeus_icon.xml']:
    assert (root / name).exists(), name
for file in (root / 'app/src/main/res').rglob('*.xml'):
    ET.parse(file)
store = (root / 'app/src/main/java/com/zeusgarage/mobile/ZeusStore.kt').read_text()
statements = re.findall(r'db\.execSQL\("""(CREATE TABLE .*?)"""\)', store, re.S)
assert len(statements) == 8, f'expected 8 SQLite tables, got {len(statements)}'
db = sqlite3.connect(':memory:')
db.execute('PRAGMA foreign_keys=ON')
for sql in statements: db.execute(sql)
required = {'vehicle', 'service_task', 'service_event', 'fuel', 'repair', 'upgrade', 'garage_note', 'dtc'}
assert {row[0] for row in db.execute("SELECT name FROM sqlite_master WHERE type='table'")} >= required
id = db.execute("INSERT INTO vehicle (nickname,year,make,model,engine) VALUES (?,?,?,?,?)",
                ('ZEUS','2019','Ford','F-250','6.7L')).lastrowid
db.execute("INSERT INTO fuel (vehicle_id,date,odometer,gallons,total_cents) VALUES (?,?,?,?,?)",
           (id,'2026-01-01',10000,20.0,7500))
assert db.execute('SELECT total_cents FROM fuel').fetchone()[0] == 7500
db.execute('DELETE FROM vehicle WHERE id=?', (id,))
assert db.execute('SELECT COUNT(*) FROM fuel').fetchone()[0] == 0, 'cascade delete failed'
print('ZEUS source checks PASSED: XML, SQLite tables, foreign keys')
