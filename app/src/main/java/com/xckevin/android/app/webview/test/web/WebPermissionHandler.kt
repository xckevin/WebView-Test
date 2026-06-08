package com.xckevin.android.app.webview.test.web

import android.Manifest
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import com.xckevin.android.app.webview.test.model.WebPermissionPolicy
import com.xckevin.android.app.webview.test.model.WebTestConfig

data class WebPermissionPrompt(
    val title: String,
    val message: String,
    val onAllow: () -> Unit,
    val onDeny: () -> Unit,
)

data class WebPermissionTextProvider(
    val webCaptureTitle: String,
    val webCaptureMessage: (String) -> String,
    val geolocationTitle: String,
    val geolocationMessage: String,
    val camera: String,
    val microphone: String,
    val protectedDevicePermissions: String,
    val joinAnd: (String, String) -> String,
) {
    companion object {
        val English = WebPermissionTextProvider(
            webCaptureTitle = "Allow web capture?",
            webCaptureMessage = { permissionLabel -> "This page wants to use $permissionLabel." },
            geolocationTitle = "Allow geolocation?",
            geolocationMessage = "This page wants to access your location.",
            camera = "camera",
            microphone = "microphone",
            protectedDevicePermissions = "protected device permissions",
            joinAnd = { first, second -> "$first and $second" },
        )
    }
}

