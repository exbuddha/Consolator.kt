@file:JvmName(JVM_CLASS_NAME)
@file:JvmMultifileClass

package iso.consolator

import androidx.lifecycle.*
import iso.consolator.AdjustOperator.Element.*
import iso.consolator.AttachOperator.Element.*
import kotlin.coroutines.*
import kotlin.coroutines.cancellation.*
import kotlin.reflect.*
import kotlinx.coroutines.Job
import iso.consolator.annotation.Asynchronous
import iso.consolator.annotation.Tag
import iso.consolator.annotation.TagType

internal suspend fun SequencerScope.isActive() =
    sequencer { isCancelled }.isFalse()

internal suspend fun SequencerScope.cancel() =
    Sequencer.cancel()

internal fun SequencerScope.commit(vararg tag: Tag): Any? = TODO()

internal suspend inline fun <R> sequencer(crossinline block: suspend Sequencer.() -> R) = sequencer?.block()

private var sequencer: Sequencer? = null
    get() = field.defaultSingleton().also { field = it }

private typealias SequencerIndex = Int

internal class Sequencer : Synchronizer<AnyTriple>, Transactor<SequencerIndex, BooleanType?>, PriorityQueue<SequencerIndex>, AdjustOperator<AnyTriple, SequencerIndex> {
    constructor() : this(DEFAULT_OBSERVER)

    private constructor(observer: AnyStepObserver) {
        this.observer = observer }

    private val observer: AnyStepObserver
    override var queue: IntMutableList = mutableListOf()
    private var seq: LiveSequence = mutableListOf()

    @Suppress("warnings")
    private val ln_init = -1

    @Suppress("warnings")
    private val ln_start = 0

    @Suppress("warnings")
    private val ln_end get() = ln_term - 1

    @Suppress("warnings")
    private val ln_term get() = seq.size

    @Suppress("warnings")
    private val ln_step = 1

    private var ln = ln_init
        get() = queue.firstOrNull() ?: (field + ln_step)

    private val work
        get() = synchronize { adjust(queue.removeFirst()) }
            .also(::ln::set)

    private var latestStep: LiveStep? = null
    private var latestCapture: Any? = null

    private fun getLifecycleOwner(step: SequencerIndex): LifecycleOwner? = null

    fun setLifecycleOwner(step: SequencerIndex, owner: LifecycleOwner) {}

    fun removeLifecycleOwner(step: SequencerIndex) {}

    fun clearLifecycleOwner(owner: LifecycleOwner) {}

    private val mLock: Any get() = seq

    override fun <R> synchronize(lock: AnyTriple?, block: () -> R) =
        synchronized(mLock, block)

    override fun AnyTriple.from(ref: AnyTriple) = TODO()

    override val descriptor
        get() = object : SequenceDescriptor {
            override fun <A : AnyTriple, B : AnyTriple> A.onValueChanged(value: B, block: BaseSequenceDescriptor.(AnyTriple) -> Any?) = TODO()
        }

    private fun init() {
        ln = ln_init
        clearFlags()
        clearLatestObjects() }

    fun start() {
        init()
        resume() }

    private fun resume(index: SequencerIndex) {
        queue.add(index)
        resume() }

    fun resumeByTag(tag: TagType) {
        resume(getIndex(tag)) }

    fun resume(tag: Tag) =
        resumeByTag(tag.id)

    fun resume() {
        isActive = true
        advance() }

    fun resumeAsync() =
        synchronize { resume() }

    fun resumeAsyncByTag(tag: TagType) =
        synchronize { resumeByTag(tag) }

    var activate = fun() = prepare()
    var next = fun(step: SequencerIndex) = jump(step)
    var run = fun(step: SequencerIndex) = commit(step)
    var bypass = fun(step: SequencerIndex) = capture(step)
    var finish = fun() = end()

    private tailrec fun advance() { tryAvoiding {
        activate() // periodic pre-configuration can be done here
        if (isCompleted) return
        while (next(ln) ?: return /* or issue task resolution */) {
            yield()
            work.let { run(it) ?: bypass(it) }.asBoolean() || return }
        isCompleted = finish() }
        if (isNotCompleted) advance() }

