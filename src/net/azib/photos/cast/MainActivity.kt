package net.azib.photos.cast

import android.content.Context
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
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import butterknife.bindView
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    val randomSwitch: Switch by bindView(R.id.randomSwitch)
    val styleSwitch: Switch by bindView(R.id.styleSwitch)
    val path: AutoCompleteTextView by bindView(R.id.photosPathEdit)
    val castPhotosButton: Button by bindView(R.id.castPhotosButton)
    val status: TextView by bindView(R.id.status)
    val gestureDetector: GestureDetector by lazy { GestureDetector(this, GestureListener()) }

    companion object {
        @JvmStatic
        internal var cast: CastClient? = null
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)

        if (resources.getBoolean(R.bool.portrait_only))
            requestedOrientation = SCREEN_ORIENTATION_PORTRAIT

        setContentView(R.layout.activity_main)

        if (cast == null)
            cast = CastClient(this)
        else
            cast!!.activity = this

        path.setAdapter(PhotoDirsSuggestionAdapter(this))
        path.setText(if (state != null) state.getString("path") else SimpleDateFormat("yyyy").format(Date()))
        path.setOnItemClickListener { parent, view, pos, id -> castPhotos() }

        castPhotosButton.setOnClickListener { castPhotos() }

        assignCommand(R.id.next_button, "next")
        assignCommand(R.id.prev_button, "prev")
        assignCommand(R.id.pause_button, "pause")
        assignCommand(R.id.next_more_button, "next:10")
        assignCommand(R.id.prev_more_button, "prev:10")
        assignCommand(R.id.mark_delete_button, "mark:delete")
        assignCommand(R.id.mark_red_button, "mark:red")
        assignCommand(R.id.mark_yellow_button, "mark:yellow")
        assignCommand(R.id.mark_green_button, "mark:green")
        assignCommand(R.id.mark_blue_button, "mark:blue")
        assignCommand(R.id.mark_0_button, "mark:0")
        assignCommand(R.id.mark_1_button, "mark:1")
        assignCommand(R.id.mark_2_button, "mark:2")
        assignCommand(R.id.mark_3_button, "mark:3")
        assignCommand(R.id.mark_4_button, "mark:4")
        assignCommand(R.id.mark_5_button, "mark:5")

        randomSwitch.setOnClickListener { cast!!.sendCommand(if (randomSwitch.isChecked) "rnd" else "seq") }

        styleSwitch.setOnClickListener { cast!!.sendCommand(if (styleSwitch.isChecked) "style:cover" else "style:contain") }
    }

    class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent) = true

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            if (Math.abs(velocityX) < Math.abs(velocityY)) return false
            if (velocityX > 0)
                cast!!.sendCommand("prev")
            else
                cast!!.sendCommand("next")
            return true
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val command = intent!!.action
        if (command != null) cast!!.sendCommand(command)
    }

    override fun onSaveInstanceState(state: Bundle) {
        super.onSaveInstanceState(state)
        state.putString("path", path.text.toString())
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    private fun assignCommand(buttonId: Int, command: String) {
        val button = findViewById(buttonId) as Button
        button.setOnClickListener { cast!!.sendCommand(command) }
    }

    private fun castPhotos() {
        cast!!.sendCommand((if (randomSwitch.isChecked) "rnd:" else "seq:") + path.text)
        path.clearFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(path.windowToken, 0)
    }

    override fun onResume() {
        super.onResume()
        cast!!.startDiscovery()
    }

    override fun onPause() {
        if (isFinishing) cast!!.stopDiscovery()
        super.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.main, menu)
        val mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item)
        val mediaRouteActionProvider = MenuItemCompat.getActionProvider(mediaRouteMenuItem) as MediaRouteActionProvider
        // Set the MediaRouteActionProvider selector for device discovery.
        mediaRouteActionProvider.routeSelector = cast!!.mediaRouteSelector
        return true
    }

    fun onMessageReceived(vararg parts: String) {
        status.text = parts[0]
        if (parts.size == 2)
            status.setOnClickListener { startActivity(Intent(ACTION_VIEW, Uri.parse(parts[1]))) }
        else
            status.isClickable = false
    }
}
