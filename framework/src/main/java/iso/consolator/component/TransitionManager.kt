package iso.consolator.component

import iso.consolator.Resolver
import iso.consolator.SchedulerScope
import iso.consolator.SchedulerStep
import iso.consolator.TransitType
import iso.consolator.asType
import iso.consolator.asTypeUnsafe
import iso.consolator.reflect.Table

sealed interface TransitionManager : Resolver {
    interface MainViewNavigator : TransitionManager, Table {
        val startMainViewNavigation: SchedulerStep
            get() = SchedulerScope.EMPTY_STEP

        val abortMainViewNavigation: SchedulerStep
            get() = SchedulerScope.EMPTY_STEP

        val timeoutMainViewNavigation: SchedulerStep
            get() = SchedulerScope.EMPTY_STEP
    }

    /**
     * Starts navigation to [destination].
     *
     * @param destination the destination.
     */
    fun commit(destination: Short): Unit = TODO()

    override fun commit(vararg context: Any?): Unit =
        commit(destination = context[0].asTypeUnsafe<TransitType>())
}

internal fun Any?.asTransitionManager(): TransitionManager? = asType<TransitionManager>()