    fun prepare() { if (ln < ln_start) ln = ln_init }

    fun jump(step: SequencerIndex) =
        if (hasError) null
        else synchronize {
            val step = adjust(step)
            ((step >= ln_start) and (step < ln_term) and
            (isNotObserving or seq[step].isAsynchronous()))
            .alsoOnTrue { queue.add(step) } }

    override fun commit(step: SequencerIndex): LiveWorkStateIdentityType? {
        var step = step
        var isLiveWork = false
        fun <R> Any?.determineByType(call: LiveStepPointer.() -> R, get: LiveStepKCallable.() -> R) =
            determineByType({ isLiveWork }, call, get)
        val (liveStep, _, async) =
            synchronize {
                step = adjust(step)
                seq[step] }
            .also { it.determine(
                { isLiveWork = true },
                { isLiveWork = false }) }
        tryPropagating({
            // process tags to reuse live step
            val liveStep = liveStep.determineByType({ invoke() }, { call() })?.asLiveStep()
            yield()
            latestStep = liveStep // live step <-> work
            if (liveStep !== null)
                synchronize {
                    getLifecycleOwner(adjust(step)) }
                ?.let { owner ->
                    liveStep.observe(owner, observer) }
                ?: liveStep.observeForever(observer)
            else return null
        }, { ex ->
            error(ex)
            return false
        })
        isObserving = true
        return if (isLiveWork)
            async.asBooleanType()
        else
            async.asAnnotations().isAsynchronous() }

    fun capture(step: SequencerIndex): LiveWorkStateIdentityType? {
        val work = synchronize { seq[adjust(step)] }
        val capture = work.second
        yield()
        latestCapture = capture
        return (if (capture is AnyKCallable)
            capture.call(work)
        else
            capture.asAnyToAnyFunction()?.invoke(work)).asType() }

    fun end() = queue.isEmpty() and isNotObserving

    fun reset(step: LiveStep? = latestStep) {
        step?.removeObserver(observer)
        isObserving = false }

    fun resetByTag(tag: TagType) {}

    fun cancel(ex: Throwable) {
        isCancelled = true
        this.ex = ex }

    fun cancelByTag(tag: TagType, ex: Throwable) = cancel(ex)

    fun error(ex: Throwable) {
        hasError = true
        this.ex = ex }

    fun errorByTag(tag: TagType, ex: Throwable) = error(ex)

    var interrupt = fun(ex: Throwable) = ex
    var interruptByTag = fun(tag: TagType, ex: Throwable) = ex

    suspend inline fun <R> SequencerScope.resetOnCancel(block: () -> R) =
        reset<CancellationException, _>(::reset, ::cancel, block)

    suspend inline fun <R> SequencerScope.resetOnError(block: () -> R) =
        reset<Throwable, _>(::reset, ::error, block)

    suspend inline fun <R> SequencerScope.resetByTagOnCancel(tag: TagType, block: () -> R) =
        resetByTag<CancellationException, _>(tag, ::resetByTag, ::cancelByTag, block)

    suspend inline fun <R> SequencerScope.resetByTagOnError(tag: TagType, block: () -> R) =
        resetByTag<Throwable, _>(tag, ::resetByTag, ::errorByTag, block)

    suspend inline fun <reified T : Throwable, R> SequencerScope.reset(reset: Work, register: (Throwable) -> Unit, block: () -> R) =
        tryCatching<T, _>(block) { ex ->
            reset()
            register(ex)
            throwIt(interrupt(ex)) }

    suspend inline fun <reified T : Throwable, R> SequencerScope.resetByTag(tag: TagType, reset: (TagType) -> Unit, register: (TagType, Throwable) -> Unit, block: () -> R) =
        tryCatching<T, _>(block) { ex ->
            reset(tag)
            register(tag, ex)
            throwIt(interruptByTag(tag, ex)) }

