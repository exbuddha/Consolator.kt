@file:JvmName(JVM_CLASS_NAME)
@file:JvmMultifileClass

package net.consolator

import androidx.annotation.*
import androidx.lifecycle.*
import iso.consolator.*
import kotlin.reflect.*
import iso.consolator.annotation.TagType

/** The layout resource ID. (base layout) */
@LayoutRes
internal var layoutResId = R.layout.background

/** The container view ID. */
@IdRes
internal var containerViewId = R.id.layout_background

/** The content layout ID. */
@LayoutRes
internal var contentLayoutId = R.layout.background

internal fun DefaultActivity.crossProvide(type: AnyKClass) =
    blockOnTrueOrRestrict(
        DefaultActivity::isCrossObjectProviderEnabled) {
    restrictCrossProvider {
    provide(type) } }

internal fun <R> DefaultActivity.crossProvide(vararg tag: TagType): KCallable<R> =
    blockOnTrueOrRestrict(
        DefaultActivity::isCrossFunctionProviderEnabled) {
    restrictCrossProvider {
    provide(*tag) } }

internal fun DefaultFragment.crossProvide(type: AnyKClass) =
    blockOnTrueOrRestrict(
        DefaultFragment::isCrossObjectProviderEnabled) {
    restrictCrossProvider {
    provide(type) } }

internal fun <R> DefaultFragment.crossProvide(vararg tag: TagType): KCallable<R> =
    blockOnTrueOrRestrict(
        DefaultFragment::isCrossFunctionProviderEnabled) {
    restrictCrossProvider {
    provide(*tag) } }

internal inline fun <R> DefaultActivity.restrictCrossProvider(crossinline block: LifecycleProvider.() -> R) =
    restrictIn<DefaultFragment, _>(block)

internal inline fun <R> DefaultFragment.restrictCrossProvider(crossinline block: LifecycleProvider.() -> R) =
    restrictIn<DefaultActivity, _>(block)

internal inline fun <reified T : LifecycleProvider, R> BaseActivity.restrictIn(crossinline block: T.() -> R) =
    restrictIn<_, T, R>(fragment, block)

internal inline fun <reified S : T, reified T : LifecycleProvider, R> BaseActivity.restrictOut(crossinline block: T.() -> R) =
    restrictOut<_, S, T, R>(fragment, block)

internal inline fun <reified T : LifecycleProvider, R> BaseFragment.restrictIn(crossinline block: T.() -> R) =
    restrictIn<_, T, R>(activity, block)

internal inline fun <reified S : T, reified T : LifecycleProvider, R> BaseFragment.restrictOut(crossinline block: T.() -> R) =
    restrictOut<_, S, T, R>(activity, block)

private inline fun <X, reified T : LifecycleProvider, R> restrictIn(provider: X, block: T.() -> R) =
    provider.blockOnTrueOrRestrict({ it.typeIs<T, _>() }) { block(provider as T) }

private inline fun <X, reified S : T, reified T : LifecycleProvider, R> restrictOut(provider: X, block: T.() -> R) =
    provider.blockOnTrueOrRestrict({ it.typeIsNot<S, _>() }) { block(provider as T) }

internal inline fun <T, R> T.blockOnTrueOrRestrict(predicate: (T) -> Boolean, block: T.() -> R) =
    if (run(predicate)) block()
    else rejectWithImplementationRestriction()

internal typealias LifecycleOwnerWork = LifecycleOwner.() -> Unit
internal typealias Work = () -> Unit