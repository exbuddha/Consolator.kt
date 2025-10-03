package net.consolator

import android.os.*
import androidx.activity.*
import androidx.annotation.*
import androidx.fragment.app.*
import androidx.lifecycle.*
import iso.consolator.*
import iso.consolator.annotation.*
import iso.consolator.component.SchedulerActivity
import iso.consolator.component.SchedulerFragment
import view.consolator.*
import android.annotation.SuppressLint

/**
 * Default scheduler activity handles base fragment creation.
 */
@Coordinate
@Tag(DEFAULT_ACTIVITY)
open class DefaultActivity : BaseActivity(), ContentLayoutReceiver, LifecycleProvider {
    /** Sets the (base) layout resource ID. */
    @Key(-2)
    fun setLayoutResourceId(@LayoutRes layoutResId: Int) {
        net.consolator.layoutResId = layoutResId }

    /** Sets the container view ID (inside) of the layout resource. */
    @Key(-1)
    fun setContainerViewId(@IdRes containerViewId: Int) {
        net.consolator.containerViewId = containerViewId }

    /** @suppress */
    override fun onCreate(savedInstanceState: Bundle?) {
        model = viewModels<MainViewModel>().value
            .also { State.VM = it }
        if (savedInstanceState !== null)
            lastSavedInstanceState = savedInstanceState
        with(lastSavedInstanceState) {
        initOrRestore(
            { setContentView(layoutResId) },
            ::setContentView)
        super.onCreate(this)
        initOrRestore(
            ::begin,
            ::restore) }
    }

    /** Sets the layout resource ID according to saved instance state. */
    private fun setContentView(savedInstanceState: Bundle) {
        with(savedInstanceState) {
        if (containsKey(LAYOUT_RES_KEY))
            setContentView(getInt(LAYOUT_RES_KEY)) } }

    /** Sets container view ID according to saved instance state. */
    private fun setContainerView(savedInstanceState: Bundle) {
        with(savedInstanceState) {
        if (containsKey(CONTAINER_VIEW_KEY))
            this@DefaultActivity.setContainerView(
                getInt(CONTAINER_VIEW_KEY),
                visible = getBoolean(CONTAINER_VISIBLE_KEY, false),
                savedInstanceState = this) } }

    /** Places the default fragment in container view. */
    private fun begin(savedInstanceState: Bundle? = null) {
        setContainerView(containerViewId,
            visible = false,
            savedInstanceState = savedInstanceState) }

    private fun restore(savedInstanceState: Bundle) {
        setContainerView(savedInstanceState) }

    private fun setContainerView(@IdRes containerViewId: Int, reorderingAllowed: Boolean = true, visible: Boolean = true, savedInstanceState: Bundle? = null) {
        addContainerView<DefaultFragment>(
            savedInstanceState?.getInt(CONTAINER_VIEW_KEY, containerViewId)
            ?: containerViewId,
            reorderingAllowed, visible, savedInstanceState) }

    /**
     * Places the scheduler fragment inside container view.
     *
     * @param F the scheduler fragment class type.
     * @param containerViewId the container view ID.
     */
    @Key(3)
    internal inline fun <reified F : SchedulerFragment> addContainerView(@IdRes containerViewId: Int, reorderingAllowed: Boolean = true, visible: Boolean = true, savedInstanceState: Bundle? = null) {
        if (containerViewId != 0)
        supportFragmentManager.commit {
            setReorderingAllowed(reorderingAllowed)
            add<F>(containerViewId, args = savedInstanceState)
            setVisible(visible) } }

    /**
     * Sets layout resource ID in saved instance state.
     *
     * @receiver the saved instance state.
     */
    fun Bundle?.enableLayoutResource(@LayoutRes layoutResId: Int) =
        requireLastSavedInstanceState()
        .applyPutInt(LAYOUT_RES_KEY, layoutResId)

    /**
     * Removes layout resource ID from saved instance state.
     *
     * @receiver the saved instance state.
     */
    fun Bundle?.disableLayoutResource() =
        requireLastSavedInstanceState()
        .applyRemove(LAYOUT_RES_KEY)

    /**
     * Sets container view ID in saved instance state.
     *
     * @receiver the saved instance state.
     */
    fun Bundle?.enableContainerView(@IdRes containerViewId: Int) =
        requireLastSavedInstanceState()
        .applyPutInt(CONTAINER_VIEW_KEY, containerViewId)

    /**
     * Removes container view ID from saved instance state.
     *
     * @receiver the saved instance state.
     */
    fun Bundle?.disableContainerView() =
        requireLastSavedInstanceState()
        .applyRemove(CONTAINER_VIEW_KEY)

    /**
     * Sets container view visibility in saved instance state.
     *
     * @receiver the saved instance state.
     */
    fun Bundle?.enableContainerVisible(visible: Boolean = true) =
        requireLastSavedInstanceState()
        .applyPutBoolean(CONTAINER_VISIBLE_KEY, visible)

    /**
     * Removes container view visibility from saved instance state.
     *
     * @receiver the saved instance state.
     */
    fun Bundle?.disableContainerVisible() =
        requireLastSavedInstanceState()
        .applyRemove(CONTAINER_VISIBLE_KEY)

    override lateinit var model: ViewModel

    /** @suppress */
    internal abstract inner class ConfigurationChangeManager : SchedulerActivity.ConfigurationChangeManager()
    /** @suppress */
    internal abstract inner class NightModeChangeManager : SchedulerActivity.NightModeChangeManager()
    /** @suppress */
    internal abstract inner class LocalesChangeManager : SchedulerActivity.LocalesChangeManager()

    override var isCrossObjectProviderEnabled = true
        get() = isForegroundProvider and field

    override var isCrossFunctionProviderEnabled = true
        get() = isForegroundProvider and field

    override fun provide(type: AnyKClass) = when (type) {
        SchedulerActivity.ConfigurationChangeManager::class ->
            object : ConfigurationChangeManager() {}
        SchedulerActivity.NightModeChangeManager::class ->
            object : NightModeChangeManager() {}
        SchedulerActivity.LocalesChangeManager::class ->
            object : LocalesChangeManager() {}
        else ->
            crossProvide(type) as Resolver }

    override fun <R> provide(vararg tag: TagType) =
        crossProvide<R>(*tag)

    internal companion object {
        /** Bundle key for the layout resource ID. */
        const val LAYOUT_RES_KEY = "1"
        /** Bundle key for the container view ID. */
        const val CONTAINER_VIEW_KEY = "2"
        /** Bundle key for the container view state. (`true` if it is visible). */
        const val CONTAINER_VISIBLE_KEY = "3"

        const val VIEW_TAG = "ACTIVITY"
    }
}