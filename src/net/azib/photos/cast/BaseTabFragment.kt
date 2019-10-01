package net.azib.photos.cast

import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import kotlinx.android.synthetic.main.photos.*
import java.text.SimpleDateFormat
import java.util.*

abstract class BaseTabFragment : Fragment() {
  protected val cast get() = (activity as MainActivity).cast
  private val inputMethodManager get() = activity!!.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
  protected val currentYear get() = SimpleDateFormat("yyyy").format(Date())

  protected fun assignCommand(button: Button, command: String) {
    button.setOnClickListener { cast.sendCommand(command) }
  }

  protected fun clearPathFocus() {
    path.clearFocus()
    inputMethodManager.hideSoftInputFromWindow(path.windowToken, 0)
  }
}