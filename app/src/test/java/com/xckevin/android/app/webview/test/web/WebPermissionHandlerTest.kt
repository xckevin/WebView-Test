package com.xckevin.android.app.webview.test.web

import android.Manifest
import android.net.Uri
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import com.xckevin.android.app.webview.test.model.WebPermissionPolicy
import com.xckevin.android.app.webview.test.model.WebTestConfig
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WebPermissionHandlerTest {
    @Test fun grantsAllowedResourceSubsetWhenAnotherRequestedResourceIsDeniedByPolicy() {
        val request = RecordingPermissionRequest(
            PermissionRequest.RESOURCE_VIDEO_CAPTURE,
            PermissionRequest.RESOURCE_AUDIO_CAPTURE,
        )
        val handler = WebPermissionHandler(
            configProvider = {
                WebTestConfig.default().copy(
                    cameraPolicy = WebPermissionPolicy.DENY,
                    microphonePolicy = WebPermissionPolicy.ALLOW,
                )
            },
            requestRuntimePermissions = { permissions, onResult ->
                assertArrayEquals(arrayOf(Manifest.permission.RECORD_AUDIO), permissions)
                onResult(permissions.associateWith { true })
            },
            showPrompt = { prompt ->
                if (prompt != null) error("Prompt should not be shown for ALLOW-only resources")
            },
        )

        handler.onPermissionRequest(request)

        assertArrayEquals(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE), request.grantedResources)
        assertFalse(request.denied)
    }

    @Test fun partialRuntimeGrantOnlyGrantsMatchingResource() {
        val request = RecordingPermissionRequest(
            PermissionRequest.RESOURCE_VIDEO_CAPTURE,
            PermissionRequest.RESOURCE_AUDIO_CAPTURE,
        )
        val handler = WebPermissionHandler(
            configProvider = {
                WebTestConfig.default().copy(
                    cameraPolicy = WebPermissionPolicy.ALLOW,
                    microphonePolicy = WebPermissionPolicy.ALLOW,
                )
            },
            requestRuntimePermissions = { permissions, onResult ->
                assertArrayEquals(
                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
                    permissions,
                )
                onResult(
                    mapOf(
                        Manifest.permission.CAMERA to false,
                        Manifest.permission.RECORD_AUDIO to true,
                    )
                )
            },
            showPrompt = { prompt ->
                if (prompt != null) error("Prompt should not be shown for ALLOW-only resources")
            },
        )

        handler.onPermissionRequest(request)

        assertArrayEquals(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE), request.grantedResources)
        assertFalse(request.denied)
    }

    @Test fun canceledMediaPermissionClearsPromptAndIgnoresStalePromptAction() {
        val request = RecordingPermissionRequest(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
        var prompt: WebPermissionPrompt? = null
        val handler = WebPermissionHandler(
            configProvider = {
                WebTestConfig.default().copy(cameraPolicy = WebPermissionPolicy.ASK_EVERY_TIME)
            },
            requestRuntimePermissions = { permissions, onResult ->
                onResult(permissions.associateWith { true })
            },
            showPrompt = { prompt = it },
        )

        handler.onPermissionRequest(request)
        val stalePrompt = prompt
        handler.onPermissionRequestCanceled(request)
        stalePrompt?.onAllow?.invoke()

        assertNull(prompt)
        assertNull(request.grantedResources)
        assertFalse(request.denied)
    }

    @Test fun geolocationHidePromptDeniesCallbackOnce() {
        val callback = RecordingGeolocationCallback()
        var prompt: WebPermissionPrompt? = null
        val handler = WebPermissionHandler(
            configProvider = {
                WebTestConfig.default().copy(geolocationPolicy = WebPermissionPolicy.ASK_EVERY_TIME)
            },
            requestRuntimePermissions = { permissions, onResult ->
                onResult(permissions.associateWith { true })
            },
            showPrompt = { prompt = it },
        )

        handler.onGeolocationPermissionsShowPrompt("https://example.com", callback)
        val stalePrompt = prompt
        handler.onGeolocationPermissionsHidePrompt()
        stalePrompt?.onAllow?.invoke()

        assertNull(prompt)
        assertEquals(listOf(false), callback.allows)
    }

    @Test fun geolocationPromptDeniesPendingMediaPromptBeforeReplacingUi() {
        val request = RecordingPermissionRequest(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
        val callback = RecordingGeolocationCallback()
        val prompts = mutableListOf<WebPermissionPrompt?>()
        val handler = WebPermissionHandler(
            configProvider = {
                WebTestConfig.default().copy(
                    cameraPolicy = WebPermissionPolicy.ASK_EVERY_TIME,
                    geolocationPolicy = WebPermissionPolicy.ASK_EVERY_TIME,
                )
            },
            requestRuntimePermissions = { permissions, onResult ->
                onResult(permissions.associateWith { true })
            },
            showPrompt = prompts::add,
        )

        handler.onPermissionRequest(request)
        val staleMediaPrompt = prompts.last()
        handler.onGeolocationPermissionsShowPrompt("https://example.com", callback)
        staleMediaPrompt?.onAllow?.invoke()

        assertTrue(request.denied)
        assertNull(request.grantedResources)
        assertEquals(emptyList<Boolean>(), callback.allows)
        assertEquals(3, prompts.size)
        assertNull(prompts[1])
    }

    @Test fun mediaPromptDeniesPendingGeolocationPromptBeforeReplacingUi() {
        val request = RecordingPermissionRequest(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
        val callback = RecordingGeolocationCallback()
        val prompts = mutableListOf<WebPermissionPrompt?>()
        val handler = WebPermissionHandler(
            configProvider = {
                WebTestConfig.default().copy(
                    microphonePolicy = WebPermissionPolicy.ASK_EVERY_TIME,
                    geolocationPolicy = WebPermissionPolicy.ASK_EVERY_TIME,
                )
            },
            requestRuntimePermissions = { permissions, onResult ->
                onResult(permissions.associateWith { true })
            },
            showPrompt = prompts::add,
        )

        handler.onGeolocationPermissionsShowPrompt("https://example.com", callback)
        val staleGeolocationPrompt = prompts.last()
        handler.onPermissionRequest(request)
        staleGeolocationPrompt?.onAllow?.invoke()

        assertEquals(listOf(false), callback.allows)
        assertNull(request.grantedResources)
        assertFalse(request.denied)
        assertEquals(3, prompts.size)
        assertNull(prompts[1])
    }
}

private class RecordingPermissionRequest(
    private vararg val requestedResources: String,
) : PermissionRequest() {
    var grantedResources: Array<String>? = null
        private set
    var denied: Boolean = false
        private set

    override fun getOrigin(): Uri? = null

    override fun getResources(): Array<String> = requestedResources.toList().toTypedArray()

    override fun grant(resources: Array<String>?) {
        grantedResources = resources
    }

    override fun deny() {
        denied = true
    }
}

private class RecordingGeolocationCallback : GeolocationPermissions.Callback {
    val allows = mutableListOf<Boolean>()

    override fun invoke(origin: String?, allow: Boolean, retain: Boolean) {
        allows.add(allow)
    }
}
