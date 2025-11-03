@file:JvmName(JVM_CLASS_NAME)
@file:JvmMultifileClass

package iso.consolator

import android.app.*
import android.content.*
import android.content.res.*
import android.os.*
import android.view.*
import androidx.annotation.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.*
import androidx.core.content.ContextCompat.registerReceiver
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.room.*
import ctx.consolator.*
import data.consolator.*
import iso.consolator.AdjustOperator.Element.Adjustable
import iso.consolator.AttachOperator.Element.Attachable
import iso.consolator.Scheduler.commit
import iso.consolator.State.*
import iso.consolator.Track.Item.*
import iso.consolator.annotation.*
import iso.consolator.annotation.Ignore
import iso.consolator.annotation.Implicit
import iso.consolator.annotation.LaunchContext.Type.*
import iso.consolator.annotation.Path.*
import iso.consolator.component.*
import iso.consolator.component.SchedulerActivity.*
import iso.consolator.exception.*
import iso.consolator.reflect.*
import java.io.*
import java.lang.*
import kotlin.annotation.AnnotationRetention.*
import kotlin.annotation.AnnotationTarget.*
import kotlin.coroutines.*
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.flow.*
import androidx.core.content.ContextCompat.RECEIVER_EXPORTED
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main

interface BaseServiceScope : ResolverScope, ReferredContext, UniqueContext.Instance {
    fun Intent?.invoke(flags: Int, startId: Int, mode: Int): Int? {
        if (SchedulerScope.isClockPreferred)
            startClockSafely()
        if (State[2] !is Resolved)
            withCallableScope {
            ::resolveState2.let { target ->
            target.commitSuspend(
                target,
                this@invoke,
                target.tag) // runtime error: tag is null  --  error requires to be re-evaluated after Kotlin 1.9
            } }
        return mode }

    @Resolving.State(2)
    @Synchronous @Tag(SVC_INIT)
    private suspend fun resolveState2(intent: Intent?, tag: Tag? = null) {
        if (State[2] is Resolved) return
        intent.setStartTime()
        fun Any?.applyTrackTag() =
            tag.result { applyTrackTag(this, items) }
        if (isLogDbNull) {
            @LaunchContext(IO) @LaunchMode(async = true)
            @Tag(STAGE_BUILD_LOG_DB)
            suspend fun buildLogDatabase(scope: SequencerScope, obj: Any?, annotations: AnnotationsMap, self: Any?) =
                scope.coordinateBuildDatabase(
                    self.applyTrackTag(),
                    ::logDb,
                    SchedulerScope::stageLogDatabaseCreated)
            Sequencer.attach(
                ::buildLogDatabase)
        }
        if (isNetDbNull) {
            @LaunchContext(IO) @LaunchMode(async = true)
            @Tag(STAGE_BUILD_NET_DB)
            suspend fun buildNetDatabase(scope: SequencerScope, obj: Any?, annotations: AnnotationsMap, self: Any?) =
                scope.coordinateBuildDatabase(
                    self.applyTrackTag(),
                    ::netDb,
                    SchedulerScope::stageNetDatabaseInitialized)
            Sequencer.attach(
                ::buildNetDatabase)
        }
        Sequencer.resumeAsync() }

    private fun Intent?.startClockSafely() = result {
        if (hasCategory(START_TIME_KEY))
            Clock.startSafely() }

    private fun Intent?.setStartTime() = result {
        trySafelyForResult {
        getLongExtra(
            START_TIME_KEY,
            foregroundContext.uniqueStartTimeOrNow()) } }
        ?.apply(::uid::set)

    private suspend inline fun <reified D : RoomDatabase> SequencerScope.coordinateBuildDatabase(identifier: Any?, instance: KMutableProperty<out D?>, noinline stage: ContextStep?) =
        buildDatabaseOrResetByTag(identifier, instance, stage, synchronize(identifier, stage))

    private suspend inline fun <reified D : RoomDatabase> SequencerScope.buildDatabaseOrResetByTag(identifier: Any?, instance: KMutableProperty<out D?>, noinline stage: ContextStep?, noinline post: AnyStep) =
        buildDatabaseOrResetByTag(identifier, instance, stage, post, ::whenNotNullOrResetByTag)

    private suspend inline fun <reified D : RoomDatabase> SequencerScope.buildDatabaseOrResetByTag(identifier: Any?, instance: KMutableProperty<out D?>, noinline stage: ContextStep?, noinline post: AnyStep, condition: PropertyBuildCondition) =
        returnTag(identifier)?.let { tag ->
        buildDatabaseOrResetByTag(instance, tag).also {
        condition(instance, tag,
            formAfterMarkingTagsForCtxReform(tag, currentJob(), stage, post)) } }

    private suspend inline fun <reified D : RoomDatabase> SequencerScope.buildDatabaseOrResetByTag(instance: KMutableProperty<out D?>, tag: TagType) =
        ref?.get()?.run {
        determine(instance, tag, ::buildDatabase) }

    private suspend inline fun <reified R> SequencerScope.determine(instance: KMutableProperty<out R?>, tag: TagType, block: KFunction<R>) =
        perceiveOrResetByTag(instance, tag, block)
        ?.apply(instance::setInstance)

    private suspend inline fun <reified R> SequencerScope.perceiveOrResetByTag(instance: KMutableProperty<out R?>, tag: TagType, block: KFunction<R>) =
        sequencer { trySafelyCanceling {
        resetByTagOnError(tag) {
        commitAsyncAndResetByTag(instance, tag, block::call) } } }

    private suspend inline fun <R> SequencerScope.commitAsyncAndResetByTag(lock: AnyKProperty, tag: TagType, crossinline block: () -> R) =
        commitAsyncOrResetByTag(lock, tag) { block().also { resetByTag(tag) } }

    private suspend inline fun <R> SequencerScope.commitAsyncOrResetByTag(lock: AnyKProperty, tag: TagType, crossinline block: () -> R) =
        commitAsyncOrFallback(lock, ::trueWhenNull, block) { resetByTag(tag) }

    private fun SequencerScope.synchronize(identifier: Any?, stage: ContextStep?) =
        formOrIgnore(stage) { form(it) }

    private inline fun SequencerScope.formOrIgnore(noinline stage: ContextStep?, crossinline block: (ContextStep) -> Any) =
        if (stage !== null) { { block(stage) } }
        else EMPTY_STEP.applyIgnoreOnce()

    private fun SequencerScope.form(stage: ContextStep) =
        suspend { change(stage) }

    private fun formAfterMarkingTagsForCtxReform(tag: TagType, job: Job, stage: ContextStep?, form: AnyStep) =
        (form afterInternalSuspended { markTagsForCtxReform(tag, job, stage, form) })!!

    private suspend inline fun whenNotNullOrResetByTag(instance: AnyKProperty, stage: TagType, step: AnyStep) =
        if (instance.isNotNull())
            step()
        else resetByTag(stage)

    private inline infix fun <R, S> (suspend () -> R)?.thenInternalSuspended(crossinline next: suspend () -> S): (suspend () -> S)? =
        thenSuspended(next)

    private inline infix fun <R, S> (suspend () -> R)?.afterInternalSuspended(crossinline prev: suspend () -> S): (suspend () -> R)? =
        afterSuspended(prev)

    override fun commit(step: AnyCoroutineStep) =
        step.markTagForSvcCommit().let { step ->
        SchedulerScope.DEFAULT_OPERATOR?.invoke(step)
        ?: attach(step) { launch { step() } } }
}

