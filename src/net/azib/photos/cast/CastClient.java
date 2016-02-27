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

public class CastClient {
  private static final String TAG = CastClient.class.getSimpleName();

  final MainActivity activity;
  final NotificationWithControls notification;
  final String appId;

  private GoogleApiClient apiClient;
  private String castSessionId;
  private boolean receiverStarted;
  private boolean reconnecting;

  private final CastChannel channel = new CastChannel();
  private final MediaRouter.Callback mediaRouterCallback = new MediaRouterCallback();
  private final MediaRouter mediaRouter;
  final MediaRouteSelector mediaRouteSelector;

  public CastClient(MainActivity activity) {
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
      if (receiverStarted) stopReceiver();
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
    if (apiClient == null) return;
    if (receiverStarted && (apiClient.isConnected() || apiClient.isConnecting())) {
      channel.unregister();
      apiClient.disconnect();
    }
    apiClient = null;
    castSessionId = null;
    receiverStarted = false;
    reconnecting = false;
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

    void register() {
      try {
        Cast.CastApi.setMessageReceivedCallbacks(apiClient, getNamespace(), channel);
      } catch (IOException e) {
        Log.e(TAG, "Exception while creating channel", e);
      }
    }

    void unregister() {
      try {
        Cast.CastApi.removeMessageReceivedCallbacks(apiClient, getNamespace());
      } catch (IOException e) {
        Log.e(TAG, "Exception while removing channel", e);
      }
    }
  }

  private class ConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks {
    @Override public void onConnected(Bundle hint) {
      Log.d(TAG, "onConnected");
      if (apiClient == null) return; // We got disconnected while this runnable was pending execution.

      try {
        if (!reconnecting) launchReceiver();
        else {
          reconnecting = false;
          if (hint != null && hint.getBoolean(Cast.EXTRA_APP_NO_LONGER_RUNNING)) {
            Log.d(TAG, "App is no longer running");
            teardown();
          } else {
            channel.register();
          }
        }
      } catch (Exception e) {
        Log.e(TAG, "Failed to launch application", e);
      }
    }

    @Override public void onConnectionSuspended(int cause) {
      Log.d(TAG, "onConnectionSuspended");
      reconnecting = true;
    }
  }

  private void launchReceiver() {
    Cast.CastApi.launchApplication(apiClient, appId, false).setResultCallback(new ResultCallback<Cast.ApplicationConnectionResult>() {
      @Override public void onResult(Cast.ApplicationConnectionResult result) {
        Log.d(TAG, "ApplicationConnectionResultCallback.onResult: statusCode" + result.getStatus().getStatusCode());
        if (result.getStatus().isSuccess()) {
          castSessionId = result.getSessionId();
          Log.d(TAG, "application name: " + result.getApplicationMetadata().getName() + ", status: " + result.getApplicationStatus() + ", sessionId: " + castSessionId + ", wasLaunched: " + result.getWasLaunched());
          receiverStarted = true;
          channel.register();
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

  public void sendCommand(String message) {
    if (apiClient != null) {
      Cast.CastApi.sendMessage(apiClient, channel.getNamespace(), message);
    } else {
      Toast.makeText(activity, "Chromecast not connected", Toast.LENGTH_SHORT).show();
    }
  }
}
