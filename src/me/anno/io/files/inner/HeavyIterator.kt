package me.anno.io.files.inner

import me.anno.cache.ThreadPool
import me.anno.io.files.FileReference

object HeavyIterator {

    @JvmStatic
    private val lockedFiles = HashSet<FileReference>()

    @JvmStatic
    private val waitingRequests = HashMap<FileReference, ArrayList<IHeavyIterable<*, *, *>>>()

    @JvmStatic
    fun <Item, Stream : Iterator<Item>, Processable> iterate(
        source: FileReference,
        iterable: IHeavyIterable<Item, Stream, Processable>
    ) {
        if (source.length() < 1e5) {
            // simple processing for small files
            iterable.openStream(source) { stream, err ->
                if (stream != null) while (stream.hasNext()) {
                    val next = stream.next()
                    if (iterable.hasInterest(stream, next)) {
                        iterable.process(stream, next, null, 0, 1)
                    }
                }
                else err?.printStackTrace()
            }
        } else {
            var waiting: List<IHeavyIterable<Item, Stream, Processable>>? = null
            val process = synchronized(this) {
                if (source in lockedFiles) {
                    // register as waiting
                    waitingRequests.getOrPut(source, ::ArrayList)
                        .add(iterable)
                    false
                } else {
                    lockedFiles.add(source)
                    @Suppress("unchecked_cast")
                    waiting = waitingRequests.remove(source) as List<IHeavyIterable<Item, Stream, Processable>>?
                    true
                }
            }
            if (process) {
                val listOfAll = if (waiting == null) listOf(iterable) else waiting!! + iterable
                process(source, listOfAll)
            }
        }
    }

    @JvmStatic
    private fun <Item, Stream : Iterator<Item>, Processable> process(
        source: FileReference, listOfAll: List<IHeavyIterable<Item, Stream, Processable>>
    ) {

        val first = listOfAll.first()

        first.openStream(source) { stream, err ->
            if (stream != null) while (stream.hasNext()) {
                val item = stream.next()
                var interested = 0
                for (iterable in listOfAll) {
                    if (iterable.hasInterest(stream, item)) {
                        interested++
                    }
                }
                if (interested > 0) {
                    var previous: Processable? = null
                    var i = 0
                    for (iterable in listOfAll) {
                        if (iterable.hasInterest(stream, item)) {
                            previous = iterable.process(stream, item, previous, i++, interested)
                        }
                    }
                }
            }
            err?.printStackTrace()
        }

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
            ThreadPool.start("HeavyIterator.process($source)") {
                @Suppress("unchecked_cast")
                process(source, waiting as List<IHeavyIterable<Item, Stream, Processable>>)
            }
        }
    }
}