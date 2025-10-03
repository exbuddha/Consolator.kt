@file:JvmName("Time")
@file:JvmMultifileClass

package iso.consolator

import ctx.consolator.now
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield

// include a contextual function with last time as parameter (also, one with time interval for adjusting inside time capsules)
// include a contextual function with scheduler scope as parameter type
// to report significant times back to other contexts (intercepting certain positives - extremely low level)
internal fun Time.adjust(last: Time) =
    (- runWhen({ this@adjust < last }, {
        if (last.isNegativeTime) {
            isNegativeTimeValueAllowed = true
            return TIME_OVERFLOW_VALUE }
        ((if (isNegativeTimeValueAllowed)
            minus(minTimeValue)
            .onNegativeValue { return TIME_OVERFLOW_VALUE }
        else runWhen({ it.isNegativeTime }, {
            isNegativeTimeValueAllowed = true
            minus(minTimeValue) })) +
        (Time.MAX_VALUE - last)
        .onNegativeValue { return TIME_OVERFLOW_VALUE })
        .onNegativeValue { return TIME_OVERFLOW_VALUE }
    }))
    .runWhen({ this@adjust > last }, {
        plus(last.also {
            if (it.isNegativeTime) isNegativeTimeValueAllowed = true })
        .onPositiveValue { return TIME_OVERFLOW_VALUE }
    })

private var isNegativeTimeValueAllowed = false
    get() = field or (minTimeValue < 0)
    set(value) {
        field = value.alsoOnTrue { minTimeValue = Time.MIN_VALUE } }

internal var minTimeValue = 0L
    private set(value) {
        // report to active contexts
        field = value
    }

internal const val TIME_OVERFLOW_VALUE = 0L

internal suspend fun Time.block() =
    isPositiveTime.onTrueValue { delay(this@block) }

internal suspend fun Time.blockOrYield() =
    block() || isYieldTime.onTrueValue { yield() }

internal val Time.isTimedOut
    get() = (now() > this) or isNegativeTimeValueAllowed and (tag.id isNot UNTIMED)

internal val Time.isNotTimedOut
    get() = now() < this

internal suspend fun DelayFunction.isTimedOut() =
    invoke().isTimedOut

internal suspend fun DelayFunction.isNotTimedOut() =
    invoke().isNotTimedOut

internal val Time.isDelayTimeElapsed
    get() = this <= 0

internal val Time.isYieldTime
    get() = this == no_delay

internal val Time.isPositiveTime
    get() = this > 0

internal val Time.isNegativeTime
    get() = this < 0

internal val Time.isZeroTime
    get() = this == 0L

internal typealias Time = Long
internal typealias TimeInterval = TimeFunctionPair
internal typealias TimePeriod = TimeFunctionTriple

internal typealias DelayFunction = suspend () -> Time
internal typealias TimeFunction = () -> Time
internal typealias TimeFunctionPair = Pair<TimeFunction, TimeFunction>
internal typealias TimeFunctionTriple = Triple<TimeFunction, TimeFunction, TimeFunction>