sealed interface SchedulerScope : ResolverScope {
    companion object : DefaultScope by ImplicitScope, ProcessScope, CoroutineStepTransactor {
        @JvmStatic fun Application.commitStart(component: KClass<out Service>) {
            commitStart()
            if (typeIs<BaseServiceScope, _>(component) or
                (this is SchedulerApplication &&
                isSchedulerServiceEnabled))
                startService(component) }

        private fun Application.commitStart() {
            instance = this
            if (typeIs<SchedulerApplication, _>()) {
                init(::configureSchedulerScope)
                invoke(::startSchedulerScope) } }

        @Tag(SCH_CONFIG)
        internal fun configureSchedulerScope() {
            enableLogger()
            enableAllLogs()
            preferClock()
            preferScheduler() }

        @Tag(SCH_START)
        internal fun startSchedulerScope() {
            if (isClockPreferred)
                clock = Clock("$SERVICE", Thread.MAX_PRIORITY, ::initClock)
                .alsoStart() }

        private fun startService(component: KClass<out Service>) {
            withForegroundContext {
            startService(
            intendFor(component)
            .putExtra(START_TIME_KEY,
                startTime)) } }

        internal fun AppCompatActivity.commitStart() {
            foregroundLifecycleOwner = this }

        internal fun Activity.commitRestart() {}

        internal fun Activity.commitResume() {}

        internal fun Activity.commitPause() {}

        internal fun Activity.commitStop() {
            foregroundLifecycleOwner = null }

        internal fun Activity.commitDestroy() {}

        internal fun Activity.commitSaveInstanceState(outState: Bundle) {}

        internal fun Fragment.commitAttach(context: Context) {}

        internal fun Fragment.commitStart() {
            if ((foregroundLifecycleOwner isObject activity) or
                foregroundLifecycleOwner.typeIs<SchedulerFragment, _>() or
                foregroundLifecycleOwner.isNullValue())
                foregroundLifecycleOwner = this }

        internal fun Fragment.commitResume() {}

        internal fun Fragment.commitPause() {}

        internal fun Fragment.commitStop() {
            if (foregroundLifecycleOwner isObject this)
                foregroundLifecycleOwner =
                    parentFragment.asType<SchedulerFragment>()
                    ?: activity.asType<SchedulerActivity>() }

        internal fun Fragment.commitSaveInstanceState(outState: Bundle) {}

        internal fun Fragment.commitDestroyView() {}

        internal fun Fragment.commitDestroy() {}

        internal fun Fragment.commitDetach() {}

        @JvmStatic val LifecycleOwner.isForegroundLifecycleOwner
            get() = isObject(foregroundLifecycleOwner)

        @JvmStatic fun buildRuntimeDatabase(context: Context) =
            commitBuildDatabase(context, ::runDb)

        @Diverging(["$STAGE_BUILD_RUN_DB"])
        @JvmStatic fun stageRuntimeDatabaseCreated(context: Context, scope: Any?) {
            /* bootstrap */ }

        @JvmStatic suspend fun buildSession() {
            if (isSessionNull)
                buildNewSession(foregroundContext.startTime) }

        @Diverging(["$STAGE_BUILD_SESSION"])
        @JvmStatic fun stageSessionCreated(context: Context, scope: Any?) {
            /* update db records */ }

        @Diverging(["$STAGE_BUILD_NET_DB"])
        internal fun stageNetDatabaseInitialized(context: Context, scope: Any?) {
            /* update net function pointers */ }

        @Path("$STAGE_INIT_NET_DB") @Proceeding(["$STAGE_BUILD_NET_DB"])
        internal suspend fun initNetworkDatabase() {
            updateNetworkCapabilities()
            updateNetworkState() }

        @Diverging(["$STAGE_BUILD_LOG_DB"])
        internal fun stageLogDatabaseCreated(context: Context, scope: Any?) {
            mainUncaughtExceptionHandler.setToFunction(
                dbUncaughtExceptionHandler::uncaughtException) }

        private val dbUncaughtExceptionHandler = object : MainUncaughtExceptionHandler {
            @Tag(UNCAUGHT_DB)
            override fun uncaughtException(thread: Thread, ex: Throwable) {
                /* record in db safely */
            }
        }

        @JvmStatic fun LifecycleOwner.detach(job: Job? = null) {}

        @JvmStatic fun LifecycleOwner.reattach(job: Job? = null) {}

        @JvmStatic fun LifecycleOwner.close(job: Job? = null) {}

        @JvmStatic fun LifecycleOwner.detach(node: SchedulerNode) {}

        @JvmStatic fun LifecycleOwner.reattach(node: SchedulerNode) {}

        @JvmStatic fun LifecycleOwner.close(node: SchedulerNode) {}

        @JvmStatic fun <R> LifecycleOwner.launch(context: CoroutineContext = SchedulerContext, start: CoroutineStart = CoroutineStart.DEFAULT, step: suspend CoroutineScope.() -> R): Job? {
            val scope = step.annotatedScope ?: annotatedScope?.firstOrNull() ?: lifecycleScope
            val (context, start, step) = scope.determineCoroutine(this, context, start, step)
            step.markTagForFloLaunch()
                .afterTrackingTagsForJobLaunch(this, context, start).let { trackedStep ->
            return scope.launch(context, start) { trackedStep() }
                .applySaveNewElement(step) } }

        internal fun <R> LifecycleOwner.relaunch(instance: JobKProperty, group: FunctionSet?, context: CoroutineContext = SchedulerContext, start: CoroutineStart = CoroutineStart.DEFAULT, step: suspend CoroutineScope.() -> R) =
            relaunch(instance, context, start, step)
            .also { job -> markTagsInGroupForJobRelaunch(instance, step, group, job, this, context, start) }

        private fun <R> LifecycleOwner.relaunch(instance: JobKProperty, context: CoroutineContext, start: CoroutineStart, step: suspend CoroutineScope.() -> R) =
            Scheduler.relaunch(instance, context, start, step)

        internal suspend fun delayOrYield(dt: Time = no_delay) = dt.blockOrYield()

        internal suspend inline fun <reified T : CancellationException> delayOrCancel(dt: Time = no_delay, msg: String? = null) {
            runUnlessOrRejectWithException<T, _, _>(msg,
                predicate = currentJob()::isCancelled,
                block = { dt.blockOrYield() }) }

        internal suspend inline fun <reified T : TimeoutCancellation> delayOrTimeout(dt: Time = no_delay, downtime: Time = no_timeout, msg: String? = null, cause: Throwable? = null) {
            runUnlessOrRejectWithException<T, _, _>(msg, cause,
                predicate = downtime::isTimedOut,
                block = { dt.blockOrYield() }) }

        internal suspend inline fun <reified T : TimeoutCancellation> delayOrTimeout(dt: Time = no_delay, downtime: Time = no_timeout, crossinline ex: (AnyArray) -> T, vararg args: Any?) {
            runUnlessOrRejectWithException<T, _, _>(ex, *args,
                predicate = downtime::isTimedOut,
                block = { dt.blockOrYield() }) }

        context(_: LifecycleOwner, _: Job)
        @JvmStatic infix fun <R> Job?.then(next: suspend SchedulerScope.(Any?, Job) -> R) = result {
            attachConjunctionToElement(next) { last, next ->
                last then next } }

        context(_: LifecycleOwner, _: Job)
        @JvmStatic infix fun <R> Job?.after(prev: suspend SchedulerScope.(Any?, Job) -> R) = result {
            attachConjunctionToElement(prev) { last, prev ->
                last after prev } }

        context(_: LifecycleOwner, _: Job)
        @JvmStatic infix fun Job?.given(predicate: SchedulerPrediction) = result {
            attachPredictionToElement(predicate) { last, predicate ->
                last given predicate } }

        context(_: LifecycleOwner, _: Job)
        @JvmStatic infix fun Job?.given(predicate: Predicate) = result {
            attachPredictionToElement(predicate) { last, predicate ->
                last given predicate } }

        context(_: LifecycleOwner, _: Job)
        @JvmStatic infix fun Job?.unless(predicate: SchedulerPrediction) = result {
            attachPredictionToElement(predicate) { last, predicate ->
                last unless predicate } }

        context(_: LifecycleOwner, _: Job)
        @JvmStatic infix fun Job?.unless(predicate: Predicate) = result {
            attachPredictionToElement(predicate) { last, predicate ->
                last unless predicate } }

        context(_: LifecycleOwner, _: Job)
        @JvmStatic infix fun <R> Job?.otherwise(next: suspend SchedulerScope.(Any?, Job) -> R) = result {
            attachConjunctionToElement(next) { last, next ->
                last otherwise next } }

        context(_: LifecycleOwner, _: Job)
        @JvmStatic infix fun <R> Job?.onCancel(action: suspend SchedulerScope.(Any?, Job) -> R) = result {
            attachConjunctionToElement(action) { last, action ->
                last onCancel action } }

        context(_: LifecycleOwner, _: Job)
        @JvmStatic infix fun <R> Job?.onError(action: suspend SchedulerScope.(Any?, Job) -> R) = result {
            attachConjunctionToElement(action) { last, action ->
                last onError action } }

        context(_: LifecycleOwner, _: Job)
        @JvmStatic infix fun <R> Job?.onTimeout(action: suspend SchedulerScope.(Any?, Job) -> R) = result {
            attachConjunctionToElement(action) { last, action ->
                last onTimeout action } }

        @JvmStatic infix fun <R, S> (suspend CoroutineScope.() -> R)?.then(next: suspend SchedulerScope.(Any?, Job) -> S) = letResult { prev ->
            attachToContext {
                prev.annotatedOrCurrentScopeReferring(next)
                    .take(prev)
                next.annotatedOrCurrentScope()
                    .take(next, prev.contextReferring(next), currentJob()) } }

        @JvmStatic infix fun <R, S> (suspend CoroutineScope.() -> R)?.after(prev: suspend SchedulerScope.(Any?, Job) -> S) = letResult { next ->
            attachToContext {
                prev.annotatedOrCurrentScopeReferring(next)
                    .take(prev, next.contextReferring(prev), currentJob())
                next.annotatedOrCurrentScope()
                    .take(next) } }

        // referables can be employed for optimizations in look-ahead strategy
        @JvmStatic infix fun <R> (suspend CoroutineScope.() -> R)?.given(predicate: SchedulerPrediction) = letResult { cond ->
            attachToContext {
                if (cond.isCurrentlyTrueGiven(predicate))
                    cond.acceptOnTrue()
                else cond.rejectOnTrue() } }

        @JvmStatic infix fun <R> (suspend CoroutineScope.() -> R)?.given(predicate: Predicate) = letResult { cond ->
            attachToContext {
                if (cond.isCurrentlyTrueGiven(predicate))
                    cond.acceptOnTrue()
                else cond.rejectOnTrue() } }

        @JvmStatic infix fun <R> (suspend CoroutineScope.() -> R)?.unless(predicate: SchedulerPrediction) = letResult { cond ->
            attachToContext {
                if (cond.isCurrentlyFalseGiven(predicate))
                    cond.acceptOnFalse()
                else cond.rejectOnFalse() } }

        @JvmStatic infix fun <R> (suspend CoroutineScope.() -> R)?.unless(predicate: Predicate) = letResult { cond ->
            attachToContext {
                if (cond.isCurrentlyFalseGiven(predicate))
                    cond.acceptOnFalse()
                else cond.rejectOnFalse() } }

        @JvmStatic infix fun <R, S> (suspend CoroutineScope.() -> R)?.otherwise(next: suspend SchedulerScope.(Any?, Job) -> S) = letResult { cond ->
            attachToContext {
                if (cond.isCurrentlyFalseReferring(next))
                    cond.acceptOnFalseReferring(next)
                else cond.rejectOnFalseReferring(next) } }

        @JvmStatic infix fun <R, S> (suspend CoroutineScope.() -> R)?.onCancel(action: suspend SchedulerScope.(Any?, Job) -> S): AnyCoroutineStep? = TODO()

        @JvmStatic infix fun <R, S> (suspend CoroutineScope.() -> R)?.onError(action: suspend SchedulerScope.(Any?, Job) -> S): AnyCoroutineStep? = TODO()

        @JvmStatic infix fun <R, S> (suspend CoroutineScope.() -> R)?.onTimeout(action: suspend SchedulerScope.(Any?, Job) -> S): AnyCoroutineStep? = TODO()

        @JvmStatic infix fun Job?.thenJob(next: Job) = this

        @JvmStatic infix fun Job?.afterJob(prev: Job) = this

        @JvmStatic infix fun Job?.givenJob(predicate: Predicate) = this

        @JvmStatic infix fun Job?.unlessJob(predicate: Predicate) = this

        @JvmStatic infix fun Job?.otherwiseJob(next: Job) = this

        @JvmStatic infix fun Job?.onCancelJob(action: Job) = this

        @JvmStatic infix fun Job?.onErrorJob(action: Job) = this

        @JvmStatic infix fun Job?.onTimeoutJob(action: Job) = this

        @JvmStatic infix fun Job?.commit(step: SchedulerStep): Job? = this

        @JvmStatic suspend inline fun <I : CoroutineScope, R> I.line(vararg id: Any?, step: CoroutineScope.() -> R): AnyCoroutineStep? = TODO()

        @JvmStatic suspend inline fun <I : CoroutineScope, R> I.path(vararg id: Any?, step: CoroutineScope.() -> R): AnyCoroutineStep? = TODO()

        @JvmStatic val RESOLVE: SchedulerStep = { context, job -> resolve(context, job) }

        @JvmStatic fun <I : CoroutineScope> I.resolve(context: Any?, job: Job) {}

        @JvmStatic fun <I : CoroutineScope> I.resolve(exit: ExitWork? = null, job: Job) {}

        @JvmStatic fun <I : CoroutineScope> I.resolve(vararg map: ErrorConvertor, job: Job) {}

        @JvmStatic fun <I : CoroutineScope, X : Throwable, R> I.resolve(conversion: ErrorMap<X, R>? = null, job: Job) {}

        @JvmStatic fun <I : CoroutineScope> I.error(context: Any?, job: Job) {}

        @JvmStatic fun <I : CoroutineScope> I.error(job: Job, exit: ExitWork? = null) {}

        @JvmStatic val RETRY: SchedulerStep = { context, job -> retry(context, job) }

        @JvmStatic fun <I : CoroutineScope> I.retry(context: Any?, job: Job) {}

        @JvmStatic fun <I : CoroutineScope> I.retry(job: Job, exit: ExitWork? = null) {}

        @JvmStatic fun <I : CoroutineScope> I.close(context: Any?, job: Job) {}

        @JvmStatic fun <I : CoroutineScope> I.close(job: Job, exit: ExitWork? = null) {}

        @JvmStatic fun <I : CoroutineScope> I.keepAlive(job: Job) = keepAliveNode(job.node)

        @JvmStatic fun <I : CoroutineScope> I.keepAliveOrClose(job: Job) = keepAliveOrCloseNode(job.node)

        @JvmStatic fun <I : CoroutineScope> I.keepAliveNode(node: SchedulerNode): Boolean = false

        @JvmStatic fun <I : CoroutineScope> I.keepAliveOrCloseNode(node: SchedulerNode) =
            keepAliveNode(node) or node.close()

        @JvmStatic fun Job.close() {}

        @JvmStatic fun change(stage: ContextStep) =
            commit { foregroundContext.stage(stage) }

        @JvmStatic fun <R> change(member: KFunction<R>, stage: ContextStep) =
            commit { foregroundContext.stage(member to stage) }

        @JvmStatic fun <R> changeLocally(owner: LifecycleOwner, member: KFunction<R>, stage: ContextStep) =
            commit { foregroundContext.stage((owner to member) to stage) }

        @JvmStatic fun <R> changeBroadly(ref: WeakContext, member: KFunction<R>, stage: ContextStep) =
            commit { foregroundContext.stage((ref to member) to stage) }

        @JvmStatic fun <R> changeGlobally(owner: LifecycleOwner, ref: WeakContext, member: KFunction<R>, stage: ContextStep) =
            commit { foregroundContext.stage(Triple(owner, ref, member) to stage) }

        context(scope: I)
        @JvmStatic suspend inline fun <reified T : Resolver, I : CoroutineScope> I.defer(crossinline step: suspend T.() -> Any?) =
            step(this@defer as T) /* error! */

        context(scope: I)
        @JvmStatic suspend inline fun <reified T : Resolver, I : CoroutineScope> I.defer(member: AnyKFunction, crossinline step: suspend T.() -> Any?) =
            step(this@defer as T) /* error! */

        context(scope: I)
        @JvmStatic suspend inline fun <reified T : Resolver, I : CoroutineScope> I.defer(receiver: I = scope, member: AnyKFunction, crossinline step: suspend T.() -> Any?) =
            step(this@defer as T) /* error! */

        @JvmStatic suspend fun currentContext() =
            currentJob().getInstance(CONTEXT).asWeakContext()?.get()!!

        @JvmStatic suspend inline fun <R> tryCancelingWithContext(crossinline block: suspend Context.() -> R) =
            tryCanceling { currentContext().block() }

        @JvmStatic suspend inline fun <R> tryCanceling(crossinline block: suspend () -> R) =
            tryCanceling(null, block)

        @JvmStatic suspend inline fun <R> tryCanceling(msg: String?, crossinline block: suspend () -> R) =
            tryCancelingSuspended(msg, block)

        @JvmStatic fun Message.send() {}

        @JvmStatic fun Message.sendDelayed(delay: Time) {}

        @JvmStatic fun Message.sendAtTime(uptime: Time) {}

        @JvmStatic fun Runnable.start() {}

        @JvmStatic fun Runnable.startDelayed(delay: Time) {}

        @JvmStatic fun Runnable.startAtTime(uptime: Time) {}

        @JvmStatic infix fun Runnable.then(next: Runnable): Runnable = apply {
            attachConjunctionToRunnable(next) { last, next ->
                (last thenRun next).callback } }

        @JvmStatic infix fun Runnable.after(prev: Runnable): Runnable = apply {
            attachConjunctionToRunnable(prev) { last, prev ->
                (last afterRun prev).callback } }

        @JvmStatic infix fun Runnable.given(predicate: MessagePredicate): Runnable = apply {
            attachPredictionToRunnable(predicate) { last, predicate ->
                (last given predicate).callback } }

        @JvmStatic infix fun Runnable.unless(predicate: MessagePredicate): Runnable = apply {
            attachPredictionToRunnable(predicate) { last, predicate ->
                (last unless predicate).callback } }

        @JvmStatic infix fun Runnable.otherwise(next: Runnable): Runnable = apply {
            attachConjunctionToRunnable(next) { last, next ->
                (last otherwiseRun next).callback } }

        @JvmStatic infix fun Runnable.onError(action: Runnable): Runnable = apply {
            attachConjunctionToRunnable(action) { last, action ->
                (last onErrorRun action).callback } }

        @JvmStatic infix fun Runnable.onTimeout(action: Runnable): Runnable = apply {
            attachConjunctionToRunnable(action) { last, action ->
                (last onTimeoutRun action).callback } }

        internal infix fun Message.thenRun(next: Runnable): Message = apply {
            attachNextConjunctionToMessage(next) }

        internal infix fun Message.afterRun(prev: Runnable): Message = apply {
            attachPrevConjunctionToMessage(prev) }

        internal infix fun Message.otherwiseRun(next: Runnable): Message = apply {
            attachConclusionToMessage(next) }

        internal infix fun Message.onErrorRun(action: Runnable): Message = apply {
            attachErrorConclusionToMessage(action) }

        internal infix fun Message.onTimeoutRun(action: Runnable): Message = apply {
            attachTimeoutConclusionToMessage(action) }

        internal infix fun Message.then(next: Message): Message = apply {
            attachNextConjunctionToMessage(next) }

        internal infix fun Message.after(prev: Message): Message = apply {
            attachPrevConjunctionToMessage(prev) }

        internal infix fun Message.given(predicate: MessagePredicate): Message = apply {
            attachTruePredictionToMessage(predicate) }

        internal infix fun Message.unless(predicate: MessagePredicate): Message = apply {
            attachFalsePredictionToMessage(predicate) }

        internal infix fun Message.otherwise(next: Message): Message = apply {
            attachConclusionToMessage(next) }

        internal infix fun Message.onError(action: Message): Message = apply {
            attachErrorConclusionToMessage(action) }

        internal infix fun Message.onTimeout(action: Message): Message = apply {
            attachTimeoutConclusionToMessage(action) }

        @JvmStatic val AnyKCallable.isScheduledAhead
            get() = hasAnnotationType<Ahead>()

        @JvmStatic val Any.isScheduledAhead
            get() = hasAnnotationType<Ahead>()

        internal fun init(block: UnitKCallable) =
            init { block.call() }

        internal fun init(block: SchedulerScopeWork) {
            init()
            invoke(block) }

        internal fun init() {
            invoke().asType<Scheduler>()
            ?.observeAsync() }

        internal fun invoke(block: UnitKCallable) = block.call()

        internal fun invoke(block: SchedulerScopeWork) = this.block()

        @Tag(CLK_INIT) @Synchronous(group = Clock::class)
        internal val initClock = Runnable {
            // turn clock until scope is active
            currentThread.log(info, SVC_TAG, "Clock is detected.") }

        internal fun preferClock() {
            DEFAULT_RESOLVER = HandlerScope }

        private fun preferSchedulerSafely() {
            if (DEFAULT_RESOLVER.isNullValue())
                DEFAULT_RESOLVER = Scheduler }

        internal fun preferScheduler() {
            preferSchedulerSafely()
            if (isSchedulerNotObserved) {
                fun observeAsync() = Scheduler.observeAsync()
                if (onMainThread)
                    observeAsync()
                else processLifecycleScope.launch(Main) {
                    observeAsync() } } }

        internal fun preferScheduler(context: CoroutineContext = Main, callback: Work) {
            preferSchedulerSafely()
            if (isSchedulerNotObserved)
                processLifecycleScope.launch(context) {
                    Scheduler.observeAsync()
                    callback() } }

        internal fun avoidClock() {
            if (isClockPreferred)
                DEFAULT_RESOLVER = null }

        internal fun avoidScheduler() {
            if (isSchedulerPreferred)
                preferClock() }

        internal fun repostByPreference(step: CoroutineStep, post: AnyStepFunction, handle: CoroutineFunction) =
            repostByPreference(
                { post(step.markTagForSchPost().toStep()) },
                { handle(step) })

        internal fun repostByPreference(step: Step, post: AnyStepFunction, handle: CoroutineFunction) =
            repostByPreference(
                { post(step.markTagForSchPost()) },
                { handle(step.toCoroutine()) })

        internal fun <R> repostByPreference(step: KCallable<R>, post: AnyStepFunction, handle: CoroutineFunction) { step.call() }

        private inline fun repostByPreference(post: Work, handle: Work) = when {
            isSchedulerPreferred and
            isSchedulerObserved ->
                post()
            isClockPreferred and
            Clock.isRunning ->
                handle()
            isSchedulerObserved ->
                post()
            else ->
                currentThread.interrupt() }

        context(_: ProcessScope)
        @JvmStatic val Thread.log
            get() = iso.consolator.log

        @JvmStatic fun isDatabaseCreated() = isRuntimeDbNotNull

        @JvmStatic fun isSessionCreated() = isSessionNotNull

        internal val isClockPreferred
            get() = DEFAULT_RESOLVER.typeIs<HandlerScope, _>()

        internal val isSchedulerPreferred
            get() = DEFAULT_RESOLVER.typeIsNot<Scheduler, _>()

        internal var isSchedulerObserved = false

        internal val isSchedulerNotObserved get() = !isSchedulerObserved

        private var DEFAULT_RESOLVER: ResolverScope? = null
            set(value) {
                // engine-wide reconfiguration
                DEFAULT_OPERATOR =
                    if (value.typeIs<HandlerScope, _>())
                        ::handleAhead
                    else null
                field = value }

        internal var DEFAULT_OPERATOR: AnyCoroutineFunction? = null
            set(value) {
                // message queue reconfiguration
                field = value }

        @JvmStatic val EMPTY_STEP: SchedulerStep = { _, _ -> }

        context(_: Implication<T>)
        override fun <T> Any.implicitly(): KCallable<T> = TODO()

        @JvmStatic fun findClass(className: String) = Class.forName(className).kotlin

        @JvmStatic operator fun invoke() = DEFAULT_RESOLVER ?: Scheduler

        override fun commit(step: AnyCoroutineStep): ResolverTransactionIdentityType =
            with(State) { onValueTypeChanged(Resolved::class) {
                this@Companion().commit(step) } }

        context(_: CoroutineScope)
        override fun <T> withLazy(callable: KCallable<T>, block: T.(KCallable<T>) -> Any?): CoroutineScope {
            // check for known global values, such as ::mainUncaughtExceptionHandler
            // by active states, such as State[-1]
            return super.withLazy(callable, block) }
    }

