@file:JvmName("Number")
@file:JvmMultifileClass

package iso.consolator

import kotlin.reflect.KCallable

sealed class IndexNumber : BaseNumber()

internal open class PreciseNumber() : BaseNumber() {
    private lateinit var order: Any

    constructor(number: Number) : this() {
        order = number }

    protected open fun reset() {
        order = zero_instance }

    protected open fun clear() {
        order = Unit }

    // with context parameters, this function becomes isolated to solution contexts
    // for example, a precise number may become aware by context in order to adjust/lock its operation context
    // a precise number can choose to expose any arbitrary number, such as its numeric base value, in unknown contexts
    // while exposing its real value, for instance in a different base, given the context it is accessed from
    override val getOrder: NumberPointer = ::translate

    override fun invokeOrder() = order as? Number ?: getOrder.invoke()

    protected var unifier: NumericUnifier = Companion
        private set

    // with context parameters, set the context of operation or intercept result
    private inline fun <reified N : Number> translate() =
        with(order) { when (this) {
            is String ->
                run(unifier::unifyString)
            is CharSequence ->
                run(unifier::unifyCharSequence)
            is AnyPair ->
                run(unifier::unifyPair)
            is AnyTriple ->
                run(unifier::unifyTriple)
            is AnyArray ->
                run(unifier::unifyArray)
            is AnyIterable ->
                run(unifier::unifyIterable)
            is Number -> /* move up if visibility changes to non-private */
                run(unifier::unifyNumber)
            else ->
                run(unifier::unifyAny) } as N }

    private companion object : NumericUnifier
}

sealed class BaseNumber : Number() {
    protected open val getOrder: NumberPointer?
        get() = zero_element

    internal open fun invokeOrder() = getOrder?.invoke() ?: zero_instance

    override fun toByte() = invokeOrder().toByte()
    override fun toDouble() = invokeOrder().toDouble()
    override fun toFloat() = invokeOrder().toFloat()
    override fun toInt() = invokeOrder().toInt()
    override fun toLong() = invokeOrder().toLong()
    override fun toShort() = invokeOrder().toShort()
}

private val zero_element: NumberPointer = 0::toByte
private val zero_instance = zero_element()

private typealias StringTransformDefaultIdentityType = Int

internal sealed interface NumericUnifier : Unifier {
    fun <N : Number> unifyNumber(it: N) = it

    fun unifyAny(it: Any) =
        unifyNumber(it as Number)

    fun unifyString(it: String) =
        parseString(it)

    fun <S : CharSequence> unifyCharSequence(it: S) =
        transformString<StringTransformDefaultIdentityType, _>(it)

    fun unifyArray(it: AnyArray) =
        unifyOrRejectOnNull(it, AnyArray::firstOrNull)

    fun unifyIterable(it: AnyIterable) =
        unifyOrRejectOnNull(it, AnyIterable::firstOrNull)

    fun unifyPair(it: AnyPair) =
        unifyOrRejectOnNull(it, AnyPair::first)

    fun unifyTriple(it: AnyTriple) =
        unifyOrRejectOnNull(it, AnyTriple::first)

    private fun parseString(it: String?): Number? = it.run(
        when (StringTransformDefaultIdentityType::class) {
            Int::class -> ::parseToInt
            Long::class -> ::parseToLong
            Short::class -> ::parseToShort
            Byte::class -> ::parseToByte
            Double::class -> ::parseToDouble
            Float::class -> ::parseToFloat
            else -> ::parseToInt })

    private inline fun <reified N : Number, S : CharSequence> transformString(it: S, convert: (S) -> String? = CharSequence::toString, transform: StringToNumberFunction = ::parseInternal) =
        unifyObject<_, _, _>(it, convert) { transform(it) as Number }
        .asTypeUnsafe<N>()

    // performs internal adjustments
    private fun parseInternal(it: String?) =
        if (StringTransformDefaultIdentityType::class isObject Int::class)
            get(it)
        else rejectWithImplementationRestriction()

    private operator fun get(it: String?) =
        parseToInt(it)

    private inline fun <I, T : Any> unifyOrRejectOnNull(it: I, block: (I) -> T?, transform: NumberKCallable = ::unifyNumber) =
        transform.call(block(it) ?: reject(it))

    @Throws
    private fun <R> reject(it: R): Nothing =
        rejectWithIllegalStateException()
}

internal inline fun <N : Number, reified R : Number> NumericUnifier.unifyNumberUnsafe(it: N): R =
    it.asTypeUnsafe<R>()

internal typealias NumberPointer = () -> Number
internal typealias IntFunction = () -> Int
internal typealias LongFunction = () -> Long
internal typealias IntPredicate = (Int) -> Boolean
internal typealias NumberKCallable = KCallable<Number>

