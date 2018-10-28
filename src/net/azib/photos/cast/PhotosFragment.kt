package net.azib.photos.cast

import android.app.Fragment
import android.os.Bundle
import android.support.v7.app.AppCompatActivity.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*

class PhotosFragment : Fragment() {
  private val cast get() = (activity as MainActivity).cast
  private val ordering get() = if (randomSwitch.isChecked) "rnd" else "seq"
  private val inputMethodManager get() = activity.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View? {
    return inflater.inflate(R.layout.activity_main, container, false)
  }

  override fun onViewCreated(view: View?, state: Bundle?) {
    path.setAdapter(PhotoDirsSuggestionAdapter(activity))
    path.setText(state?.getString("path") ?: SimpleDateFormat("yyyy").format(Date()))
    path.setOnItemClickListener { _, _, _, _ -> castPhotos() }

    castPhotosButton.setOnClickListener { castPhotos() }

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
    styleSwitch.setOnClickListener {
      cast.sendCommand(if (styleSwitch.isChecked) "style:cover" else "style:contain")
    }
  }

  private fun assignCommand(button: Button, command: String) {
    button.setOnClickListener { cast.sendCommand(command) }
  }

  private fun castPhotos() {
    cast.sendCommand("$ordering:${path.text}")
    clearPathFocus()
  }

  private fun clearPathFocus() {
    path.clearFocus()
    inputMethodManager.hideSoftInputFromWindow(path.windowToken, 0)
  }
}