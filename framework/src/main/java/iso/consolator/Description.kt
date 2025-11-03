@file:JvmName("Description")
@file:JvmMultifileClass

package iso.consolator

import androidx.lifecycle.*
import kotlin.reflect.*
import kotlin.annotation.AnnotationRetention.*
import kotlin.annotation.AnnotationTarget.*
import kotlinx.coroutines.*
import iso.consolator.annotation.Tag
import iso.consolator.annotation.TagType

context(_: LifecycleOwner)
fun <I : Thread, T, R> I.from(lic: KCallable<T>, block: () -> R) = block()

context(_: LifecycleOwner)
fun <I : Thread, R> I.from(lic: Tag, block: () -> R) = block()

context(_: LifecycleOwner)
fun <I : Thread, R> I.from(lic: TagType, block: () -> R) = block()

context(_: LifecycleOwner)
fun <I : Thread, S : ProcessScope, T, R> I.from(lic: KCallable<T>, scope: S, thru: S.() -> R) = scope.thru()

context(_: LifecycleOwner)
fun <I : Thread, S : ProcessScope, R> I.from(lic: Tag, scope: S, thru: S.() -> R) = scope.thru()

context(_: LifecycleOwner)
fun <I : Thread, S : ProcessScope, R> I.from(lic: TagType, scope: S, thru: S.() -> R) = scope.thru()

context(_: LifecycleOwner)
fun <I : Runnable, T, R> I.from(lic: KCallable<T>, block: () -> R) = block()

context(_: LifecycleOwner)
fun <I : Runnable, R> I.from(lic: Tag, block: () -> R) = block()

context(_: LifecycleOwner)
fun <I : Runnable, R> I.from(lic: TagType, block: () -> R) = block()

context(_: LifecycleOwner)
fun <I : Runnable, S : ProcessScope, T, R> I.from(lic: KCallable<T>, scope: S, thru: S.() -> R) = scope.thru()

context(_: LifecycleOwner)
fun <I : Runnable, S : ProcessScope, R> I.from(lic: Tag, scope: S, thru: S.() -> R) = scope.thru()

context(_: LifecycleOwner)
fun <I : Runnable, S : ProcessScope, R> I.from(lic: TagType, scope: S, thru: S.() -> R) = scope.thru()

// timeframe descriptors - contextual divergences in or out of items are described

internal typealias BaseStepDescriptor = Descriptor<AnyStep, AnyStep>

internal interface StepDescriptor : BaseStepDescriptor

private typealias BaseCoroutineDescriptor = Descriptor<AnyCoroutineStep, AnyCoroutineStep>

private interface CoroutineDescriptor : BaseCoroutineDescriptor

private typealias BaseJobDescriptor = Descriptor<Job, Job>

private interface JobDescriptor : BaseJobDescriptor

private typealias BaseJobStepDescriptor = Descriptor<Job, AnyCoroutineStep>

private interface JobStepDescriptor : BaseJobStepDescriptor

private typealias BaseJobContinuationDescriptor = Descriptor<Job, JobContinuation>

private interface JobContinuationDescriptor : BaseJobContinuationDescriptor

internal typealias BaseSequenceDescriptor = Descriptor<AnyTriple, AnyTriple>

internal interface SequenceDescriptor : BaseSequenceDescriptor

// stateful descriptors - state changes are described in item sets

private typealias BaseRunnableDescriptor = Descriptor<LockState, BaseState>

private interface RunnableDescriptor : BaseRunnableDescriptor

internal typealias BaseDescriptor = Descriptor<BaseState, BaseState>

internal interface MessageDescriptor : BaseDescriptor

@Retention(SOURCE)
@Target(CLASS, CONSTRUCTOR, FUNCTION, PROPERTY, PROPERTY_GETTER, PROPERTY_SETTER)
@Repeatable
annotation class Event(
    val transit: TransitType = no_transit) {

    @Retention(SOURCE)
    @Target(FUNCTION, PROPERTY)
    @Repeatable
    annotation class Listening(
        val channel: ChannelType = no_channel,
        val timeout: Time = no_timeout) {

        @Retention(SOURCE)
        @Target(FUNCTION, PROPERTY)
        @Repeatable
        annotation class OnEvent(
            val transit: TransitType = no_transit)
    }

    @Retention(SOURCE)
    @Target(FUNCTION, PROPERTY)
    @Repeatable
    annotation class Committing(
        val channel: ChannelType = no_channel)

    @Retention(SOURCE)
    @Target(FUNCTION, PROPERTY)
    annotation class Retrying(
        val channel: ChannelType = no_channel)

    @Retention(SOURCE)
    @Target(FUNCTION, PROPERTY)
    annotation class Repeating(
        val channel: ChannelType = no_channel,
        val count: Int = 0)
}

