@file:JvmName(JVM_CLASS_NAME)
@file:JvmMultifileClass

package iso.consolator

import android.os.Message
import kotlin.reflect.*
import iso.consolator.annotation.TagType

internal fun message(callback: Runnable): Message = TODO()

internal fun message(what: Int): Message = TODO()

internal fun Message.detach(): Message? = null

private fun Message.close() {}

internal fun FunctionSet.saveMessage(self: AnyKCallable, tag: TagType) =
    save(self, tag, Item.Type.Message)

internal fun Message.attachNextConjunctionToMessage(target: Message): Message = TODO()

internal fun Message.attachPrevConjunctionToMessage(target: Message): Message = TODO()

internal fun Message.attachNextConjunctionToMessage(target: Runnable): Message = TODO()

internal fun Message.attachPrevConjunctionToMessage(target: Runnable): Message = TODO()

internal fun Message.attachConclusionToMessage(target: Runnable): Message = TODO()

internal fun Message.attachErrorConclusionToMessage(target: Runnable): Message = TODO()

internal fun Message.attachTimeoutConclusionToMessage(target: Runnable): Message = TODO()

internal fun Message.attachTruePredictionToMessage(predicate: MessagePredicate): Message = TODO()

internal fun Message.attachFalsePredictionToMessage(predicate: MessagePredicate): Message = TODO()

internal fun Message.attachConclusionToMessage(target: Message): Message = TODO()

internal fun Message.attachErrorConclusionToMessage(target: Message): Message = TODO()

internal fun Message.attachTimeoutConclusionToMessage(target: Message): Message = TODO()

private fun Message.lastMarkedMessage(): Message = TODO()

private fun Message.asCoroutine() = callback.asCoroutine()

private fun Message.asStep() = callback.asStep()

private fun Message.asRunnable() = callback

internal fun Any?.asMessage() = asType<Message>()

private typealias MessagePointer = () -> Message
private typealias MessageFunction = (Message) -> Any?
internal typealias MessagePredicate = (Message) -> PredicateIdentityType

private typealias MessageKFunction = KFunction<Message>