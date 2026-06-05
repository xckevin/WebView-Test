package com.xckevin.android.app.webview.test.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TestCaseDao {
    @Query("SELECT * FROM test_cases ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<TestCaseEntity>>

    @Query("SELECT * FROM test_cases WHERE id = :id")
    suspend fun getById(id: Long): TestCaseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TestCaseEntity): Long

    @Delete
    suspend fun delete(entity: TestCaseEntity)
}