@Retention(SOURCE)
@Target(CLASS, CONSTRUCTOR, FUNCTION, ANNOTATION_CLASS, PROPERTY, PROPERTY_GETTER, PROPERTY_SETTER, ANNOTATION_CLASS)
annotation class Track(
    val id: TrackID = no_track) {

    @Retention(SOURCE)
    @Target(CLASS, FUNCTION, PROPERTY_GETTER, PROPERTY, PROPERTY_SETTER, ANNOTATION_CLASS)
    annotation class Item(
        val id: ItemID = no_item) {

        @Retention(SOURCE)
        @Target(FUNCTION, PROPERTY)
        annotation class Ahead /* affects system/scheduler post calls and explains timeframe collisions */

        @Retention(SOURCE)
        @Target(FUNCTION, PROPERTY)
        annotation class First /* affects scheduler commits and post calls */

        @Retention(SOURCE)
        @Target(FUNCTION, PROPERTY)
        annotation class Last /* affects scheduler commits and post calls */
    }

    @Retention(SOURCE)
    @Target(CLASS, FUNCTION, PROPERTY, ANNOTATION_CLASS)
    annotation class Relative(
        /* sets timeframe-related requirements among sibling annotations as well as static meta-data */
        val id: ItemID = no_item) {

        @Retention(SOURCE)
        @Target(FUNCTION, PROPERTY)
        annotation class Post {

            @Retention(SOURCE)
            @Target(FUNCTION, PROPERTY)
            annotation class To(
                val item: ItemID = no_item,
                val ref: ReferenceID = Nothing::class)
        }

        @Retention(SOURCE)
        @Target(FUNCTION, PROPERTY)
        annotation class Prior {

            @Retention(SOURCE)
            @Target(FUNCTION, PROPERTY)
            annotation class To(
                val item: ItemID = no_item,
                val ref: ReferenceID = Nothing::class)
        }

        @Retention(SOURCE)
        @Target(FUNCTION, PROPERTY)
        annotation class To(
            val track: TrackID = no_track,
            val item: ItemID = no_item,
            val ref: ReferenceID = Nothing::class) {

            @Retention(SOURCE)
            @Target(FUNCTION, PROPERTY)
            annotation class Include(
                val refs: ReferenceArray = [])

            @Retention(SOURCE)
            @Target(FUNCTION, PROPERTY)
            annotation class Exclude(
                val refs: ReferenceArray = [])

            @Retention(SOURCE)
            @Target(FUNCTION, PROPERTY)
            annotation class Retain(
                val refs: ReferenceArray = [])
        }
    }
}

@Retention(SOURCE)
@Target(FUNCTION, PROPERTY)
annotation class NoItem

internal fun filterIsEvent(it: Any) = it.typeIs<Event, _>()

internal const val empty_string = ""
internal const val no_track = empty_string
internal const val no_item = ItemID.MIN_VALUE

private typealias TrackID = PathType
internal typealias ItemID = TagType
private typealias ReferenceID = AnnotationClass
private typealias ReferenceArray = Array<ReferenceID>

// terminology annotations set order within long annotation sequences

@Retention(SOURCE)
@Target(CLASS, FUNCTION, ANNOTATION_CLASS)
internal annotation class TERM {

    @Retention(SOURCE)
    @Target(CLASS, FUNCTION, ANNOTATION_CLASS)
    annotation class BEGIN(
        val line: TermLineID)

    @Retention(SOURCE)
    @Target(CLASS, FUNCTION, ANNOTATION_CLASS)
    annotation class END(
        val line: TermLineID)


    @Retention(SOURCE)
    @Target(CLASS, FUNCTION, ANNOTATION_CLASS)
    annotation class META {

        @Retention(SOURCE)
        @Target(CLASS, FUNCTION, ANNOTATION_CLASS)
        annotation class DATA(
            val value: String = empty_string,
            val type: AnyKClass = Nothing::class)

        @Retention(SOURCE)
        @Target(CLASS, FUNCTION, ANNOTATION_CLASS)
        annotation class TYPE(
            val value: String = empty_string,
            val type: AnyKClass = Nothing::class)

        @Retention(SOURCE)
        @Target(CLASS, FUNCTION, ANNOTATION_CLASS)
        annotation class RAW(
            val value: StringArray = [],
            val type: AnyKClass = Nothing::class)
    }
}

