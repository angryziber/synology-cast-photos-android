package net.azib.photos.cast

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.photos.*

class VideosFragment : BaseTabFragment() {
  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View? {
    return inflater.inflate(R.layout.videos, container, false)
  }

  override fun onViewCreated(view: View, state: Bundle?) {
    path.setAdapter(DirsSuggestionAdapter(activity!!, getString(R.string.videos_dirs)))
    path.setText(state?.getString("path") ?: currentYear)
    path.setOnItemClickListener { _, _, _, _ -> castVideos() }
    path.setOnEditorActionListener { _, _, _ -> castVideos(); true }

    castButton.setOnClickListener { castVideos() }

    assignCommand(nextButton, "next")
    assignCommand(prevButton, "prev")
    assignCommand(pauseButton, "pause")
    assignCommand(nextMoreButton, "next:10")
    assignCommand(prevMoreButton, "prev:10")
  }

  private fun castVideos() {
    cast.sendCommand("videos:${path.text}")
    clearPathFocus()
  }
}