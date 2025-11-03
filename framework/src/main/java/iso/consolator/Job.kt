@file:JvmName(JVM_CLASS_NAME)
@file:JvmMultifileClass

package iso.consolator

import androidx.lifecycle.*
import kotlin.coroutines.*
import kotlin.reflect.*
import kotlinx.coroutines.*
import iso.consolator.annotation.Tag
import iso.consolator.annotation.TagType

internal sealed interface Continuum {
    val reason: Rationale // internally known reason for starting a job continuation
}

internal fun SchedulerNode.close(): Boolean = true

internal fun Job.close(node: SchedulerNode): Boolean = true

internal val Job.node: SchedulerNode
    get() = TODO()

internal fun FunctionSet.saveCoroutine(self: AnyKCallable, tag: Tag) =
    save(self, tag, Item.Type.Coroutine)

internal fun FunctionSet.saveCoroutine(self: AnyKCallable, tag: TagType) =
    save(self, tag, Item.Type.Coroutine)

internal open class CoroutineItem<R>(target: KCallable<R>) : Item<R>(target) {
    init {
        type = Type.Coroutine }

    open fun onSaveLifecycleOwner(owner: LifecycleOwner?) = this

    open fun onSaveCoroutineContext(context: CoroutineContext?) = this

    open fun onSaveCoroutineStart(start: CoroutineStart) = this

    open fun onSaveJob(job: Job) = this

    open fun onContextReform(job: Job, stage: ContextStep?, form: AnyStep): Item<R> {
        onSaveJob(job)
        onSave(CTX_STEP, stage)
        onSave(FORM, form)
        return this }

    open fun onJobLaunch(job: Job, context: CoroutineContext, start: CoroutineStart): CoroutineItem<R> {
        onSaveJob(job)
        onSaveCoroutineContext(context)
        onSaveCoroutineStart(start)
        return this }

    open fun onJobRelaunch(job: Job, owner: LifecycleOwner?, context: CoroutineContext, start: CoroutineStart) =
        onSaveLifecycleOwner(owner)
        .onJobLaunch(job, context, start)

    open fun <I : CoroutineScope, T> onJobContinuationRepeat(block: suspend (I?, Any?) -> T, self: AnyKCallable, tag: Tag, job: Job, predicate: Prediction?, delay: DelayFunction): CoroutineItem<R> {
        // optionally, replace target with self
        self.asType<KCallable<R>>()?.apply(::setTarget)
        updateJob(job, predicate, delay)
        return this }

    open fun <I : CoroutineScope, S, T> onJobExtensionRepeat(block: suspend (I?, Any?, S) -> T, self: AnyKCallable, tag: Tag, job: Job, predicate: Prediction?, delay: DelayFunction): CoroutineItem<R> {
        updateJob(job, predicate, delay)
        return this }

    private fun updateJob(job: Job, predicate: Prediction?, delay: DelayFunction) {
        onSaveJob(job)
        onSave(PREDICATE, predicate)
        onSave(DELAY, delay) }
}

internal fun Any?.asCoroutineItem() = asType<CoroutineItem<*>>()

// enables marked job and itemizes steps (optionally include context as argument)
internal fun Job.applySaveNewElement(step: AnyCoroutineStep) = this

// applying statement performs an operation on last marked step and next chained item
// and returns the next step attached in the resolved chain
private inline fun Job.applyToElement(crossinline statement: AnyCoroutinePointer): AnyCoroutineStep = TODO()

internal fun <R> Job.attachConjunctionToElement(target: suspend SchedulerScope.(Any?, Job) -> R, operation: (AnyCoroutineStep, suspend SchedulerScope.(Any?, Job) -> R) -> AnyCoroutineStep?): AnyCoroutineStep =
    applyToElement { operation(run(::lastMarkedCoroutineStep), target) }

internal fun Job.attachPredictionToElement(predicate: SchedulerPrediction, operation: (AnyCoroutineStep, SchedulerPrediction) -> AnyCoroutineStep?): AnyCoroutineStep =
    applyToElement { operation(run(::lastMarkedCoroutineStep), predicate) }

internal fun Job.attachPredictionToElement(predicate: Predicate, operation: (AnyCoroutineStep, Predicate) -> AnyCoroutineStep?): AnyCoroutineStep =
    applyToElement { operation(run(::lastMarkedCoroutineStep), predicate) }

private fun Job.markedCoroutineStep(): AnyCoroutineStep = TODO()

private fun Job.lastMarkedCoroutineStep(): AnyCoroutineStep = TODO()

private fun Job.getTag() = markedCoroutineStep().asReference().tag!!.id

// from this point on, step and context are the same
// this may be the coroutine context or the context of the step for the job
// steps that are concurrent (by design) will be double-pointed for uniqueness

