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

  private GoogleApiClient apiClient;
  private CastDevice selectedDevice;
  private boolean started;
  private boolean waitingForReconnect;
  private String castSessionId;

  private final CastChannel channel = new CastChannel();
  private final MediaRouter.Callback mediaRouterCallback = new MediaRouterCallback();
  private final MediaRouter mediaRouter;
  final MediaRouteSelector mediaRouteSelector;

  public PhotoCaster(MainActivity activity) {
    this.activity = activity;
    notification = new NotificationWithControls(activity);

    // Configure Cast device discovery
    mediaRouter = MediaRouter.getInstance(activity.getApplicationContext());
    mediaRouteSelector = new MediaRouteSelector.Builder().addControlCategory(categoryForCast(getAppId())).build();
  }

  String getAppId() {
    return activity.getString(R.string.app_id);
  }

  /**
   * Start the receiver app
   */
  private void launchReceiver() {
    try {
      Cast.Listener castListener = new Cast.Listener() {
        @Override public void onApplicationDisconnected(int errorCode) {
          Log.d(TAG, "application has stopped");
          teardown();
        }
      };
      // Connect to Google Play services
      apiClient = new GoogleApiClient.Builder(activity)
          .addApi(Cast.API, Cast.CastOptions.builder(selectedDevice, castListener).build())
          .addConnectionCallbacks(new ConnectionCallbacks())
          .addOnConnectionFailedListener(new ConnectionFailedListener())
          .build();

      apiClient.connect();
    } catch (Exception e) {
      Log.e(TAG, "Failed launchReceiver", e);
    }
  }

  /**
   * Tear down the connection to the receiver
   */
  void teardown() {
    Log.d(TAG, "teardown");
    if (apiClient != null) {
      if (started) {
        if (apiClient.isConnected() || apiClient.isConnecting()) {
          try {
            Cast.CastApi.stopApplication(apiClient, castSessionId);
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
    selectedDevice = null;
    waitingForReconnect = false;
    castSessionId = null;
    notification.cancel();
  }

  void onPause() {
    mediaRouter.removeCallback(mediaRouterCallback);
  }

  void onResume() {
    // Start media router discovery
    mediaRouter.addCallback(mediaRouteSelector, mediaRouterCallback, CALLBACK_FLAG_REQUEST_DISCOVERY);
  }

  class ConnectionFailedListener implements GoogleApiClient.OnConnectionFailedListener {
    @Override public void onConnectionFailed(ConnectionResult result) {
      Log.e(TAG, "onConnectionFailed ");
      teardown();
    }
  }

  class CastChannel implements Cast.MessageReceivedCallback {
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

  class ConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks {
    @Override public void onConnected(Bundle connectionHint) {
      Log.d(TAG, "onConnected");

      if (apiClient == null) {
        // We got disconnected while this runnable was pending execution.
        return;
      }

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
          // Launch the receiver app
          Cast.CastApi.launchApplication(apiClient, getAppId(), false).setResultCallback(new ResultCallback<Cast.ApplicationConnectionResult>() {
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
      } catch (Exception e) {
        Log.e(TAG, "Failed to launch application", e);
      }
    }

    void registerChannel() {
      try {
        Cast.CastApi.setMessageReceivedCallbacks(apiClient, channel.getNamespace(), channel);
      } catch (IOException e) {
        Log.e(TAG, "Exception while creating channel", e);
      }
    }

    @Override public void onConnectionSuspended(int cause) {
      Log.d(TAG, "onConnectionSuspended");
      waitingForReconnect = true;
    }
  }

  /**
   * Callback for MediaRouter events
   */
  class MediaRouterCallback extends MediaRouter.Callback {
    @Override public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo info) {
      Log.d(TAG, "onRouteSelected");
      // Handle the user route selection.
      selectedDevice = CastDevice.getFromBundle(info.getExtras());

      launchReceiver();
    }

    @Override public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo info) {
      Log.d(TAG, "onRouteUnselected: info=" + info);
      teardown();
      selectedDevice = null;
    }
  }

  void sendCommand(String message) {
    if (apiClient != null) {
      Cast.CastApi.sendMessage(apiClient, channel.getNamespace(), message);
    } else {
      Toast.makeText(activity, "Chromecast not connected", Toast.LENGTH_SHORT).show();
    }
  }
}