    // preserve tags
    private fun resettingFirstly(step: SequencerStep) = step after { _, _, _ -> reset() }
    private fun resettingLastly(step: SequencerStep) = step then { _, _, _ -> reset() }
    private fun resettingByTagFirstly(step: SequencerStep) = step after { _, _, _ -> resetByTag(getTag(step)) }
    private fun resettingByTagLastly(step: SequencerStep) = step then { _, _, _ -> resetByTag(getTag(step)) }

    private fun getTag(step: SequencerStep) = returnTag(step)!!

    var isActive = false

    val isNotActive get() = !isActive

    var isObserving = false

    val isNotObserving get() = !isObserving

    var isCompleted = false

    val isNotCompleted get() = !isCompleted

    var isCancelled = false

    val hasNoError get() = !hasError

    var hasError = false

    var ex: Throwable? = null

    private fun yield() { if (isCancelled) rejectWithException<Propagate>() }

    fun clearFlags() {
        isActive = false
        isObserving = false
        isCompleted = false
        isCancelled = false
        clearError() }
    fun clearError() {
        hasError = false
        ex = null }
    fun clearLatestObjects() {
        latestStep = null
        latestCapture = null }
    fun clearObjects() {
        seq.clear()
        queue.clear()
        clearLatestObjects() }

    companion object : AdjustOperator<AnyKCallable, SequencerIndex> {
        var DEFAULT_OBSERVER = AnyStepObserver {
            it?.block() /* or apply (live step) capture function internally */ }

        override fun adjust(index: SequencerIndex): SequencerIndex = index

        override fun attach(step: AnyKCallable, vararg args: Any?): Any? = TODO()

        override fun attach(index: SequencerIndex, step: AnyKCallable, vararg args: Any?): Any? = TODO()

        fun <R> attach(step: SequencerFunction<R>, vararg args: Any?): Any? = TODO()

        fun <R> attach(index: SequencerIndex, step: SequencerFunction<R>, vararg args: Any?): Any? = TODO()

        suspend fun attach(step: AnyTriple, tag: TagType? = step.getTag()) = sequencer { attach(step, tag) }
        suspend fun attachOnce(step: AnyTriple, tag: TagType? = step.getTag()) = sequencer { attachOnce(step, tag) }
        suspend fun attachAfter(step: AnyTriple, tag: TagType? = step.getTag()) = sequencer { attachAfter(step, tag) }
        suspend fun attachBefore(step: AnyTriple, tag: TagType? = step.getTag()) = sequencer { attachBefore(step, tag) }
        suspend fun attachOnceAfter(step: AnyTriple, tag: TagType? = step.getTag()) = sequencer { attachOnceAfter(step, tag) }
        suspend fun attachOnceBefore(step: AnyTriple, tag: TagType? = step.getTag()) = sequencer { attachOnceBefore(step, tag) }

        private fun AnyTriple.getTag() = if (isLiveCall) tag.id else null /* or lookup in temps */

        @Suppress("warnings")
        const val attached_already = -1

        suspend fun start() = sequencer { start() }
        suspend fun resumeByTag(tag: TagType) = sequencer { resumeByTag(tag) }
        suspend fun resume(tag: Tag) = sequencer { resume(tag) }
        suspend fun resume() = sequencer { resume() }
        suspend fun resumeAsync() = sequencer { resumeAsync() }
        suspend fun resumeAsyncByTag(tag: TagType) = sequencer { resumeAsyncByTag(tag) }

        suspend fun isActive() = sequencer { isActive }

        suspend fun cancel() = sequencer { isCancelled = true }

        inline fun <R> Sequencer.withSequence(block: LiveSequence.() -> R) = with(seq, block)
    }

    override fun adjust(index: SequencerIndex) = index

    private fun LiveSequence.attach(element: AnyTriple) =
        add(element)
    private fun LiveSequence.attach(index: SequencerIndex, element: AnyTriple) =
        add(index, element)

    private fun AnyTriple.setTag(tag: TagType?) = this

    private fun getIndex(tag: TagType): SequencerIndex = TODO()

    private fun <N : Number> N?.asIndex() = this?.toInt() ?: 0

    private fun <N : Number> Iterable<N?>.asIndexRange() = IntRange(0, seq.size)

