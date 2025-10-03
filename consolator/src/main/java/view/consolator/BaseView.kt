package view.consolator

import android.content.*
import android.view.*

internal abstract class BaseView(context: Context) : View(context), ViewProvider {
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // expire internal resources
    }
}