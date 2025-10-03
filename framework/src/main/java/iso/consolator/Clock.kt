@file:JvmName(JVM_CLASS_NAME)
@file:JvmMultifileClass

package iso.consolator

import android.os.*
import iso.consolator.Lock.*
import kotlin.reflect.KCallable
import kotlinx.coroutines.CoroutineScope

// runnable <-> message
internal fun post(callback: Runnable) = clock?.post?.invoke(callback)
internal fun postAhead(callback: Runnable) = clock?.postAhead?.invoke(callback)

internal var clock: Clock? = null
    get() = field.defaultSingleton().also { field = it }

internal inline fun <R> withClock(block: Clock.Companion.() -> R) =
    with(Clock, block)

internal typealias ClockIndex = Number

internal open class Clock(
    name: String,
    priority: Int = currentThread.priority
) : HandlerThread(name, priority), Synchronizer<BaseState>, Transactor<Message, ClockTransactionIdentityType>, PriorityQueue<Runnable>, AdjustOperator<Runnable, ClockIndex> {
    @JvmField var handler: Handler? = null
    final override lateinit var queue: RunnableList

    init {
        this.priority = priority
        register() }

    constructor() : this("$CLOCK")

    constructor(callback: RunnableKCallable) : this() {
        register(callback) }

    constructor(callback: Runnable) : this() {
        register(callback) }

    constructor(name: String, priority: Int = currentThread.priority, callback: RunnableKCallable) : this(name, priority) {
        register(callback) }

    constructor(name: String, priority: Int = currentThread.priority, callback: Runnable) : this(name, priority) {
        register(callback) }

    var id = -1
        private set

    val isStarted get() = id != -1
    val isNotStarted get() = id == -1

    override fun start() {
        id = indexOf(queue)
        super.start() }

    fun alsoStart(): Clock {
        start()
        return this }

    fun startAsync() =
        commitLockedByFunction(::isAlive::not, ::start)

    fun alsoStartAsync(): Clock {
        startAsync()
        return this }

    @JvmField var isRunning = false

    override fun run() {
        hLock = Open
        handler = object : Handler(looper) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                DEFAULT_HANDLER(msg) } }
        isRunning = true
        queue.run() }

    override fun commit(step: Message) =
        if (isSynchronized(step))
            synchronize(State of step) {
                if (queue.run(step, false))
                    step.callback
                        .markTagForClkExec()
                        .run() }
        else step.callback.exec()

    private fun RunnableList.run(msg: Message? = null, isIdle: Boolean = true) =
        with(precursorOf(msg)) {
            forEach {
            var ln = it
            synchronized(sLock) {
                ln = adjust(ln)
                queue[ln]
            }.exec(isIdle)
            synchronized(sLock) {
                removeAt(adjust(ln))
            } }
            hasNotTraversed(msg) }

    private fun precursorOf(msg: Message?) = queue.indices

    private fun IntProgression.adjust(ln: ClockIndex) = adjust(index = ln)

    private fun IntProgression.hasNotTraversed(msg: Message?) = true

    private fun Runnable.exec(isIdle: Boolean = true) {
        markTagForClkExec()
        if (isIdle and run(::isSynchronized))
            synchronize(block = ::run)
        else run() }

    private fun isSynchronized(msg: Message) =
        isSynchronized(msg.callback) or
        msg.asReference().isSynchronized()

    private fun isSynchronized(callback: Runnable) =
        getAnyCoroutine(callback).asReference().isSynchronized() or
        callback.asReference().isSynchronized() or
        callback::run.isSynchronized()

    private fun AnyKCallable.isSynchronized() =
        annotations.any(::filterIsSynchronized)

    private lateinit var hLock: Lock

    override fun <R> synchronize(lock: BaseState?, block: () -> R) =
        with(hLock) {
        synchronized(switch(lock, block)) {
            unlock(Closed::invoke)
            block().also {
            release(it, Open::invoke)
        } } }

    override fun BaseState.from(ref: BaseState) = TODO()

    override val descriptor
        get() = object : MessageDescriptor {
            override fun <A : BaseState, B : BaseState> A.onValueChanged(value: B, block: BaseDescriptor.(BaseState) -> Any?) = TODO()
        }

    override fun adjust(index: ClockIndex) = when (index) {
        is Int -> index
        else -> getEstimatedIndex(index) }

    private fun getEstimatedIndex(delay: ClockIndex) = queue.size

    private var sLock = Any()

    override fun attach(step: Runnable, vararg args: Any?) =
        synchronized<Unit>(sLock) { with(queue) {
            add(step)
            markTagsForClkAttach(step, size) } }

    override fun attach(index: ClockIndex, step: Runnable, vararg args: Any?) =
        synchronized<Unit>(sLock) {
            queue.add(index.toInt(), step)
            // remark items in queue for adjustment
            markTagsForClkAttach(step, index) }

    @JvmField var post = fun(callback: Runnable) =
        handler?.post(callback)
        ?: attach(callback)

    @JvmField var postAhead = fun(callback: Runnable) =
        handler?.postAtFrontOfQueue(callback)
        ?: attach(0, callback)

    fun clearObjects() {
        handler = null
        queue.clear() }

    override fun quit(): Boolean {
        run(HandlerScope.threads::expire)
        return super.quit()
    }

    override fun quitSafely(): Boolean {
        run(HandlerScope.threads::expire)
        return super.quitSafely()
    }

    companion object : RunnableGrid by mutableListOf() {
        @JvmStatic private var DEFAULT_HANDLER: HandlerFunction = { commit(it) }

        @JvmStatic private fun Clock.register() {
            HandlerScope.threads = object : Queue<Thread>() {
                override fun get(index: Int): Thread = TODO()

                override fun indexOf(element: Thread): Int = TODO()

                override fun isEmpty() = Companion.isEmpty()

                override fun iterator(): Iterator<Thread> = TODO()

                override fun lastIndexOf(element: Thread): Int = TODO()

                override fun listIterator(): ListIterator<Thread> = TODO()

                override fun listIterator(index: Int): ListIterator<Thread> = TODO()

                override fun subList(fromIndex: Int, toIndex: Int): List<Thread> = TODO()

                override fun containsAll(elements: Collection<Thread>): Boolean = TODO()

                override fun contains(element: Thread): Boolean = TODO()

                override val size
                    get() = Companion.size
            }
            queue = mutableListOf()
            add(queue) }

        @JvmStatic private fun <R : Runnable> Clock.register(callback: KCallable<R>) {
            // process tag
            queue.add(callback.call()) }

        @JvmStatic private fun <R : Runnable> Clock.register(callback: R) {
            queue.add(callback) }

        @JvmStatic fun <T> getMessage(step: suspend CoroutineScope.() -> T): Message? = null

        @JvmStatic fun <T> getRunnable(step: suspend CoroutineScope.() -> T): Runnable? = null

        @JvmStatic fun <R : Runnable, T> getCoroutine(callback: R): (suspend CoroutineScope.() -> T)? = null

        @JvmStatic fun <R : Runnable> getAnyCoroutine(callback: R) = getCoroutine<R, Any?>(callback)

        @JvmStatic fun <R> getMessage(step: suspend () -> R): Message? = null

        @JvmStatic fun <T> getRunnable(step: suspend () -> T): Runnable? = null

        @JvmStatic fun <R : Runnable, T> getStep(callback: R): (suspend () -> T)? = null

        @JvmStatic fun <T> getEstimatedDelay(step: suspend CoroutineScope.() -> T): Time? = null

        @JvmStatic fun <T> getDelay(step: suspend CoroutineScope.() -> T): Time? = null

        @JvmStatic fun <T> getTime(step: suspend CoroutineScope.() -> T): Time? = null

        @JvmStatic fun <T> getEstimatedDelay(step: suspend () -> T): Time? = null

        @JvmStatic fun <T> getDelay(step: suspend () -> T): Time? = null

        @JvmStatic fun <T> getTime(step: suspend () -> T): Time? = null

        @JvmStatic fun startSafely() = apply {
            if (isNotStarted) start() }

        @JvmStatic val isRunning
            get() = clock?.isRunning.isTrue()

        @JvmStatic val isNotRunning
            get() = clock?.isRunning.isNotTrue()

        @JvmStatic fun quit() = clock?.quit()

        @JvmStatic fun apply(block: Clock.() -> Unit) = clock?.apply(block)
        @JvmStatic fun <R> run(block: Clock.() -> R) = clock?.run(block)
    }
}

private typealias HandlerFunction = Clock.(Message) -> ClockTransactionIdentityType
private typealias RunnableList = MutableList<Runnable>
private typealias RunnableGrid = MutableList<RunnableList>

private typealias ClockTransactionIdentityType = Unit /* symbolic to run() in Runnable */

internal val SVC_TAG
    get() = if (onMainThread) "SERVICE" else "CLOCK"