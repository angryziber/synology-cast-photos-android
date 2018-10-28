package net.azib.photos.cast

import android.app.Fragment
import android.os.Bundle
import android.support.v7.app.AppCompatActivity.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import kotlinx.android.synthetic.main.photos.*
import java.text.SimpleDateFormat
import java.util.*

class VideosFragment : Fragment() {
  private val cast get() = (activity as MainActivity).cast
  private val inputMethodManager get() = activity.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View? {
    return inflater.inflate(R.layout.videos, container, false)
  }

  override fun onViewCreated(view: View?, state: Bundle?) {
    path.setText(state?.getString("path") ?: SimpleDateFormat("yyyy").format(Date()))
    path.setOnItemClickListener { _, _, _, _ -> castVideos() }

    castButton.setOnClickListener { castVideos() }

    assignCommand(nextButton, "next")
    assignCommand(prevButton, "prev")
    assignCommand(pauseButton, "pause")
    assignCommand(nextMoreButton, "next:10")
    assignCommand(prevMoreButton, "prev:10")
  }

  private fun assignCommand(button: Button, command: String) {
    button.setOnClickListener { cast.sendCommand(command) }
  }

  private fun castVideos() {
    cast.sendCommand("videos:${path.text}")
    clearPathFocus()
  }

  private fun clearPathFocus() {
    path.clearFocus()
    inputMethodManager.hideSoftInputFromWindow(path.windowToken, 0)
  }
}