    private fun <N : Number> Iterable<N?>.firstIndex() = first()

    private fun <N : Number> Iterable<N?>.lastIndex() = last()

    private fun Any?.asBoolean() = asBooleanUnsafe()

    override fun attach(step: AnyTriple, vararg args: Any?) =
        synchronize { withSequence {
            attach(step)
            ln_end }.also { index ->
            markTagsForSeqAttach(args.firstOrNull() /* tag */, step, index) } }

    fun attachOnce(work: AnyTriple) =
        attachWhen({ work.isNotAttached() }, work)

    fun attachOnce(work: AnyTriple, tag: TagType? = null): SequencerIndex = TODO()

    fun attachOnce(range: IntRange, work: AnyTriple) =
        attachWhen({ work.isNotAttached(range) }, work)

    fun attachOnce(first: SequencerIndex, last: SequencerIndex, work: AnyTriple) =
        attachWhen({ work.isNotAttached(first, last) }, work)

    override fun attach(index: SequencerIndex, step: AnyTriple, vararg args: Any?) =
        synchronize { withSequence {
            attach(index, step)
            markTagsForSeqAttach(args.firstOrNull() /* tag */, step, index)
            // remark proceeding work in sequence for adjustment
            index } }

    fun attachOnce(index: SequencerIndex, work: AnyTriple) =
        attachWhen({ work.isNotAttached(index) }, index, work)

    fun attachOnce(range: IntRange, index: SequencerIndex, work: AnyTriple) =
        attachWhen({ work.isNotAttached(range, index) }, index, work)

    fun attachOnce(first: SequencerIndex, last: SequencerIndex, index: SequencerIndex, work: AnyTriple) =
        attachWhen({ work.isNotAttached(first, last, index) }, index, work)

    fun attachAfter(work: AnyTriple, tag: TagType? = null) =
        attach(after, work, tag)

    fun attachBefore(work: AnyTriple, tag: TagType? = null) =
        attach(before, work, tag)

    fun attachOnceAfter(work: AnyTriple) =
        attachOnce(after, work)

    fun attachOnceAfter(work: AnyTriple, tag: TagType? = null): SequencerIndex = TODO()

    fun attachOnceBefore(work: AnyTriple) =
        attachOnce(before, work)

    fun attachOnceBefore(work: AnyTriple, tag: TagType? = null): SequencerIndex = TODO()

    private fun stepAfterTrackingTagsForSeqLaunch(step: SequencerStep, index: IntFunction, context: CoroutineContext? = null) =
        (step after { _, _, _ -> currentJob().let { job ->
            synchronize { markTagsForSeqLaunch(step, job, adjust(index()), context) } } })!!

    // optionally or by tag, provide return value of step to the next attached in sequence
    // value type = SequencerStepIdentityType

    fun attach(async: Boolean = false, step: SequencerStep): AnyTriple {
        var index = ln_init
        return stepToNull(async) { liveData(block = {
            stepAfterTrackingTagsForSeqLaunch(step, { index })(null /* optional value */, null /* annotations */, step) }) }
            .also { index = attachByTag(it, step) } }

    fun attach(async: Boolean = false, step: SequencerStep, capture: CaptureFunction): AnyTriple {
        var index = ln_init
        return Triple({ liveData(block = {
            stepAfterTrackingTagsForSeqLaunch(step, { index })(null, null, step) }) },
            capture, async)
            .also { index = attachByTag(it, step) } }

    fun attach(context: CoroutineContext, async: Boolean = false, step: SequencerStep): AnyTriple {
        var index = ln_init
        return stepToNull(async) { liveData(context, block = {
            stepAfterTrackingTagsForSeqLaunch(step, { index }, context)(null, null, step) }) }
            .also { index = attachByTag(it, step) } }

    fun attach(context: CoroutineContext, async: Boolean = false, step: SequencerStep, capture: CaptureFunction): AnyTriple {
        var index = ln_init
        return Triple({ liveData(context, block = {
            stepAfterTrackingTagsForSeqLaunch(step, { index }, context)(null, null, step) }) },
            capture, async)
            .also { index = attachByTag(it, step) } }

