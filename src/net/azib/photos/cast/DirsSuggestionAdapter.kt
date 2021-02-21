package net.azib.photos.cast

import android.widget.ArrayAdapter
import android.widget.Filter
import androidx.fragment.app.FragmentActivity
import java.net.URL
import java.util.Collections.emptyList

class DirsSuggestionAdapter(context: FragmentActivity, private val receiver: Receiver, private val path: String) : ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line) {
  private var suggestions: List<String> = emptyList()

  override fun getCount() = suggestions.size
  override fun getItem(index: Int) = suggestions[index]

  fun getSuggestions(dir: CharSequence?): List<String> {
    try {
      val lastPlus = dir!!.lastIndexOf('+') + 1
      val prefix = dir.substring(0, lastPlus)
      val query = dir.substring(lastPlus)
      if (query.length <= 2) return emptyList()
      val url = URL("${receiver.url}$path?dir=$query&accessToken=${receiver.token}")
      return url.openStream().bufferedReader().useLines { it.map { prefix + it }.sortedDescending().toList() }
    } catch (e: Exception) {
      return emptyList()
    }
  }

  override fun getFilter(): Filter {
    return object : Filter() {
      override fun performFiltering(constraint: CharSequence?): FilterResults {
        val results = FilterResults()
        suggestions = getSuggestions(constraint)
        results.values = suggestions
        results.count = suggestions.size
        return results
      }

      override fun publishResults(constraint: CharSequence?, results: FilterResults) {
        if (results.count > 0)
          notifyDataSetChanged()
        else
          notifyDataSetInvalidated()
      }
    }
  }
}