    override fun commit(step: AnyCoroutineStep) =
        Companion().commit(step)

    @Suppress("warnings")
    sealed interface Task<out R> {
        context(_: Manager<*>)
        fun implicitly(applied: Boolean = true, vararg args: Any?): Task<out R> =
            runWhen({ applied }) { with(Manager) { intercept(*args) } }

        sealed class Manager<in R> {
            companion object : Manager<Self>(), Interceptor<Self>, Function.Context.Resolver<Any?, Any?, Any?> {
                override fun <S : Self> Self.intercept(vararg args: Any?): S = this as S

                override suspend fun invoke(args: AnyArray): Any? = TODO()

                override suspend fun VarArray.invoke(vararg args: Any?): Any? = TODO()
            }
        }

        sealed interface Invokable<out R> : Task<R> {
            sealed interface ByTag<out R> : Invokable<R> {
                context(_: Task<*>, _: Manager<*>)
                operator fun invoke(tag: Tag = this.tag): R = TODO()
            }
        }

        sealed interface Function<out T> : Task<T> {
            fun interface Single<in P1, out R> : Function<R>, suspend (P1) -> R {
                override suspend fun invoke(p1: P1): R
            }

            fun interface Tuple<in P1, in P2, out R> : Function<R>, suspend (P1, P2) -> R {
                override suspend fun invoke(p1: P1, p2: P2): R
            }

