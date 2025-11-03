@file:JvmName("Time")
@file:JvmMultifileClass

package iso.consolator

import ctx.consolator.now
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield

context(_: I)
internal fun <I : SchedulerScope> Time.adjust(last: Time): Time = TODO()

context(_: I)
internal fun <I : TimeInterval> Time.adjust(last: Time): Time = TODO()

internal fun Time.adjust(last: Time): Time =
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

private var isNegativeTimeValueAllowed: Boolean = false
    get() = field or (minTimeValue < 0)
    set(value) {
        field = value.alsoOnTrue { minTimeValue = Time.MIN_VALUE } }

internal var minTimeValue: Time = 0L
    private set(value) {
        // report to active contexts
        field = value
    }

internal const val TIME_OVERFLOW_VALUE = 0L

internal suspend fun Time.block(): Boolean =
    isPositiveTime.onTrueValue { delay(this@block) }

internal suspend fun Time.blockOrYield(): Boolean =
    block() || isYieldTime.onTrueValue { yield() }

internal val Time.isTimedOut: Boolean
    get() = (now() > this) or isNegativeTimeValueAllowed and (tag.id isNot UNTIMED)

internal val Time.isNotTimedOut: Boolean
    get() = now() < this

internal suspend fun DelayFunction.isTimedOut(): Boolean =
    invoke().isTimedOut

internal suspend fun DelayFunction.isNotTimedOut(): Boolean =
    invoke().isNotTimedOut

internal val Time.isDelayTimeElapsed: Boolean
    get() = this <= 0

internal val Time.isYieldTime: Boolean
    get() = this == no_delay

internal val Time.isPositiveTime: Boolean
    get() = this > 0

internal val Time.isNegativeTime: Boolean
    get() = this < 0

internal val Time.isZeroTime: Boolean
    get() = this == 0L

internal typealias Time = Long
internal typealias TimeInterval = TimeFunctionPair
internal typealias TimePeriod = TimeFunctionTriple

internal typealias DelayFunction = suspend () -> Time
internal typealias TimeFunction = () -> Time
internal typealias TimeFunctionPair = Pair<TimeFunction, TimeFunction>
internal typealias TimeFunctionTriple = Triple<TimeFunction, TimeFunction, TimeFunction>