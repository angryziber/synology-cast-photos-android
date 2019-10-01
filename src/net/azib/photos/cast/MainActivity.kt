package net.azib.photos.cast

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.FragmentPagerAdapter
import androidx.core.view.MenuItemCompat
import androidx.viewpager.widget.ViewPager
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.ActionBar.Tab
import androidx.appcompat.app.ActionBar.TabListener
import androidx.appcompat.app.AppCompatActivity
import androidx.mediarouter.app.MediaRouteActionProvider
import android.view.*
import kotlinx.android.synthetic.main.photos.*
import kotlinx.android.synthetic.main.tabs.*

enum class CastType {
  Photos, Videos
}

class MainActivity : AppCompatActivity(), TabListener {
  private lateinit var sectionsPagerAdapter: SectionsPagerAdapter
  lateinit var cast: CastClient

  override fun onCreate(state: Bundle?) {
    super.onCreate(state)

    if (resources.getBoolean(R.bool.portrait_only))
      requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

    setContentView(R.layout.tabs)
    sectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

    container.setAdapter(sectionsPagerAdapter)

    val actionBar = supportActionBar!!.apply {
      navigationMode = ActionBar.NAVIGATION_MODE_TABS
    }

    container.setOnPageChangeListener(object: ViewPager.SimpleOnPageChangeListener() {
      override fun onPageSelected(position: Int) {
        actionBar.setSelectedNavigationItem(position)
      }
    })

    sectionsPagerAdapter.let {
      for (i in 0 until it.count) {
        actionBar.addTab(actionBar.newTab().setText(it.getPageTitle(i)).setTabListener(this))
      }
    }

    cast = CastClient(this)
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

  override fun onTabSelected(tab: Tab, fragmentTransaction: FragmentTransaction) {
    container.setCurrentItem(tab.position, true)
  }

  override fun onTabUnselected(tab: Tab, fragmentTransaction: FragmentTransaction) {}

  override fun onTabReselected(tab: Tab, fragmentTransaction: FragmentTransaction) {}

  inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
    override fun getCount() = CastType.values().size
    override fun getPageTitle(position: Int) = CastType.values()[position].name

    override fun getItem(position: Int) = when (position) {
      0 -> PhotosFragment()
      1 -> VideosFragment()
      else -> null
    }
  }
}
