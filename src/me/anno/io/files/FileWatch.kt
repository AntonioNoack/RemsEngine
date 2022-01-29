package me.anno.io.files

import me.anno.Build
import me.anno.Engine
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.utils.OS.desktop
import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds.*
import java.nio.file.WatchKey
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

// file change watcher
// todo why are meshes disappearing, but not re-appearing?
// todo also watch mesh files, e.g. for parallel tinkering e.g. in Blender with meshes :)
// todo if we can work together with Blender nicely, we can skip a lot of own mesh generation :)
// https://docs.oracle.com/javase/tutorial/essential/io/notification.html
// http://jnotify.sourceforge.net/

object FileWatch {

    /**
     * will be called, when e.g. a Mesh, loaded from a file, is created,
     * because updates of that file are of interest to us
     * */
    fun addWatchDog(file: FileReference) = addWatchDog(file, file)

    /**
     * will be called, when e.g. a Mesh, loaded from file, is freed
     * */
    fun removeWatchDog(file: FileReference) = removeWatchDog(file, file)

    private val LOGGER = LogManager.getLogger(FileWatch::class)

    private val watcher = FileSystems.getDefault().newWatchService()

    private val watched = HashMap<String, Pair<WatchKey, ArrayList<FileReference>?>>()

    private val neverDisable = true

    private fun addWatchDog(file: FileReference, original: FileReference) {
        if (Build.isShipped) return // no change will occur, as everything will be internal
        if (file is FileFileRef && file.isDirectory) {
            synchronized(this) {
                watched.getOrPut(file.absolutePath) {
                    LOGGER.debug("Adding watch dog to $file")
                    val path = Paths.get(file.absolutePath)
                    val key = path.register(
                        watcher,
                        ENTRY_CREATE,
                        ENTRY_DELETE,
                        ENTRY_MODIFY
                    )
                    Pair(key, if (neverDisable) null else ArrayList())
                }.second?.add(original)
            }
        } else addWatchDog(file.getParent() ?: return, original)
    }

    private fun removeWatchDog(file: FileReference, original: FileReference) {
        if (Build.isShipped || neverDisable) return // no change will occur, as everything will be internal
        if (file is FileFileRef && file.isDirectory) {
            synchronized(this) {
                val fileList = watched[file.absolutePath]
                if (fileList != null) {
                    val (key, list) = fileList
                    list!!
                    list.remove(original)
                    if (list.isEmpty()) {
                        LOGGER.debug("Removed watch dog from $file")
                        watched.remove(file.absolutePath)
                        try {
                            key.cancel()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        } else removeWatchDog(file.getParent() ?: return, original)
    }

    init {
        thread(name = "FileWatch") {
            while (!Engine.shutdown) {
                val key = watcher.poll(10L, TimeUnit.MILLISECONDS)
                if (key != null) {

                    for (event in key.pollEvents()) {

                        val kind = event.kind()
                        if (kind == OVERFLOW) continue

                        val fileName = event.context() as Path
                        val folder = key.watchable()
                            .toString()
                            .replace('\\', '/')
                        val absolutePath = "$folder/$fileName"
                        FileReference.invalidate(absolutePath)
                        LOGGER.debug("$kind $absolutePath")

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
            synchronized(watched) {
                for ((key, _) in watched.values) {
                    key.cancel()
                }
            }
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val tmpDir = getReference(desktop, "tmp")
        tmpDir.mkdirs()
        val tmpFile = getReference(tmpDir, "x.txt")
        addWatchDog(tmpFile)
        Thread.sleep(100)
        tmpFile.writeText("") // must be registered
        Thread.sleep(100)
        tmpFile.writeText("hey") // must be registered
        Thread.sleep(100)
        tmpFile.writeBytes(byteArrayOf(1, 2, 3))
        Thread.sleep(100)
        removeWatchDog(tmpFile)
        Thread.sleep(100)
        tmpFile.delete() // should not be registered
        Thread.sleep(100)
        Engine.requestShutdown()
    }

}