package net.azib.photos.cast

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.photos.*

class PhotosFragment : BaseTabFragment() {
  private val ordering get() = if (randomSwitch.isChecked) "rnd" else "seq"

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View? {
    return inflater.inflate(R.layout.photos, container, false)
  }

  override fun onViewCreated(view: View, state: Bundle?) {
    path.setAdapter(DirsSuggestionAdapter(activity!!, getString(R.string.photos_dirs)))
    path.setText(state?.getString("path") ?: currentYear)
    path.setOnItemClickListener { _, _, _, _ -> castPhotos() }
    path.setOnEditorActionListener { _, _, _ -> castPhotos(); true }

    castButton.setOnClickListener { castPhotos() }

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
    modeSwitch.setOnClickListener {
      cast.sendCommand(if (modeSwitch.isChecked) "mode:video" else "mode:img")
    }
    styleSwitch.setOnClickListener {
      cast.sendCommand(if (styleSwitch.isChecked) "style:cover" else "style:contain")
    }
  }

  private fun castPhotos() {
    cast.sendCommand("photos:$ordering:${path.text}")
    clearPathFocus()
  }
}