internal fun Any?.asInt() = asType<Int>()
internal fun Any?.asLong() = asType<Long>()

const val STAGE_BUILD_RUN_DB = 1001
const val STAGE_BUILD_SESSION = 1002
internal const val STAGE_BUILD_LOG_DB = 1003
internal const val STAGE_BUILD_NET_DB = 1004
internal const val STAGE_INIT_NET_DB = 1005

const val APP_INIT = 1890

const val DEFAULT_ACTIVITY = 1
const val DEFAULT_FRAGMENT = 2
const val MAIN_FRAGMENT = 3

internal const val START_TIME_KEY = "1"
internal const val MODE_KEY = "2"
internal const val ACTION_KEY = "3"

internal const val APP_DB = 7
internal const val LOG_DB = 8
internal const val NET_DB = 9
internal const val SESSION = 10

const val ON_ATTACH = 1200
const val ON_VIEW_CREATED = 1201

internal const val CLK_INIT = 1891
internal const val CLK_ATTACH = 1877
internal const val CLK_EXEC = 1878
internal const val CTX_REFORM = 1879
internal const val CTX_STEP = 1880
internal const val JOB_LAUNCH = 1881
internal const val JOB_REPEAT = 1890
internal const val FLO_LAUNCH = 1883
internal const val SCH_COMMIT = 1884
internal const val SCH_LAUNCH = 1885
internal const val SCH_EXEC = 1886
internal const val SCH_POST = 1887
internal const val SEQ_ATTACH = 1891
internal const val SEQ_LAUNCH = 1889
internal const val SVC_INIT = -1892
internal const val SVC_COMMIT = 1892
internal const val SCH_CONFIG = 1893
internal const val SCH_START = -1884
internal const val NULL_STEP = 2000
internal const val UNCAUGHT_DB = 1894
const val UNCAUGHT_SHARED = 1895

internal const val APP = 1850
internal const val ACTIVITY = 1851
internal const val FRAGMENT = 1852
internal const val VIEW = 1853
internal const val CONTEXT = 1854
internal const val OWNER = 1855
internal const val SHARED = 1878
internal const val SERVICE = 1879
internal const val CLOCK = 1880
internal const val HANDLER = 1881
internal const val CTRL = 1882
internal const val CTX = 1883
internal const val FLO = 1884
internal const val SCH = 1885
internal const val SEQ = 1886
internal const val LOG = 1887
internal const val NET = 1888
internal const val DB = 1889

internal const val CONFIG = 1830
const val START = 1831
const val RESTART = 1832
const val RESUME = 1833
const val PAUSE = 1834
const val STOP = 1835
const val SAVE = 1847
const val DESTROY = 1848
internal const val EXPIRE = 1849

const val MAIN = 1700
internal const val KEEP = 1810
internal const val JOB = 1811
internal const val BUILD = 1701
internal const val INIT = 1702
internal const val LAUNCH = 1812
internal const val COMMIT = 1703
internal const val EXEC = 1813
internal const val ATTACH = 1704
internal const val WORK = 1814
internal const val LIVEWORK = 1815
internal const val STEP = 1816
internal const val FORM = 1705
internal const val REFORM = 1817
internal const val INDEX = 1706
internal const val REGISTER = 1707
internal const val UNREGISTER = 1708
internal const val REPEAT = 1828
internal const val UNTIMED = -1710
internal const val BYPASS = -1709
internal const val DELAY = 1709
const val MIN_DELAY = -1701
internal const val MAX_DELAY = 1701
internal const val UNDELAYED = -1709
internal const val YIELDING = 1820
internal const val UNYIELDING = -1820
internal const val CALL = 1710
internal const val POST = 1711
internal const val CALLBACK = 1712
internal const val FUNC = 1713
internal const val MSG = 1821
internal const val WHAT = 1822
internal const val PREDICATE = 1714
internal const val SUCCESS = 1715
internal const val ERROR = 1716
internal const val UPDATE = 1823
const val EXCEPTION = 1717
internal const val CAUSE = 1824
internal const val MESSAGE = 1825
const val EXCEPTION_CAUSE = 1718
const val EXCEPTION_MESSAGE = 1826
const val EXCEPTION_CAUSE_MESSAGE = 1827
internal const val IGNORE = 1719
const val UNCAUGHT = 1828
const val OVERFLOW = -1720
const val NOW = 1720
internal const val INTERVAL = 1721
internal const val MIN_INTERVAL = 1829

internal const val TRUE = 1
internal const val FALSE = 2
internal const val MIN = 3
internal const val MAX = 4
internal const val ACTIVE = 5
internal const val INACTIVE = 6
internal const val NULL = -1

internal const val TAG_DOT = 1
internal const val TAG_DASH = 2
internal const val TAG_AT = 3
internal const val TAG_HASH = 4