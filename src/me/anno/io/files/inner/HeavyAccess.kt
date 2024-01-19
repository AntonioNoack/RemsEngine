package me.anno.io.files.inner

import me.anno.io.files.FileReference
import kotlin.concurrent.thread

object HeavyAccess {

    @JvmStatic
    private val lockedFiles = HashSet<FileReference>()

    @JvmStatic
    private val waitingRequests = HashMap<FileReference, ArrayList<IHeavyAccess<*>>>()

    @JvmStatic
    fun <Stream> access(
        source: FileReference,
        access: IHeavyAccess<Stream>,
        onError: (Exception) -> Unit
    ) {
        if (source.length() < 1e5) {
            // simple processing for small files
            access.openStream(source) { stream, e ->
                if (stream != null) {
                    access.process(stream)
                    access.closeStream(source, stream)
                } else onError(e!!)
            }
        } else {
            var waiting: List<IHeavyAccess<Stream>>? = null
            val process = synchronized(this) {
                if (source in lockedFiles) {
                    // register as waiting
                    waitingRequests.getOrPut(source) { ArrayList() }
                        .add(access)
                    false
                } else {
                    lockedFiles.add(source)
                    @Suppress("unchecked_cast")
                    waiting = waitingRequests.remove(source) as List<IHeavyAccess<Stream>>?
                    true
                }
            }
            if (process) {
                val listOfAll = if (waiting == null) listOf(access) else waiting!! + access
                process(source, listOfAll, onError)
            }
        }
    }

    @JvmStatic
    private fun <Stream> process(
        source: FileReference,
        listOfAll: List<IHeavyAccess<Stream>>,
        onError: (Exception) -> Unit
    ) {
        val first = listOfAll.first()
        first.openStream(source) { stream, e ->
            if (stream != null) {
                for (entry in listOfAll) {
                    entry.process(stream)
                }
                first.closeStream(source, stream)

                // process all instances, which were waiting because of us
                val waiting = synchronized(this) {
                    val waiting = waitingRequests.remove(source)
                    if (waiting.isNullOrEmpty()) {
                        // we are done 🥳
                        lockedFiles.remove(source)
                        null
                    } else {
                        // process all missing instances
                        // open new thread?
                        waiting
                    }
                }

                if (waiting != null) {
                    // new thread, because our original is finished anyway
                    thread(name = "HeavyAccess.process($source)") {
                        @Suppress("unchecked_cast")
                        process(source, waiting as List<IHeavyAccess<Stream>>, onError)
                    }
                }
            } else onError(e!!)
        }
    }
}