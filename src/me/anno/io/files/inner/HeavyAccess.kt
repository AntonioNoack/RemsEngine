package me.anno.io.files.inner

import me.anno.io.files.FileReference
import me.anno.utils.Threads
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

object HeavyAccess {

    @JvmStatic
    private val ctr = AtomicInteger()

    @JvmStatic
    private val waitingRequests = ConcurrentHashMap<FileReference, HeavyTask<*>>()

    private class HeavyTask<Stream> {
        val requests = ArrayList<IHeavyAccess<Stream>>()
    }

    @JvmStatic
    fun <Stream> access(
        source: FileReference,
        access: IHeavyAccess<Stream>,
        onError: (Exception) -> Unit
    ) {
        val task0 = waitingRequests.getOrPut(source) { HeavyTask<Stream>() }

        @Suppress("UNCHECKED_CAST")
        val task = task0 as HeavyTask<Stream>
        val needsWorkerThread = synchronized(task) {
            task.requests.add(access)
            task.requests.size == 1
        }
        if (needsWorkerThread) {
            Threads.start("HeavyAccess<$source,#${ctr.incrementAndGet()}>") {
                process(source, task, onError)
            }
        }
    }

    @JvmStatic
    private fun <Stream> process(
        source: FileReference, task: HeavyTask<Stream>,
        onError: (Exception) -> Unit
    ) {
        val first = task.requests.firstOrNull()
        first?.openStream(source) { stream, e ->
            if (stream != null) {
                while (true) {
                    val taskI = synchronized(task) {
                        task.requests.removeLastOrNull()
                    } ?: break // if empty, all work is done -> we're finished
                    taskI.process(stream)
                }
                first.closeStream(source, stream)
            } else if (e != null) {
                onError(e)
            }
        }
    }
}