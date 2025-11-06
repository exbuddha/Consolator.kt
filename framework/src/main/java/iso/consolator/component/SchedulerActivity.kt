package iso.consolator.component

import android.Manifest.permission.ACCESS_NETWORK_STATE
import android.Manifest.permission.INTERNET
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import ctx.consolator.ReferredContext
import ctx.consolator.WeakContext
import iso.consolator.Resolver
import iso.consolator.asType
import iso.consolator.commitToConfigurationChangeManager
import iso.consolator.commitToLocalesChangeManager
import iso.consolator.commitToNightModeChangeManager
import iso.consolator.foregroundLifecycleOwner
import iso.consolator.isPermissionGranted
import iso.consolator.withSchedulerScope
import iso.consolator.annotation.Scope
import iso.consolator.annotation.Tag

@Scope
abstract class SchedulerActivity : AppCompatActivity(), ReferredContext {
    /** Flag indicating whether or not network callback is enabled. */
    abstract val isNetworkCallbackEnabled: Boolean
    /** Flag indicating whether or not network callback is disabled. */
    abstract val isNetworkCallbackDisabled: Boolean

    /** Starts network callbacks. */
    abstract fun startNetworkCallbacks()

    /** Stops network callbacks. */
    abstract fun stopNetworkCallbacks()

    /**
     * Enables network callbacks in saved instance state.
     *
     * @receiver the saved instance state.
     */
    abstract fun Bundle?.enableNetworkCallbacks(): Bundle

    /**
     * Disables network callbacks in saved instance state.
     *
     * @receiver the saved instance state.
     */
    abstract fun Bundle?.disableNetworkCallbacks(): Bundle

    /** The foreground fragment or `null` if it doesn't exist. */
    open val fragment
        get() = foregroundLifecycleOwner.asType<Fragment>()

    /**
     * Sets the activity tag.
     *
     * @param tag the tag annotation.
     */
    protected fun setTag(tag: Tag?) {
        /* register in source */ }

    /** @suppress
     *
     * Starts scheduler and network callback activities.
     *
     * @sample onStart
     */
    override fun onStart() {
        super.onStart()
        withSchedulerScope {
        commitStart() }
        startNetworkCallbacks()
    }

    /** @suppress
     *
     * Restarts scheduler activity.
     *
     * @sample onRestart
     */
    override fun onRestart() {
        super.onRestart()
        withSchedulerScope {
        commitRestart() }
    }

    /** @suppress
     *
     * Resumes scheduler activity.
     *
     * @sample onResume
     */
    override fun onResume() {
        super.onResume()
        withSchedulerScope {
        commitResume() }
    }

    /** @suppress
     *
     * Pauses scheduler activity.
     *
     * @sample onPause
     */
    override fun onPause() {
        super.onPause()
        withSchedulerScope {
        commitPause() }
    }

    /** @suppress
     *
     * Stops scheduler activity.
     *
     * @sample onStop
     */
    override fun onStop() {
        stopNetworkCallbacks()
        withSchedulerScope {
        commitStop() }
        super.onStop()
    }

    /** @suppress
     *
     * Destroys scheduler activity.
     *
     * @sample onDestroy
     */
    override fun onDestroy() {
        withSchedulerScope {
        commitDestroy() }
        super.onDestroy()
    }

    /** @suppress
     *
     * Writes network callback state to out-state bundle.
     *
     * @param outState the bundle.
     */
    override fun onSaveInstanceState(outState: Bundle) {
        if (isNetworkCallbackEnabled)
            outState.enableNetworkCallbacks()
        withSchedulerScope {
        commitSaveInstanceState(outState) }
        super.onSaveInstanceState(outState)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        commitToConfigurationChangeManager(newConfig)
    }

    override fun onNightModeChanged(mode: Int) {
        super.onNightModeChanged(mode)
        commitToNightModeChangeManager(mode)
    }

    override fun onLocalesChanged(locales: LocaleListCompat) {
        super.onLocalesChanged(locales)
        commitToLocalesChangeManager(locales)
    }

    abstract inner class ConfigurationChangeManager : ActivityChangeResolver()
    abstract inner class NightModeChangeManager : ActivityChangeResolver()
    abstract inner class LocalesChangeManager : ActivityChangeResolver()

    abstract inner class ActivityChangeResolver : Resolver {
        override fun commit(vararg context: Any?) = TODO()
    }

    protected companion object {
        /** Checks whether or not network state access permission is granted in manifest file. */
        val Context.isNetworkStateAccessPermitted
            get() = isPermissionGranted(ACCESS_NETWORK_STATE)

        /** Checks whether or not internet access permission is granted in manifest file. */
        val Context.isInternetAccessPermitted
            get() = isPermissionGranted(INTERNET)
    }

    private fun asActivity() = this as Activity

    final override var ref: WeakContext? = null
        get() = field.receive(this).also { field = it }
}