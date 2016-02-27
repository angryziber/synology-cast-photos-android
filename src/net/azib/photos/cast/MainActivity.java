package net.azib.photos.cast;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.view.*;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import java.text.SimpleDateFormat;
import java.util.Date;

import static android.content.Intent.ACTION_VIEW;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

public class MainActivity extends AppCompatActivity {
	Switch randomSwitch, styleSwitch;
	AutoCompleteTextView path;
	TextView status;

	PhotoCaster caster;
	GestureDetector gestureDetector;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getResources().getBoolean(R.bool.portrait_only))
			setRequestedOrientation(SCREEN_ORIENTATION_PORTRAIT);

		setContentView(R.layout.activity_main);

		caster = new PhotoCaster(this);

		path = (AutoCompleteTextView) findViewById(R.id.photosPathEdit);
		path.setAdapter(new PhotoDirsSuggestionAdapter(this));
		path.setText(new SimpleDateFormat("yyyy").format(new Date()));
		path.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				castPhotos();
			}
		});

		Button castPhotosButton = (Button) findViewById(R.id.castPhotosButton);
		castPhotosButton.setOnClickListener(new OnClickListener() {
			@Override public void onClick(View v) {
				castPhotos();
			}
		});

		assignCommand(R.id.next_button, "next");
		assignCommand(R.id.prev_button, "prev");
		assignCommand(R.id.pause_button, "pause");
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

		gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
			@Override public boolean onDown(MotionEvent e) {
				return true;
			}

			@Override public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
				if (Math.abs(velocityX) < Math.abs(velocityY)) return false;
				if (velocityX > 0) caster.sendCommand("prev");
				else caster.sendCommand("next");
				return true;
			}
		});

		status = (TextView) findViewById(R.id.status);

		randomSwitch = (Switch) findViewById(R.id.randomSwitch);
		randomSwitch.setOnClickListener(new OnClickListener() {
			@Override public void onClick(View v) {
				caster.sendCommand(randomSwitch.isChecked() ? "rnd" : "seq");
			}
		});

		styleSwitch = (Switch) findViewById(R.id.styleSwitch);
		styleSwitch.setOnClickListener(new OnClickListener() {
			@Override public void onClick(View v) {
				caster.sendCommand(styleSwitch.isChecked() ? "style:cover" : "style:contain");
			}
		});

		String command = getIntent().getStringExtra("command");
		if (command != null) caster.sendCommand(command);
	}

	@Override public boolean onTouchEvent(MotionEvent event) {
		gestureDetector.onTouchEvent(event);
		return super.onTouchEvent(event);
	}

	private void assignCommand(int buttonId, final String command) {
		Button button = (Button) findViewById(buttonId);
		button.setOnClickListener(new OnClickListener() {
			@Override public void onClick(View view) {
				caster.sendCommand(command);
			}
		});
	}

	private void castPhotos() {
    caster.sendCommand((randomSwitch.isChecked() ? "rnd:" : "seq:") + path.getText());
		path.clearFocus();
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(path.getWindowToken(), 0);
	}

	@Override protected void onResume() {
		super.onResume();
		caster.onResume();
	}

	@Override protected void onPause() {
		if (isFinishing()) caster.onPause();
		super.onPause();
	}

	@Override public void onDestroy() {
		caster.teardown();
		super.onDestroy();
	}

	@Override public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.main, menu);
		MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
		MediaRouteActionProvider mediaRouteActionProvider = (MediaRouteActionProvider) MenuItemCompat.getActionProvider(mediaRouteMenuItem);
		// Set the MediaRouteActionProvider selector for device discovery.
		mediaRouteActionProvider.setRouteSelector(caster.mediaRouteSelector);
		return true;
	}

	public void onMessageReceived(final String...parts) {
		status.setText(parts[0]);
		if (parts.length == 2)
			status.setOnClickListener(new View.OnClickListener() {
				@Override public void onClick(View v) {
					startActivity(new Intent(ACTION_VIEW, Uri.parse(parts[1])));
				}
			});
		else
			status.setClickable(false);
	}
}