            fun interface Triplet<in P1, in P2, in P3, out R> : Function<R>, suspend (P1, P2, P3) -> R {
                override suspend fun invoke(p1: P1, p2: P2, p3: P3): R
            }

            fun interface Quadruple<in P1, in P2, in P3, in P4, out R> : Function<R>, suspend (P1, P2, P3, P4) -> R {
                override suspend fun invoke(p1: P1, p2: P2, p3: P3, p4: P4): R
            }

            fun interface Pentuple<in P1, in P2, in P3, in P4, in P5, out R> : Function<R>, suspend (P1, P2, P3, P4, P5) -> R {
                override suspend fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5): R
            }

            fun interface Hextuple<in P1, in P2, in P3, in P4, in P5, in P6, out R> : Function<R>, suspend (P1, P2, P3, P4, P5, P6) -> R {
                override suspend fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6): R
            }

            sealed interface Explicator<in S, in T, out R> : VectorFunction<T, R> {
                suspend operator fun S.invoke(vararg args: T): R
            }

            sealed interface Context : Table {
                sealed interface Resolver<U, in T : U, out R> : Context, Explicator<Array<out U>, T, R>
            }
        }

        fun interface VectorFunction<in T, out R> : suspend (Array<out T>) -> R {
            override suspend fun invoke(args: Array<out T>): R
        }

        // task types enable code injection capabilities and features

        sealed interface Initiative<in P1, in P2, out T> : Function.Tuple<P1, P2, T>, Invokable.ByTag<T> {
            sealed interface Step : Type<SchedulerScope, Self, JobContinuationDefaultIdentityType> {
                companion object : Step {
                    override suspend fun invoke(scope: SchedulerScope, self: Self) = JobContinuationDefaultIdentityType::class.reconstruct()
            } }

            sealed interface Prediction : Type<SchedulerScope, Self, BooleanType> {
                companion object : Prediction {
                    override suspend fun invoke(scope: SchedulerScope, self: Self): BooleanType = true
            } }

            fun interface Type<in P1, in P2, out T> : Initiative<P1, P2, T> {
                companion object : Type<SchedulerScope, Self, JobContinuationIdentityType?> {
                    override suspend fun invoke(scope: SchedulerScope, self: Self): JobContinuationIdentityType? = null
            } }
        }

        sealed interface Accumulative<in P1, in P2, in P3, out T> : Function.Triplet<P1, P2, P3, T>, Invokable.ByTag<T> {
            sealed interface Step : Type<SchedulerScope, Self, TaskInput, JobExtensionDefaultIdentityType> {
                companion object : Step {
                    override suspend fun invoke(scope: SchedulerScope, self: Self, initial: TaskInput) = JobExtensionDefaultIdentityType::class.reconstruct()
            } }

            sealed interface Prediction : Type<SchedulerScope, Self, TaskInput, BooleanType> {
                companion object : Prediction {
                    override suspend fun invoke(scope: SchedulerScope, self: Self, initial: Any?): BooleanType = true
            } }

            fun interface Type<in P1, in P2, in P3, out T> : Accumulative<P1, P2, P3, T> {
                companion object : Type<SchedulerScope, Self, TaskInput, JobExtensionIdentityType?> {
                    override suspend fun invoke(scope: SchedulerScope, self: Self, initial: TaskInput): JobExtensionIdentityType? = null
            } }
        }

        sealed interface Programmatic<in P1, in P2, in P3, out T> : Initiative<P1, P2, T>, Accumulative<P1, P2, P3, T> {
            sealed interface Step : Type<SchedulerScope, Self, TaskInput, JobExtensionDefaultIdentityType> {
                companion object : Step {
                    override suspend fun invoke(scope: SchedulerScope, self: Self) = JobExtensionDefaultIdentityType::class.reconstruct()
                    override suspend fun invoke(scope: SchedulerScope, self: Self, initial: TaskInput) = JobExtensionDefaultIdentityType::class.reconstruct()
            } }

            sealed interface Prediction : Type<SchedulerScope, Self, TaskInput, BooleanType> {
                companion object : Prediction {
                    override suspend fun invoke(scope: SchedulerScope, self: Self): BooleanType = true
                    override suspend fun invoke(scope: SchedulerScope, self: Self, initial: TaskInput): BooleanType = true
            } }

            sealed interface Type<in P1, in P2, in P3, out T> : Programmatic<P1, P2, P3, T> {
                companion object : Type<SchedulerScope, Self, TaskInput, JobExtensionIdentityType?> {
                    override suspend fun invoke(scope: SchedulerScope, self: Self): JobExtensionIdentityType? = null
                    override suspend fun invoke(scope: SchedulerScope, self: Self, initial: TaskInput): JobExtensionIdentityType? = null
            } }
        }
    }
}

inline fun <R> withSchedulerScope(crossinline block: SchedulerScope.Companion.() -> R) =
    with(SchedulerScope, block)

sealed class CallableSchedulerScope : CallableResolverScope {
    companion object : CallableSchedulerScope(), DefaultCallableScope by ImplicitCallableScope {
        override fun <I : LifecycleOwner> I.call(step: AnyKCallable, vararg args: Any?): Job? = currentThreadJob()

        override fun <I : LifecycleOwner> I.callBy(step: AnyKCallable, args: KParameterMap): Job? = currentThreadJob()

        context(_: CoroutineScope)
        fun <I : LifecycleOwner, R> I.callSelfReferring(step: KCallable<R>, vararg args: Any?): Job? = currentThreadJob()

        context(_: CoroutineScope)
        fun <I : LifecycleOwner, R> I.callSelfReferringBy(step: KCallable<R>, args: KParameterMap): Job? = currentThreadJob()

        context(_: LifecycleOwner)
        @JvmStatic infix fun <R> Job?.then(next: KCallable<R>) = this

        context(_: LifecycleOwner)
        @JvmStatic infix fun <I : CoroutineScope, R> Job?.thenImplicitly(next: suspend I.(Any?, Job) -> R) = this

        context(_: LifecycleOwner)
        @JvmStatic infix fun <I : CoroutineScope, R> Job?.thenSuspendedImplicitly(next: suspend (I, AnyArray?) -> R) = this

        context(_: LifecycleOwner)
        @JvmStatic infix fun <R> Job?.after(prev: KCallable<R>) = this

        context(_: LifecycleOwner)
        @JvmStatic infix fun <I : CoroutineScope, R> Job?.afterImplicitly(prev: suspend I.(Any?, Job) -> R) = this

        context(_: LifecycleOwner)
        @JvmStatic infix fun <I : CoroutineScope, R> Job?.afterSuspendedImplicitly(prev: suspend (I, AnyArray?) -> R) = this

        context(_: LifecycleOwner)
        @JvmStatic infix fun Job?.given(predicate: Predicate) = this

        context(_: LifecycleOwner)
        @JvmStatic infix fun Job?.givenSuspended(predicate: Prediction) = this

        context(_: LifecycleOwner)
        @JvmStatic infix fun <I : CoroutineScope, R : BooleanType> Job?.givenImplicitly(predicate: suspend I.(Any?, Job) -> R) = this

        context(_: LifecycleOwner)
        @JvmStatic infix fun <I : CoroutineScope, R : BooleanType> Job?.givenSuspendedImplicitly(predicate: suspend (I, AnyArray?) -> R) = this

        context(_: LifecycleOwner)
        @JvmStatic infix fun Job?.unless(predicate: Predicate) = this

        context(_: LifecycleOwner)
        @JvmStatic infix fun Job?.unlessSuspended(predicate: Prediction) = this

        context(_: LifecycleOwner)
        @JvmStatic infix fun <I : CoroutineScope, R : BooleanType> Job?.unlessImplicitly(predicate: suspend I.(Any?, Job) -> R) = this

        context(_: LifecycleOwner)
        @JvmStatic infix fun <I : CoroutineScope, R : BooleanType> Job?.unlessSuspendedImplicitly(predicate: suspend (I, AnyArray?) -> R) = this

        context(_: LifecycleOwner)
        @JvmStatic infix fun <R> Job?.otherwise(next: KCallable<R>) = this

        context(_: LifecycleOwner)
        @JvmStatic infix fun <I : CoroutineScope, R> Job?.otherwiseImplicitly(next: suspend I.(Any?, Job) -> R) = this

        context(_: LifecycleOwner)
        @JvmStatic infix fun <I : CoroutineScope, R> Job?.otherwiseSuspendedImplicitly(next: suspend (I, AnyArray?) -> R) = this

        context(_: LifecycleOwner)
        @JvmStatic infix fun <R> Job?.onCancel(action: KCallable<R>) = this

        context(_: LifecycleOwner)
        @JvmStatic infix fun <I : CoroutineScope, R> Job?.onCancelImplicitly(next: suspend I.(Any?, Job) -> R) = this

        context(_: LifecycleOwner)
        @JvmStatic infix fun <I : CoroutineScope, R> Job?.onCancelSuspendedImplicitly(next: suspend (I, AnyArray?) -> R) = this

        context(_: LifecycleOwner)
        @JvmStatic infix fun <R> Job?.onError(action: KCallable<R>) = this

        context(_: LifecycleOwner)
        @JvmStatic infix fun <I : CoroutineScope, R> Job?.onErrorImplicitly(next: suspend I.(Any?, Job) -> R) = this

        context(_: LifecycleOwner)
        @JvmStatic infix fun <I : CoroutineScope, R> Job?.onErrorSuspendedImplicitly(next: suspend (I, AnyArray?) -> R) = this

        context(_: LifecycleOwner)
        @JvmStatic infix fun <R> Job?.onTimeout(action: KCallable<R>) = this

        context(_: LifecycleOwner)
        @JvmStatic infix fun <I : CoroutineScope, R> Job?.onTimeoutImplicitly(next: suspend I.(Any?, Job) -> R) = this

        context(_: LifecycleOwner)
        @JvmStatic infix fun <I : CoroutineScope, R> Job?.onTimeoutSuspendedImplicitly(next: suspend (I, AnyArray?) -> R) = this

        context(_: LifecycleOwner)
        @JvmStatic infix fun Job?.commit(step: SchedulerStep): Job? = this
    }

    // requires locating receiver
    override fun commit(step: AnyKCallable) =
        step.call(step.tag /* optional */)

    override fun AnyKCallable.commitStep(scope: CoroutineScope, vararg args: Any?) =
        commitSuspend(scope, *args)

    override fun AnyKCallable.commitStepBy(scope: CoroutineScope, args: KParameterMap) =
        commitSuspendBy(args) // TODO - set scope in params map

    override fun AnyKCallable.commitSuspend(vararg args: Any?) =
        commit { callSuspend(*args) }

    override fun AnyKCallable.commitSuspendBy(args: KParameterMap) =
        commit { callSuspendBy(args) }

    override fun AnyKCallable.commit(vararg args: Any?) =
        call(*args)

