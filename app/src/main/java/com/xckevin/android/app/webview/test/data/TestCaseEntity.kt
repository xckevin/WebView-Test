package com.xckevin.android.app.webview.test.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "test_cases")
data class TestCaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val url: String,
    val note: String,
    val configJson: String,
    val createdAt: Long,
    val updatedAt: Long,
    val lastOpenedAt: Long?,
)
