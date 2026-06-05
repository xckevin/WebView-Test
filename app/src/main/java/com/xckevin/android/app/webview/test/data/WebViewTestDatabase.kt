package com.xckevin.android.app.webview.test.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.xckevin.android.app.webview.test.model.SourceType

@Database(
    entities = [TestCaseEntity::class, HistoryEntryEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(SourceTypeConverter::class)
abstract class WebViewTestDatabase : RoomDatabase() {
    abstract fun testCaseDao(): TestCaseDao
    abstract fun historyDao(): HistoryDao
}

class SourceTypeConverter {
    @TypeConverter fun fromSourceType(value: SourceType): String = value.name
    @TypeConverter fun toSourceType(value: String): SourceType = SourceType.valueOf(value)
}
