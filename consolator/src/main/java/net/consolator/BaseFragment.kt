package net.consolator

import android.content.*
import android.os.*
import android.view.*
import androidx.fragment.app.*
import androidx.lifecycle.*
import ctx.consolator.asWeakReference
import iso.consolator.*
import iso.consolator.from
import iso.consolator.Event.*
import iso.consolator.Event.Listening.*
import iso.consolator.SchedulerScope.Companion.buildRuntimeDatabase
import iso.consolator.SchedulerScope.Companion.buildSession
import iso.consolator.SchedulerScope.Companion.change
import iso.consolator.SchedulerScope.Companion.close
import iso.consolator.SchedulerScope.Companion.defer
import iso.consolator.SchedulerScope.Companion.error
import iso.consolator.SchedulerScope.Companion.isDatabaseCreated
import iso.consolator.SchedulerScope.Companion.isSessionCreated
import iso.consolator.SchedulerScope.Companion.keepAliveOrClose
import iso.consolator.SchedulerScope.Companion.line
import iso.consolator.SchedulerScope.Companion.resolve
import iso.consolator.SchedulerScope.Companion.stageRuntimeDatabaseCreated
import iso.consolator.SchedulerScope.Companion.stageSessionCreated
import iso.consolator.SchedulerScope.Companion.tryCanceling
import iso.consolator.SchedulerScope.Companion.tryCancelingWithContext
import iso.consolator.SchedulerScope.Companion.RESOLVE
import iso.consolator.SchedulerScope.Companion.RETRY
import iso.consolator.State.*
import iso.consolator.annotation.*
import iso.consolator.annotation.LaunchContext.Type.*
import iso.consolator.annotation.Path.*
import iso.consolator.annotation.Pathwise.*
import iso.consolator.component.SchedulerFragment
import iso.consolator.component.TransitionManager
import kotlin.annotation.AnnotationRetention.*
import kotlin.annotation.AnnotationTarget.*
import kotlinx.coroutines.*
import kotlinx.coroutines.CoroutineStart.LAZY
import net.consolator.BaseActivity.Companion.ABORT_NAV_MAIN_UI
import net.consolator.BaseActivity.Companion.COMMIT_NAV_MAIN_UI
import net.consolator.BaseApplication.Companion.ACTION_MIGRATE_APP

/**
 * Adds support for database migration and main view transition.
 */
internal open class BaseFragment : SchedulerFragment(contentLayoutId), TransitionManager.MainViewNavigator {
    init { setTag(this::class.tag) }

    /** @suppress
     *
     * Starts and stages the database and session creation.
     */
    @Tag(ON_ATTACH)
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (State[1] is Resolved) return
        val context = context.asWeakReference()
        trySafely { currentThread
        .from(
            ON_ATTACH,
            CallableSchedulerScope) {
        call(
            ::rememberContext, context)
        .commit { _, job -> job
        .then(
            ::startDatabaseCreation)
        .then(
            ::stageDatabaseCreation)
        .given(
            ::isDatabaseCreated)
        .otherwiseImplicitly(
            RETRY)
        .then(
            ::startSessionCreation)
        .then(
            ::stageSessionCreation)
        .given(
            ::isSessionCreated)
        .otherwiseImplicitly(
            RETRY)
        .onError(
            ::errorSessionCreation)
        .onCancelImplicitly(
            RETRY)
        .then(
            ::resumeTrack
    ) } } } }

    /** @suppress
     *
     * Starts the database migration process and the main view navigation.
     */
    @Tag(ON_VIEW_CREATED)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (State[1] is Resolved) return
        trySafely { currentThread
        .from(
            ON_VIEW_CREATED,
            CallableSchedulerScope) {
        call(
            ::startMigration)
        .commit { _, job -> job
        .otherwise(
            ::startMainViewNavigation)
        .onError(
            ::abortMainViewNavigation)
        .onTimeout(
            ::timeoutMainViewNavigation)
        .thenImplicitly(
            RESOLVE
    ) } } } }

    /** @suppress
     *
     * Reattaches the main view group track to the scope.
     */
    override fun onResume() {
        if (State[1] !is Resolved)
            withSchedulerScope {
            reattach(MainViewGroup::class) }
        super.onResume()
    }

    /** @suppress
     *
     * Detaches the main view group from the scope.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        withSchedulerScope {
        close(MainViewGroup::class) }
    }

    @LaunchContext(IO) @LaunchMode(LAZY)
    @JobTreeRoot @MainViewGroup
    @Resolving.State(1)
    @Retrying @Pathwise([ FromLastCancellation::class ])
    @DelayTime(view_min_delay)
    @WithContext @Tag(ON_ATTACH)
    override val rememberContext = SchedulerResumption { _, context ->
        context /* auto-register */ }

    @Parallel @Path("$STAGE_BUILD_RUN_DB")
    override val startDatabaseCreation = SchedulerFunction { _, _, _ ->
        tryCancelingWithContext(::buildRuntimeDatabase) }

    @Committing @Event(ACTION_MIGRATE_APP)
    override val stageDatabaseCreation = SchedulerFunction { _, _, _ ->
        change(::stageRuntimeDatabaseCreated) }

    @Path("$STAGE_BUILD_SESSION")
    override val startSessionCreation = SchedulerFunction { _, _, _ ->
        tryCanceling(::buildSession) }

    @Committing @Event(ACTION_MIGRATE_APP)
    override val stageSessionCreation = SchedulerFunction { _, _, _ ->
        change(::stageSessionCreated) }

    @Committing @Line
    override val errorSessionCreation = SchedulerFunction { _, _, _ ->
        State[1] = Ambiguous }

    @Resolving @Track.Item
    override fun resumeTrack(scope: CoroutineScope?, context: Any?, job: Job) {
        scope?.resolve(map = arrayOf(
            /* catch cancellation and/or error */
            { err: Throwable? -> when (err) {
                is CancellationException -> true
                else -> false } }
            to {
                errorSessionCreation(scope, context, job) }
        ), job) }

    @LaunchScope
    @LaunchMode(LAZY)
    @MainViewGroup
    @Listening @OnEvent(ACTION_MIGRATE_APP)
    override val startMigration: CoroutineStep = {
        defer<MigrationManager, _> {
            // listen to db updates
            // preload data
            // reset function pointers
            // repeat until stable
            commit(
                TransitionManager::class,
                COMMIT_NAV_MAIN_UI) } }

    @OnEvent(COMMIT_NAV_MAIN_UI)
    override val startMainViewNavigation = SchedulerFunction { _, _, _ ->
        defer<TransitionManager, _> {
            State[1] = Succeeded
            close(MainViewGroup::class)
            commit(COMMIT_NAV_MAIN_UI) } }

    @OnEvent(ABORT_NAV_MAIN_UI)
    override val abortMainViewNavigation = SchedulerFunction { _, _, job ->
        defer<TransitionManager, _> {
            State[1] = Failed
            keepAliveOrClose(job)
            commit(ABORT_NAV_MAIN_UI) } }

    @Committing
    override val timeoutMainViewNavigation = SchedulerFunction { _, _, job ->
        line { State[1] = Unresolved }
        error(job) }

    internal companion object {
        const val VIEW_TAG = "FRAGMENT"
    }
}