@file:JvmName("Provider")
@file:JvmMultifileClass

package view.consolator

import iso.consolator.ViewState
import iso.consolator.reflect.PropertyArray

interface ViewProvider : PropertyArray<Int> {
    override operator fun <T> PropertyArray<Int>.get(key: Int?): T
}

internal fun Any.asViewState() = this as ViewState