package net.azib.photos.cast

import android.widget.ArrayAdapter
import android.widget.Filter
import androidx.fragment.app.FragmentActivity
import java.net.URL
import java.util.Collections.emptyList

class DirsSuggestionAdapter(context: FragmentActivity, val receiver: Receiver, val urlSuffix: String) : ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line) {
  private var suggestions: List<String> = emptyList()

  override fun getCount() = suggestions.size
  override fun getItem(index: Int) = suggestions[index]

  fun getSuggestions(dir: CharSequence?): List<String> {
    try {
      val url = URL("${receiver.url}$urlSuffix?dir=$dir&accessToken=${receiver.token}")
      url.openStream().bufferedReader().useLines { return it.toList().sortedDescending() }
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