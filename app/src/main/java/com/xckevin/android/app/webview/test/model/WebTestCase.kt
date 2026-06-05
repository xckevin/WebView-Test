package com.xckevin.android.app.webview.test.model

data class WebTestCase(
    val id: Long,
    val name: String,
    val url: String,
    val note: String,
    val config: WebTestConfig,
    val createdAt: Long,
    val updatedAt: Long,
    val lastOpenedAt: Long?,
)
