package net.azib.photos.cast

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
import android.net.Uri
import android.os.Bundle
import android.support.v4.view.MenuItemCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.MediaRouteActionProvider
import android.view.GestureDetector
import android.view.Menu
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
  lateinit var gestureDetector: GestureDetector

  companion object {
    lateinit var cast: CastClient
  }

  override fun onCreate(state: Bundle?) {
    super.onCreate(state)

    if (resources.getBoolean(R.bool.portrait_only))
      requestedOrientation = SCREEN_ORIENTATION_PORTRAIT

    setContentView(R.layout.activity_main)

    try {
      cast.activity = this
    }
    catch (e: UninitializedPropertyAccessException) {
      cast = CastClient(this)
    }

    path.setAdapter(PhotoDirsSuggestionAdapter(this))
    path.setText(state?.getString("path") ?: SimpleDateFormat("yyyy").format(Date()))
    path.setOnItemClickListener { parent, view, pos, id -> castPhotos() }

    castPhotosButton.setOnClickListener { castPhotos() }
    castVideosButton.setOnClickListener { castVideos() }

    assignCommand(nextButton, "next")
    assignCommand(prevButton, "prev")
    assignCommand(pauseButton, "pause")
    assignCommand(nextMoreButton, "next:10")
    assignCommand(prevMoreButton, "prev:10")
    assignCommand(markDeleteButton, "mark:delete")
    assignCommand(markRedButton, "mark:red")
    assignCommand(markYellowButton, "mark:yellow")
    assignCommand(markGreenButton, "mark:green")
    assignCommand(markBlueButton, "mark:blue")
    assignCommand(mark0Button, "mark:0")
    assignCommand(mark1Button, "mark:1")
    assignCommand(mark2Button, "mark:2")
    assignCommand(mark3Button, "mark:3")
    assignCommand(mark4Button, "mark:4")
    assignCommand(mark5Button, "mark:5")

    randomSwitch.setOnClickListener {
      cast.sendCommand(if (randomSwitch.isChecked) "rnd" else "seq")
    }
    styleSwitch.setOnClickListener {
      cast.sendCommand(if (styleSwitch.isChecked) "style:cover" else "style:contain")
    }

    gestureDetector = GestureDetector(this, GestureListener())
  }

  class GestureListener : GestureDetector.SimpleOnGestureListener() {
    override fun onDown(e: MotionEvent) = true

    override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
      if (Math.abs(velocityX) < Math.abs(velocityY)) return false
      if (velocityX > 0)
        cast.sendCommand("prev")
      else
        cast.sendCommand("next")
      return true
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    val command = intent.action
    if (command != null) cast.sendCommand(command)
  }

  override fun onSaveInstanceState(state: Bundle) {
    super.onSaveInstanceState(state)
    state.putString("path", path.text.toString())
  }

  override fun onTouchEvent(event: MotionEvent): Boolean {
    gestureDetector.onTouchEvent(event)
    return super.onTouchEvent(event)
  }

  private fun assignCommand(button: Button, command: String) {
    button.setOnClickListener { cast.sendCommand(command) }
  }

  private fun castPhotos() {
    cast.sendCommand("photos:" + path.text)
    if (!randomSwitch.isChecked) cast.sendCommand("seq:" + path.text)
    clearPathFocus()
  }

  private fun castVideos() {
    cast.sendCommand("videos:" + path.text)
    clearPathFocus()
  }

  private fun clearPathFocus() {
    path.clearFocus()
    (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(path.windowToken, 0)
  }

  override fun onResume() {
    super.onResume()
    cast.startDiscovery()
  }

  override fun onPause() {
    if (isFinishing) cast.stopDiscovery()
    super.onPause()
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    super.onCreateOptionsMenu(menu)
    menuInflater.inflate(R.menu.main, menu)
    val mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item)
    val mediaRouteActionProvider = MenuItemCompat.getActionProvider(mediaRouteMenuItem) as MediaRouteActionProvider
    // Set the MediaRouteActionProvider selector for device discovery.
    mediaRouteActionProvider.routeSelector = cast.mediaRouteSelector
    return true
  }

  fun onMessageReceived(parts: List<String>) {
    status.text = parts[0]
    if (parts.size == 2)
      status.setOnClickListener { startActivity(Intent(ACTION_VIEW, Uri.parse(parts[1]))) }
    else
      status.isClickable = false
  }
}