    override fun AnyKCallable.commitBy(args: KParameterMap) =
        callBy(args)

    override fun AnyKCallable.commitSafely(vararg args: Any?) =
        determine(
            { commitSuspend(this, *args) },
            { commit(*args) })

    override fun AnyKCallable.commitSafelyBy(args: KParameterMap) =
        determine(
            { commitSuspendBy(args) },
            { commitBy(args) })

    private inline fun AnyKCallable.determine(callSuspend: AnyFunction, call: AnyFunction) =
        if (isSuspend) callSuspend() else call()
}

inline fun <R> withCallableScope(crossinline block: CallableSchedulerScope.Companion.() -> R) =
    with(CallableSchedulerScope, block)

@Coordinate
internal object Scheduler : SchedulerScope, LiveStepReceiver.Synchronizer<LiveStepIdentityType>() {
    @Key(1)
    @JvmField var activityConfigurationChangeManager: ConfigurationChangeManager? = null

    @Key(2)
    @JvmField var activityNightModeChangeManager: NightModeChangeManager? = null

    @Key(3)
    @JvmField var activityLocalesChangeManager: LocalesChangeManager? = null

    @Key(4)
    @JvmField var applicationMigrationManager: ApplicationMigrationManager? = null

    @JvmStatic fun observe() {
        run(::observeForever) }

    @JvmStatic fun observeAsync() = commitLockedByFunctionUnless(::hasObservers, ::observe)

    @JvmStatic fun observe(owner: LifecycleOwner) = observe(owner, this) /* register lifecycle owner */

    @JvmStatic fun ignore() {
        run(::removeObserver) }

    override fun onActive() {
        super.onActive()
        SchedulerScope.isSchedulerObserved = true }

    override fun onInactive() {
        // check lifecycle owner state
        SchedulerScope.isSchedulerObserved = false }

    @JvmStatic fun <T : Resolver> commit(resolver: KClass<out T>, provider: Any, vararg context: Any?) =
        when (resolver) {
            ApplicationMigrationManager::class ->
                ::applicationMigrationManager.require(provider)
            ConfigurationChangeManager::class ->
                ::activityConfigurationChangeManager.require(provider)
            NightModeChangeManager::class ->
                ::activityNightModeChangeManager.require(provider)
            LocalesChangeManager::class ->
                ::activityLocalesChangeManager.require(provider)
            MemoryManager::class ->
                object : MemoryManager {}
            else -> null
        }?.commit(*context)

    @JvmStatic fun clearResolverObjects() {
        activityConfigurationChangeManager = null
        activityNightModeChangeManager = null
        activityLocalesChangeManager = null
        applicationMigrationManager = null }

    override val coroutineContext
        get() = Default

    override fun commit(step: AnyCoroutineStep) =
        attach(step.markTagForSchCommit(), ::launch)

    override fun onChanged(value: AnyStep?) {
        if (value !== null)
        if (value.hasAnnotationType<NoItem>())
            value.block()
        else
            value.markTagForSchExec()
            ?.run { synchronize(this, ::block) } }

    override fun <R> synchronize(lock: AnyStep?, block: () -> R) =
        if (lock !== null) {
            if (withSchedulerScope {
                lock.isScheduledAhead })
                block()
            else {
            fun AnyFunctionList.run() { map {
                remove(it)
                it() } }
            if (lock.isScheduledLast) {
                queue.run()
                block() }
            else
            if (lock.isScheduledFirst)
                block().also {
                queue.run() }
            else {
                queue.add(block)
                Unit.type() } } }
        else Unit.type()

    override infix fun AnyStep.from(ref: AnyStep) = TODO()

    override val descriptor
        get() = object : StepDescriptor {
            context(_: AnyDescriptor)
            override fun <A : AnyStep, B : AnyStep> A.onValueChanged(value: B, block: BaseStepDescriptor.(AnyStep) -> Any?) = TODO()
        }

    override var queue: AnyFunctionList = mutableListOf()

    @JvmStatic operator fun <R> invoke(work: Scheduler.() -> R) = this.work()
}

internal fun commit(scope: CoroutineScope? = SchedulerScope(), step: AnyCoroutineStep) =
    (scope
    ?: step.annotatedScope
    ?: foregroundLifecycleOwner?.lifecycleScope
    ?: service
    ?: SchedulerScope()
    ).run {
    if (this is ResolverScope)
        commit(step)
    else (
        findClassMemberFunction {
            (it.name `is` "commit") and
            (it.parameters.size `is` 2) and
            (it.parameters.second().name `is` "step") }
        ?: return commit(step)
        ).call(this, step) }

internal inline fun <reified T : Resolver, reified I> commit(vararg context: Any?) =
    commit(T::class, I::class, *context)

internal inline fun <reified T : Resolver> LifecycleOwner.commit(member: UnitKFunction, vararg context: Any?) =
    commit(T::class, this, member, *context)

internal inline fun <reified T : Resolver> Activity.commit(member: UnitKFunction, vararg context: Any?) =
    commit(T::class, this, member, *context)

internal inline fun <reified T : Resolver> Context.commit(member: UnitKFunction, vararg context: Any?) =
    commit(T::class, this, member, *context)

fun <R> schedule(step: KCallable<R>) =
    SchedulerScope.repostByPreference(step, AnyStep::post, ::handle)

internal fun <R> scheduleAhead(step: KCallable<R>) =
    SchedulerScope.repostByPreference(step, AnyStep::postAhead, ::handleAhead)

internal fun schedule(step: Step) =
    SchedulerScope.repostByPreference(step, AnyStep::post, ::handle)

internal fun scheduleAhead(step: Step) =
    SchedulerScope.repostByPreference(step, AnyStep::postAhead, ::handleAhead)

internal fun attach(step: AnyCoroutineStep) =
    attach(step,
        if (SchedulerScope.isClockPreferred)
            ::handle
        else ::launch,
        ::reattach)

internal fun attach(step: AnyCoroutineStep, enlist: AnyCoroutineFunction) =
    attach(step, enlist, ::reattach)

private fun attach(step: AnyCoroutineStep, enlist: AnyCoroutineFunction, transfer: AnyCoroutineFunction) =
    when (val result = trySafelyForResult { enlist(step) }) {
        null, false ->
            transfer(step)
        true, is Job ->
            result
        else -> if (Clock.isNotRunning)
            transfer(step.applyEnlistedOnce())
        else
            result }

private fun reattach(step: AnyCoroutineStep) =
    try {
        if (step.isEnlisted)
            trySafelyForResult { detach(step) }
            ?.run(::launch)
        else launch(step) }
    catch (_: Throwable) {
        repost { step() } }

private fun detach(step: AnyCoroutineStep) =
    (withClock { run {
        synchronize {
            ::isRunning.then {
            getRunnable(step)?.detach()
            ?: getMessage(step)?.detach()?.asRunnable() } }
        ?.asCoroutine() } }
    ).applyUnlistedOnce()
    ?: step

internal fun repost(step: CoroutineStep) =
    SchedulerScope.repostByPreference(step, AnyStep::post, ::handle)

internal fun repostAhead(step: CoroutineStep) =
    SchedulerScope.repostByPreference(step, AnyStep::postAhead, ::handleAhead)

private fun AnyStep.post() = run(Scheduler::postValue)
private fun AnyStep.postAhead() = run(Scheduler::setValue)

private fun launch(step: AnyCoroutineStep) =
    step.annotatedScopeOrScheduler().launch { step() } // internal for Scheduler.commit, attach, and reattach

internal sealed interface SchedulerContext : CoroutineContext {
    companion object : SchedulerContext, ReceiverItem<CoroutineScope>,
        ActiveContext<BaseContextSynchronizer>(
            /* communicates with jobs controllers via functional interface in state synchronizer */
            object : ActiveContextSynchronizer by State {
                init { attach(foregroundFragment) }
        }) {
        private fun <R> foldAsync(initial: R, operation: (R, CoroutineContext.Element) -> R) =
            asFunctionSetCoordinator()
            .synchronize(
                asActiveContextSynchronizer().id)
            { foldSync(initial, operation) }

        private fun <R> foldSync(initial: R, operation: (R, CoroutineContext.Element) -> R) =
            operation(initial, SchedulerElement)

        /* update continuation state
        // register initial and operation - return operation */
        override fun <R> fold(initial: R, operation: (R, CoroutineContext.Element) -> R) =
            foldSync(initial, operation)

        /* notify element continuation (key <-> element)
        // register element key - return element */
        override fun <E : CoroutineContext.Element> get(key: CoroutineContext.Key<E>) = null

        /* reimpose continuation rules
        // register minus key - return context */
        override fun minusKey(key: CoroutineContext.Key<*>) = this

        // sets focus to scope
        override fun <R> ReceiverItem<R>.set(value: R) = SchedulerContext

        // informs context of item requirements
        override fun <R> scope(item: KCallable<R>) = TODO()

        // intercept new context
        override fun plus(context: CoroutineContext) =
            super.plus(context)
    }

    // notifies an in-context step about its start mode
    fun <S> notify(step: S, start: CoroutineStart = CoroutineStart.DEFAULT, owner: LifecycleOwner? = null) = this
}

private interface SchedulerElement : CoroutineContext.Element {
    companion object : SchedulerElement {
        @JvmStatic override val key
            get() = SchedulerKey
    }
}

private interface SchedulerKey : CoroutineContext.Key<SchedulerElement> {
    companion object : SchedulerKey
}

internal fun <R> CoroutineScope.relaunch(instance: JobKProperty, group: FunctionSet?, context: CoroutineContext = SchedulerContext, start: CoroutineStart = CoroutineStart.DEFAULT, step: suspend CoroutineScope.() -> R) =
    relaunch(instance, context, start, step)
    .also { job -> markTagsInGroupForJobRelaunch(instance, step, group, job, null, context, start) }

private fun <R> CoroutineScope.relaunch(instance: JobKProperty, context: CoroutineContext, start: CoroutineStart, step: suspend CoroutineScope.() -> R) =
    instance.requireActive {
    launch(context, start, step) }

internal fun <R> launch(context: CoroutineContext = SchedulerContext, start: CoroutineStart = CoroutineStart.DEFAULT, step: suspend CoroutineScope.() -> R) =
    step.markTagForSchLaunch()
        .afterTrackingTagsForJobLaunch(null, context, start).let { trackedStep ->
    Scheduler.launch(context, start) { trackedStep() }
        .applySaveNewElement(step) }

private fun CoroutineScope.determineCoroutine(owner: LifecycleOwner, context: CoroutineContext, start: CoroutineStart, step: AnyCoroutineStep) =
    Triple(
        // context key <-> step <-> owner
        with(context) {
        if (isSchedulerContext)
            asSchedulerContext()
            .notify(step, start, owner)
        else
            run(parent::plus) },
        start,
        step)

private val CoroutineContext.parent: CoroutineContext
    get() = SchedulerContext

private val CoroutineContext.isSchedulerContext
    get() = typeIs<SchedulerContext, _>() or
            get(SchedulerKey).isSchedulerElement

private val CoroutineContext.Element?.isSchedulerElement
    get() = typeIs<SchedulerElement, _>()

