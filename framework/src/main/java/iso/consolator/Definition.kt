@file:JvmName("Definition")
@file:JvmMultifileClass

package iso.consolator

import android.view.*
import androidx.lifecycle.*

// universally known truth for an intent, outcome, or cause for actions
internal interface Truth {
    context(_: Resolver)
    fun intend(): ActionIntent
}

// internally understood cause for an action
enum class Reason : Truth, Rationale {
    ;

    interface Record
}

// use map as implicit context receiver
internal fun <M : Map<K, V>, K, V, R : Truth> M.interpreted(reason: Rationale): R = TODO()

typealias Rationale = Comparable<Reason>

// unit element type for logical contextual resolution
sealed interface ActionIntent

// internal working of descriptor with coordinators forms the logical context for programmatic context resolution
// operates in three hierarchical levels/frames:
//  A. dependency (progressive)
//  B. expectancy (regressive)
//  C. order (template)
// order and dependency planes are cold planes in the computer language way of problem solving
// this is a requirement that mandates itself by the involvement of time and unknown entities in existence at runtime
// with respect to limitations in computation such as memory shortage and logical destruction
// in a program, order governs the dependency layer and its design (language, descriptiveness, etc.)
// expectancy then takes on a humanly intelligent form of speech and thinking and sets the tone for the AI layer
// functions in this interface define their context parameter specific to their scope and use cases
sealed interface Descriptor<in S, out T> {
    context(_: AnyDescriptor)
    fun <A : S, B : S> A.onValueChanged(value: B, block: Descriptor<S, T>.(T) -> Any?) = this@Descriptor
}

// exposes flexible/awareness points of synchronous units in active contexts
// internal interface for structural pointer optimized for resolving program logic
sealed interface Refactor<S> {
    infix fun S.from(ref: S): Any?
}

// defines interactive contexts for routines, steps, and states (timeframes, order of executions, etc.)
interface Coordinator<S, out L : S> : Refactor<S> {
    // demands intermediate states to be defined for context resolution and view state definitions
    override infix fun S.from(ref: S): L

    fun <R> synchronize(lock: S? = null, block: () -> R): R

    // external interface optimized for expressive logic (reactivity and flexibility) isolated by freedom on axis of concurrency
    // possibly use context parameter allowing calls only from state descriptor and registered views and to support active contexts
    val descriptor: Descriptor<S, L>?
}

// defines logical contexts for units in execution models such as re-invocation builders and reactive reflectors
sealed interface ContextCoordinator<S, out L : S> : Coordinator<S, L> {
    // connects to internal state referables
    fun S.acquire(): S

    // restrict call site access to active context by context parameter (optional)
    infix fun S.issue(cmd: (S, L) -> ActionIntent)
}

// logical context variances

sealed interface ContraVariantContextCoordinator<in I, S, out L : S> : ContextCoordinator<S, L>

sealed interface CoVariantContextCoordinator<out I, S, out L : S> : ContextCoordinator<S, L>

// view state coordinators

sealed interface VariableCoordinator<S> : Coordinator<S, S>
sealed interface DirectCoordinator<S> : VariableCoordinator<S>

typealias StateCoordinator = Coordinator<State, State>
typealias VariableStateCoordinator = VariableCoordinator<State>
typealias DirectStateCoordinator = DirectCoordinator<State>
typealias ViewStateCoordinator = Coordinator<State, ViewState>

interface ViewCoordinator<in V : ViewGroup, out S : ViewState> : ContraVariantContextCoordinator<V, State, S>, ViewModelConnector {
    // notifies view or view group - coordinates response internally
    context(_: ViewState)
    fun <S : ViewState> V.onViewStateChanged(vararg occurrence: S): Array<out S>
}

interface ViewGroupCoordinator<out U : View, out S : ViewState> : CoVariantContextCoordinator<U, State, S>, ViewModelConnector {
    // notifies view connected to another view or view group - broadcasts response externally
    context(_: ViewGroup, _: ViewState)
    fun <V : View, S : ViewState> V.onViewStateChanged(vararg occurrence: S): Array<out S>
}

// demands view states to be the units for transacting with view models
sealed interface ViewModelConnector {
    fun attach(model: ViewModel)
    fun detach(model: ViewModel)
}