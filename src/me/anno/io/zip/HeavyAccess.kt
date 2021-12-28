package me.anno.io.zip

import me.anno.io.files.FileReference
import kotlin.concurrent.thread

object HeavyAccess {



    private val lockedFiles = HashSet<FileReference>()
    private val waitingRequests = HashMap<FileReference, ArrayList<IHeavyAccess<*>>>()

    fun <Stream> access(
        source: FileReference,
        access: IHeavyAccess<Stream>
    ) {
        if (source.length() < 1e5) {
            // simple processing for small files
            val stream = access.openStream(source)
            access.process(stream)
            access.closeStream(source, stream)
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
                    waiting = waitingRequests.remove(source) as List<IHeavyAccess<Stream>>?
                    true
                }
            }
            if (process) {
                val listOfAll = if (waiting == null) listOf(access) else waiting!! + access
                process(source, listOfAll)
            }
        }
    }

    private fun <Stream> process(source: FileReference, listOfAll: List<IHeavyAccess<Stream>>) {

        val first = listOfAll.first()
        val stream = first.openStream(source)
        for(entry in listOfAll){
            entry.process(stream)
        }
        first.closeStream(source, stream)

        // process all instances, which were waiting because of us
        val waiting = synchronized(this) {
            val waiting = waitingRequests.remove(source)
            if (waiting == null || waiting.isEmpty()) {
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
            // new thread, because our original is finished anyways
            thread(name = "HeavyAccess.process($source)") {
                process(source, waiting as List<IHeavyAccess<Stream>>)
            }
        }

    }

}