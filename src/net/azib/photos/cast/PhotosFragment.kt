package net.azib.photos.cast

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.photos.*
import org.json.JSONObject

class PhotosFragment : BaseTabFragment() {
  private val ordering get() = if (randomSwitch.isChecked) "rnd" else "seq"

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View? {
    return inflater.inflate(R.layout.photos, container, false)
  }

  override fun onViewCreated(view: View, state: Bundle?) {
    path.setAdapter(DirsSuggestionAdapter(activity!!, cast.receiver, getString(R.string.suggestPhotosPath)))
    path.setText(state?.getString("path") ?: currentYear)
    path.setOnItemClickListener { _, _, _, _ -> sendPath() }
    path.setOnEditorActionListener { _, _, _ -> sendPath(); true }

    castButton.setOnClickListener { sendPath() }

    interval.addTextChangedListener(object : TextWatcher {
      override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
      override fun afterTextChanged(p0: Editable?) {}
      override fun onTextChanged(v: CharSequence?, p1: Int, p2: Int, p3: Int) {
        if (v?.isNotEmpty() == true) cast.sendCommand("interval:$v")
      }
    })

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
      cast.sendCommand(ordering)
    }
    photosSwitch.setOnClickListener {
      cast.sendCommand((if (photosSwitch.isChecked) "show" else "hide") + ":photos")
    }
    videosSwitch.setOnClickListener {
      cast.sendCommand((if (videosSwitch.isChecked) "show" else "hide") + ":videos")
    }
    modeSwitch.setOnClickListener {
      cast.sendCommand(if (modeSwitch.isChecked) "mode:video" else "mode:img")
    }
    styleSwitch.setOnClickListener {
      cast.sendCommand(if (styleSwitch.isChecked) "style:cover" else "style:contain")
    }
    mapSwitch.setOnClickListener {
      cast.sendCommand(if (mapSwitch.isChecked) "show:map" else "hide:map")
    }
  }

  private fun sendPath() {
    cast.sendCommand("$ordering:${path.text}")
    clearPathFocus()
  }

  fun updateState(state: JSONObject) {
    state.optString("path").let { if (it.isNotEmpty()) path.setText(it) }
    randomSwitch.isChecked = state.getBoolean("random")
    modeSwitch.isChecked = state.getString("mode") == "video"
    interval.setText(state.getString("interval"))
    styleSwitch.isChecked = state.getString("style") == "cover"
    mapSwitch.isChecked = state.optBoolean("map")
    photosSwitch.isChecked = state.getBoolean("photos")
    videosSwitch.isChecked = state.getBoolean("videos")
  }
}