// revises state dependency maps that correlate with other execution contexts
// acquires stateful validation pointers from current active context coordinators
// re-invokes pointers to affect valid states via continuous flexible points in recurring (coroutine) contexts
// operates by context parameters in scheduler execution model
internal sealed class ActiveContext<S : FunctionSetPointer>(internal var id: S) : PropertyReference.Stub<S>(id) {
    // attunes to scope definitions (execution model) for items
    abstract fun <R> scope(item: KCallable<R>)
}

// use active context as context parameter
private fun ActiveScope.invalidate() = Unit

// validation pointers and live references
private sealed interface ActiveReference<R : Refactor<*>, A : ActionIntent> : Adjustable.By<R, A> {
    override fun onAttach(index: A): Attachable<A>

    override fun onAttachBy(container: R): Attachable.By<R, A>
}

internal sealed interface ActiveState<in E> : State {
    sealed interface BooleanTypeState<in E> : ActiveState<E>, BooleanState
}

// referable element in active contexts
internal sealed interface Referable<in E> {
    operator fun invoke(element: E): Responsive<E>
}

private sealed interface Addressable

// responsive units in active contexts
internal sealed interface Responsive<in E> : Referable<E>, ActiveState<E>

// comparable to logical elements (in resolved states) or known states
private sealed interface Logical<in E> : Responsive<E>, ActiveState.BooleanTypeState<E>

// stateful looper unit with responsive states
private interface Looper<E> : Logical<E>

// flexible element in active sub-contexts
private sealed interface Flexible<E, out S : Responsive<E>> : Referable<E> {
    override operator fun invoke(element: E): S
}

// reflector element in super-contexts
private sealed interface Reflector<E, S : Responsive<E>> : Flexible<E, S>, Refactor<S> {
    override infix fun S.from(ref: S): Referable<E>
}

private typealias JobReferable = Referable<Job>
private typealias AnyReferable = Referable<Any>

// execution model

private typealias JobStateReflector = Flexible<Job, Responsive<Job>>
private typealias AnyStateReflector = Flexible<Any, Responsive<Any>>

private sealed class OpenActiveState<E> : OpenState(), ActiveState<E>

internal interface OpenContextCoordinator<S, out L : S> : ContextCoordinator<S, L>, FunctionSetPointer {
    fun merge(context: Coordinator<S, S>?): ContextCoordinator<S, S> {
        if (context.isNullValue()) { /* issue system-wide notify detached fragment */ }
        return this }

    override fun invoke(): FunctionSet
}

internal interface ContextSynchronizer<L> : Synchronizer<L>, OpenContextCoordinator<L, L> {
    fun L.acquire(vararg context: Any?, block: Coordinator<L, L>.(StateArray) -> Unit)

    fun <S : State, R> access(state: S, block: Coordinator<L, L>.(S) -> R): R

    fun release(lock: State)
}

internal interface ActiveContextSynchronizer : BaseContextSynchronizer {
    fun attach(fragment: Fragment?) =
        merge(
            (fragment as? SchedulerFragment)
            ?.view.asType<DirectStateCoordinator>())
}

private fun <S : FunctionSetPointer> Any.asActiveContext() = asTypeUnsafe<ActiveContext<S>>()
private fun Any.asActiveContextSynchronizer() = asActiveContext<FunctionSetPointer>()
private fun Any.asFunctionSetCoordinator() = asSynchronizer<FunctionSetPointer>()

internal typealias BaseContextSynchronizer = ContextSynchronizer<State>
private typealias DirectContextCoordinator = OpenContextCoordinator<State, State>
internal typealias BaseContextCoordinator = ContextCoordinator<State, State>

private suspend fun CoroutineScope.registerContext(context: WeakContext) {
    currentJob().setInstance(CONTEXT, context) }

// active references can be generated at this point - steps can express (by tag) items they depend on
// scheduler context allows to communicate to refactoring layer by intercepting chained calls in connected statements
// in order to infuse/declare flexible points ahead of runtime
private fun AnyCoroutineStep.afterTrackingTagsForJobLaunch(owner: LifecycleOwner? = null, context: CoroutineContext, start: CoroutineStart) =
    run(::returnTag)?.let { tag -> withSchedulerScope {
    (after { _, job -> markTagsForJobLaunch(tag, this@afterTrackingTagsForJobLaunch, job, owner, context, start) })!! } }
    ?: this

// process tag to identify parent/relative job block
// numeric unifiers can be handy here
private fun FunctionSet.relateByTag(tag: Tag) =
    relateByTagOrFallback(tag) { tag ->
    when (tag) {
        INET_FUNCTION -> findByTag(INET)
        else -> null } }

private fun FunctionSet.relateByTag(tag: Tag, translate: TagConversion) =
    relateByTagOrFallback(tag) { tag ->
    translate(tag)
        .resultWhen({ isNot(tag) }, ::findByTag) }

private inline fun FunctionSet.relateByTagOrFallback(tag: Tag, fallback: (TagType) -> FunctionItem?) =
    tag.id.let { tag ->
    findByTag(tag) ?:
    fallback(tag) }

private fun FunctionSet.relateInstanceByTag(tag: Tag) =
    relateByTag(tag)?.instance

private fun FunctionSet.relateInstanceByTag(tag: Tag, translate: TagConversion) =
    relateByTag(tag, translate)?.instance

internal fun Job.getInstance(tag: TagType) =
    jobs?.findInstanceByTag(tag)

internal fun Job.setInstance(tag: TagType, value: Any?) {
    // addressable layer work
    value.markTag(tag, jobs) }

private var jobs: FunctionSet? = null

private var livesteps: FunctionSet? = null

private var callbacks: FunctionSet? = null

internal var items: FunctionSet? = null

internal open class Item<R>(target: KCallable<R>) : Reference.Stub<R>(target), Addressed<R>, Tagged {
    open fun onSave(subtag: TagType, value: Any?) = also { when (subtag) {
        FUNC ->
            if (target.isNullValue())
                value.asType<KCallable<R>>()?.apply(::setTarget)
            else {
                /* save sub-function */ }
    } }

    open fun onSaveIndex(index: Number) = this

    companion object Scope : ImplicitScope<Tag>, ImplicitResultScope {
        context(_: Implication<T>)
        override fun <T> Tag.implicitly(): KCallable<T> = TODO()

        override fun <S : AnyKCallable> AnyKCallable.intercept(vararg args: Any?): S = TODO()

        override fun invoke(step: Interception?): Interceptor<AnyKCallable> {
            return super.invoke(step)
        }

        @JvmStatic fun <T> find(ref: Coordinate): T = TODO()

        @JvmStatic fun <T> find(target: AnyKClass = Any::class, key: KeyType): T = TODO()

        @JvmStatic fun <R, I : Item<R>> Item<R>.reload(property: KCallable<R>): I = TODO()

        @JvmStatic fun <R, I : Item<R>> Item<R>.reload(tag: Tag): I = TODO()

        @JvmStatic fun <R, I : Item<R>> Item<R>.reload(tag: TagType): I = TODO()

        @JvmStatic fun <R, I : Item<R>> Item<R>.reload(target: AnyKClass, key: KeyType): I = TODO()
    }

    override var target: KCallable<R>? = null

    override fun setTarget(target: KCallable<R>): Addressed<R> {
        this.target = target
        return this }

    override var tag: TagType? = null
        get() = target?.tag?.id ?: field ?: target.hashTag()

    override fun setTag(tag: TagType): Tagged {
        this.tag = tag
        return this }

    lateinit var type: Type

    fun setType(type: Type): Item<R> {
        this.type = type
        return this }

    enum class Type {
        Coroutine,
        JobContinuation,
        JobExtension,
        SchedulerStep,
        SchedulerPrediction,
        SchedulerConfiguration,
        ContextStep,
        LiveStep,
        Step,
        Work,
        Runnable,
        Message,
        Lock,
        State,
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnyItem) return other == target
        if (target != other.target) return false
        if (tag != other.tag) return false
        if (type != other.type) return false
        return true }

    override fun hashCode() = target.hashCode()

    override fun toString() = tag.toString()
}

@JvmInline value class Value<V>(private val value: PropertyReference<V>) : Addressed<V>, Tagged, KProperty<V> by value, CharSequence {
    constructor(value: V) : this(PropertyReference.Stub(value)) {
        this.value.tag?.let { register(it) } }

    internal companion object Cache : ImplicitScope<CoroutineScope>, MutableMap<Any, AnyKCallable> by mutableMapOf() {
        context(_: Implication<T>)
        override fun <T> CoroutineScope.implicitly(): KCallable<T> = TODO()

        @JvmStatic private fun <V> Value<V>.register(tag: Tag) = set(tag, this)

        @JvmStatic private fun <V> Value<V>.saveTag(tag: TagType) = set(tag, this)

        @JvmStatic private fun <V> Value<V>.saveTarget(target: KCallable<V>) =
            target.tag?.let { set(it, target) }

        @JvmStatic inline fun <reified V : Any> filterByTag(target: KCallable<V>, transform: (Tag, Any) -> TagType? = ::matchByTagOrValue, comparator: TagType.(Any?) -> Boolean = TagType::equals) =
            target.tag?.let { tag ->
                filter { it.key.let { key ->
                    (key isNotObject tag) and
                    comparator(tag.id, transform(tag, key)) }
                }.values }

        @JvmStatic inline fun <reified V : Any> findByTag(target: KCallable<V>, transform: (Tag, Any) -> TagType? = ::matchByTagOrValue, comparator: TagType.(Any?) -> Boolean = TagType::equals) =
            filterByTag(target, transform, comparator)?.firstOrNull()?.call().asType<V>()

        @JvmStatic fun matchByTagOrValue(tag: Tag, key: Any) =
            if (key is Tag) key.id else key.asTagType()

        @JvmStatic private fun matchByTag(tag: Tag, key: Any) =
            key.asTag()?.id

        @JvmStatic private fun matchByValue(tag: Tag, key: Any) =
            resultUnless({ key is Tag }, Any::asTagType)
    }

    override val target: KCallable<V>?
        get() = TODO()

    override fun setTarget(target: KCallable<V>): Value<V> {
        saveTarget(target)
        return this }

    override val tag: TagType
        get() = TODO()

    override fun setTag(tag: TagType): Value<V> {
        saveTag(tag)
        return this }

    val type
        get() = value.getInstance()
            ?.let { it::class }
            ?: Nothing::class

    override fun toString() =
        value.getInstance().toString()

    override fun get(index: Int) =
        toString()[index]

    override fun subSequence(startIndex: Int, endIndex: Int) =
        toString().subSequence(startIndex, endIndex)

    override val length: Int
        get() = toString().length
}

internal fun <I : AnyTransactor, T> I.getInstance(ref: Coordinate) =
    with(ref) { getInstance<_, T>(target, key) }

private fun <I : AnyTransactor, T> I.getInstance(target: AnyKClass = Any::class, key: KeyType): T = TODO()

private fun <I : AnyTransactor> I.coroutineStep(target: AnyKClass, key: KeyType) =
    getInstance<_, AnyCoroutineStep>(target, key)

private fun <I : AnyTransactor> I.liveStep(target: AnyKClass, key: KeyType) =
    getInstance<_, SequencerStep>(target, key)

