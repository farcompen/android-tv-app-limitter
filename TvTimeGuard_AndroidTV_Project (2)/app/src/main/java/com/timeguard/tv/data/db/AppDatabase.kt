package com.timeguard.tv.data.db

import android.content.Context
import androidx.room.*

@Entity(tableName = "managed_apps")
data class ManagedAppEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val enabled: Boolean = false,
    val dailyLimitMinutes: Int = 120,
    val isChildAllowed: Boolean = true
)

@Entity(
    tableName = "daily_usage",
    primaryKeys = ["packageName", "date"]
)
data class DailyUsageEntity(
    val packageName: String,
    val date: String,
    val usedSeconds: Int
)

@Dao
interface AppDao {

    @Query("SELECT * FROM managed_apps")
    suspend fun getAllApps(): List<ManagedAppEntity>

    @Query("SELECT * FROM managed_apps WHERE packageName = :pkg LIMIT 1")
    suspend fun getAppByPackage(pkg: String): ManagedAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateApp(app: ManagedAppEntity)

    @Query("SELECT * FROM daily_usage WHERE packageName = :pkg AND date = :date LIMIT 1")
    suspend fun getDailyUsage(
        pkg: String,
        date: String
    ): DailyUsageEntity?

    @Query("""
        INSERT OR REPLACE INTO daily_usage
        (packageName, date, usedSeconds)
        VALUES (:pkg, :date, :used)
    """)
    suspend fun insertOrUpdateUsage(
        pkg: String,
        date: String,
        used: Int
    )

    @Query("SELECT * FROM daily_usage WHERE date >= :startDate")
    suspend fun getUsageHistory(
        startDate: String
    ): List<DailyUsageEntity>
}

@Database(
    entities = [
        ManagedAppEntity::class,
        DailyUsageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appDao(): AppDao

    companion object {

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {

            return instance ?: synchronized(this) {

                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tv_timeguard_local.db"
                )
                    .build()
                    .also {
                        instance = it
                    }
            }
        }
    }
}