private typealias TermLineID = Char

// include contextual implicit receivers Context, LifecycleOwner, <I : CoroutineScope>, Descriptor

inline infix fun <R, S> (suspend () -> R)?.thenSuspended(crossinline next: suspend () -> S): (suspend () -> S)? = letResult { {
    it()
    next() } }

inline infix fun <R, S> (suspend () -> R)?.afterSuspended(crossinline prev: suspend () -> S): (suspend () -> R)? = letResult { {
    prev()
    it() } }

inline infix fun <R, S> (suspend () -> R)?.thruSuspended(crossinline pass: suspend (R) -> S): (suspend () -> S)? = letResult { {
    pass(it()) } }

inline fun <R> (suspend () -> R)?.givenSuspended(crossinline predicate: Predicate, crossinline fallback: suspend () -> R): (suspend () -> R)? = letResult { {
    if (predicate()) it() else fallback() } }

inline fun <R> (suspend () -> R)?.unlessSuspended(noinline predicate: Predicate, crossinline fallback: suspend () -> R): (suspend () -> R)? =
    givenSuspended(predicate::isFalse, fallback)

inline infix fun Step?.givenSuspended(crossinline predicate: Predicate) =
    givenSuspended(predicate) {}

@Suppress("NOTHING_TO_INLINE")
inline infix fun Step?.unlessSuspended(noinline predicate: Predicate) =
    unlessSuspended(predicate) {}

inline infix fun <I, T : I, R, S> (suspend (T) -> R)?.thenSuspended(crossinline next: suspend (I) -> S): (suspend (T) -> S)? = letResult { {
    it(it)
    next(it) } }

inline infix fun <I, T : I, R, S> (suspend (T) -> R)?.afterSuspended(crossinline prev: suspend (I) -> S): (suspend (T) -> R)? = letResult { {
    prev(it)
    it(it) } }

inline infix fun <T, R, S> (suspend (T) -> R)?.thruSuspended(crossinline pass: suspend (R) -> S): (suspend (T) -> S)? = letResult { {
    pass(it(it)) } }

inline fun <I, T : I, R> (suspend (T) -> R)?.givenSuspended(crossinline predicate: Predicate, crossinline fallback: suspend (I) -> R): (suspend (T) -> R)? = letResult { {
    if (predicate()) it(it) else fallback(it) } }

inline fun <I, T : I, R> (suspend (T) -> R)?.unlessSuspended(noinline predicate: Predicate, crossinline fallback: suspend (I) -> R): (suspend (T) -> R)? =
    givenSuspended(predicate::isFalse, fallback)

inline infix fun <I, T : I, U, R, S> (suspend (T, U) -> R)?.thenSuspended(crossinline next: suspend (I, U) -> S): (suspend (T, U) -> S)? = letResult { { t, u ->
    it(t, u)
    next(t, u) } }

inline infix fun <I, T : I, U, R, S> (suspend (T, U) -> R)?.afterSuspended(crossinline prev: suspend (I, U) -> S): (suspend (T, U) -> R)? = letResult { { t, u ->
    prev(t, u)
    it(t, u) } }

inline infix fun <T, U, R, S> (suspend (T, U) -> R)?.thruSuspended(crossinline pass: suspend (R) -> S): (suspend (T, U) -> S)? = letResult { { t, u ->
    pass(it(t, u)) } }

inline fun <I, T : I, U, R> (suspend (T, U) -> R)?.givenSuspended(crossinline predicate: Predicate, crossinline fallback: suspend (I, U) -> R): (suspend (T, U) -> R)? = letResult { { t, u ->
    if (predicate()) it(t, u) else fallback(t, u) } }

inline fun <I, T : I, U, R> (suspend (T, U) -> R)?.unlessSuspended(noinline predicate: Predicate, crossinline fallback: suspend (I, U) -> R): (suspend (T, U) -> R)? =
    givenSuspended(predicate::isFalse, fallback)