    fun attach(index: SequencerIndex, async: Boolean = false, step: SequencerStep) =
        stepToNull(async) { liveData(block = {
            stepAfterTrackingTagsForSeqLaunch(step, { index })(null, null, step) }) }
            .alsoAttachByTag(index, step)

    fun attach(index: SequencerIndex, async: Boolean = false, step: SequencerStep, capture: CaptureFunction) =
        Triple({ liveData(block = {
            stepAfterTrackingTagsForSeqLaunch(step, { index })(null, null, step) }) },
            capture, async)
            .alsoAttachByTag(index, step)

    fun attach(index: SequencerIndex, context: CoroutineContext, async: Boolean = false, step: SequencerStep) =
        stepToNull(async) { liveData(context, block = {
            stepAfterTrackingTagsForSeqLaunch(step, { index }, context)(null, null, step) }) }
            .alsoAttachByTag(index, step)

    fun attach(index: SequencerIndex, context: CoroutineContext, async: Boolean = false, step: SequencerStep, capture: CaptureFunction) =
        Triple({ liveData(context, block = {
            stepAfterTrackingTagsForSeqLaunch(step, { index }, context)(null, null, step) }) },
            capture, async)
            .alsoAttachByTag(index, step)

    fun attachAfter(async: Boolean = false, step: SequencerStep): AnyTriple {
        var index = ln_init
        return stepToNull(async) { liveData(block = {
            stepAfterTrackingTagsForSeqLaunch(step, { index })(null, null, step) }) }
            .also { index = attachAfterByTag(it, step) } }

    fun attachAfter(async: Boolean = false, step: SequencerStep, capture: CaptureFunction): AnyTriple {
        var index = ln_init
        return Triple({ liveData(block = {
            stepAfterTrackingTagsForSeqLaunch(step, { index })(null, null, step) }) },
            capture, async)
            .also { index = attachAfterByTag(it, step) } }

    fun attachAfter(context: CoroutineContext, async: Boolean = false, step: SequencerStep): AnyTriple {
        var index = ln_init
        return stepToNull(async) { liveData(context, block = {
            stepAfterTrackingTagsForSeqLaunch(step, { index }, context)(null, null, step) }) }
            .also { index = attachAfterByTag(it, step) } }

    fun attachAfter(context: CoroutineContext, async: Boolean = false, step: SequencerStep, capture: CaptureFunction): AnyTriple {
        var index = ln_init
        return Triple({ liveData(context, block = {
            stepAfterTrackingTagsForSeqLaunch(step, { index }, context)(null, null, step) }) },
            capture, async)
            .also { index = attachAfterByTag(it, step) } }

    fun attachBefore(async: Boolean = false, step: SequencerStep): AnyTriple {
        var index = ln_init
        return stepToNull(async) { liveData(block = {
            stepAfterTrackingTagsForSeqLaunch(step, { index })(null, null, step) }) }
            .also { index = attachBeforeByTag(it, step) } }

    fun attachBefore(async: Boolean = false, step: SequencerStep, capture: CaptureFunction): AnyTriple {
        var index = ln_init
        return Triple({ liveData(block = {
            stepAfterTrackingTagsForSeqLaunch(step, { index })(null, null, step) }) },
            capture, async)
            .also { index = attachBeforeByTag(it, step) } }

    fun attachBefore(context: CoroutineContext, async: Boolean = false, step: SequencerStep): AnyTriple {
        var index = ln_init
        return stepToNull(async) { liveData(context, block = {
            stepAfterTrackingTagsForSeqLaunch(step, { index }, context)(null, null, step) }) }
            .also { index = attachBeforeByTag(it, step) } }

    fun attachBefore(context: CoroutineContext, async: Boolean = false, step: SequencerStep, capture: CaptureFunction): AnyTriple {
        var index = ln_init
        return Triple({ liveData(context, block = {
            stepAfterTrackingTagsForSeqLaunch(step, { index }, context)(null, null, step) }) },
            capture, async)
            .also { index = attachBeforeByTag(it, step) } }

