package iso.consolator.component

import iso.consolator.AnyCoroutineStep
import iso.consolator.Resolver
import iso.consolator.Routine
import iso.consolator.State
import iso.consolator.applicationMigrationManager
import iso.consolator.foregroundFragment
import iso.consolator.fulfill
import iso.consolator.withSchedulerScope
import iso.consolator.EMPTY_COROUTINE
import iso.consolator.annotation.Referred
import iso.consolator.reflect.Table

abstract class MigrationManager : Resolver {
    interface Transactor : Table {
        val startMigration
            @Referred get() = EMPTY_COROUTINE
    }

    override fun commit(step: AnyCoroutineStep) =
        super.commit(withSchedulerScope {
            step then { _, job ->
                ((State of job) as? Routine)
                ?.fulfill() } } as AnyCoroutineStep)

    override fun commit(vararg context: Any?) =
        when (context.firstOrNull()) {
            TransitionManager::class ->
                foregroundFragment.asTransitionManager()
                ?.commit(context[1])
            else -> null }

    var progress: Byte = 0
        get() = field.toPercentage()
        protected set

    private fun Byte.toPercentage() =
        times(100).div(Byte.MAX_VALUE).toByte()

    internal fun expire() = ::applicationMigrationManager.expire()
}

typealias ApplicationMigrationManager = MigrationManager