inline infix fun <I, T : I, U, V, R, S> (suspend (T, U, V) -> R)?.thenSuspended(crossinline next: suspend (I, U, V) -> S): (suspend (T, U, V) -> S)? = letResult { { t, u, v ->
    it(t, u, v)
    next(t, u, v) } }

inline infix fun <I, T : I, U, V, R, S> (suspend (T, U, V) -> R)?.afterSuspended(crossinline prev: suspend (I, U, V) -> S): (suspend (T, U, V) -> R)? = letResult { { t, u, v ->
    prev(t, u, v)
    it(t, u, v) } }

inline infix fun <T, U, V, R, S> (suspend (T, U, V) -> R)?.thruSuspended(crossinline pass: suspend (R) -> S): (suspend (T, U, V) -> S)? = letResult { { t, u, v ->
    pass(it(t, u, v)) } }

inline fun <I, T : I, U, V, R> (suspend (T, U, V) -> R)?.givenSuspended(crossinline predicate: Predicate, crossinline fallback: suspend (I, U, V) -> R): (suspend (T, U, V) -> R)? = letResult { { t, u, v ->
    if (predicate()) it(t, u, v) else fallback(t, u, v) } }

inline fun <I, T : I, U, V, R> (suspend (T, U, V) -> R)?.unlessSuspended(noinline predicate: Predicate, crossinline fallback: suspend (I, U, V) -> R): (suspend (T, U, V) -> R)? =
    givenSuspended(predicate::isFalse, fallback)

inline infix fun <I, T : I, U, V, W, R, S> (suspend (T, U, V, W) -> R)?.thenSuspended(crossinline next: suspend (I, U, V, W) -> S): (suspend (T, U, V, W) -> S)? = letResult { { t, u, v, w ->
    it(t, u, v, w)
    next(t, u, v, w) } }

inline infix fun <I, T : I, U, V, W, R, S> (suspend (T, U, V, W) -> R)?.afterSuspended(crossinline prev: suspend (I, U, V, W) -> S): (suspend (T, U, V, W) -> R)? = letResult { { t, u, v, w ->
    prev(t, u, v, w)
    it(t, u, v, w) } }

inline infix fun <T, U, V, W, R, S> (suspend (T, U, V, W) -> R)?.thruSuspended(crossinline pass: suspend (R) -> S): (suspend (T, U, V, W) -> S)? = letResult { { t, u, v, w ->
    pass(it(t, u, v, w)) } }

inline fun <I, T : I, U, V, W, R> (suspend (T, U, V, W) -> R)?.givenSuspended(crossinline predicate: Predicate, crossinline fallback: suspend (I, U, V, W) -> R): (suspend (T, U, V, W) -> R)? = letResult { { t, u, v, w ->
    if (predicate()) it(t, u, v, w) else fallback(t, u, v, w) } }

inline fun <I, T : I, U, V, W, R> (suspend (T, U, V, W) -> R)?.unlessSuspended(noinline predicate: Predicate, crossinline fallback: suspend (I, U, V, W) -> R): (suspend (T, U, V, W) -> R)? =
    givenSuspended(predicate::isFalse, fallback)

inline infix fun <I, T : I, R, S> (suspend T.() -> R)?.then(crossinline next: suspend I.() -> S): (suspend T.() -> S)? = letResult { {
    it()
    next() } }

context(scope: I)
inline infix fun <T : I, I, R, S> (suspend T.() -> R)?.thenSuspendedImplicitly(crossinline next: suspend (I) -> S): (suspend T.() -> S)? = letResult { {
    it()
    next(scope) } }

inline infix fun <I, T : I, R, S> (suspend T.() -> R)?.after(crossinline prev: suspend I.() -> S): (suspend T.() -> R)? = letResult { {
    prev()
    it() } }

context(scope: I)
inline infix fun <T : I, I, R, S> (suspend T.() -> R)?.afterSuspendedImplicitly(crossinline prev: suspend (I) -> S): (suspend T.() -> R)? = letResult { {
    prev(scope)
    it() } }

inline infix fun <I, T : I, R, S> (suspend T.() -> R)?.thru(crossinline pass: suspend I.(R) -> S): (suspend T.() -> S)? = letResult { {
    pass(it()) } }

