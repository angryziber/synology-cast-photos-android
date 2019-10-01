package net.azib.photos.cast

import android.app.Activity
import android.os.Bundle
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import androidx.mediarouter.media.MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY
import android.util.Log
import android.widget.Toast
import com.google.android.gms.cast.Cast
import com.google.android.gms.cast.CastDevice
import com.google.android.gms.cast.CastMediaControlIntent.categoryForCast
import com.google.android.gms.cast.LaunchOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import java.io.IOException

class CastClient(var activity: Activity) {
  private val TAG = javaClass.simpleName
  private val notification = NotificationWithControls(activity)
  private val appId = activity.getString(R.string.app_id)

  private val api = Cast.CastApi
  private var apiClient: GoogleApiClient? = null
  private var castSessionId: String? = null
  private var receiverStarted = false
  private var reconnecting = false

  private val channel = CastChannel()
  private val mediaRouterCallback = MediaRouterCallback()
  private val mediaRouter = MediaRouter.getInstance(activity.applicationContext)
  val mediaRouteSelector = MediaRouteSelector.Builder().addControlCategory(categoryForCast(appId)).build()

  fun startDiscovery() {
    mediaRouter.addCallback(mediaRouteSelector, mediaRouterCallback, CALLBACK_FLAG_REQUEST_DISCOVERY)
  }

  fun stopDiscovery() {
    mediaRouter.removeCallback(mediaRouterCallback)
  }

  private inner class MediaRouterCallback : MediaRouter.Callback() {
    override fun onRouteSelected(router: MediaRouter, info: MediaRouter.RouteInfo) {
      connect(CastDevice.getFromBundle(info.extras))
    }

    override fun onRouteUnselected(router: MediaRouter, info: MediaRouter.RouteInfo) {
      if (receiverStarted) stopReceiver()
      teardown()
    }
  }

  private fun connect(device: CastDevice) {
    try {
      apiClient = GoogleApiClient.Builder(activity)
          .addApi(Cast.API, Cast.CastOptions.Builder(device, DisconnectListener()).build())
          .addConnectionCallbacks(ConnectionCallbacks())
          .addOnConnectionFailedListener(ConnectionFailedListener())
          .build()

      apiClient!!.connect()
    } catch (e: Exception) {
      Log.e(TAG, "Failed connect", e)
    }
  }

  internal fun teardown() {
    Log.d(TAG, "teardown")
    val client = apiClient ?: return
    if (receiverStarted && (client.isConnected || client.isConnecting)) {
      channel.unregister()
      client.disconnect()
    }
    apiClient = null
    castSessionId = null
    receiverStarted = false
    reconnecting = false
    notification.cancel()
  }

  private inner class ConnectionFailedListener : GoogleApiClient.OnConnectionFailedListener {
    override fun onConnectionFailed(result: ConnectionResult) {
      Log.e(TAG, "onConnectionFailed ")
      teardown()
    }
  }

  private inner class DisconnectListener : Cast.Listener() {
    override fun onApplicationDisconnected(errorCode: Int) {
      Log.d(TAG, "application has stopped")
      teardown()
    }
  }

  private inner class CastChannel : Cast.MessageReceivedCallback {
    val namespace = activity.getString(R.string.namespace)

    override fun onMessageReceived(castDevice: CastDevice, namespace: String, message: String) {
      Log.d(TAG, "onMessageReceived: $message")
      val parts = message.split("\\|".toRegex(), 2)
      notification.notify(parts[0], mediaRouter.mediaSessionToken)
      (activity as MainActivity).onMessageReceived(parts)
    }

    internal fun register() {
      try {
        api.setMessageReceivedCallbacks(apiClient, namespace, channel)
      } catch (e: IOException) {
        Log.e(TAG, "Exception while creating channel", e)
      }
    }

    internal fun unregister() {
      try {
        api.removeMessageReceivedCallbacks(apiClient, namespace)
      } catch (e: IOException) {
        Log.e(TAG, "Exception while removing channel", e)
      }
    }
  }

  private inner class ConnectionCallbacks : GoogleApiClient.ConnectionCallbacks {
    override fun onConnected(hint: Bundle?) {
      Log.d(TAG, "onConnected")
      if (apiClient == null) return  // We got disconnected while this runnable was pending execution.

      try {
        if (!reconnecting)
          launchReceiver()
        else {
          reconnecting = false
          if (hint != null && hint.getBoolean(Cast.EXTRA_APP_NO_LONGER_RUNNING)) {
            Log.d(TAG, "App is no longer running")
            teardown()
          } else {
            channel.register()
          }
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to launch application", e)
      }
    }

    override fun onConnectionSuspended(cause: Int) {
      Log.d(TAG, "onConnectionSuspended")
      reconnecting = true
    }
  }

  private fun launchReceiver() {
    api.launchApplication(apiClient, appId, LaunchOptions()).setResultCallback { result ->
      Log.d(TAG, "ApplicationConnectionResultCallback.onResult: statusCode " + result.status.statusCode)
      if (result.status.isSuccess) {
        castSessionId = result.sessionId
        Log.d(TAG, "application name: ${result.applicationMetadata.name}, status: ${result.applicationStatus}, sessionId: ${castSessionId}, wasLaunched: ${result.wasLaunched}")
        receiverStarted = true
        channel.register()
      } else {
        Log.e(TAG, "application could not launch")
        teardown()
      }
    }
  }

  private fun stopReceiver() {
    if (apiClient == null || castSessionId == null) return
    api.stopApplication(apiClient, castSessionId)
  }

  fun sendCommand(message: String) {
    if (apiClient != null)
      api.sendMessage(apiClient, channel.namespace, message)
    else
      Toast.makeText(activity, "Chromecast not connected", Toast.LENGTH_SHORT).show()
  }
}
