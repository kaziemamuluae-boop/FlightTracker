package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FavoriteFlightEntity::class,
        FavoriteAirportEntity::class,
        FavoriteAirlineEntity::class,
        FavoriteAircraftEntity::class,
        SearchHistoryEntity::class,
        CachedAirportEntity::class,
        CachedAirlineEntity::class,
        FlightAlertEntity::class,
        PositionSnapshotEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoritesDao(): FavoritesDao
    abstract fun historyDao(): HistoryDao
    abstract fun cacheDao(): CacheDao
    abstract fun alertsAndPlaybackDao(): AlertsAndPlaybackDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "flight_tracker.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build().also {
                    INSTANCE = it
                }
            }
        }
    }
}