class WebPermissionHandler(
    private val configProvider: () -> WebTestConfig,
    private val requestRuntimePermissions: (Array<String>, (Map<String, Boolean>) -> Unit) -> Unit,
    private val showPrompt: (WebPermissionPrompt?) -> Unit,
    private val onMessage: (String) -> Unit = {},
    private val onUserFlow: (String, String) -> Unit = { _, _ -> },
    private val textProvider: WebPermissionTextProvider = WebPermissionTextProvider.English,
) {
    private var pendingMediaRequest: PendingRequest? = null
    private var pendingGeolocationRequest: PendingRequest? = null

    fun onPermissionRequest(request: PermissionRequest?) {
        if (request == null) return
        cancelPendingMediaRequest(completeWithDeny = true)
        cancelPendingGeolocationRequest(completeWithDeny = true)

        val requestedResources = request.resources.orEmpty()
        val supportedResources = requestedResources.filter {
            it == PermissionRequest.RESOURCE_VIDEO_CAPTURE ||
                it == PermissionRequest.RESOURCE_AUDIO_CAPTURE
        }

        if (supportedResources.isEmpty()) {
            request.deny()
            onMessage("Web permission denied: unsupported resources")
            onUserFlow("Web permission denied: unsupported resources", requestedResources.joinToString())
            return
        }

        val config = configProvider()
        val allowedResources = supportedResources.filter { resource ->
            resource.policy(config) != WebPermissionPolicy.DENY
        }
        if (allowedResources.isEmpty()) {
            request.deny()
            onMessage("Web permission denied by policy")
            onUserFlow("Web permission denied by policy", supportedResources.permissionLabel())
            return
        }

        val runtimePermissions = allowedResources
            .flatMap { it.runtimePermissions() }
            .distinct()
            .toTypedArray()
        val shouldAskUser = allowedResources.any { resource ->
            resource.policy(config) == WebPermissionPolicy.ASK_EVERY_TIME
        }

        val pendingRequest = PendingRequest(
            matchesPermissionRequest = { candidate -> candidate === request },
            grant = { resources -> request.grant(resources) },
            deny = { request.deny() },
        )
        pendingMediaRequest = pendingRequest
        val grantAfterRuntimePermission = {
            grantMediaAfterRuntimePermission(
                pendingRequest = pendingRequest,
                resources = allowedResources,
                runtimePermissions = runtimePermissions,
            )
        }

        if (shouldAskUser) {
            showPrompt(
                WebPermissionPrompt(
                    title = textProvider.webCaptureTitle,
                    message = textProvider.webCaptureMessage(allowedResources.permissionLabel()),
                    onAllow = grantAfterRuntimePermission,
                    onDeny = {
                        if (pendingMediaRequest === pendingRequest) {
                            pendingMediaRequest = null
                        }
                        pendingRequest.deny()
                        if (pendingRequest.isCompleted) {
                            onMessage("Web permission denied by user")
                            onUserFlow("Web permission denied by user", allowedResources.permissionLabel())
                        }
                    },
                )
            )
        } else {
            grantAfterRuntimePermission()
        }
    }

    fun onPermissionRequestCanceled(request: PermissionRequest?) {
        val pendingRequest = pendingMediaRequest ?: return
        if (!pendingRequest.matchesPermissionRequest(request)) return

        pendingRequest.cancelWithoutCallback()
        pendingMediaRequest = null
        showPrompt(null)
        onMessage("Web permission request canceled")
        onUserFlow("Web permission request canceled", "")
    }

    fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: GeolocationPermissions.Callback?,
    ) {
        if (callback == null) return
        cancelPendingMediaRequest(completeWithDeny = true)
        cancelPendingGeolocationRequest(completeWithDeny = true)

        when (configProvider().geolocationPolicy) {
            WebPermissionPolicy.DENY -> {
                callback.invoke(origin, false, false)
                onMessage("Geolocation denied by policy")
                onUserFlow("Geolocation denied by policy", origin.orEmpty())
            }

            WebPermissionPolicy.ALLOW -> {
                val pendingRequest = pendingGeolocationRequest(origin, callback)
                pendingGeolocationRequest = pendingRequest
                grantGeolocationAfterRuntimePermission(pendingRequest)
            }

            WebPermissionPolicy.ASK_EVERY_TIME -> {
                val pendingRequest = pendingGeolocationRequest(origin, callback)
                pendingGeolocationRequest = pendingRequest
                showPrompt(
                    WebPermissionPrompt(
                        title = textProvider.geolocationTitle,
                        message = textProvider.geolocationMessage,
                        onAllow = { grantGeolocationAfterRuntimePermission(pendingRequest) },
                        onDeny = {
                            if (pendingGeolocationRequest === pendingRequest) {
                                pendingGeolocationRequest = null
                            }
                            pendingRequest.deny()
                            if (pendingRequest.isCompleted) {
                                onMessage("Geolocation denied by user")
                                onUserFlow("Geolocation denied by user", origin.orEmpty())
                            }
                        },
                    )
                )
            }
        }
    }

    fun onGeolocationPermissionsHidePrompt() {
        val pendingRequest = pendingGeolocationRequest ?: return
        pendingRequest.deny()
        pendingGeolocationRequest = null
        showPrompt(null)
        onMessage("Geolocation prompt hidden")
        onUserFlow("Geolocation prompt hidden", "")
    }

    fun cancelPendingPrompts() {
        cancelPendingMediaRequest(completeWithDeny = true)
        cancelPendingGeolocationRequest(completeWithDeny = true)
    }

    private fun grantMediaAfterRuntimePermission(
        pendingRequest: PendingRequest,
        resources: List<String>,
        runtimePermissions: Array<String>,
    ) {
        requestRuntimePermissions(runtimePermissions) { grants ->
            if (pendingMediaRequest !== pendingRequest) {
                pendingRequest.cancelWithoutCallback()
                return@requestRuntimePermissions
            }

            pendingMediaRequest = null
            val grantedResources = resources.filter { resource ->
                resource.runtimePermissions().all { permission -> grants[permission] == true }
            }
            if (grantedResources.isNotEmpty()) {
                pendingRequest.grant(grantedResources.toTypedArray())
                if (pendingRequest.isCompleted) {
                    val deniedResources = resources - grantedResources.toSet()
                    val suffix = if (deniedResources.isEmpty()) {
                        ""
                    } else {
                        "; denied ${deniedResources.permissionLabel()}"
                    }
                    onMessage("Web permission granted: ${grantedResources.permissionLabel()}$suffix")
                    onUserFlow(
                        "Web permission granted: ${grantedResources.permissionLabel()}",
                        "runtime=${runtimePermissions.joinToString()}$suffix",
                    )
                }
            } else {
                pendingRequest.deny()
                if (pendingRequest.isCompleted) {
                    onMessage("Web permission denied: Android runtime permission missing")
                    onUserFlow("Web permission denied: Android runtime permission missing", runtimePermissions.joinToString())
                }
            }
            showPrompt(null)
        }
    }

    private fun grantGeolocationAfterRuntimePermission(
        pendingRequest: PendingRequest,
    ) {
        requestRuntimePermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)) { grants ->
            if (pendingGeolocationRequest !== pendingRequest) {
                pendingRequest.cancelWithoutCallback()
                return@requestRuntimePermissions
            }

            pendingGeolocationRequest = null
            val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
            if (granted) {
                pendingRequest.grant(emptyArray())
                if (pendingRequest.isCompleted) {
                    onMessage("Geolocation granted")
                    onUserFlow("Geolocation granted", "")
                }
            } else {
                pendingRequest.deny()
                if (pendingRequest.isCompleted) {
                    onMessage("Geolocation denied: Android runtime permission missing")
                    onUserFlow("Geolocation denied: Android runtime permission missing", Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
            showPrompt(null)
        }
    }

    private fun pendingGeolocationRequest(
        origin: String?,
        callback: GeolocationPermissions.Callback,
    ): PendingRequest =
        PendingRequest(
            matchesPermissionRequest = { false },
            grant = { callback.invoke(origin, true, false) },
            deny = { callback.invoke(origin, false, false) },
        )

    private fun cancelPendingMediaRequest(completeWithDeny: Boolean) {
        val pendingRequest = pendingMediaRequest ?: return
        if (completeWithDeny) {
            pendingRequest.deny()
        } else {
            pendingRequest.cancelWithoutCallback()
        }
        pendingMediaRequest = null
        showPrompt(null)
    }

    private fun cancelPendingGeolocationRequest(completeWithDeny: Boolean) {
        val pendingRequest = pendingGeolocationRequest ?: return
        if (completeWithDeny) {
            pendingRequest.deny()
        } else {
            pendingRequest.cancelWithoutCallback()
        }
        pendingGeolocationRequest = null
        showPrompt(null)
    }

    private fun String.policy(config: WebTestConfig): WebPermissionPolicy =
        when (this) {
            PermissionRequest.RESOURCE_VIDEO_CAPTURE -> config.cameraPolicy
            PermissionRequest.RESOURCE_AUDIO_CAPTURE -> config.microphonePolicy
            else -> WebPermissionPolicy.DENY
        }

    private fun String.runtimePermissions(): List<String> =
        when (this) {
            PermissionRequest.RESOURCE_VIDEO_CAPTURE -> listOf(Manifest.permission.CAMERA)
            PermissionRequest.RESOURCE_AUDIO_CAPTURE -> listOf(Manifest.permission.RECORD_AUDIO)
            else -> emptyList()
        }

    private fun List<String>.permissionLabel(): String =
        map {
            when (it) {
                PermissionRequest.RESOURCE_VIDEO_CAPTURE -> textProvider.camera
                PermissionRequest.RESOURCE_AUDIO_CAPTURE -> textProvider.microphone
                else -> textProvider.protectedDevicePermissions
            }
        }.distinct().joinWithAnd()

    private fun List<String>.joinWithAnd(): String =
        when (size) {
            0 -> ""
            1 -> first()
            2 -> textProvider.joinAnd(this[0], this[1])
            else -> dropLast(1).joinToString(", ") + ", " + textProvider.joinAnd("", last()).trim()
        }
}

private class PendingRequest(
    val matchesPermissionRequest: (PermissionRequest?) -> Boolean,
    private val grant: (Array<String>) -> Unit,
    private val deny: () -> Unit,
) {
    var isCompleted: Boolean = false
        private set

    fun grant(resources: Array<String>) {
        if (isCompleted) return

        isCompleted = true
        grant.invoke(resources)
    }

    fun deny() {
        if (isCompleted) return

        isCompleted = true
        deny.invoke()
    }

    fun cancelWithoutCallback() {
        isCompleted = true
    }
}
