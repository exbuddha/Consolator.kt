@file:JvmName(JVM_CLASS_NAME)
@file:JvmMultifileClass

package iso.consolator

import android.os.*
import android.view.*
import androidx.lifecycle.*
import iso.consolator.Lock.*
import iso.consolator.component.*
import kotlin.reflect.*
import kotlinx.coroutines.*

sealed interface StateDescriptor<in S : State, out L : State> : Descriptor<State, L> {
    context(_: AnyDescriptor)
    fun <A : S, T : StateType> A.onValueTypeChanged(type: T, block: Descriptor<S, L>.(L) -> Any?) = this@StateDescriptor
}

typealias BaseStateDescriptor = Descriptor<State, State>
private typealias DirectStateDescriptor = StateDescriptor<State, State>

// view state descriptors - view state changes are described for view items

interface ViewStateDescriptor<out S : State> : StateDescriptor<ViewState, S>

private typealias BaseViewStateDescriptor = Descriptor<ViewState, ViewState>
private typealias ImplicitViewStateDescriptor = Descriptor<State, ViewState>
private typealias ExplicitViewStateDescriptor = StateDescriptor<ViewState, ViewState>

private typealias IndirectViewStateDescriptor = StateDescriptor<ViewState, State>
typealias DirectViewStateDescriptor = ViewStateDescriptor<ViewState>

// in addition to advanced scheduler scopes that will be available for contextual features
// callables can be considered as the origination points for some features and routines
// they may receive any value in order to resolve their own or another active context
// states concurrently maintain and transact with the flow of communication among routines
// context parameter allows to selectively operate in active contexts or on routines

internal fun <R, S : R> KCallable<R>.receiveUniquely(value: S) = value

context(_: AnyKProperty)
internal fun <R, S : R> KCallable<R>.receiveUniquely(value: S) = value

internal fun <R, S : R> KCallable<R>.receive(value: S) =
    receiveUniquely(value).also(run(::asKMutableProperty)!!::setInstance)

context(_: AnyKProperty)
internal fun <R, S : R> KCallable<R>.receive(value: S) =
    run { receiveUniquely(value).also(run(::asKMutableProperty)!!::setInstance) }

internal fun <R> KCallable<R>.determine(vararg subroutine: KCallable<R>? = asTypedArray()) = this

context(_: AnyKProperty)
internal fun <R> KCallable<R>.determine(vararg subroutine: KCallable<R>? = asTypedArray()) = this

internal fun <R> KCallable<R>.perceive(vararg subroutine: KCallable<R>? = asTypedArray()) = this

context(_: AnyKProperty)
internal fun <R> KCallable<R>.perceive(vararg subroutine: KCallable<R>? = asTypedArray()) = this

internal fun <R> KCallable<R>.satisfy(vararg subroutine: KCallable<R>? = asTypedArray()) = this

context(_: AnyKProperty)
internal fun <R> KCallable<R>.satisfy(vararg subroutine: KCallable<R>? = asTypedArray()) = this

internal fun <R> KCallable<R>.falsify(vararg subroutine: KCallable<R>? = asTypedArray()) = this

context(_: AnyKProperty)
internal fun <R> KCallable<R>.falsify(vararg subroutine: KCallable<R>? = asTypedArray()) = this

internal fun <R> KCallable<R>.fulfill(vararg subroutine: KCallable<R>? = asTypedArray()) = this

context(_: AnyKProperty)
internal fun <R> KCallable<R>.fulfill(vararg subroutine: KCallable<R>? = asTypedArray()) = this

internal fun <R> KCallable<R>.forfeit(vararg subroutine: KCallable<R>? = asTypedArray()) = this

context(_: AnyKProperty)
internal fun <R> KCallable<R>.forfeit(vararg subroutine: KCallable<R>? = asTypedArray()) = this

internal fun <R> KCallable<R>.resolve(vararg subroutine: KCallable<R>? = asTypedArray()) = this

context(_: AnyKProperty)
internal fun <R> KCallable<R>.resolve(vararg subroutine: KCallable<R>? = asTypedArray()) = this

internal fun <R> KCallable<R>.synchronize(block: () -> R) = synchronized(this, block)

context(_: AnyKProperty)
internal fun <R> KCallable<R>.synchronize(block: () -> R) = synchronized(this, block)

internal operator fun <R> KCallable<R>.plus(lock: AnyKCallable) = this

// view state controllers

internal class GroupCoordinator<out S : ViewState>() : ViewCoordinator<ViewGroup, S>, ViewStateDescriptor<S> {
    constructor(fragment: SchedulerFragment) : this()

    override fun State.acquire() = TODO()

    override infix fun State.issue(cmd: (State, S) -> ActionIntent) = TODO()

    override infix fun State.from(ref: State) = TODO()

