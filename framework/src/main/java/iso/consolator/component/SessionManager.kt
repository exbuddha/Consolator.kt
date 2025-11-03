package iso.consolator.component

import iso.consolator.SchedulerScope
import iso.consolator.SchedulerStep
import iso.consolator.annotation.Referred
import iso.consolator.reflect.Table

sealed interface SessionManager : Table {
    val startSessionCreation: SchedulerStep
        @Referred get() = SchedulerScope.EMPTY_STEP

    val stageSessionCreation: SchedulerStep
        @Referred get() = SchedulerScope.EMPTY_STEP

    val errorSessionCreation: SchedulerStep
        @Referred get() = SchedulerScope.EMPTY_STEP
}