context(scope: I)
inline infix fun <T : I, I, R, S> (suspend T.() -> R)?.thruSuspendedImplicitly(crossinline pass: suspend (I, R) -> S): (suspend T.() -> S)? = letResult { {
    pass(scope, it()) } }

inline fun <I, T : I, R> (suspend T.() -> R)?.given(crossinline predicate: Predicate, crossinline fallback: suspend I.() -> R): (suspend T.() -> R)? = letResult { {
    if (predicate()) it() else fallback() } }

context(scope: I)
inline fun <T : I, I, R> (suspend T.() -> R)?.givenSuspendedImplicitly(crossinline predicate: Predicate, crossinline fallback: suspend (I) -> R): (suspend T.() -> R)? =
    given(predicate) { fallback(scope) }

inline fun <I, T : I, R> (suspend T.() -> R)?.unless(noinline predicate: Predicate, crossinline fallback: suspend I.() -> R): (suspend T.() -> R)? =
    given(predicate::isFalse, fallback)

context(scope: I)
inline fun <T : I, I, R> (suspend T.() -> R)?.unlessSuspendedImplicitly(noinline predicate: Predicate, crossinline fallback: suspend (I) -> R): (suspend T.() -> R)? =
    unless(predicate) { fallback(scope) }

inline infix fun <I, T : I, U, R, S> (suspend T.(U) -> R)?.then(crossinline next: suspend I.(U) -> S): (suspend T.(U) -> S)? = letResult { {
    it(it)
    next(it) } }

context(scope: I)
inline infix fun <T : I, I, U, R, S> (suspend T.(U) -> R)?.thenSuspendedImplicitly(crossinline next: suspend (I, U) -> S): (suspend T.(U) -> S)? = letResult { {
    it(it)
    next(scope, it) } }

inline infix fun <I, T : I, U, R, S> (suspend T.(U) -> R)?.after(crossinline prev: suspend I.(U) -> S): (suspend T.(U) -> R)? = letResult { {
    prev(it)
    it(it) } }

context(scope: I)
inline infix fun <T : I, I, U, R, S> (suspend T.(U) -> R)?.afterSuspendedImplicitly(crossinline prev: suspend (I, U) -> S): (suspend T.(U) -> R)? = letResult { {
    prev(scope, it)
    it(it) } }

inline infix fun <I, T : I, U, R, S> (suspend T.(U) -> R)?.thru(crossinline pass: suspend I.(R) -> S): (suspend T.(U) -> S)? = letResult { {
    pass(it(it)) } }

context(scope: I)
inline infix fun <T : I, I, U, R, S> (suspend T.(U) -> R)?.thruSuspendedImplicitly(crossinline pass: suspend (I, R) -> S): (suspend T.(U) -> S)? = letResult { {
    pass(scope, it(it)) } }

inline fun <I, T : I, U, R> (suspend T.(U) -> R)?.given(crossinline predicate: Predicate, crossinline fallback: suspend I.(U) -> R): (suspend T.(U) -> R)? = letResult { {
    if (predicate()) it(it) else fallback(it) } }

context(scope: I)
inline fun <T : I, I, U, R> (suspend T.(U) -> R)?.givenSuspendedImplicitly(crossinline predicate: Predicate, crossinline fallback: suspend (I, U) -> R): (suspend T.(U) -> R)? =
    given(predicate) { fallback(scope, it) }

inline fun <I, T : I, U, R> (suspend T.(U) -> R)?.unless(noinline predicate: Predicate, crossinline fallback: suspend I.(U) -> R): (suspend T.(U) -> R)? =
    given(predicate::isFalse, fallback)

context(scope: I)
inline fun <T : I, I, U, R> (suspend T.(U) -> R)?.unlessSuspendedImplicitly(noinline predicate: Predicate, crossinline fallback: suspend (I, U) -> R): (suspend T.(U) -> R)? =
    unless(predicate) { fallback(scope, it) }

inline infix fun <I, T : I, U, V, R, S> (suspend T.(U, V) -> R)?.then(crossinline next: suspend I.(U, V) -> S): (suspend T.(U, V) -> S)? = letResult { { u, v ->
    it(u, v)
    next(u, v) } }

context(scope: I)
inline infix fun <T : I, I, U, V, R, S> (suspend T.(U, V) -> R)?.thenSuspendedImplicitly(crossinline next: suspend (I, U, V) -> S): (suspend T.(U, V) -> S)? = letResult { { u, v ->
    it(u, v)
    next(scope, u, v) } }

