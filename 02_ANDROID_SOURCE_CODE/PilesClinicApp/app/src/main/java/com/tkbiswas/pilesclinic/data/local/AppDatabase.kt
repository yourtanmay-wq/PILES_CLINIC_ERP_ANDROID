package com.tkbiswas.pilesclinic.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        EnquiryEntity::class,
        RegistrationEntity::class,
        FollowUpEntity::class,
        PaymentEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun enquiryDao(): EnquiryDao
    abstract fun registrationDao(): RegistrationDao
    abstract fun followUpDao(): FollowUpDao
    abstract fun paymentDao(): PaymentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "piles_clinic.db"
                )
                    // Never lose local data silently: a destructive fallback is
                    // NOT used. If a future schema change needs a migration,
                    // add a Migration object here instead of wiping data.
                    .build()
                    .also { INSTANCE = it }
            }

        /** Phase 9: needed before overwriting the .db file during a Restore. */
        fun closeInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }
}
