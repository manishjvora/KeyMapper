package io.github.sds100.keymapper.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.sds100.keymapper.KeyMap
import io.github.sds100.keymapper.profile.Profile
import io.github.sds100.keymapper.typeconverter.ActionTypeTypeConverter
import io.github.sds100.keymapper.typeconverter.ExtraListTypeConverter
import io.github.sds100.keymapper.typeconverter.TriggerListTypeConverter

/**
 * Created by sds100 on 05/09/2018.
 */

@Database(version = 2, entities = [KeyMap::class, Profile::class], exportSchema = true)
@TypeConverters(
        TriggerListTypeConverter::class,
        ActionTypeTypeConverter::class,
        ExtraListTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        private const val DATABASE_NAME = "key_map_database"

        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val oldTableName = "${KeyMapDao.TABLE_NAME}_old"

                //rename the old table then create a new table with the foreign keys and transfer the old data
                database.execSQL(
                        "ALTER TABLE ${KeyMapDao.TABLE_NAME} RENAME TO $oldTableName"
                )

                database.execSQL(
                        "CREATE TABLE IF NOT EXISTS ${KeyMapDao.TABLE_NAME} (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `trigger_list` TEXT NOT NULL, `flags` INTEGER NOT NULL, `is_enabled` INTEGER NOT NULL, `profile_id` INTEGER, `action_type` TEXT, `action_data` TEXT, `action_extras` TEXT, FOREIGN KEY(`profile_id`) REFERENCES `profiles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )

                database.execSQL("INSERT INTO ${KeyMapDao.TABLE_NAME} (id, trigger_list, flags, is_enabled, action_type, action_data, action_extras) SELECT * FROM $oldTableName")
                database.execSQL("CREATE  INDEX `index_keymaps_profile_id` ON `${KeyMapDao.TABLE_NAME}` (`profile_id`)")

                //create profile table
                database.execSQL("CREATE TABLE IF NOT EXISTS `${ProfileDao.TABLE_NAME}` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `trigger_type` TEXT NOT NULL, `trigger_extras` TEXT NOT NULL)")
            }
        }

        fun getInstance(ctx: Context): AppDatabase {

            if (INSTANCE == null) {
                //must be application context to prevent memory leaking
                INSTANCE = Room.databaseBuilder(
                        ctx.applicationContext,
                        AppDatabase::class.java,
                        DATABASE_NAME
                ).apply {
                    addMigrations(MIGRATION_1_2)
                }.build()
            }

            return INSTANCE!!
        }
    }

    abstract fun keyMapDao(): KeyMapDao
    abstract fun profileDao(): ProfileDao
}