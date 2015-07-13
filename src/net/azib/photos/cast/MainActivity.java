/*
 * Copyright (C) 2014 Google Inc. All Rights Reserved. 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package net.azib.photos.cast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.Cast.ApplicationConnectionResult;
import com.google.android.gms.cast.Cast.MessageReceivedCallback;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

/**
 * Main activity to send messages to the receiver.
 */
public class MainActivity extends AppCompatActivity {
	private static final String TAG = MainActivity.class.getSimpleName();

	private MediaRouter mediaRouter;
	private MediaRouteSelector mediaRouteSelector;
	private MediaRouter.Callback mediaRouterCallback;
	private CastDevice selectedDevice;
	private GoogleApiClient apiClient;
	private Cast.Listener castListener;
	private ConnectionCallbacks connectionCallbacks;
	private ConnectionFailedListener connectionFailedListener;
	private CastChannel channel;
	private boolean started;
	private boolean waitingForReconnect;
	private String castSessionId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		ActionBar actionBar = getSupportActionBar();
		actionBar.setBackgroundDrawable(new ColorDrawable(android.R.color.transparent));

		AutoCompleteTextView path = (AutoCompleteTextView) findViewById(R.id.photosPathEdit);
		path.setAdapter(new PhotoDirsSuggestionAdapter(this));
		path.setText(new SimpleDateFormat("yyyy").format(new Date()));

