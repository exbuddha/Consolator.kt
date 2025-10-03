package iso.consolator.component

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.annotation.LayoutRes
import iso.consolator.GroupCoordinator
import iso.consolator.SharedCoordinator
import iso.consolator.ViewCoordinator
import iso.consolator.ViewGroupCoordinator
import iso.consolator.ViewState
import iso.consolator.withSchedulerScope
import iso.consolator.annotation.Scope
import iso.consolator.annotation.Tag
import kotlin.annotation.AnnotationRetention.*
import kotlin.annotation.AnnotationTarget.*

@Scope
abstract class SchedulerFragment(@LayoutRes contentLayoutId: Int) : Fragment(contentLayoutId),
    ContextManager.Tracker, ContextManager.DatabaseCreator, SessionManager {
    protected constructor(@LayoutRes contentLayoutId: Int, transit: SchedulerFragment) : this(contentLayoutId) {
        // re-establish communication with job controller and view models
    }

    /**
     * Sets the fragment tag.
     *
     * @param tag the tag annotation.
     */
    protected fun setTag(tag: Tag?) {
        /* register in source */ }

    /** @suppress */
    override fun onAttach(context: Context) {
        super.onAttach(context)
        withSchedulerScope {
        commitAttach(context) }
    }

    /** @suppress */
    override fun onStart() {
        super.onStart()
        withSchedulerScope {
        commitStart() }
    }

    /** @suppress */
    override fun onResume() {
        super.onResume()
        withSchedulerScope {
        commitResume() }
    }

    /** @suppress */
    override fun onPause() {
        super.onPause()
        withSchedulerScope {
        commitPause() }
    }

    /** @suppress */
    override fun onStop() {
        withSchedulerScope {
        commitStop() }
        super.onStop()
    }

    /** @suppress */
    override fun onSaveInstanceState(outState: Bundle) {
        // write view-coordinator state to bundle
        withSchedulerScope {
        commitSaveInstanceState(outState) }
        super.onSaveInstanceState(outState)
    }

    /** @suppress */
    override fun onDestroyView() {
        withSchedulerScope {
        commitDestroyView() }
        super.onDestroyView()
    }

    /** @suppress */
    override fun onDestroy() {
        withSchedulerScope {
        commitDestroy() }
        super.onDestroy()
    }

    /** @suppress */
    override fun onDetach() {
        withSchedulerScope {
        commitDetach() }
        super.onDetach()
    }

    protected val groupCoordinator: ViewCoordinator<ViewGroup, ViewState>
        get() = run(::GroupCoordinator)

    protected val sharedCoordinator: ViewGroupCoordinator<ViewGroup, ViewState>
        get() = run(::SharedCoordinator)

    @Retention(SOURCE)
    @Target(FUNCTION, PROPERTY)
    protected annotation class MainViewGroup

    /** @suppress */
    protected abstract inner class MigrationManager : iso.consolator.component.MigrationManager()
}