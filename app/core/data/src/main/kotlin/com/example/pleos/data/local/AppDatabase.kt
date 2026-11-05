package com.example.pleos.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.pleos.data.local.dao.CareDao
import com.example.pleos.data.local.dao.DriveSessionDao
import com.example.pleos.data.local.dao.RecordDao
import com.example.pleos.data.local.dao.VehicleDao
import com.example.pleos.data.local.entity.CareItemStateEntity
import com.example.pleos.data.local.entity.DriveSampleEntity
import com.example.pleos.data.local.entity.DriveSessionEntity
import com.example.pleos.data.local.entity.PendingVisitEntity
import com.example.pleos.data.local.entity.RecordEntity
import com.example.pleos.data.local.entity.RentalPeriodEntity
import com.example.pleos.data.local.entity.VehicleEntity

/**
 * Local source of truth (ARCHITECTURE 2.4). Version 1 already holds every table in
 * FEATURE_LOGIC 4, including the ones no screen uses yet, so the first release needs no migration.
 * Schemas are exported to core/data/schemas for future migrations.
 */
@Database(
    entities = [
        VehicleEntity::class,
        RecordEntity::class,
        DriveSessionEntity::class,
        DriveSampleEntity::class,
        CareItemStateEntity::class,
        RentalPeriodEntity::class,
        PendingVisitEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
internal abstract class AppDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun recordDao(): RecordDao
    abstract fun driveSessionDao(): DriveSessionDao
    abstract fun careDao(): CareDao

    companion object {
        const val NAME = "pleos.db"
    }
}
