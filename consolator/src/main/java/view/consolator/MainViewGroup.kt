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

    context(_: DirectViewStateDescriptor, view: V)
    protected fun <T : ViewStateCoordinator, V : MainViewGroup> T.revise(state: ViewState) =
        with(view) { with(state) {
        determine(
            { onViewStateChanged(it) },
            { onViewStateChanged(it) }) } }

    context(_: DirectViewStateDescriptor, state: S)
    private inline fun <T : ViewStateCoordinator, S : ViewState, R> T.determine(group: GroupController.(S) -> R, shared: SharedController.(S) -> R) =
        when (this) {
            is ViewCoordinator<*,*> ->
                (this as GroupController).group(state)
            else ->
                (this as SharedController).shared(state) }

    override lateinit var descriptor: DirectViewStateDescriptor
}