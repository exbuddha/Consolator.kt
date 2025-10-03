@file:JvmName("Bundle")
@file:JvmMultifileClass

package net.consolator

import android.os.Bundle
import net.consolator.BaseActivity.Companion.INIT_INSTANCE_STATE_KEY

internal inline fun <R> Bundle?.initOrRestore(init: Bundle?.() -> R, restore: Bundle.() -> R) =
    if (this === null || isInitInstanceState)
        init()
    else restore()

internal val Bundle.isInitInstanceState
    get() = getBoolean(INIT_INSTANCE_STATE_KEY)

internal fun Bundle.applyPutBoolean(key: String, value: Boolean = true) = applyOperator(Bundle::putBoolean, key, value)

internal fun Bundle.applyPutInt(key: String, value: Int) = applyOperator(Bundle::putInt, key, value)

internal inline fun <T> Bundle.applyOperator(operator: Bundle.(String, T) -> Any?, key: String, value: T) = apply { operator(key, value) }

internal fun Bundle.applyRemove(key: String) = apply { remove(key) }