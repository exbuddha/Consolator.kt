package net.consolator

import android.os.*
import androidx.lifecycle.*
import iso.consolator.*
import iso.consolator.annotation.Key
import iso.consolator.component.SchedulerActivity

/**
 * Adds support to scheduler activity for network communication.
 *
 * @property model the view model.
 */
sealed class BaseActivity : SchedulerActivity() {
    init { setTag(this::class.tag) }

    /** Network callback enabler function. */
    private var enableNetworkCallbacks: LifecycleOwnerWork? = null
    /** Network callback disabler function. */
    private var disableNetworkCallbacks: LifecycleOwnerWork? = null

    final override val isNetworkCallbackEnabled
        get() = enableNetworkCallbacks.isNotNullValue()
    final override val isNetworkCallbackDisabled
        get() = enableNetworkCallbacks.isNullValue()

    /** Sets all network callback functions. */
    private fun setNetworkCallbacks() {
        setNetworkStateCallback()
        setInternetCallback() }

    /** Sets network state callback functions if permission is granted. */
    private fun setNetworkStateCallback() {
        if (isNetworkStateAccessPermitted) {
            enableNetworkCallbacks = ::registerNetworkCallback
            disableNetworkCallbacks = ::unregisterNetworkCallback } }

    /** Sets internet callback functions if permission is granted. */
    private fun setInternetCallback() {
        if (isInternetAccessPermitted) {
            enableNetworkCallbacks = enableNetworkCallbacks?.then(::startInternetCallback)
            disableNetworkCallbacks = disableNetworkCallbacks?.then(::unregisterInternetCallback) } }

    @Key(1)
    final override fun startNetworkCallbacks() {
        enableNetworkCallbacks?.invoke(this) }

    @Key(2)
    final override fun stopNetworkCallbacks() {
        disableNetworkCallbacks?.invoke(this) }

    /** Clears network callback functions. */
    internal fun clearNetworkCallbacks() {
        enableNetworkCallbacks = null
        disableNetworkCallbacks = null }

    /** @suppress */
    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState !== null)
            lastSavedInstanceState = savedInstanceState
        with(lastSavedInstanceState) {
        super.onCreate(this)
        initOrRestore(
            { if (isNetworkCallbackDisabled) setNetworkCallbacks() },
            ::setNetworkCallbacks) }
    }

    /** Sets network callback enabled flag according to saved instance state.*/
    private fun setNetworkCallbacks(savedInstanceState: Bundle) {
        with(savedInstanceState) {
        if (isNetworkCallbackDisabled and
            getBoolean(ENABLE_NETWORK_CALLBACKS_KEY))
            this@BaseActivity.setNetworkCallbacks() } }

    final override fun Bundle?.enableNetworkCallbacks() =
        requireLastSavedInstanceState()
        .applyPutBoolean(ENABLE_NETWORK_CALLBACKS_KEY)

    final override fun Bundle?.disableNetworkCallbacks() =
        requireLastSavedInstanceState()
        .applyRemove(ENABLE_NETWORK_CALLBACKS_KEY)

    /** Requires and sets the saved instance state. */
    internal fun Bundle?.requireLastSavedInstanceState() =
        this ?: Bundle(5).applyPutBoolean(INIT_INSTANCE_STATE_KEY, true).also { lastSavedInstanceState = it }

    /** The last saved instance state. */
    @JvmField internal var lastSavedInstanceState: Bundle? = null

    /** The view model. */
    abstract val model: ViewModel

    internal companion object {
        /** Transit key for starting the navigation to main UI. */
        const val COMMIT_NAV_MAIN_UI: Short = 2
        /** Transit key for aborting the navigation to main UI. */
        const val ABORT_NAV_MAIN_UI: Short = 3

        /** Bundle key for saved instance's initial flag. (`true` if it is initial) */
        const val INIT_INSTANCE_STATE_KEY = "0"
        /** Bundle key for network callback enable flag. (`true` if it is enabled) */
        const val ENABLE_NETWORK_CALLBACKS_KEY = "4"
    }
}