    override fun <R> synchronize(lock: State?, block: () -> R) = TODO()

    override fun attach(model: ViewModel) = TODO()

    override fun detach(model: ViewModel) = TODO()

    context(_: ViewState)
    override fun <S : ViewState> ViewGroup.onViewStateChanged(vararg occurrence: S) = occurrence

    context(_: AnyDescriptor)
    override fun <A : State, B : State> A.onValueChanged(value: B, block: Descriptor<State, S>.(S) -> Any?) = TODO()

    context(_: AnyDescriptor)
    override fun <A : ViewState, T : StateType> A.onValueTypeChanged(type: T, block: Descriptor<ViewState, S>.(S) -> Any?) = TODO()

    override val descriptor get() = this

    override fun equals(other: Any?) = isObject(other)

    override fun hashCode() = run(System::identityHashCode)
}

internal class SharedCoordinator<out S : ViewState>() : ViewGroupCoordinator<ViewGroup, S>, ViewStateDescriptor<S> {
    constructor(fragment: SchedulerFragment) : this()

    override fun State.acquire() = TODO()

    override infix fun State.issue(cmd: (State, S) -> ActionIntent) = TODO()

    override infix fun State.from(ref: State) = TODO()

    override fun <R> synchronize(lock: State?, block: () -> R) = TODO()

    override fun attach(model: ViewModel) = TODO()

    override fun detach(model: ViewModel) = TODO()

    context(_: ViewGroup, _: ViewState)
    override fun <V : View, S : ViewState> V.onViewStateChanged(vararg occurrence: S) = occurrence

    context(_: AnyDescriptor)
    override fun <A : State, B : State> A.onValueChanged(value: B, block: Descriptor<State, S>.(S) -> Any?) = TODO()

    context(_: AnyDescriptor)
    override fun <A : ViewState, T : StateType> A.onValueTypeChanged(type: T, block: Descriptor<ViewState, S>.(S) -> Any?) = TODO()

    override val descriptor get() = this

    override fun equals(other: Any?) = isObject(other)

    override fun hashCode() = run(System::identityHashCode)
}

// dependencies work in three isolated scopes:
//   1. functional
//   2. contextual (super-functional)
//   3. low-level (view states and user states)
// super-functional dependency layer understands functional requirements by refactoring logic [*]
// functional dependencies are closely tied to low-level requirements in synchronizer logic
// low-level requirements are derived from base states via coordinator.descriptors
// refactoring layer translates descriptions to context-coordinator instructions by dependency revision [*]
// action intents are used to derive instructions from state-coordinator conversion maps
// referables are modelled to intent types and variables in context-coordinator instruction sets
// system maintains active contexts and controls the models (related to program contexts and events)
// engine coordinates system routines (related to execution models and preferences)

internal sealed interface ActiveScope

internal sealed interface Dependence<in S> {
    fun revise(vararg map: Array<out S>) {}
}

internal sealed interface StateDependence<in S : State> : Dependence<S> {
    context(_: CoroutineScope)
    fun <R, O : S> determined(vararg state: S?): (O) -> R

    sealed interface Inside<in S : State> : StateDependence<S> {
        fun FunctionSet.withState(vararg ref: S): FunctionSetPointer = { this }
    }

    sealed interface Outside<in S : State> : StateDependence<S> {
        fun FunctionSet.withoutState(vararg ref: S): FunctionSetPointer = { this }
    }

    sealed interface Indirect<in S : State> : Inside<S>, Outside<S> {
        override fun FunctionSet.withState(vararg ref: S): FunctionSetPointer = { this }
        override fun FunctionSet.withoutState(vararg ref: S): FunctionSetPointer = { this }
    }

    sealed interface Direct<S : State> : Inside<S>, Outside<S> {
        override fun FunctionSet.withState(vararg ref: S): FunctionSetPointer = { this }
        override fun FunctionSet.withoutState(vararg ref: S): FunctionSetPointer = { this }
    }
}

internal sealed interface StateInterceptor<in S : State> : StateDependence<S>, Interceptor<S>

internal typealias BasicDependence = Dependence<BaseState>
internal typealias NumericDependence = Dependence<Number>

sealed interface LockState : State, FunctionSetState {
    // register key and block with thread (key may be a message)
    context(_: Any)
    fun <R> LockState.switch(key: Any?, block: () -> R) =
        super<State>.invoke(key, block)

    // assign pre-block lock invoker to registered key
    context(_: Any)
    infix fun LockState.unlock(pass: (LockState) -> Lock) =
        pass(this).also {
        this[-1] = it }

    // assign post-block lock invoker to registered key, record value
    context(_: Any)
    fun <R> LockState.release(value: R, pass: (LockState) -> LockState): R {
        this[-2] = pass(this)
        return value }

