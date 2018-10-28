package net.azib.photos.cast

import android.app.*
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.support.v13.app.FragmentPagerAdapter
import android.support.v4.view.MenuItemCompat
import android.support.v4.view.ViewPager
import android.support.v4.app.FragmentTransaction
import android.support.v7.app.ActionBar.Tab
import android.support.v7.app.ActionBar.TabListener
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.MediaRouteActionProvider
import android.view.*
import kotlinx.android.synthetic.main.photos.*
import kotlinx.android.synthetic.main.tabs.*

class MainActivity : AppCompatActivity(), TabListener {
  private lateinit var sectionsPagerAdapter: SectionsPagerAdapter
  lateinit var cast: CastClient

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    if (resources.getBoolean(R.bool.portrait_only))
      requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

    setContentView(R.layout.tabs)
    sectionsPagerAdapter = SectionsPagerAdapter(fragmentManager)

    container.adapter = sectionsPagerAdapter

    val actionBar = supportActionBar!!.apply {
      navigationMode = ActionBar.NAVIGATION_MODE_TABS
    }

    container.setOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
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
    container.currentItem = tab.position
  }

  override fun onTabUnselected(tab: Tab, fragmentTransaction: FragmentTransaction) {}

  override fun onTabReselected(tab: Tab, fragmentTransaction: FragmentTransaction) {}

  inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
    override fun getItem(position: Int): Fragment {
      return PhotosFragment()
    }

    override fun getCount(): Int {
      return 2
    }

    override fun getPageTitle(position: Int): CharSequence? {
      when (position) {
        0 -> return getString(R.string.tab_photos)
        1 -> return getString(R.string.tab_videos)
      }
      return null
    }
  }
}
