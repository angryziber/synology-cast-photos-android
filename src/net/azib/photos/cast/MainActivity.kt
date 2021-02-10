package net.azib.photos.cast

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuItemCompat
import androidx.mediarouter.app.MediaRouteActionProvider
import kotlinx.android.synthetic.main.main.*
import org.json.JSONObject

class MainActivity: AppCompatActivity() {
  lateinit var castAppId: String
  lateinit var receivers: List<Receiver>
  lateinit var cast: CastClient

  override fun onCreate(state: Bundle?) {
    super.onCreate(state)
    castAppId = resources.getString(R.string.castAppId)
    val receiverPath = resources.getString(R.string.receiverPath)
    receivers = resources.getStringArray(R.array.receivers).map {
      val p = it.split("|")
      Receiver(p[0], p[1] + receiverPath, p[2])
    }

    if (resources.getBoolean(R.bool.portrait_only))
      requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

    cast = CastClient(this, castAppId, receivers.first())
    setContentView(R.layout.main)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    val command = intent.action
    if (command != null) cast.sendCommand(command)
  }

  override fun onResume() {
    super.onResume()
    cast.startDiscovery()
  }

  override fun onPause() {
    if (isFinishing) cast.stopDiscovery()
    super.onPause()
  }

  fun selectAppId(m: MenuItem) {
    if (m.subMenu.size() == 0) {
      receivers.forEach {
        appId -> m.subMenu.add(appId.name).setOnMenuItemClickListener {
          cast.receiver = appId
          true
        }
      }
    }
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
    if (parts[0].startsWith("state:")) {
      (container as PhotosFragment).updateState(JSONObject(parts[0].substring("state:".length)))
      return
    }
    status.text = parts[0]
    if (parts.size == 2)
      status.setOnClickListener { startActivity(Intent(ACTION_VIEW, Uri.parse(parts[1]))) }
    else
      status.isClickable = false
  }
}