    // post param state (param may be a registered key or an internal lock)
    override fun invoke(vararg param: Any?) =
        super<FunctionSetState>.invoke(*param)
}

sealed interface Routine : State, KCallable<LockState>

// routine descriptors - relations among internal variables are described

private interface RoutineDescriptor : BaseRoutineDescriptor

sealed interface FunctionSetState : State, FunctionSetPointer

sealed class BaseState : State, IndexNumber() {
    // handle static states
    override fun equals(other: Any?) = when (other) {
        is State -> super.equals(other)
        is Number -> super.equals(other)
        else -> super.equals(other) }

    override fun hashCode() = toInt()
}

sealed class OpenState : BaseState() {
    abstract fun <S : Number> S.asUnifiedStateId(): Number

    // may rollback unto description plane
    internal abstract inner class BooleanTypeState : OpenState(), BooleanState
}

sealed interface BooleanState : State, BooleanType

internal fun Number?.toStateId() = asType<StateID>()

internal fun AnyKCallable.asState() = State of asKProperty()!!

// user states are registered and loaded with control flow logic at runtime
// they can be retrieved as live references and provide access to job controllers

internal typealias StateID = Short

sealed interface ViewState : State

infix fun <S : State, T> S?.`is`(value: T) = this == value

infix fun <T> Any?.stateIs(value: T) = `is`(value)

sealed interface State {
    data object Succeeded : Resolved
    data object Failed : Resolved

    sealed interface Resolved : State {
        companion object : Resolved {
            @JvmStatic inline infix fun where(predicate: BooleanPointer) =
                runUnless<State, _>({ predicate() stateIs true }) { Unresolved }

            @JvmStatic inline infix fun unless(predicate: BooleanPointer) =
                runWhen<State, _>({ predicate() stateIs true }) { Unresolved }
    } }

    sealed interface Unresolved : State { companion object : Unresolved }
    sealed interface Ambiguous : State { companion object : Ambiguous }

