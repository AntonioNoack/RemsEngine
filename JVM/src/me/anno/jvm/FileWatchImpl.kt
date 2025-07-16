package me.anno.jvm

import me.anno.Build
import me.anno.Engine
import me.anno.engine.Events
import me.anno.io.files.FileFileRef
import me.anno.io.files.FileReference
import me.anno.io.files.FileRootRef
import me.anno.io.files.FileWatch
import me.anno.io.files.InvalidRef
import me.anno.io.files.Reference
import me.anno.utils.Threads
import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchKey
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

object FileWatchImpl {

    private val LOGGER = LogManager.getLogger(FileWatch::class)

    private val watcher = FileSystems.getDefault().newWatchService()

    private val watched = HashMap<String, Pair<WatchKey, ArrayList<FileReference>?>>()

    var neverDisable = true

    fun register() {
        FileWatch.watchDogAddImpl = { addWatchDog(it, it) }
        FileWatch.watchDogRemoveImpl = { removeWatchDog(it, it) }
    }

    private fun addWatchDog(file: FileReference, original: FileReference) {
        if (Build.isShipped) return // no change will occur, as everything will be internal
        val file = file.resolved()
        if (file is FileFileRef && file.isDirectory) {
            synchronized(this) {
                watched.getOrPut(file.absolutePath) {
                    LOGGER.debug("Adding watch dog to {}", file)
                    val path = Paths.get(file.absolutePath)
                    val key = path.register(
                        watcher,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE,
                        StandardWatchEventKinds.ENTRY_MODIFY
                    )
                    Pair(key, if (neverDisable) null else ArrayList())
                }.second?.add(original)
            }
        } else if (file != FileRootRef && file != InvalidRef) {
            addWatchDog(file.getParent(), original)
        }
    }

    private fun removeWatchDog(file: FileReference, original: FileReference) {
        if (Build.isShipped || neverDisable) return // no change will occur, as everything will be internal
        val file = file.resolved()
        if (file is FileFileRef && file.isDirectory) {
            synchronized(this) {
                val fileList = watched[file.absolutePath]
                if (fileList != null) {
                    val (key, list) = fileList
                    list!!
                    list.remove(original)
                    if (list.isEmpty()) {
                        LOGGER.debug("Removed watch dog from {}", file)
                        watched.remove(file.absolutePath)
                        try {
                            key.cancel()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        } else if (file != FileRootRef && file != InvalidRef) {
            removeWatchDog(file.getParent(), original)
        }
    }

    init {
        Threads.runWorkerThread("FileWatch") {
            while (!Engine.shutdown) {
                val key = watcher.poll(10L, TimeUnit.MILLISECONDS)
                if (key != null) {

                    for (event in key.pollEvents()) {

                        val kind = event.kind()
                        if (kind == StandardWatchEventKinds.OVERFLOW) {
                            key.reset()
                            continue
                        }

                        val fileName = event.context() as Path
                        val folder = key.watchable()
                            .toString()
                            .replace('\\', '/')

                        val absolutePath = "$folder/$fileName"
                        if (absolutePath !in FileWatch.ignoredFiles) {
                            Events.addEvent {
                                if (absolutePath !in FileWatch.ignoredFiles) {
                                    Reference.invalidate(absolutePath)
                                }
                            }
                        }

                        LOGGER.debug("{} {}", kind, absolutePath)

                        // they say the directory is no longer valid...
                        // but are all of them that?, so do we really have to quit?
                        /*val isValid = */key.reset()
                        /*if (!isValid){
                            LOGGER.warn("Directory $folder is no longer valid")
                            break
                        }*/
                    }
                }
            }
            synchronized(this) {
                for ((key, _) in watched.values) {
                    key.cancel()
                }
            }
        }
    }
}