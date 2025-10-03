package view.consolator

import androidx.annotation.LayoutRes
import iso.consolator.annotation.Key

interface ContentLayoutReceiver {
    @Key(0)
    fun setContentLayoutId(@LayoutRes contentLayoutId: Int) {
        net.consolator.contentLayoutId = contentLayoutId }
}