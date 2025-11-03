@file:JvmName(JVM_CLASS_NAME)
@file:JvmMultifileClass

package iso.consolator

sealed interface ProcessScope

internal fun <I : ProcessScope> I.windDown() {
    if (this is HandlerScope) windDown()
}

private fun HandlerScope.windDown() {
    Clock.apply {
    Process.setThreadPriority(threadId, Process.THREAD_PRIORITY_DEFAULT) } }

private fun newThread(group: ThreadGroup, name: String, priority: Int, target: Runnable) = Thread(group, target, name).also { it.priority = priority }
private fun newThread(name: String, priority: Int, target: Runnable) = Thread(target, name).also { it.priority = priority }
private fun newThread(priority: Int, target: Runnable) = Thread(target).also { it.priority = priority }

internal val onMainThread get() = currentThread.isMainThread

val currentThread
    get() = Thread.currentThread()

val Thread.isMainThread
    get() = isObject(main_thread)

internal val main_thread = currentThread

internal typealias ExceptionHandler = Thread.UncaughtExceptionHandler
internal typealias Process = android.os.Process