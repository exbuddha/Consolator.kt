package view.consolator

import android.content.*
import android.view.*
import iso.consolator.*

internal abstract class BaseViewGroup(context: Context) : ViewGroup(context), ViewProvider {
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) = Unit

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // expire internal resources
    }

    protected open lateinit var controller: ViewStateCoordinator
}