    companion object : ActiveContextSynchronizer, DirectStateDescriptor, OpenState(), StateDependence.Inside<State>, NumericUnifier {
        @JvmStatic lateinit var VM: ViewModel

        context(self: Any)
        @JvmStatic fun ofSelf(): State = Ambiguous

        @JvmStatic fun of(vararg args: Any?): State = Ambiguous

        @JvmStatic infix fun <V : View> of(view: V): State? = null

        internal infix fun <J : Job> of(job: J): State? = null

        internal infix fun of(msg: Message): BaseState? = null

        internal infix fun <R : Runnable> of(callback: R): State? = null

        internal infix fun <P : AnyKProperty> of(property: P): State = Ambiguous

        abstract class BaseViewState : ViewState

        // blocks committed by container must be cleared by lifecycle expiry
        context(_: DirectStateDescriptor)
        @JvmStatic fun <R : State> accept(container: ViewGroup?, descriptor: KMutableProperty<out BaseStateDescriptor>, block: BaseStateDescriptor.(State) -> R): State {
            // register container - prepare for view restarts
            // set view state descriptor in view group
            // use callable item to set/get value if this intrinsically throws an unsupported exception at runtime due to being a reified type parameter
            descriptor.setInstance(object : DirectViewStateDescriptor {
                context(_: AnyDescriptor)
                override fun <A : State, B : State> A.onValueChanged(value: B, block: ImplicitViewStateDescriptor.(ViewState) -> Any?) =
                    descriptor.getAsTypeUnsafe<_, ImplicitViewStateDescriptor>().apply {
                    when (this@onValueChanged) {
                        is BaseViewState -> { /* assign new transit map to the instance of change for the view state */ }
                        else -> {} } }

                context(_: AnyDescriptor)
                override fun <A : ViewState, T : StateType> A.onValueTypeChanged(type: T, block: BaseViewStateDescriptor.(ViewState) -> Any?) =
                    descriptor.getAsTypeUnsafe<_, ExplicitViewStateDescriptor>().apply {
                    when (type) {
                        Resolved::class -> { /* assign new transit map to the type of change out of resolved state for the view state */ }
                        else -> {} } }
            })
            return block(object : BaseViewState() {}) /* or assigned init state */ }

        // accepts internal states and generated states from registered views
        @JvmStatic inline fun <S : State, reified R : S> initial(state: S) =
            when (state) {
                is BaseViewState -> state // mark initial view state for view group or view
                else -> this
            } as R

        // registers states to call site specs and accepts known arguments
        context(_: DirectStateDescriptor)
        @JvmStatic fun State.register(vararg args: Any?): State {
            args.firstOrNull()?.let { first ->
            when (first) {
                is Bundle -> { /* register saved instance state for view group */ }
            } }
            return this }

        // enables communication with internal state referables
        context(_: AnyDescriptor)
        override fun <A : State, B : State> A.onValueChanged(value: B, block: BaseStateDescriptor.(State) -> Any?): DirectStateDescriptor =
            when (this) {
            this@Companion -> {
                // register commit block
                this@Companion }
            else ->
                TODO() }

        context(_: AnyDescriptor)
        override fun <A : State, T : StateType> A.onValueTypeChanged(type: T, block: BaseStateDescriptor.(State) -> Any?) = TODO()

        // enables super-imposing view groups (registered views)
        context(_: View)
        @JvmStatic fun <V : View, A : State, B : State> V.onViewStateChanged(first: A, second: B, block: ImplicitViewStateDescriptor.(StateArray) -> Any?): Unit = TODO()

        context(_: CoroutineScope)
        override fun <R, O : State> determined(vararg state: State?): (O) -> R = TODO()

        override infix fun State.from(ref: State) = TODO()

        override fun State.acquire(vararg context: Any?, block: StateCoordinator.(StateArray) -> Unit) = TODO()

        // allows to coordinate view states with internal states
        override fun State.acquire() = TODO()

        override fun <S : State, R> access(state: S, block: StateCoordinator.(S) -> R) = TODO()

        override infix fun State.issue(cmd: (State, State) -> ActionIntent) = TODO()

        override fun release(lock: State) =
            when (lock) {
                is ViewState -> { /* update referables in context - revise dependency maps */ }
                else -> Unit }

        override fun merge(context: StateCoordinator?): BaseContextCoordinator = this

        override fun FunctionSet.withState(vararg ref: State) = TODO()

        override fun invoke(): FunctionSet = TODO()

        @JvmStatic override val descriptor
            get() = this

        override fun <R> synchronize(lock: State?, block: () -> R) =
            synchronized(lock ?: this, block)

        override operator fun get(id: StateID): State = when (id.asSelectableStateId()) {
            1 -> Resolved unless ::isRuntimeDbOrSessionNull
            2 -> Resolved unless ::isLogDbOrNetDbNull
            else -> Open
        }

        override operator fun set(id: StateID, state: Any) { when (id.asSelectableStateId()) {
            -1 -> if (state is Resolved) when {
                currentThread.isMainThread -> {
                    /* touch context has been called and main uncaught exception handler is initialized */ }
                else -> {
                    /* report to handler scope */ } }
            1 -> if (state is Resolved) SchedulerScope().windDown()
        } }

        private fun <S : Number> S.asSelectableStateId() = asUnifiedStateId().toInt()

        override fun <S : Number> S.asUnifiedStateId(): StateID = run(::unifyNumberUnsafe)

        override operator fun plus(state: Any): State = Ambiguous
        override operator fun minus(state: Any): State = Ambiguous
        override operator fun rangeTo(state: Any): State = Ambiguous
        override operator fun not(): State = Ambiguous
        override operator fun contains(state: Any) = false

        internal operator fun plusAssign(lock: Any) {}
        internal operator fun minusAssign(lock: Any) {}
    }

    operator fun invoke(vararg param: Any?) =
        (param.firstOrNull() ?: this) as Lock

    operator fun get(id: StateID) = this

    operator fun set(id: StateID, state: Any) = when (id.asSelectableStateId()) {
        -1 -> { /* unlock state */ }
        -2 -> { run(::release) }
        else -> Unit }

    operator fun plus(state: Any) = this
    operator fun minus(state: Any) = this
    operator fun rangeTo(state: Any) = this
    operator fun not(): State = this
    operator fun contains(state: Any) = isObject(state)
}

// state transaction functions

internal inline fun <L : Any, R, S : R> transact(noinline lock: () -> L, predicate: (L?) -> Boolean = { true }, block: (L) -> R, fallback: () -> S? = { null }): R? {
    if (predicate(null))
        lock().let { key ->
        commitAsync(key, predicate)
            { return block(key) } }
    return fallback() }

internal inline fun <R, S : R> transact(state: State, predicate: StatePredicate, block: (Any) -> R, fallback: () -> S? = { null }): R? {
    if (predicate(state, null))
        state().let { lock ->
        commitAsync(lock,
            { predicate(state, lock) },
            { return block(lock) }) }
    return fallback() }

private typealias StatePredicate = (State, Any?) -> PredicateIdentityType
private typealias AnyStatePredicate = (Any, Any?) -> PredicateIdentityType

internal typealias StateArray = Array<out State>
internal typealias ViewStateArray = Array<out ViewState>

internal typealias StateType = KClass<out State>
internal typealias ViewStateType = KClass<out ViewState>

private inline fun <R, reified S : KCallable<R>> S.asTypedArray() =
    arrayOf<S>(asTypeUnsafe())

private typealias BaseRoutineDescriptor = Descriptor<Routine, State>
internal typealias AnyDescriptor = Descriptor<*,*>