    private fun AnyTriple.alsoAttachByTag(index: SequencerIndex, step: SequencerStep) = also {
        attachByTag(index, this, step) }

    private fun attachByTag(work: AnyTriple, step: SequencerStep) =
        attach(work, returnTag(step))

    private fun attachByTag(index: SequencerIndex, work: AnyTriple, step: SequencerStep) =
        attach(index, work, returnTag(step))

    private fun attachAfterByTag(work: AnyTriple, step: SequencerStep) =
        attachAfter(work, returnTag(step))

    private fun attachBeforeByTag(work: AnyTriple, step: SequencerStep) =
        attachBefore(work, returnTag(step))

    fun capture(block: CaptureFunction) =
        attach(nullStepTo(block))

    fun captureOnce(block: CaptureFunction) =
        captureWhen({ block.isNotAttached() }, block)

    fun captureOnce(range: IntRange, block: CaptureFunction) =
        captureWhen({ block.isNotAttached(range) }, block)

    fun captureOnce(first: SequencerIndex, last: SequencerIndex, block: CaptureFunction) =
        captureWhen({ block.isNotAttached(first, last) }, block)

    fun capture(index: SequencerIndex, block: CaptureFunction) =
        attach(index, nullStepTo(block))

    fun captureAfter(block: CaptureFunction) =
        attachAfter(nullStepTo(block))

    fun captureBefore(block: CaptureFunction) =
        attachBefore(nullStepTo(block))

    fun captureOnce(index: SequencerIndex, block: CaptureFunction) =
        captureWhen({ block.isNotAttached(index) }, index, block)

    fun captureOnce(range: IntRange, index: SequencerIndex, block: CaptureFunction) =
        captureWhen({ block.isNotAttached(range, index) }, index, block)

    fun captureOnce(first: SequencerIndex, last: SequencerIndex, index: SequencerIndex, block: CaptureFunction) =
       captureWhen({ block.isNotAttached(first, last, index) }, index, block)

    fun captureOnceAfter(block: CaptureFunction) =
        captureOnce(after, block)

    fun captureOnceBefore(block: CaptureFunction) =
        captureOnce(before, block)

    private inline fun attachWhen(crossinline predicate: Predicate, work: AnyTriple) =
        synchronize { blockOnTrueOrReject(predicate) { attach(work) } }

    private inline fun attachWhen(crossinline predicate: Predicate, index: SequencerIndex, work: AnyTriple) =
        synchronize { blockOnTrueOrReject(predicate) { attach(index, work) } }

    private inline fun captureWhen(predicate: Predicate, noinline block: CaptureFunction) =
        blockOnTrueOrReject(predicate) { capture(block) }

    private inline fun captureWhen(predicate: Predicate, index: SequencerIndex, noinline block: CaptureFunction) =
        blockOnTrueOrReject(predicate) { capture(index, block) }

    private inline fun blockOnTrueOrReject(predicate: Predicate, block: IntFunction) =
        if (predicate()) block()
        else attached_already

    private fun stepToNull(async: Boolean = false, step: LiveStepPointer) = Triple(step, nullBlock, async)
    private fun nullStepTo(block: CaptureFunction) = Triple(nullStep, block, false)

    private val nullStep: LiveStepPointer = { null }
    private val nullBlock: CaptureFunction? = null

    private inline fun <R> Any?.determineByType(isLiveWork: AnyClassTypePredicate = Any?::isKCallable, call: LiveStepPointer.() -> R, get: LiveStepKCallable.() -> R) =
        resultWhen({ isLiveWork(this) },
            { asTypeUnsafe<LiveStepPointer>().call() },
            { asTypeUnsafe<LiveStepKCallable>().get() })

    private fun Annotations.isAsynchronous() =
        firstOrNull { it is Asynchronous && it.enabled }.isNotNullValue()

    private fun AnyTriple.isSameWork(work: AnyTriple) =
        isObject(work) or ((first isObject work.first) and (second isObject work.second))

