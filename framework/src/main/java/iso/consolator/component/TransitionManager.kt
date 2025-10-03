package iso.consolator.component

import iso.consolator.Resolver
import iso.consolator.SchedulerScope
import iso.consolator.TransitType
import iso.consolator.asType
import iso.consolator.asTypeUnsafe
import iso.consolator.reflect.Table

sealed interface TransitionManager : Resolver {
    interface MainViewNavigator : TransitionManager, Table {
        val startMainViewNavigation
            get() = SchedulerScope.EMPTY_STEP

        val abortMainViewNavigation
            get() = SchedulerScope.EMPTY_STEP

        val timeoutMainViewNavigation
            get() = SchedulerScope.EMPTY_STEP
    }

    /**
     * Starts navigation to [destination].
     *
     * @param destination the destination.
     */
    fun commit(destination: Short) = Unit

    override fun commit(vararg context: Any?) =
        commit(destination = context[0].asTypeUnsafe<TransitType>())
}

internal fun Any?.asTransitionManager() = asType<TransitionManager>()