internal fun <R, S> (suspend CoroutineScope.() -> R).attachToContext(next: suspend CoroutineScope.() -> S): AnyCoroutineStep = TODO()

private fun <R> (suspend CoroutineScope.() -> R)?.markedJob(): Job = TODO()

internal fun <R, S> (suspend CoroutineScope.() -> R)?.contextReferring(next: suspend SchedulerScope.(Any?, Job) -> S?): Any? = TODO()

internal suspend fun <R> CoroutineScope.take(next: suspend CoroutineScope.() -> R): Any? {
    /* mark value (auto-register) - throw or return value */
    TODO() }

internal suspend fun <R> CoroutineScope.take(next: suspend SchedulerScope.(Any?, Job) -> R, context: Any?, job: Job): Any? {
    /* throw or return value */
    TODO() }

internal suspend fun <R> (suspend CoroutineScope.() -> R)?.isCurrentlyTrueGiven(predicate: SchedulerPrediction): Boolean = TODO()

internal suspend fun <R> (suspend CoroutineScope.() -> R)?.isCurrentlyTrueGiven(predicate: Predicate) =
    predicate()

internal suspend fun <R> (suspend CoroutineScope.() -> R)?.isCurrentlyFalseGiven(predicate: SchedulerPrediction): Boolean = TODO()

internal suspend fun <R> (suspend CoroutineScope.() -> R)?.isCurrentlyFalseGiven(predicate: Predicate) =
    predicate().not()

internal suspend fun <R, S> (suspend CoroutineScope.() -> R)?.isCurrentlyFalseReferring(target: suspend SchedulerScope.(Any?, Job) -> S) =
    currentConditionReferring(target).not()

private suspend fun <R> (suspend CoroutineScope.() -> R)?.currentCondition() = true

private suspend fun <R, S> (suspend CoroutineScope.() -> R)?.currentConditionReferring(target: suspend SchedulerScope.(Any?, Job) -> S) = true

private suspend fun <R> (suspend CoroutineScope.() -> R)?.accept(): Any? { TODO() }

internal suspend fun <R> (suspend CoroutineScope.() -> R)?.acceptOnTrue(): Any? {
    /* current context must resolve first then provide the next step */
    TODO() }

internal suspend fun <R> (suspend CoroutineScope.() -> R)?.acceptOnFalse(): Any? { TODO() }

internal suspend fun <R, S> (suspend CoroutineScope.() -> R)?.acceptOnFalseReferring(target: suspend SchedulerScope.(Any?, Job) -> S): Any? {
    /* target may be switched in-place here */
    target.annotatedOrCurrentScope()
        .take(target, contextReferring(target), markedJob())
    TODO() }

private suspend fun <R> (suspend CoroutineScope.() -> R)?.reject(): Any? { TODO() }

internal suspend fun <R> (suspend CoroutineScope.() -> R)?.rejectOnTrue(): Any? { TODO() }

internal suspend fun <R> (suspend CoroutineScope.() -> R)?.rejectOnFalse(): Any? { TODO() }

internal suspend fun <R, S> (suspend CoroutineScope.() -> R)?.rejectOnFalseReferring(target: suspend SchedulerScope.(Any?, Job) -> S): Any? {
    /* target must be used to find the next step in current context */
    TODO() }

internal fun <R> (suspend CoroutineScope.() -> R)?.annotatedOrCurrentScope(): CoroutineScope = TODO()

internal fun <R> (suspend SchedulerScope.(Any?, Job) -> R).annotatedOrCurrentScope(): CoroutineScope = TODO()

internal fun <R> (suspend CoroutineScope.() -> R).annotatedOrCurrentScopeReferring(target: suspend SchedulerScope.(Any?, Job) -> R): CoroutineScope = TODO()

internal fun <R> (suspend SchedulerScope.(Any?, Job) -> R).annotatedOrCurrentScopeReferring(target: AnyCoroutineStep): CoroutineScope = TODO()

// from this point on, job controllers handle the execution of individual steps
// following a build-structure form that is also reconfigurable they react to other continuations

private fun Job.currentCoroutineStep(): AnyCoroutineStep = TODO()

internal suspend fun currentJob() = currentCoroutineContext().job
internal fun currentThreadJob() = ::currentJob.block()

internal val Job?.isNotActive get() = this === null || !isActive

internal fun Any?.toJobId() = asJob().hashCode()

internal fun Any?.asJob() = asType<Job>()

internal inline fun <T : Job> KMutableProperty<out T?>.requireActive(block: () -> T?) =
    require(Job?::isNotActive, block)

private typealias JobKFunction = KFunction<Job?>
internal typealias JobKProperty = KMutableProperty<Job?>