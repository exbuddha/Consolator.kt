package iso.consolator.component

import iso.consolator.SchedulerScope
import iso.consolator.annotation.Referred
import iso.consolator.reflect.Table

sealed interface SessionManager : Table {
    val startSessionCreation
        @Referred get() = SchedulerScope.EMPTY_STEP

    val stageSessionCreation
        @Referred get() = SchedulerScope.EMPTY_STEP

    val errorSessionCreation
        @Referred get() = SchedulerScope.EMPTY_STEP
}