package com.mochi.tl

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// ===== DAOs =====

@Dao
interface PromptDao {
    @Query("SELECT * FROM prompts")
    suspend fun getAll(): List<PromptTemplate>

    @Upsert
    suspend fun upsertAll(items: List<PromptTemplate>)

    @Query("DELETE FROM prompts WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM prompts")
    suspend fun clear()
}

@Dao
interface GlossaryDao {
    @Query("SELECT * FROM glossary")
    suspend fun getAll(): List<GlossaryEntry>

    @Upsert
    suspend fun upsertAll(items: List<GlossaryEntry>)

    @Query("DELETE FROM glossary WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM glossary")
    suspend fun clear()
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects")
    suspend fun getAll(): List<TranslationProject>

    @Upsert
    suspend fun upsertAll(items: List<TranslationProject>)

    @Query("DELETE FROM projects WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM projects")
    suspend fun clear()
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY createdAt DESC")
    suspend fun getAll(): List<TranslationRecord>

    @Upsert
    suspend fun upsertAll(items: List<TranslationRecord>)

    @Query("DELETE FROM history WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM history")
    suspend fun clear()
}

@Dao
interface TranslationCacheDao {
    @Query("SELECT * FROM translation_cache WHERE cacheKey = :key LIMIT 1")
    suspend fun get(key: String): TranslationCache?

    @Upsert
    suspend fun upsert(item: TranslationCache)

    @Query("DELETE FROM translation_cache")
    suspend fun clear()
}

// ===== Database =====

/**
 * Database Room MochiTL. Menggantikan penyimpanan koleksi berbasis
 * SharedPreferences JSON (projects, prompts, glossary, history).
 * Pengaturan sederhana (API key, temperature, dll.) tetap di SharedPreferences.
 */
@Database(
    entities = [
        PromptTemplate::class,
        GlossaryEntry::class,
        TranslationProject::class,
        TranslationRecord::class,
        TranslationCache::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(StringListConverter::class)
abstract class MochiTlDatabase : RoomDatabase() {
    abstract fun promptDao(): PromptDao
    abstract fun glossaryDao(): GlossaryDao
    abstract fun projectDao(): ProjectDao
    abstract fun historyDao(): HistoryDao
    abstract fun translationCacheDao(): TranslationCacheDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `translation_cache` (" +
                        "`cacheKey` TEXT PRIMARY KEY NOT NULL, " +
                        "`translatedText` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
            }
        }
    }
}
