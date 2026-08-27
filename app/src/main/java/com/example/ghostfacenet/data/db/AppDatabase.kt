package com.example.ghostfacenet.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PerfilEntity::class, PerfilFotoEntity::class, FaceEmbeddingEntity::class],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun faceDao(): FaceDao

    companion object {
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE face_embeddings " +
                        "ADD COLUMN perfil_update_at INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `perfil_fotos` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `perfil_id` INTEGER NOT NULL,
                        `foto_base64` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updateAt` INTEGER NOT NULL,
                        FOREIGN KEY(`perfil_id`) REFERENCES `perfiles`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_perfil_fotos_perfil_id` " +
                        "ON `perfil_fotos` (`perfil_id`)"
                )
                database.execSQL(
                    "ALTER TABLE `face_embeddings` ADD COLUMN `foto_id` INTEGER"
                )
                database.execSQL(
                    "ALTER TABLE `face_embeddings` ADD COLUMN `foto_update_at` INTEGER"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_face_embeddings_foto_id` " +
                        "ON `face_embeddings` (`foto_id`)"
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ghostfacenet.db"
                )
                    .addMigrations(MIGRATION_5_6)
                    .addMigrations(MIGRATION_6_7)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