private fun <I : AnyTransactor> I.step(target: AnyKClass, key: KeyType) =
    getInstance<_, AnyStep>(target, key)

private fun <I : AnyTransactor> I.runnable(target: AnyKClass, key: KeyType) =
    getInstance<_, Runnable>(target, key)

internal fun FunctionSet.addFunction(function: Any, tag: TagType?, keep: Boolean) =
    add(function.toFunctionItem(
        tag?.let { { it } }
            ?: currentThreadJob()::hashTag,
        keep))

internal fun Any.toFunctionItem(tag: TagTypePointer, keep: Boolean) =
    FunctionItem(tag, this to keep) /* provides extra filters */

internal val FunctionItem.instance
    get() = second.asAnyToBooleanPair()?.first

internal fun FunctionSet.findByTag(tag: TagType, transform: (TagType, FunctionItem) -> TagType? = { _, it -> it.first() }) =
    find { tag `is` transform(tag, it) }

internal fun FunctionSet.findInstanceByTag(tag: TagType, transform: (TagType, FunctionItem) -> TagType? = { _, it -> it.first() }) =
    findByTag(tag, transform)?.instance

private fun FunctionSet.save(self: AnyKCallable, tag: Tag) =
    with(tag) { save(self, id, keep) }

internal fun FunctionSet.save(self: AnyKCallable, tag: Tag, type: Item.Type) =
    save(self, tag)
    .alsoAsItemSetType(type)

private fun FunctionSet.save(function: AnyKCallable, tag: TagType) =
    function.tag?.apply {
    save(function, combineTags(tag, id), keep) }

internal fun FunctionSet.save(function: AnyKCallable, tag: TagType, type: Item.Type) =
    save(function, tag)
    .alsoAsItemSetType(type)

private fun FunctionSet.save(function: AnyKCallable, tag: TagType?, keep: Boolean) =
    tag?.let(::findInstanceByTag)
        ?.also { if (it is AnyItem) { with(it) {
            onSave(FUNC, function)
            onSave(KEEP, keep) } } }
    ?: (if (function.asReference().isItemized)
            Item(function)
            .onSave(KEEP, keep)
        else
            function)
        .also {
        addFunction(it, tag, keep) }

// marking

private fun <T> T.itemized() = this

internal fun AnyKCallable.markTag(group: FunctionSet?) =
    tag?.also { group?.save(itemized(), it) }

private fun Any?.markValue(tag: TagType) {}

internal fun Any?.markTag(group: FunctionSet?) =
    asReference().markTag(group)

internal fun Any?.markTag(tag: TagType, group: FunctionSet?) =
    group?.save(itemized().asReference(), tag)

internal fun Any.markSequentialTag(tag: TagType?, id: TagType, group: FunctionSet?) =
    tag?.let { tag ->
    reduceTags(tag, TAG_DASH, id)
    .also { this@markSequentialTag.markTag(it, group) } }

fun Any?.applyTrackTag(tag: Tag?, group: FunctionSet?) =
    tag.result { applyMarkTag(this, group) }

internal fun <T> T.applyMarkTag(group: FunctionSet?) = apply { markTag(group) }

internal fun <T> T.applyMarkTag(tag: TagType, group: FunctionSet?) = apply { markTag(tag, group) }

internal fun <T> T.applyMarkTag(tag: Tag, group: FunctionSet?) = applyMarkTag(tag.id, group)

private fun AnyStep?.markTagForSchExec() = applyMarkTag(SCH_EXEC, items)
private fun AnyStep.markTagForSchPost() = applyMarkTag(SCH_POST, items)

private fun AnyCoroutineStep.markTagForFloLaunch() = applyMarkTag(FLO_LAUNCH, jobs)
private fun AnyCoroutineStep.markTagForSchCommit() = applyMarkTag(SCH_COMMIT, jobs)
private fun AnyCoroutineStep.markTagForSchLaunch() = applyMarkTag(SCH_LAUNCH, jobs)
private fun AnyCoroutineStep.markTagForSchPost() = applyMarkTag(SCH_POST, jobs)
internal fun AnyCoroutineStep.markTagForSvcCommit() = applyMarkTag(SVC_COMMIT, items)

internal fun Runnable.markTagForClkExec() = applyMarkTag(CLK_EXEC, callbacks)

private fun <T> T.alsoAsItemSetType(type: Item.Type) = also { if (this is AnyItem) setType(type) }

private fun markTagsForJobLaunch(tag: TagType, step: AnyCoroutineStep, job: Job, owner: LifecycleOwner?, context: CoroutineContext, start: CoroutineStart) =
    tag.asTagType()?.also { tag ->
    jobs?.saveCoroutine(step.itemized().asReference(), tag)
        ?.asCoroutineItem()
        ?.onSaveLifecycleOwner(owner)
        ?.onJobLaunch(job, context, start) }

private fun markTagsInGroupForJobRelaunch(instance: JobKProperty, block: AnyCoroutineStep, group: FunctionSet?, job: Job, owner: LifecycleOwner?, context: CoroutineContext, start: CoroutineStart) =
    block.asReference().let { parent ->
    instance.tag?.also { tag ->
    group?.saveCoroutine(parent.itemized(), tag)
        ?.asCoroutineItem()
        ?.onJobRelaunch(job, owner, context, start) } }

internal fun <I : CoroutineScope, R> markTagsForJobContinuationRepeat(step: suspend (I?, Any?) -> R, group: FunctionSet?, job: Job, predicate: Prediction?, delay: DelayFunction) =
    group?.apply {
    step.asReference().let { block ->
    block.tag?.also { tag ->
    (relateInstanceByTag(tag)
        ?: saveCoroutine(block.itemized(), tag)
        ).asCoroutineItem()
        ?.onJobContinuationRepeat(step, block, tag, job, predicate, delay) } } }

internal fun <I : CoroutineScope, S, R> markTagsForJobExtensionRepeat(step: suspend (I?, Any?, S) -> R, group: FunctionSet?, job: Job, predicate: Prediction?, delay: DelayFunction): FunctionSet? = TODO()

internal fun markTagsForSeqAttach(tag: Any?, step: AnyTriple, index: Int) =
    tag?.asTagType()?.also { tag ->
    (livesteps?.run {
        findInstanceByTag(tag)
        ?: LiveStepItem<Any?>().also {
            add(it.toFunctionItem({ tag }, true)) } }
        ).asLiveStepItem()
        ?.onAttachBy(step)
        ?.onAttach(index) }

internal fun markTagsForSeqLaunch(step: SequencerStep, job: Job, index: Int, context: CoroutineContext?) =
    step.asReference().let { step ->
    step.tag?.also { tag ->
    livesteps?.saveLiveStep(step.itemized(), tag)
        ?.asLiveStepItem()
        ?.onObserve(job, index, context) } }

internal fun markTagsForCtxReform(tag: TagType?, job: Job, stage: ContextStep?, form: AnyStep) =
    tag?.also { tag ->
    items?.findInstanceByTag(tag)
        ?.asCoroutineItem()
        ?.onContextReform(job, stage, form) }

internal fun markTagsForClkAttach(step: Any, index: Number) =
    when (step) {
    is Runnable ->
        getTag(step)?.also { tag ->
        callbacks?.saveRunnable(step::run.itemized(), tag)
            ?.asRunnableItem()
            ?.onAttachBy(step)
            ?.onAttach(index) }
    is Message ->
        getTag(step)?.also { tag ->
        callbacks?.saveRunnable(step.callback::run.itemized(), tag)
            ?.asRunnableItem()
            ?.onAttachBy(step)
            ?.onAttach(index) }
    else ->
        null }

@Suppress("warnings")
private open class ObjectReference<R>(target: KCallable<R>) : PropertyReference.Stub<R>(target), KMutableProperty<R> {
    internal constructor(obj: R) : this(TODO())

    override fun requireReference(): KMutableProperty<R> =
        super.requireReference().asTypeUnsafe()

    override val setter = requireReference().setter
}

internal fun <R> R.asInternalMutableProperty(annotations: AnnotationsMap? = null): KMutableProperty<R> =
    if (this is ObjectReference<*>)
        asTypeUnsafe()
    else run(::ObjectReference)
        .apply { extraAnnotations = annotations }

internal fun <R> R.asInternalKMutableProperty(annotations: AnnotationsMap? = null): KMutableProperty<R> =
    if (this is KMutableProperty<*>)
        asTypeUnsafe()
    else run(::ObjectReference)
        .apply { extraAnnotations = annotations }

internal fun <R> R.asInternalProperty(annotations: AnnotationsMap? = null): KProperty<R> =
    if (this is PropertyReference<*>)
        asTypeUnsafe()
    else PropertyReference.Stub(this@asInternalProperty)
        .apply { extraAnnotations = annotations }

internal fun <R> R.asInternalKProperty(annotations: AnnotationsMap? = null): KProperty<R> =
    if (this is KProperty<*>)
        asTypeUnsafe()
    else PropertyReference.Stub(this@asInternalKProperty)
        .apply { extraAnnotations = annotations }

internal fun <R> R.asInternalCallable(annotations: AnnotationsMap? = null): KCallable<R> =
    if (this is CallableReference<*>)
        asTypeUnsafe()
    else CallableReference.Stub(this@asInternalCallable)
        .apply { extraAnnotations = annotations }

internal fun <R> R.asInternalKCallable(annotations: AnnotationsMap? = null): KCallable<R> =
    if (this is KCallable<*>)
        asTypeUnsafe()
    else CallableReference.Stub(this@asInternalKCallable)
        .apply { extraAnnotations = annotations }

fun <R> R.asReference(annotations: AnnotationsMap? = null): KCallable<R> =
    if (this is KCallable<*>)
        asTypeUnsafe()
    else Reference.Stub(this@asReference)
        .apply { extraAnnotations = annotations }

fun <R> KCallable<R>.asReference(): Reference<R> = Reference.Stub(this@asReference)

internal val Any?.transit: Transit
    get() = when (this) {
        is Number -> toTransit()
        else -> asReference().event?.transit }

private val LifecycleOwner.annotatedScope: Array<out CoroutineScope>?
    get() = null // search in superclasses

private val Any.annotatedScope
    get() = trySafelyForResult { asReference().annotatedScope!!.let { annotation ->
        if (with(annotation.type) {
            isObject(Scheduler::class) or
            isObject(SchedulerScope::class) })
            SchedulerScope()
        else
        when (annotation.provider) {
            Any::class ->
                run(annotation.type::reconstruct)
            Activity::class ->
                foregroundActivity?.provide(annotation.type)
            Fragment::class ->
                foregroundFragment?.provide(annotation.type)
            else ->
                rejectWithImplementationRestriction()
        } as CoroutineScope } }

private fun Any.annotatedOrSchedulerScope() = annotatedScope ?: SchedulerScope()

private fun Any.annotatedScopeOrScheduler() = annotatedScope ?: Scheduler

private fun AnyFunction.invokeOnceWhenScheduledAhead() = apply {
    if (withSchedulerScope {
        isScheduledAhead }) {
        invoke()
        return applyIgnoreOnce() } }

internal fun AnyFunction.invokeWhenNotIgnored() {
    if (isNotIgnored) invoke() }

