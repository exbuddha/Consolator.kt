@file:JvmName("Time")
@file:JvmMultifileClass

package iso.consolator

import ctx.consolator.now
import iso.consolator.annotation.Tag

internal sealed interface TimeCapsule {
    val uptime: Time?
    val downtime: Time?

    fun getEstimatedDelayToUpTime(): Time
    fun getEstimatedDelayToDownTime(): Time

    abstract class Record : TimeCapsule {
        abstract val first: Time?
        abstract val last: Time?

        abstract var attempts: Int?

        internal companion object Table {
            @JvmStatic operator fun invoke(): Record = object : Record() {
                override var uptime: Time? = null
                override var downtime: Time? = null

                override var first: Time? = null
                override var last: Time? = null

                override var attempts: Int? = null

                lateinit var collection: Collection<TimeCapsule>

                override fun getEstimatedDelayToUpTime(): Time = TODO()
                override fun getEstimatedDelayToDownTime(): Time = TODO()

                override fun asRecord(): Record = this
            }
        }

        // include a contextual function with time capsule as context parameter
        fun TimeCapsule.toDelayTimeRecord(defaultLast: Time = minTimeValue): DelayTimeCapsule.Record = object : DelayTimeCapsule.Record() {
            override val uptime: Time? = null
            override var downtime: Time = 0L

            override var first: Time? = null
            override var last: Time = asRecord()?.last ?: defaultLast

            override var dt: Time = 0L

            override var attempts: Int? = asRecord()?.attempts

            override fun getEstimatedDelayToUpTime(): Time = TODO()
            override fun getEstimatedDelayToDownTime(): Time = TODO()

            override fun asRecord(): Record? = this@toDelayTimeRecord as? Record // potential memory leak!
        }
    }

    // geared by context parameters - returns singletons by preference
    fun asRecord(): Record?
}

internal sealed interface DelayTimeCapsule : TimeCapsule {
    var dt: Time

    abstract class Record : TimeCapsule.Record(), DelayTimeCapsule {
        abstract override var downtime: Time

        abstract override var first: Time?
        abstract override var last: Time

        override fun toTimeInterval(): TimeInterval = ::last.toPointer() to ::dt.toPointer()
        override fun toTimePeriod(): TimePeriod = Triple(::last.toPointer(), ::dt.toPointer(), ::downtime.toPointer())
    }

    fun toTimeInterval(): TimeInterval
    fun toTimePeriod(): TimePeriod

    override fun asRecord(): TimeCapsule.Record? = object : TimeCapsule.Record() {
        override val uptime = this@DelayTimeCapsule.uptime
        override val downtime = this@DelayTimeCapsule.downtime

        override val first = (this@DelayTimeCapsule as Record).first
        override val last = (this@DelayTimeCapsule as Record).last

        override var attempts = (this@DelayTimeCapsule as Record).attempts

        override fun getEstimatedDelayToUpTime() = no_delay
        override fun getEstimatedDelayToDownTime() = no_delay

        override fun asRecord(): TimeCapsule.Record = this
    }
}

internal fun TimePeriod.hasTimePeriodElapsed(): Boolean =
    hasTimePeriodElapsed(first(), second(), third())

internal fun TimePeriod.getDelayTime(): Time =
    getDelayTime(first(), second(), third())

internal fun TimeInterval.hasTimeIntervalElapsed(): Boolean =
    hasTimeIntervalElapsed(first(), second())

internal fun TimeInterval.getDelayTime(): Time =
    getDelayTime(first(), second())

internal fun hasTimePeriodElapsed(last: Time, interval: Time, downtime: Time): Boolean =
    (last == 0L) or getDelayTime(last, interval, downtime).isDelayTimeElapsed

internal fun getDelayTime(last: Time, interval: Time, downtime: Time): Time =
    now().let { now ->
    now.adjustedBy(last, interval).run {
        val endtime = plus(now)
        runWhen({ endtime > downtime }) { return endtime - downtime } } }

// include a contextual function with last time as parameter
internal fun hasTimeIntervalElapsed(last: Time, interval: Time): Boolean =
    (last == 0L) or getDelayTime(last, interval).isDelayTimeElapsed

// include a contextual function with scheduler scope as parameter type
// to resolve significant overflows by callback or issuance
internal fun getDelayTime(last: Time, interval: Time): Time =
    now().adjustedBy(last, interval) // catch overflows
    .runWhen({ it > interval }) { return interval }

private fun Time.adjustedBy(last: Time, interval: Time): Time =
    adjust(last).plus(interval)

/**
 * The minimum delay time used for resolving scheduler tasks when view is active.
 *
 * default value: `300` milliseconds
 */
@Tag(MIN_DELAY)
const val view_min_delay = 300L

@Tag(UNDELAYED)
internal const val no_delay = 0L

@Tag(UNTIMED)
internal const val no_timeout = -2L

@Tag(UNYIELDING)
internal const val no_yield = -1L