    private fun AnyTriple.isNotSameWork(work: AnyTriple) =
        isNotObject(work) and (first isNotObject work.first) and (second isNotObject work.second)

    private fun AnyTriple.isSameCapture(block: CaptureFunction) =
        second isObject block

    private fun AnyTriple.isNotSameCapture(block: CaptureFunction) =
        second isNotObject block

    private fun AnyTriple.isNotAttached() =
        seq.noneReversed { it.isSameWork(this) }

    private fun AnyTriple.isNotAttached(range: IntRange): Boolean {
        range.forEach {
            if (seq[it].isSameWork(this))
                return false }
        return true }

    private fun AnyTriple.isNotAttached(first: SequencerIndex, last: SequencerIndex): Boolean {
        for (i in first..last)
            if (seq[i].isSameWork(this))
                return false
        return true }

    private fun AnyTriple.isNotAttached(index: SequencerIndex) =
        none(index) { it.isSameWork(this) }

    private fun AnyTriple.isNotAttached(range: IntRange, index: SequencerIndex) = when {
        range.isEmpty() -> true
        index - range.first <= range.last - index ->
            range.none { seq[it].isSameWork(this) }
        else ->
            range.noneReversed { seq[it].isSameWork(this) } }

    private fun AnyTriple.isNotAttached(first: SequencerIndex, last: SequencerIndex, index: SequencerIndex) = when {
        first < last -> true
        index - first <= last - index ->
            seq.none { it.isSameWork(this) }
        else ->
            seq.noneReversed { it.isSameWork(this) } }

    private fun CaptureFunction.isNotAttached() =
        seq.noneReversed { it.isSameCapture(this) }

    private fun CaptureFunction.isNotAttached(range: IntRange): Boolean {
        range.forEach {
            if (seq[it].isSameCapture(this))
                return false }
        return true }

    private fun CaptureFunction.isNotAttached(first: SequencerIndex, last: SequencerIndex): Boolean {
        for (i in first..last)
            if (seq[i].isSameCapture(this))
                return false
        return true }

    private fun CaptureFunction.isNotAttached(index: SequencerIndex) =
        none(index) { it.isSameCapture(this) }

    private fun CaptureFunction.isNotAttached(range: IntRange, index: SequencerIndex) = when {
        range.isEmpty() -> true
        index - range.first <= range.last - index ->
            range.none { seq[it].isSameCapture(this) }
        else ->
            range.noneReversed { seq[it].isSameCapture(this) } }

    private fun CaptureFunction.isNotAttached(first: SequencerIndex, last: SequencerIndex, index: SequencerIndex) = when {
        first < last -> true
        index - first <= last - index ->
            seq.none { it.isSameCapture(this) }
        else ->
            seq.noneReversed { it.isSameCapture(this) } }

    private inline fun none(index: SequencerIndex, predicate: AnyTriplePredicate) = withSequence { when {
        index < size / 2 ->
            none(predicate)
        else ->
            noneReversed(predicate) } }

    private inline fun LiveSequence.noneReversed(predicate: AnyTriplePredicate): Boolean {
        if (isEmpty()) return true
        for (i in ln_end downTo ln_start)
            if (predicate(this[i]))
                return false
        return true }

    private inline fun IntRange.noneReversed(predicate: IntPredicate): Boolean {
        reversed().forEach {
            if (predicate(it))
                return false }
        return true }

    private val leading
        get() = ln_start until withSequence { ln.runUnless({ it < ln_term }) { ln_term } }

    private val trailing
        get() = ln.onPositiveValue { plus(1) } until seq.size

    private val before
        get() = when {
            ln <= ln_start -> ln_start
            ln < ln_term -> ln - ln_step
            else -> ln_term }

    private val after
        get() = when {
            ln < ln_start -> ln_start
            ln < ln_term -> ln + ln_step
            else -> ln_term }
}

internal open class LiveStepItem<R>(target: KCallable<R>) : CoroutineItem<R>(target), Adjustable.By<AnyTriple, SequencerIndex>, Observable {
    init {
        type = Type.LiveStep }

    internal constructor() : this(TODO("keep a temp by tag"))

