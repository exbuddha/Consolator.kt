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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        withSchedulerScope {
        commitAttach(context) }
    }

    override fun onStart() {
        super.onStart()
        withSchedulerScope {
        commitStart() }
    }

    override fun onResume() {
        super.onResume()
        withSchedulerScope {
        commitResume() }
    }

    override fun onPause() {
        super.onPause()
        withSchedulerScope {
        commitPause() }
    }

    override fun onStop() {
        withSchedulerScope {
        commitStop() }
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // write view-coordinator state to bundle
        withSchedulerScope {
        commitSaveInstanceState(outState) }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        withSchedulerScope {
        commitDestroyView() }
        super.onDestroyView()
    }

    override fun onDestroy() {
        withSchedulerScope {
        commitDestroy() }
        super.onDestroy()
    }

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

    protected abstract inner class MigrationManager : iso.consolator.component.MigrationManager()
}