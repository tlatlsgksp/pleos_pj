package com.example.pleos.data.local

import androidx.room.RoomDatabase

/**
 * Local source of truth for records (fuel, maintenance, expenses).
 *
 * Placeholder: the data model is not designed yet, so there are no entities and no `@Database`
 * annotation. Add entities, DAOs, `@Database(entities = [...], version = 1)` and a Hilt provider
 * together once the schema is decided.
 */
abstract class AppDatabase : RoomDatabase()
