package view.consolator

import android.content.*
import android.os.*
import android.view.*
import iso.consolator.*

@Suppress("UNCHECKED_CAST")
internal abstract class MainViewGroup(context: Context) : BaseViewGroup(context), StateCoordinator by State, ViewReceiver {
    constructor(inflater: LayoutInflater, layout: ViewGroup, savedInstanceState: Bundle?) : this(layout.context)

    override fun onDetachedFromWindow() {
        (State of this)!![-2] = State.Ambiguous
        super.onDetachedFromWindow()
    }

    // convert view to context parameter
    protected fun <T : ViewStateCoordinator, V : MainViewGroup> T.revise(view: V, state: ViewState) =
        with(view) {
        determine(state,
            { view.onViewStateChanged(it) },
            { view.onViewStateChanged(it) }) }

    // include descriptor as context parameter
    protected inline fun <T : ViewStateCoordinator, S : ViewState, R> T.determine(state: S, group: GroupController.(S) -> R, shared: SharedController.(S) -> R) =
        when (this) {
            is ViewCoordinator<*,*> ->
                (this as GroupController).group(state)
            else ->
                (this as SharedController).shared(state) }

    override lateinit var descriptor: DirectViewStateDescriptor
}