inline infix fun <I, T : I, U, V, R, S> (suspend T.(U, V) -> R)?.after(crossinline prev: suspend I.(U, V) -> S): (suspend T.(U, V) -> R)? = letResult { { u, v ->
    prev(u, v)
    it(u, v) } }

context(scope: I)
inline infix fun <T : I, I, U, V, R, S> (suspend T.(U, V) -> R)?.afterSuspendedImplicitly(crossinline prev: suspend (I, U, V) -> S): (suspend T.(U, V) -> R)? = letResult { { u, v ->
    prev(scope, u, v)
    it(u, v) } }

inline infix fun <I, T : I, U, V, R, S> (suspend T.(U, V) -> R)?.thru(crossinline pass: suspend I.(R) -> S): (suspend T.(U, V) -> S)? = letResult { { u, v ->
    pass(it(u, v)) } }

context(scope: I)
inline infix fun <T : I, I, U, V, R, S> (suspend T.(U, V) -> R)?.thruSuspendedImplicitly(crossinline pass: suspend (I, R) -> S): (suspend T.(U, V) -> S)? = letResult { { u, v ->
    pass(scope, it(u, v)) } }

inline fun <I, T : I, U, V, R> (suspend T.(U, V) -> R)?.given(crossinline predicate: Predicate, crossinline fallback: suspend I.(U, V) -> R): (suspend T.(U, V) -> R)? = letResult { { u, v ->
    if (predicate()) it(u, v) else fallback(u, v) } }

context(scope: I)
inline fun <T : I, I, U, V, R> (suspend T.(U, V) -> R)?.givenSuspendedImplicitly(crossinline predicate: Predicate, crossinline fallback: suspend (I, U, V) -> R): (suspend T.(U, V) -> R)? =
    given(predicate) { u, v -> fallback(scope, u, v) }

inline fun <I, T : I, U, V, R> (suspend T.(U, V) -> R)?.unless(noinline predicate: Predicate, crossinline fallback: suspend I.(U, V) -> R): (suspend T.(U, V) -> R)? =
    given(predicate::isFalse, fallback)

context(scope: I)
inline fun <T : I, I, U, V, R> (suspend T.(U, V) -> R)?.unlessSuspendedImplicitly(noinline predicate: Predicate, crossinline fallback: suspend (I, U, V) -> R): (suspend T.(U, V) -> R)? =
    unless(predicate) { u, v -> fallback(scope, u, v) }

inline infix fun <I, T : I, U, V, W, R, S> (suspend T.(U, V, W) -> R)?.then(crossinline next: suspend I.(U, V, W) -> S): (suspend T.(U, V, W) -> S)? = letResult { { u, v, w ->
    it(u, v, w)
    next(u, v, w) } }

context(scope: I)
inline infix fun <T : I, I, U, V, W, R, S> (suspend T.(U, V, W) -> R)?.thenSuspendedImplicitly(crossinline next: suspend (I, U, V, W) -> S): (suspend T.(U, V, W) -> S)? = letResult { { u, v, w ->
    it(u, v, w)
    next(scope, u, v, w) } }

inline infix fun <I, T : I, U, V, W, R, S> (suspend T.(U, V, W) -> R)?.after(crossinline prev: suspend I.(U, V, W) -> S): (suspend T.(U, V, W) -> R)? = letResult { { u, v, w ->
    prev(u, v, w)
    it(u, v, w) } }

context(scope: I)
inline infix fun <T : I, I, U, V, W, R, S> (suspend T.(U, V, W) -> R)?.afterSuspendedImplicitly(crossinline prev: suspend (I, U, V, W) -> S): (suspend T.(U, V, W) -> R)? = letResult { { u, v, w ->
    prev(scope, u, v, w)
    it(u, v, w) } }

inline infix fun <I, T : I, U, V, W, R, S> (suspend T.(U, V, W) -> R)?.thru(crossinline pass: suspend I.(R) -> S): (suspend T.(U, V, W) -> S)? = letResult { { u, v, w ->
    pass(it(u, v, w)) } }

context(scope: I)
inline infix fun <T : I, I, U, V, W, R, S> (suspend T.(U, V, W) -> R)?.thruSuspendedImplicitly(crossinline pass: suspend (I, R) -> S): (suspend T.(U, V, W) -> S)? = letResult { { u, v, w ->
    pass(scope, it(u, v, w)) } }

