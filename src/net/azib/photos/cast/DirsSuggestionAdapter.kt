package net.azib.photos.cast

import android.widget.ArrayAdapter
import android.widget.Filter
import androidx.fragment.app.FragmentActivity
import java.net.URL
import java.util.Collections.emptyList

class DirsSuggestionAdapter(context: FragmentActivity, val appId: AppId, val urlSuffix: String) : ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line) {
  private var suggestions: List<String> = emptyList()

  override fun getCount() = suggestions.size
  override fun getItem(index: Int) = suggestions[index]

  fun getSuggestions(dir: CharSequence?): List<String> {
    try {
      val url = URL("${appId.url}$urlSuffix?dir=$dir&accessToken=${appId.token}")
      url.openStream().bufferedReader().useLines { return it.toList() }
    } catch (e: Exception) {
      return emptyList()
    }
  }

  override fun getFilter(): Filter {
    return object : Filter() {
      override fun performFiltering(constraint: CharSequence?): Filter.FilterResults {
        val results = Filter.FilterResults()
        suggestions = getSuggestions(constraint)
        results.values = suggestions
        results.count = suggestions.size
        return results
      }

      override fun publishResults(constraint: CharSequence?, results: Filter.FilterResults) {
        if (results.count > 0)
          notifyDataSetChanged()
        else
          notifyDataSetInvalidated()
      }
    }
  }
}