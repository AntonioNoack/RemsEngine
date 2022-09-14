package me.anno.mesh.assimp.io

import me.anno.io.files.FileReference
import org.apache.logging.log4j.LogManager
import org.lwjgl.assimp.AIFile
import org.lwjgl.assimp.AIFileIO
import org.lwjgl.system.MemoryUtil

object AIFileIOImpl {

    private val LOGGER = LogManager.getLogger(AIFileIOImpl::class)
    var debug = false

    fun create(file: FileReference, root: FileReference): AIFileIO {

        val fileIO = AIFileIO.calloc()
        val byteCache = HashMap<FileReference, ByteArray>()
        val openedStreams = HashMap<Long, IFileIOStream>()

        fileIO.set({ _, fileNamePtr, openModePtr ->

            var fileName = MemoryUtil.memUTF8(fileNamePtr)
            val openMode = MemoryUtil.memUTF8(openModePtr)
            if (openMode != "rb") throw UnsupportedOperationException("Expected rb as mode")

            // LOGGER.info("name/mode: $fileName / $openMode")
            // if (fileName != file.name) throw RuntimeException()

            if (fileName.startsWith("/")) fileName = fileName.substring(1)
            if ('\\' in fileName) fileName = fileName.replace('\\', '/')
            val file1 = if (fileName == file.name) file
            else root.getChild(fileName)

            if (file1.exists) {

                val length = file1.length()

                val callbacks = AIFile.create()
                openedStreams[callbacks.address()] = if (length < 50_000_000) {// huge files are not cached
                    AIFileIOStream2(byteCache.getOrPut(file1) {
                        file1.readBytesSync()
                    })
                } else AIFileIOStream(file1)

                callbacks.set(
                    { aiFile, dstBufferPtr, size, count ->
                        val answer = openedStreams[aiFile]!!.read(dstBufferPtr, size, count)
                        if (debug) LOGGER.debug("read *${aiFile.toString(16)}, *${dstBufferPtr.toString(16)}, $size, $count -> $answer")
                        answer
                    },
                    { _, charArray, size1, size2 ->
                        // write process
                        throw UnsupportedOperationException("Writing is not supported, $charArray, $size1*$size2")
                    },
                    { aiFile ->
                        if (debug) LOGGER.debug("get pos *${aiFile.toString(16)} -> ${openedStreams[aiFile]!!.position}")
                        openedStreams[aiFile]!!.position
                    },
                    { aiFile ->
                        if (debug) LOGGER.debug("get length *${aiFile.toString(16)} -> ${openedStreams[aiFile]!!.length}")
                        openedStreams[aiFile]!!.length
                    },
                    { aiFile, offset, whence ->
                        val answer = openedStreams[aiFile]!!.seek(offset, whence)
                        if (debug) LOGGER.debug("seek *${aiFile.toString(16)}, $offset, $whence -> $answer")
                        answer
                    },
                    {
                        // flush
                        throw UnsupportedOperationException("Flush is not supported")
                    },
                    0L
                )

                if (debug) LOGGER.debug("\nopen $fileName as *${callbacks.address().toString(16)}")
                callbacks.address()

            } else 0L

        }, { _, aiFile ->
            if (debug) LOGGER.debug("close *${aiFile.toString(16)}\n")
            // close the stream
            openedStreams[aiFile]?.close()
            openedStreams.remove(aiFile)
        }, 0L)

        return fileIO

    }

}