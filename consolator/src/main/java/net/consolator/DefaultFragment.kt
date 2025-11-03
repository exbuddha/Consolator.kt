package net.consolator

import android.content.*
import android.os.*
import android.view.*
import androidx.annotation.*
import androidx.fragment.app.*
import androidx.lifecycle.*
import ctx.consolator.*
import iso.consolator.*
import iso.consolator.annotation.*
import iso.consolator.component.ApplicationMigrationManager
import iso.consolator.component.SchedulerFragment
import iso.consolator.component.TransitionManager
import iso.consolator.exception.*
import kotlin.reflect.*
import kotlinx.coroutines.*
import view.consolator.*
import androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN
import net.consolator.BaseActivity.Companion.ABORT_NAV_MAIN_UI
import net.consolator.BaseActivity.Companion.COMMIT_NAV_MAIN_UI
import net.consolator.DefaultFragment.ActiveFragment.Companion.synchronizer
import net.consolator.DefaultFragment.ReactiveFragment

/**
 * Default scheduler fragment handles main view transition.
 */
@Coordinate
@Tag(DEFAULT_FRAGMENT)
internal open class DefaultFragment : BaseFragment(), LifecycleProvider {
    /**
     * Removes and adds the main view.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        inflater.inflate(contentLayoutId, container, false).also { view ->
        if (container !== null)
            removeThenAddMainView(view, inflater, container, savedInstanceState) }

    private fun removeThenAddMainView(view: View, inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle?) {
        if (container.id `is` view.id)
            container.removeView(view)
        if (view is ViewGroup)
            view.addView(
                object : MainView(inflater, container, savedInstanceState) {
                    // enables round-trip of inner items for interception
                    override fun <T> ItemArray.get(key: Int?): T = TODO()
                    override fun <T> set(key: Int?, value: T) = TODO()
                }) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(savedInstanceState) {
        initOrRestore(
            { begin(view, this) },
            { restore(view, this) }) }
    }

    private fun begin(view: View, savedInstanceState: Bundle? = null) {
        if (activity.typeIs<DefaultActivity, _>()) {
        parentFragmentManager.commit {
            show(this@DefaultFragment) }
        withSchedulerScope {
        currentThread.log(info, VIEW_TAG, "Main fragment view is created.") } } }

    private fun restore(view: View, savedInstanceState: Bundle) {
        if (activity.typeIs<DefaultActivity, _>()) {
        /* resume past work */ } }

    /** Replaces the fragment with main fragment. */
    private fun transit(body: FragmentTransaction.() -> Unit = {
        setTransition(TRANSIT_FRAGMENT_OPEN)
        with(this@DefaultFragment) {
        replace(id,
            run(::MainFragment)) } }) {
        @NoItem fun step() {
            parentFragmentManager.commit(body = body) }
        schedule(::step) }

    /** Empty. */
    private fun cancel() = Unit

    override fun commit(destination: Short) { when (destination) {
        COMMIT_NAV_MAIN_UI ->
            transit()
        ABORT_NAV_MAIN_UI ->
            cancel() } }

    override fun onDestroy() {
        onDestroyTail?.invoke()
        super.onDestroy()
    }

    private var onDestroyTail: Work? = null

    /**
     * Main scheduler fragment handles main view group communication.
     */
    @Tag(MAIN_FRAGMENT)
    class MainFragment(default: DefaultFragment? = null) : SchedulerFragment(contentLayoutId, default as SchedulerFragment),
        CrossProvider by default ?: object : InactiveCrossProvider {
            override var isCrossObjectProviderEnabled = false
            override var isCrossFunctionProviderEnabled = false }
    {
        init { default?.apply {
            /* save view state before transition or replace tag and owner in scheduler scope */
            onDestroyTail = { with(this@MainFragment) {
                isCrossObjectProviderEnabled = true
                isCrossFunctionProviderEnabled = true } } } }

        /**
         * Adds the main view group.
         */
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
            super.onCreateView(inflater, container, savedInstanceState)?.also { view ->
            addMainViewGroup(view, inflater, container, savedInstanceState) }

        override fun onDestroyView() {
            synchronizer = null
            super.onDestroyView()
        }

        private fun addMainViewGroup(view: View, inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) {
            if (container?.id == view.id &&
                view is ViewGroup)
                view.addView(
                    object : DefaultViewGroup(inflater, container, savedInstanceState) {
                        // enables round-trip of inner views or item for interceptions
                        override fun <T> ItemArray.get(key: Int?): T = TODO()
                        override fun <T> set(key: Int?, value: T) = TODO()

                        override var controller: ViewStateCoordinator = groupCoordinator

                        init {
                            let { view -> with(State) {
                            accept(view, view::descriptor) {
                                run<_, ViewState>(::initial)
                                .register(savedInstanceState)
                                .also { with(view.descriptor) {
                                    it.asViewState()
                                    .onValueTypeChanged(State.Resolved::class) {
                                    /* assign next state */
                                    with(view) { controller.revise(it) }
                            } } } }
                            (controller as ViewModelConnector)
                            .attach(VM) } } }
                    }
                    .apply(::synchronizer::set)) }

        override fun resumeTrack(scope: CoroutineScope?, context: Any?, job: Job) {
            withCallableScope { callSelfReferring(::resumeTrack) } }

        override fun provide(type: AnyKClass) = when (type) {
            ApplicationMigrationManager::class ->
                object : MigrationManager() {}
            else ->
                blockOnTrueOrRestrict(
                    LifecycleOwner::isForegroundProvider) {
                defaultActivity.crossProvide(type) } }

        override fun <R> provide(vararg tag: TagType) =
            blockOnTrueOrRestrict(
                LifecycleOwner::isForegroundProvider) {
            defaultActivity.crossProvide<R>(*tag) }

        private val defaultActivity get() = activity as DefaultActivity
    }

    /**
     * Active scheduler fragment communicates with state synchronizer.
     */
    abstract class ActiveFragment(@LayoutRes contentLayoutId: Int) : Fragment(contentLayoutId) {
        constructor(main: DefaultFragment? = null) : this(contentLayoutId)

        companion object {
            /** The state synchronizer. */
            @JvmStatic var synchronizer: StateCoordinator? = null
                set(value) { field = when (value) {
                    is DefaultViewGroup -> {
                        /* initialize synchronizer */
                        field }
                    is View -> {
                        /* synchronize with default view group */
                        field }
                    else -> {
                        /* clear internal resources when context is default fragment */
                        null } } }
    } }

    /**
     * Reactive scheduler fragment handles lifecycle callbacks and controls view sections.
     *
     * @constructor Creates a reactive fragment.
     */
    open class ReactiveFragment(main: DefaultFragment? = null) : ActiveFragment(main) {
        protected constructor(main: DefaultFragment,
            onAttach: ContextWork? = null,
            onStart: FragmentWork? = null,
            onResume: FragmentWork? = null,
            onPause: FragmentWork? = null,
            onStop: FragmentWork? = null,
            onSaveInstanceState: BundleWork?,
            onDestroyView: FragmentWork? = null,
            onDestroy: Work? = null,
            onDetach: Work? = null
        ) : this(main) {
            onAttachInit = onAttach
            onStartInit = onStart
            onResumeInit = onResume
            onPauseTail = onPause
            onStopTail = onStop
            onSaveInstanceStateInit = onSaveInstanceState
            onDestroyViewTail = onDestroyView
            onDestroyTail = onDestroy
            onDetachTail = onDetach
        }

        override fun onAttach(context: Context) {
            ::onAttachInit.relayWork(context) {
            super.onAttach(context) } }

        override fun onStart() {
            ::onStartInit.relayWork {
            super.onStart() } }

        override fun onResume() {
            ::onResumeInit.relayWork {
            super.onResume() } }

        override fun onPause() {
            ::onPauseTail.relayWork {
            super.onPause() } }

        override fun onStop() {
            ::onStopTail.relayWork {
            super.onStop() } }

        override fun onSaveInstanceState(outState: Bundle) {
            ::onSaveInstanceStateInit.relayWork(outState) {
            super.onSaveInstanceState(it) } }

        override fun onDestroyView() {
            ::onDestroyViewTail.relayWork {
            super.onDestroyView() } }

        override fun onDestroy() {
            onDestroyTail?.invoke()
            super.onDestroy() }

        override fun onDetach() {
            onDetachTail?.invoke()
            super.onDetach() }

        private var onAttachInit: ContextWork? = null
        private var onStartInit: FragmentWork? = null
        private var onResumeInit: FragmentWork? = null
        private var onPauseTail: FragmentWork? = null
        private var onStopTail: FragmentWork? = null
        private var onSaveInstanceStateInit: BundleWork? = null
        private var onDestroyViewTail: FragmentWork? = null
        private var onDestroyTail: Work? = null
        private var onDetachTail: Work? = null

        private fun <R : S, S> KMutableProperty<out (ReactiveFragment.() -> R)?>.relayWork(`super`: ReactiveFragment.() -> S) =
            with(this@ReactiveFragment) {
            getInstance()?.invoke(this)
            `super`().also {
            expire() } }

        private fun <T, R : S, S> KMutableProperty<out (ReactiveFragment.(T) -> R)?>.relayWork(value: T, `super`: ReactiveFragment.(T) -> S) =
            with(this@ReactiveFragment) {
            getInstance()?.invoke(this, value)
            `super`(value).also {
            expire() } }

        private fun AnyKMutableProperty.expire() = setter.call(null)
    }

    protected abstract inner class MigrationManager : SchedulerFragment.MigrationManager()

    override var isCrossObjectProviderEnabled = true
        get() = isForegroundProvider and field

    override var isCrossFunctionProviderEnabled = true
        get() = isForegroundProvider and field

    override fun provide(type: AnyKClass) = when (type) {
        TransitionManager::class ->
            this
        ApplicationMigrationManager::class ->
            object : MigrationManager() {}
        else ->
            crossProvide(type) }

    override fun <R> provide(vararg tag: TagType) =
        crossProvide<R>(*tag)
}

private typealias DefaultViewGroup = MainViewGroup

private typealias BundleWork = ReactiveFragment.(Bundle) -> Unit
private typealias ContextWork = ReactiveFragment.(Context) -> Unit
private typealias FragmentWork = ReactiveFragment.() -> Unit