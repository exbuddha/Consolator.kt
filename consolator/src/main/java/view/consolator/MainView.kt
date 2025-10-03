package view.consolator

import android.content.*
import android.os.*
import android.view.*

internal abstract class MainView(context: Context) : BaseView(context), ViewReceiver {
    constructor(inflater: LayoutInflater, layout: ViewGroup, savedInstanceState: Bundle?) : this(layout.context)
}