inline fun <I, T : I, U, V, W, R> (suspend T.(U, V, W) ->R)?.given(crossinline predicate: Predicate, crossinline fallback: suspend I.(U, V, W) -> R): (suspend T.(U, V, W) -> R)? = letResult { { u, v, w ->
    if (predicate()) it(u, v, w) else fallback(u, v, w) } }

context(scope: I)
inline fun <T : I, I, U, V, W, R> (suspend T.(U, V, W) ->R)?.givenSuspendedImplicitly(crossinline predicate: Predicate, crossinline fallback: suspend (I, U, V, W) -> R): (suspend T.(U, V, W) -> R)? =
    given(predicate) { u, v, w -> fallback(scope, u, v, w) }

inline fun <I, T : I, U, V, W, R> (suspend T.(U, V, W) ->R)?.unless(noinline predicate: Predicate, crossinline fallback: suspend I.(U, V, W) -> R): (suspend T.(U, V, W) -> R)? =
    given(predicate::isFalse, fallback)

context(scope: I)
inline fun <T : I, I, U, V, W, R> (suspend T.(U, V, W) ->R)?.unlessSuspendedImplicitly(noinline predicate: Predicate, crossinline fallback: suspend (I, U, V, W) -> R): (suspend T.(U, V, W) -> R)? =
    unless(predicate) { u, v, w -> fallback(scope, u, v, w) }

inline infix fun <R, S> (() -> R)?.then(crossinline next: () -> S): (() -> S)? = letResult { {
    it()
    next() } }

inline infix fun <R, S> (() -> R)?.after(crossinline prev: () -> S): (() -> R)? = letResult { {
    prev()
    it() } }

inline infix fun <R, S> (() -> R)?.thru(crossinline pass: (R) -> S): (() -> S)? = letResult { {
    pass(it()) } }

inline fun <R> (() -> R)?.given(crossinline predicate: Predicate, crossinline fallback: () -> R): (() -> R)? = letResult { {
    if (predicate()) it() else fallback() } }

inline fun <R> (() -> R)?.unless(noinline predicate: Predicate, crossinline fallback: () -> R): (() -> R)? =
    given(predicate::isFalse, fallback)

inline infix fun AnyFunction?.given(crossinline predicate: Predicate) =
    given(predicate) {}

@Suppress("NOTHING_TO_INLINE")
inline infix fun AnyFunction?.unless(noinline predicate: Predicate) =
    unless(predicate) {}

inline infix fun <T, R, S> ((T) -> R)?.then(crossinline next: (T) -> S): ((T) -> S)? = letResult { {
    it(it)
    next(it) } }

inline infix fun <T, R, S> ((T) -> R)?.after(crossinline prev: (T) -> S): ((T) -> R)? = letResult { {
    prev(it)
    it(it) } }

inline infix fun <T, R, S> ((T) -> R)?.thru(crossinline pass: (R) -> S): ((T) -> S)? = letResult { {
    pass(it(it)) } }

inline fun <T, R> ((T) -> R)?.given(crossinline predicate: Predicate, crossinline fallback: (T) -> R): ((T) -> R)? = letResult { {
    if (predicate()) it(it) else fallback(it) } }

inline fun <T, R> ((T) -> R)?.unless(noinline predicate: Predicate, crossinline fallback: (T) -> R): ((T) -> R)? =
    given(predicate::isFalse, fallback)

inline infix fun AnyToAnyFunction?.given(crossinline predicate: Predicate) =
    given(predicate) {}

@Suppress("NOTHING_TO_INLINE")
inline infix fun AnyToAnyFunction?.unless(noinline predicate: Predicate) = letResult {
    unless(predicate) {} }

internal fun <R> KCallable<R>.with(vararg args: Any?): () -> R = {
    this@with.call(*args) }

internal fun <R> call(vararg args: Any?): (KCallable<R>) -> R = {
    it.call(*args) }

internal fun <R> KCallable<R>.with(args: KParameterMap): () -> R = {
    this@with.callBy(args) }

internal fun <R> callBy(args: KParameterMap): (KCallable<R>) -> R = {
    it.call(*it.mapToTypedArray(args)) }

internal fun <R> KCallable<R>.mapToTypedArray(args: KParameterMap) =
    parameters.map(args::get).toTypedArray()