		Button castPhotosButton = (Button) findViewById(R.id.castPhotosButton);
		castPhotosButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				castPhotos();
			}
		});

		assignCommand(R.id.next_button, "next");
		assignCommand(R.id.prev_button, "prev");
		assignCommand(R.id.next_more_button, "next:10");
		assignCommand(R.id.prev_more_button, "prev:10");
		assignCommand(R.id.mark_delete_button, "mark:delete");
		assignCommand(R.id.mark_red_button, "mark:red");
		assignCommand(R.id.mark_yellow_button, "mark:yellow");
		assignCommand(R.id.mark_green_button, "mark:green");
		assignCommand(R.id.mark_blue_button, "mark:blue");
		assignCommand(R.id.mark_0_button, "mark:0");
		assignCommand(R.id.mark_1_button, "mark:1");
		assignCommand(R.id.mark_2_button, "mark:2");
		assignCommand(R.id.mark_3_button, "mark:3");
		assignCommand(R.id.mark_4_button, "mark:4");
		assignCommand(R.id.mark_5_button, "mark:5");

		// Configure Cast device discovery
		mediaRouter = MediaRouter.getInstance(getApplicationContext());
		mediaRouteSelector = new MediaRouteSelector.Builder()
				.addControlCategory(CastMediaControlIntent.categoryForCast(getAppId())).build();
		mediaRouterCallback = new MyMediaRouterCallback();
	}

	private void assignCommand(int buttonId, final String command) {
		Button button = (Button) findViewById(buttonId);
		button.setOnClickListener(new OnClickListener() {
			@Override public void onClick(View view) {
				sendCommand(command);
			}
		});
	}

	private String getAppId() {
		return getString(R.string.app_id);
	}

	private void castPhotos() {
    Switch randomSwitch = (Switch) findViewById(R.id.randomSwitch);
    AutoCompleteTextView path = (AutoCompleteTextView) findViewById(R.id.photosPathEdit);
    sendCommand((randomSwitch.isChecked() ? "rnd:" : "seq:") + path.getText());
	}

	@Override
	protected void onResume() {
		super.onResume();
		// Start media router discovery
		mediaRouter.addCallback(mediaRouteSelector, mediaRouterCallback,
        MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
	}

	@Override
	protected void onPause() {
		if (isFinishing()) {
			// End media router discovery
			mediaRouter.removeCallback(mediaRouterCallback);
		}
		super.onPause();
	}

	@Override
	public void onDestroy() {
		teardown();
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.main, menu);
		MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
		MediaRouteActionProvider mediaRouteActionProvider = (MediaRouteActionProvider) MenuItemCompat
				.getActionProvider(mediaRouteMenuItem);
		// Set the MediaRouteActionProvider selector for device discovery.
		mediaRouteActionProvider.setRouteSelector(mediaRouteSelector);
		return true;
	}

	/**
	 * Callback for MediaRouter events
	 */
	private class MyMediaRouterCallback extends MediaRouter.Callback {

		@Override
		public void onRouteSelected(MediaRouter router, RouteInfo info) {
			Log.d(TAG, "onRouteSelected");
			// Handle the user route selection.
			selectedDevice = CastDevice.getFromBundle(info.getExtras());

			launchReceiver();
		}

		@Override
		public void onRouteUnselected(MediaRouter router, RouteInfo info) {
			Log.d(TAG, "onRouteUnselected: info=" + info);
			teardown();
			selectedDevice = null;
		}
	}

	/**
	 * Start the receiver app
	 */
	private void launchReceiver() {
		try {
			castListener = new Cast.Listener() {

				@Override
				public void onApplicationDisconnected(int errorCode) {
					Log.d(TAG, "application has stopped");
					teardown();
				}

			};
			// Connect to Google Play services
			connectionCallbacks = new ConnectionCallbacks();
			connectionFailedListener = new ConnectionFailedListener();
			Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions.builder(selectedDevice, castListener);
			apiClient = new GoogleApiClient.Builder(this)
					.addApi(Cast.API, apiOptionsBuilder.build())
					.addConnectionCallbacks(connectionCallbacks)
					.addOnConnectionFailedListener(connectionFailedListener)
					.build();

			apiClient.connect();
		} catch (Exception e) {
			Log.e(TAG, "Failed launchReceiver", e);
		}
	}

	/**
	 * Google Play services callbacks
	 */
	private class ConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks {
		@Override
		public void onConnected(Bundle connectionHint) {
			Log.d(TAG, "onConnected");

			if (apiClient == null) {
				// We got disconnected while this runnable was pending
				// execution.
				return;
			}

			try {
				if (waitingForReconnect) {
					waitingForReconnect = false;

					// Check if the receiver app is still running
					if ((connectionHint != null) && connectionHint.getBoolean(Cast.EXTRA_APP_NO_LONGER_RUNNING)) {
						Log.d(TAG, "App is no longer running");
						teardown();
					} else {
						// Re-create the custom message channel
						try {
							Cast.CastApi.setMessageReceivedCallbacks(apiClient, channel.getNamespace(), channel);
						} catch (IOException e) {
							Log.e(TAG, "Exception while creating channel", e);
						}
					}
				} else {
					// Launch the receiver app
					Cast.CastApi
							.launchApplication(apiClient, getAppId(), false)
							.setResultCallback(
									new ResultCallback<Cast.ApplicationConnectionResult>() {
										@Override
										public void onResult(
												ApplicationConnectionResult result) {
											Status status = result.getStatus();
											Log.d(TAG,
													"ApplicationConnectionResultCallback.onResult: statusCode"
															+ status.getStatusCode());
											if (status.isSuccess()) {
												ApplicationMetadata applicationMetadata = result.getApplicationMetadata();
												castSessionId = result.getSessionId();
												String applicationStatus = result.getApplicationStatus();
												boolean wasLaunched = result.getWasLaunched();
												Log.d(TAG,
														"application name: "
																+ applicationMetadata
																.getName()
																+ ", status: "
																+ applicationStatus
																+ ", sessionId: "
																+ castSessionId
																+ ", wasLaunched: "
																+ wasLaunched);
												started = true;

												// Create the custom message
												// channel
												channel = new CastChannel();
												try {
													Cast.CastApi.setMessageReceivedCallbacks(apiClient, channel.getNamespace(), channel);
												} catch (IOException e) {
													Log.e(TAG, "Exception while creating channel", e);
												}
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

		@Override
		public void onConnectionSuspended(int cause) {
			Log.d(TAG, "onConnectionSuspended");
			waitingForReconnect = true;
		}
	}

	/**
	 * Google Play services callbacks
	 */
	private class ConnectionFailedListener implements GoogleApiClient.OnConnectionFailedListener {
		@Override
		public void onConnectionFailed(ConnectionResult result) {
			Log.e(TAG, "onConnectionFailed ");
			teardown();
		}
	}

	/**
	 * Tear down the connection to the receiver
	 */
	private void teardown() {
		Log.d(TAG, "teardown");
		if (apiClient != null) {
			if (started) {
				if (apiClient.isConnected()  || apiClient.isConnecting()) {
					try {
						Cast.CastApi.stopApplication(apiClient, castSessionId);
						if (channel != null) {
							Cast.CastApi.removeMessageReceivedCallbacks(apiClient, channel.getNamespace());
							channel = null;
						}
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
	}

	private void sendCommand(String message) {
		if (apiClient != null && channel != null) {
			try {
				Cast.CastApi.sendMessage(apiClient,
						channel.getNamespace(), message)
						.setResultCallback(new ResultCallback<Status>() {
							@Override
							public void onResult(Status result) {
								if (!result.isSuccess()) {
									Log.e(TAG, "Sending message failed");
								}
							}
						});
			} catch (Exception e) {
				Log.e(TAG, "Exception while sending message", e);
			}
		} else {
			Toast.makeText(MainActivity.this, "Chromecast not connected", Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Custom message channel
	 */
	class CastChannel implements MessageReceivedCallback {
		public String getNamespace() {
			return getString(R.string.namespace);
		}

		@Override
		public void onMessageReceived(CastDevice castDevice, String namespace, String message) {
      Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
			Log.d(TAG, "onMessageReceived: " + message);
		}
	}
}
