package net.azib.photos.cast

import android.app.Activity
import android.widget.ArrayAdapter
import android.widget.Filter
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.util.*
import java.util.Collections.emptyList

class PhotoDirsSuggestionAdapter(context: Activity) : ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line) {
  private var suggestions: List<String> = ArrayList()
  private val accessToken = context.getString(R.string.photos_dirs_access_token)
  private val url = context.getString(R.string.photos_dirs_url)

  override fun getCount(): Int {
    return suggestions.size
  }

  override fun getItem(index: Int): String {
    return suggestions[index]
  }

  fun getSuggestions(dir: String): List<String> {
    try {
      val url = URL("$url?dir=$dir&accessToken=$accessToken")
      BufferedReader(InputStreamReader(url.openStream())).useLines { return it.toList() }
    } catch (e: Exception) {
      return emptyList()
    }
  }

  override fun getFilter(): Filter {
    return object : Filter() {
      override fun performFiltering(constraint: CharSequence?): Filter.FilterResults {
        val filterResults = Filter.FilterResults()
        if (constraint != null) {
          suggestions = getSuggestions(constraint.toString())
          filterResults.values = suggestions
          filterResults.count = suggestions.size
        }
        return filterResults
      }

      override fun publishResults(constraint: CharSequence, results: Filter.FilterResults?) {
        if (results != null && results.count > 0)
          notifyDataSetChanged()
        else
          notifyDataSetInvalidated()
      }
    }
  }
}