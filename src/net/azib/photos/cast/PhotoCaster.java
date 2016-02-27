package net.azib.photos.cast;

import android.os.Bundle;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;

import java.io.IOException;

import static android.support.v7.media.MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY;
import static com.google.android.gms.cast.CastMediaControlIntent.categoryForCast;

public class PhotoCaster {
  private static final String TAG = PhotoCaster.class.getSimpleName();

  final MainActivity activity;
  final NotificationWithControls notification;
  final String appId;

  private GoogleApiClient apiClient;
  private String castSessionId;
  private boolean started;
  private boolean waitingForReconnect;

  private final CastChannel channel = new CastChannel();
  private final MediaRouter.Callback mediaRouterCallback = new MediaRouterCallback();
  private final MediaRouter mediaRouter;
  final MediaRouteSelector mediaRouteSelector;

  public PhotoCaster(MainActivity activity) {
    this.activity = activity;
    notification = new NotificationWithControls(activity);
    appId = activity.getString(R.string.app_id);

    // Configure Cast device discovery
    mediaRouter = MediaRouter.getInstance(activity.getApplicationContext());
    mediaRouteSelector = new MediaRouteSelector.Builder().addControlCategory(categoryForCast(appId)).build();
  }

  public void startDiscovery() {
    mediaRouter.addCallback(mediaRouteSelector, mediaRouterCallback, CALLBACK_FLAG_REQUEST_DISCOVERY);
  }

  public void stopDiscovery() {
    mediaRouter.removeCallback(mediaRouterCallback);
  }

  private class MediaRouterCallback extends MediaRouter.Callback {
    @Override public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo info) {
      connect(CastDevice.getFromBundle(info.getExtras()));
    }

    @Override public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo info) {
      stopReceiver();
      teardown();
    }
  }

  private void connect(CastDevice device) {
    try {
      apiClient = new GoogleApiClient.Builder(activity)
          .addApi(Cast.API, Cast.CastOptions.builder(device, new DisconnectListener()).build())
          .addConnectionCallbacks(new ConnectionCallbacks())
          .addOnConnectionFailedListener(new ConnectionFailedListener())
          .build();

      apiClient.connect();
    } catch (Exception e) {
      Log.e(TAG, "Failed connect", e);
    }
  }

  void teardown() {
    Log.d(TAG, "teardown");
    if (apiClient != null) {
      if (started) {
        if (apiClient.isConnected() || apiClient.isConnecting()) {
          try {
            Cast.CastApi.removeMessageReceivedCallbacks(apiClient, channel.getNamespace());
          } catch (IOException e) {
            Log.e(TAG, "Exception while removing channel", e);
          }
          apiClient.disconnect();
        }
        started = false;
      }
      apiClient = null;
    }
    waitingForReconnect = false;
    castSessionId = null;
    notification.cancel();
  }

  private class ConnectionFailedListener implements GoogleApiClient.OnConnectionFailedListener {
    @Override public void onConnectionFailed(ConnectionResult result) {
      Log.e(TAG, "onConnectionFailed ");
      teardown();
    }
  }

  private class DisconnectListener extends Cast.Listener {
    @Override public void onApplicationDisconnected(int errorCode) {
      Log.d(TAG, "application has stopped");
      teardown();
    }
  };

  private class CastChannel implements Cast.MessageReceivedCallback {
    public String getNamespace() {
      return activity.getString(R.string.namespace);
    }

    @Override public void onMessageReceived(CastDevice castDevice, String namespace, String message) {
      Log.d(TAG, "onMessageReceived: " + message);
      String[] parts = message.split("\\|", 2);
      notification.notify(parts[0]);
      activity.onMessageReceived(parts);
    }
  }

  private class ConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks {
    @Override public void onConnected(Bundle connectionHint) {
      Log.d(TAG, "onConnected");

      // We got disconnected while this runnable was pending execution.
      if (apiClient == null) return;

      try {
        if (waitingForReconnect) {
          waitingForReconnect = false;

          // Check if the receiver app is still running
          if (connectionHint != null && connectionHint.getBoolean(Cast.EXTRA_APP_NO_LONGER_RUNNING)) {
            Log.d(TAG, "App is no longer running");
            teardown();
          } else {
            registerChannel();
          }
        } else {
          launchReceiver();
        }
      } catch (Exception e) {
        Log.e(TAG, "Failed to launch application", e);
      }
    }

    @Override public void onConnectionSuspended(int cause) {
      Log.d(TAG, "onConnectionSuspended");
      waitingForReconnect = true;
    }
  }

  private void launchReceiver() {
    Cast.CastApi.launchApplication(apiClient, appId, false).setResultCallback(new ResultCallback<Cast.ApplicationConnectionResult>() {
      @Override public void onResult(Cast.ApplicationConnectionResult result) {
        Log.d(TAG, "ApplicationConnectionResultCallback.onResult: statusCode" + result.getStatus().getStatusCode());
        if (result.getStatus().isSuccess()) {
          castSessionId = result.getSessionId();
          Log.d(TAG, "application name: " + result.getApplicationMetadata().getName() + ", status: " + result.getApplicationStatus() + ", sessionId: " + castSessionId + ", wasLaunched: " + result.getWasLaunched());
          started = true;
          registerChannel();
        } else {
          Log.e(TAG, "application could not launch");
          teardown();
        }
      }
    });
  }

  private void stopReceiver() {
    if (apiClient == null || castSessionId == null) return;
    Cast.CastApi.stopApplication(apiClient, castSessionId);
  }

  private void registerChannel() {
    try {
      Cast.CastApi.setMessageReceivedCallbacks(apiClient, channel.getNamespace(), channel);
    } catch (IOException e) {
      Log.e(TAG, "Exception while creating channel", e);
    }
  }

  public void sendCommand(String message) {
    if (apiClient != null) {
      Cast.CastApi.sendMessage(apiClient, channel.getNamespace(), message);
    } else {
      Toast.makeText(activity, "Chromecast not connected", Toast.LENGTH_SHORT).show();
    }
  }
}
