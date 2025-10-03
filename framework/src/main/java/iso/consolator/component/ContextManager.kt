package iso.consolator.component

import iso.consolator.SchedulerResumption
import iso.consolator.SchedulerScope
import iso.consolator.rejectWithIllegalStateException
import iso.consolator.annotation.Referred
import iso.consolator.reflect.Table
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

sealed interface ContextManager : Table {
    sealed interface DatabaseCreator : ContextManager, MigrationManager.Transactor {
        val startDatabaseCreation
            @Referred get() = SchedulerScope.EMPTY_STEP

        val stageDatabaseCreation
            @Referred get() = SchedulerScope.EMPTY_STEP

        // optionally, include default scope as context parameter - will add security checkpoint
        val errorDatabaseCreation
            @Referred get() = SchedulerScope.EMPTY_STEP
    }

    fun interface Tracker : Table {
        @Referred fun resumeTrack(scope: CoroutineScope?, context: Any?, job: Job)
    }

    val rememberContext: SchedulerResumption
        @Referred get() = rejectWithIllegalStateException()
}