private fun <R> (suspend CoroutineScope.() -> R).toStep() = suspend { invoke(annotatedOrSchedulerScope()) }

private fun Step.toCoroutine(): CoroutineStep = { this@toCoroutine() }

private fun Step.toLiveStep(): SequencerStep = { _, _, _ -> invoke() }

@Retention(SOURCE)
@Target(CONSTRUCTOR, FUNCTION, PROPERTY, PROPERTY_GETTER, PROPERTY_SETTER)
private annotation class Itemize

@Retention(SOURCE)
@Target(CONSTRUCTOR, FUNCTION, PROPERTY, PROPERTY_GETTER, PROPERTY_SETTER)
private annotation class Enlisted

@Retention(SOURCE)
@Target(CONSTRUCTOR, FUNCTION, PROPERTY, PROPERTY_GETTER, PROPERTY_SETTER)
private annotation class Unlisted

internal val AnyStep.isScheduledFirst
    get() = hasAnnotationType<First>()

internal val AnyStep.isScheduledLast
    get() = hasAnnotationType<Last>()

val AnyKClass.tag
    get() = annotations.find(::filterIsTag).asType<Tag>()

val AnyKCallable.tag
    get() = annotations.find(::filterIsTag).asType<Tag>()

private val AnyKCallable.annotatedScope
    get() = annotations.find(::filterIsScope).asType<Scope>()

private val AnyKCallable.launchScope
    get() = annotations.find(::filterIsLaunchScope).asType<LaunchScope>()

private val AnyKCallable.event
    get() = annotations.find(::filterIsEvent).asType<Event>()

private val AnyKCallable.events
    get() = annotations.filterIsInstance<Event>()

private val AnyKCallable.isItemized
    get() = hasAnnotationType<Itemize>()

internal val Any?.tag
    get() = asReference().annotations.filterIsInstance<Tag>().last()

internal val Any?.tags
    get() = asReference().annotations.filterIsInstance<Tag>()

internal val Any.isKept
    get() = hasAnnotationType<Kept>()

internal val Any.isInternal
    get() = hasAnnotationType<Internal>()

internal val Any?.isInternalFirst
    get() = hasNullableAnnotationType<Internal.First>()

internal val Any?.isInternalLast
    get() = hasNullableAnnotationType<Internal.Last>()

internal val Any.isImplicit
    get() = hasAnnotationType<Implicit>()

internal val Any.isExposed
    get() = hasAnnotationType<Exposed>()

internal val Any.isReceived
    get() = hasAnnotationType<Received>()

private val Any.isIgnored
    get() = hasAnnotationType<Ignore>()

private val Any.isNotIgnored
    get() = hasNoAnnotationType<Ignore>()

private val Any.isEnlisted
    get() = hasAnnotationType<Enlisted>()

private val Any.isUnlisted
    get() = hasAnnotationType<Unlisted>()

// TODO - pick up from temporarily itemized set

private fun <T> T.applyAnnotation(type: AnnotationClass) = this

internal fun <T> T.applyKeptOnce() =
    runWhen({ hasNoNullableAnnotationType<Kept>() }) { applyAnnotation(Kept::class) }

internal fun <T> T.applyInternalOnce() =
    runWhen({ hasNoNullableAnnotationType<Internal>() }) { applyAnnotation(Internal::class) }

internal fun <T> T.applyInternalFirstOnce() =
    runWhen({ hasNoNullableAnnotationType<Internal.First>() }) { applyAnnotation(Internal.First::class) }

internal fun <T> T.applyInternalLastOnce() =
    runWhen({ hasNoNullableAnnotationType<Internal.Last>() }) { applyAnnotation(Internal.Last::class) }

internal fun <T> T.applyIgnoreOnce() =
    runWhen({ hasNoNullableAnnotationType<Ignore>() }) { applyAnnotation(Ignore::class) }

internal fun <T> T.applyEnlistedOnce() =
    runWhen({ hasNoNullableAnnotationType<Enlisted>() }) { applyAnnotation(Enlisted::class) }

internal fun <T> T.applyUnlistedOnce() =
    runWhen({ hasNoNullableAnnotationType<Unlisted>() }) { applyAnnotation(Unlisted::class) }

internal fun filterIsSynchronous(it: Any) = it.typeIs<Synchronous, _>()

internal fun filterIsSynchronized(it: Any) = it.typeIs<Synchronized, _>()

internal fun Any?.asCoroutineScope() = asType<CoroutineScope>()
internal fun CoroutineContext.asSchedulerContext() = asTypeUnsafe<SchedulerContext>()
internal fun Any?.asWork() = asType<Work>()
private fun Any?.asItem() = asType<AnyItem>()
private fun Any?.asTag() = asType<Tag>()

private typealias SchedulerScopeWork = SchedulerScope.Companion.() -> Unit
private typealias SchedulerWork = Scheduler.() -> Unit

internal typealias SchedulerNode = AnnotationClass
internal typealias SchedulerPath = Array<ThrowableKClass>
typealias SchedulerStep = suspend CoroutineScope.(Any?, Job) -> SchedulerStepIdentityType
fun interface SchedulerFunction : suspend (CoroutineScope, Any?, Job) -> SchedulerStepIdentityType
private typealias AnySchedulerStep = suspend CoroutineScope.(Any?, Job) -> Any?
fun interface AnySchedulerFunction : suspend (CoroutineScope, Any?, Job) -> Any?
internal typealias SchedulerPrediction = suspend CoroutineScope.(Any?, Job) -> PredictionIdentityType
fun interface SchedulerPredicate : suspend (CoroutineScope, Any?, Job) -> PredicateIdentityType

internal typealias SchedulerStepIdentityType = Any?

internal typealias JobContinuation = suspend (Any?, Any?) -> JobContinuationDefaultIdentityType
internal typealias AnyJobContinuation = suspend (Any?, Any?) -> JobContinuationIdentityType
internal typealias JobExtension = suspend (Any?, Any?, Any?) -> JobExtensionDefaultIdentityType
internal typealias AnyJobExtension = suspend (Any?, Any?, Any?) -> JobExtensionIdentityType

private typealias JobContinuationIdentityType = Any?
private typealias JobContinuationDefaultIdentityType = Unit
private typealias JobExtensionIdentityType = Any?
private typealias JobExtensionDefaultIdentityType = Unit

private typealias JobPredicate = (Job) -> PredicateIdentityType
internal typealias Prediction = suspend () -> PredicateIdentityType

internal typealias PropertyBuildCondition = suspend (AnyKProperty, TagType, AnyStep) -> PropertyBuildConditionIdentityType
private typealias PropertyPredicate = suspend (AnyKProperty) -> PredictionIdentityType
private typealias PropertyStateFunction = suspend (AnyKProperty) -> Any?

private typealias PropertyBuildConditionIdentityType = Any?

private typealias AnyItem = Item<*>
internal typealias FunctionSet = MutableSet<FunctionItem>
internal typealias FunctionSetPointer = () -> FunctionSet
internal typealias FunctionItem = TagTypeToAnyPair
private typealias TagTypeToAnyPair = Pair<TagTypePointer, Any>

internal typealias CoroutineFunction = (CoroutineStep) -> Any?
internal typealias AnyCoroutineFunction = (AnyCoroutineStep) -> Any?
private typealias CoroutinePointer = () -> CoroutineStep?
internal typealias AnyCoroutinePointer = () -> AnyCoroutineStep?
private typealias StepFunction = (Step) -> Any?
private typealias AnyStepFunction = (AnyStep) -> Any?
private typealias StepPointer = () -> Step

internal typealias Work = () -> Unit
internal typealias Step = suspend () -> Unit
internal typealias AnyStep = AnySuspendFunction
internal typealias AnyToAnyStep = AnyToAnySuspendFunction
typealias CoroutineStep = suspend CoroutineScope.() -> Unit
internal typealias AnyCoroutineStep = suspend CoroutineScope.() -> Any?
private typealias AnyFlowCollector = FlowCollector<Any?>
private typealias Self = SchedulerScope.Task<*>
private typealias TaskInput = Any?

internal fun Context.registerReceiver(filter: IntentFilter) =
    registerReceiver(this, receiver, filter, null,
        clock?.alsoStartAsync()?.handler,
        RECEIVER_EXPORTED)

// tagging

internal fun getTag(stage: ContextStep): TagType = returnTag(stage)!!
private fun getTag(callback: Runnable): TagType? = returnTag(callback)
private fun getTag(msg: Message): TagType? = returnTag(msg)

private fun combineTags(tag: TagType, self: TagType?) =
    if (self === null) tag
    else reduceTags(tag, TAG_DOT, self.toTagType())

internal fun reduceTags(vararg tag: TagType) =
    (if (TagType::class.isNumber)
        tag.map(TagType::toInt)
            .reduce(Int::processTag)
    else
        tag.map(TagType::toString)
            .reduce(String::processTag)).asTagTypeUnsafe()

internal fun returnTag(it: Any?) = it.asReference().tag?.id

private fun Any?.hashTag() = hashCode().toTagType()

private fun Number.toTagType() =
    resultUnless({ TagType::class.isNumber }, Any::toString).asTagTypeUnsafe()

private fun String.toTagType() =
    resultWhen({ TagType::class.isNumber }, String::toInt).asTagTypeUnsafe()

private val KClass<TagType>.isNumber
    get() = TagType::class isNotObject String::class

private val KClass<TagType>.isString
    get() = (TagType::class isObject String::class) or
            (TagType::class isObject CharSequence::class)

private fun Number.processTag(it: Number) = toInt().plus(it.toInt())
private fun String.processTag(it: String) = plus(it)

private typealias TaggedProvider = Pair<Provider, Tag>
private typealias TaggedCallableProvider = Pair<CallableProvider, Tag>
private typealias TagConversionMapEntry = Pair<Int, TagType>
private typealias TagTransformationMapEntry = Pair<TagType, Any?>
private typealias TagTranslationMapEntry = Pair<TagType, TagType>

internal typealias PathType = String
internal typealias PathArray = Array<PathType>
private typealias TagTypePointer = () -> TagType?

internal typealias LevelType = Byte
internal typealias ChannelType = Short
internal typealias TransitType = Short
private typealias Transit = TransitType?

internal fun Array<out Tag>.mapToTagArray() = mapToTypedArray { it.id }
internal fun Array<out Path>.mapToTagArray() = mapToTypedArray { it.id }

internal fun Number.toLevel() = toByte()
internal fun Number?.toChannel() = asType<ChannelType>()
internal fun Number?.toTransit() = asType<TransitType>()
internal fun Number?.toCoordinateTarget(): AnyKClass = Any::class
internal fun Number?.toCoordinateKey() = asType<KeyType>()

private fun Any?.asTagType() = asType<TagType>()
private fun Any?.asTagTypeUnsafe() = asTypeUnsafe<TagType>()

internal const val no_channel: ChannelType = 0
internal const val no_transit: TransitType = 0

internal val EMPTY_COROUTINE: CoroutineStep = {}
internal val EMPTY_STEP = suspend {}
private val EMPTY_WORK = {}

private typealias CoroutineKFunction = KFunction<CoroutineStep?>
private typealias ResolverKClass = KClass<out Resolver>
private typealias ResolverKProperty = KMutableProperty<out Resolver?>