    override fun onAttach(index: SequencerIndex): LiveStepItem<R> {
        super.onAttach(index)
        onSaveIndex(index)
        return this }

    override fun onAttachBy(container: AnyTriple): LiveStepItem<R> {
        super.onAttachBy(container)
        onSave(LIVEWORK, container)
        return this }

    override fun onObserve(job: Job, index: SequencerIndex, context: CoroutineContext?): LiveStepItem<R> {
        super.onObserve(job, index, context)
        onSaveJob(job)
        onSaveIndex(index) // optionally, readjust by remarks or from seq
        onSaveCoroutineContext(context)
        return this }
}

internal sealed class LiveStepReceiver<T> : MutableLiveData<(suspend () -> T)?>() {
    internal sealed class Synchronizer<T> : LiveStepReceiver<T>(), LiveStepObserver<T>, LiveStepSynchronizer<T>, CaptureFunctionQueue<T>
}

internal sealed interface LiveStepObserver<T> : Observer<(suspend () -> T)?>

internal sealed interface CaptureFunctionQueue<T> : PriorityQueue<() -> T> {
    companion object : BaseCaptureFunctionQueue {
        override var queue: AnyFunctionList = mutableListOf()
            set(value) {
                /* process tags for any purpose */
                field = value }
} }

internal suspend fun SequencerScope.change(stage: ContextStep) =
    getTag(stage).let { tag ->
    resetByTag(tag) {
    commit { foregroundContext.stage(tag to stage) } } }

internal suspend fun <R> SequencerScope.capture(block: () -> R) =
    emit {
        reset()
        block() }

internal suspend fun <R> SequencerScope.captureByTag(tag: TagType, block: () -> R) =
    emit {
        resetByTag(tag)
        block() }

private suspend inline fun <R> SequencerScope.reset(block: () -> R): R {
    reset()
    return block() }

private suspend inline fun <R> SequencerScope.resetByTag(tag: TagType, block: () -> R): R {
    resetByTag(tag)
    return block() }

internal fun SequencerScope.reset() = iso.consolator.reset()
internal fun SequencerScope.resetByTag(tag: TagType) = iso.consolator.resetByTag(tag)

private fun reset() { sequencer?.reset() }
internal fun resetByTag(tag: TagType) { sequencer?.resetByTag(tag) }

internal fun FunctionSet.saveLiveStep(self: AnyKCallable, tag: Tag) =
    save(self, tag, Item.Type.LiveStep)

private fun SequencerStep.setTagTo(step: Step) = this

private fun Any.asLiveStep() = asTypeUnsafe<LiveStep>()
internal fun Any?.asLiveStepItem() = asType<LiveStepItem<*>>()
internal fun Any?.asLiveStepPointer() = asTypeUnsafe<LiveStepPointer>()

internal typealias SequencerScope = LiveDataScope<AnyStep?>
internal typealias SequencerStep = suspend SequencerScope.(Any?, Any?, Any?) -> SequencerStepIdentityType
internal fun interface SequencerFunction<R> : suspend (SequencerScope, Any?, Annotations?, SequencerFunction<R>?) -> R
private typealias SequencerPrediction = suspend SequencerScope.(Any?, Any?) -> PredictionIdentityType
internal fun interface SequencerPredicate<R : BooleanType> : SequencerFunction<R>
private typealias AnyStepObserver = Observer<AnyStep?>
internal typealias LiveStep = LiveData<AnyStep?>
internal typealias LiveStepPointer = () -> LiveStep?
internal typealias CaptureFunction = AnyToAnyFunction
private typealias BaseCaptureFunctionQueue = CaptureFunctionQueue<CaptureFunctionIdentityType>
private typealias LiveSequence = MutableList<AnyTriple>

private typealias SequencerStepIdentityType = Any? /* symbolic to AnyStep in LiveStep and AnyStepObserver, and AnyFunction in Scheduler */
private typealias CaptureFunctionIdentityType = Any? /* symbolic to AnyToAnyFunction */
internal typealias LiveStepIdentityType = Any? /* symbolic to SequencerStep */