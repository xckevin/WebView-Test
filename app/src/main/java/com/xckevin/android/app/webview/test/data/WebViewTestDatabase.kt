package com.xckevin.android.app.webview.test.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.xckevin.android.app.webview.test.model.SourceType

@Database(
    entities = [HistoryEntryEntity::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(SourceTypeConverter::class)
abstract class WebViewTestDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
}

val MIGRATION_1_2_DROP_TEST_CASES = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS test_cases")
    }
}

class SourceTypeConverter {
    @TypeConverter fun fromSourceType(value: SourceType): String = value.name
    @TypeConverter fun toSourceType(value: String): SourceType = SourceType.valueOf(value)
}
