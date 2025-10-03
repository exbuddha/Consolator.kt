@file:JvmName("ViewController")
@file:JvmMultifileClass

package view.consolator

import android.view.ViewGroup
import iso.consolator.ViewCoordinator
import iso.consolator.ViewGroupCoordinator
import iso.consolator.ViewState

internal typealias GroupController = ViewCoordinator<ViewGroup, ViewState>
internal typealias SharedController = ViewGroupCoordinator<ViewGroup, ViewState>