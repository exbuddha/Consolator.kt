@file:JvmName("Provider")
@file:JvmMultifileClass

package net.consolator

import androidx.lifecycle.LifecycleOwner
import iso.consolator.AnyKClass
import iso.consolator.FunctionProvider
import iso.consolator.ObjectProvider
import iso.consolator.rejectWithImplementationRestriction
import iso.consolator.withSchedulerScope
import iso.consolator.annotation.TagType
import kotlin.reflect.KCallable

internal val LifecycleOwner.isForegroundProvider
    get() = withSchedulerScope { isForegroundLifecycleOwner }

internal val LifecycleProvider.isForegroundProvider
    get() = withSchedulerScope { isForegroundLifecycleOwner }

internal sealed interface LifecycleProvider : LifecycleOwner, CrossProvider

internal interface CrossProvider : CrossObjectProvider, CrossFunctionProvider

internal interface InactiveCrossProvider : CrossProvider {
    override fun provide(type: AnyKClass): Any = rejectWithImplementationRestriction()
    override fun <R> provide(vararg tag: TagType): KCallable<R> = rejectWithImplementationRestriction()
}

internal interface CrossObjectProvider : ObjectProvider {
    fun enableCrossObjectProvider() {
        isCrossObjectProviderEnabled = true }

    fun disableCrossObjectProvider() {
        isCrossObjectProviderEnabled = false }

    var isCrossObjectProviderEnabled: Boolean
}

internal interface CrossFunctionProvider : FunctionProvider {
    fun enableCrossFunctionProvider() {
        isCrossFunctionProviderEnabled = true }

    fun disableCrossFunctionProvider() {
        isCrossFunctionProviderEnabled = false }

    var isCrossFunctionProviderEnabled: Boolean
}