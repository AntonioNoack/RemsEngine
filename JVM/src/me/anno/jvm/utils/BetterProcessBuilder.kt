package me.anno.jvm.utils

import me.anno.Engine
import me.anno.config.DefaultConfig
import me.anno.io.files.FileReference
import me.anno.maths.Maths
import me.anno.utils.OS
import me.anno.utils.Threads
import org.apache.logging.log4j.LogManager
import java.io.File
import java.io.InputStream

class BetterProcessBuilder(
    program: String?,
    ownArgumentCount: Int,
    isLowPriority: Boolean
) {

    constructor(arguments: List<String>) :
            this(arguments[0], arguments.size, false) {
        addAll(arguments.subList(1, arguments.size))
    }

    constructor(
        program: FileReference, ownArgumentCount: Int,
        isLowPriority: Boolean
    ) : this(program.absolutePath, ownArgumentCount, isLowPriority)

    private val args = ArrayList<String>(ownArgumentCount + 1 + (if (isLowPriority) 3 else 0))

    init {
        if (isLowPriority) {
            when {
                OS.isLinux -> {
                    // task: test whether this works
                    // -> seems to work in WSL :)
                    // -20 .. +19, 0 = user_space, < 0 = super, > 0 = lower
                    val niceness = Maths.clamp(DefaultConfig["cpu.linux.subprocess-niceness", 10], -20, 19)
                    if (niceness > 0) {
                        args += "nice"
                        args += "-n"
                        args += niceness.toString()
                    }
                }
                OS.isWindows -> {
                    // todo find a working way on windows...
                    // cannot run my spellchecker, why ever...
                    // args += "start"
                    // args += "\"$program\"" // process name
                    // args += "/low"
                }
            }
        }
        if (program != null) {
            args += program
        }
    }

    operator fun plusAssign(argument: String) {
        args += argument
    }

    operator fun plusAssign(arguments: Collection<String>) {
        args += arguments
    }

    fun add(argument: String): BetterProcessBuilder {
        args += argument
        return this
    }

    fun addAll(arguments: Collection<String>): BetterProcessBuilder {
        args += arguments
        return this
    }

    fun start(): Process {
        LOGGER.debug(args.joinToString(" ") {
            if (' ' in it) "\"${it.replace("\"", "\\\"")}\""
            else it
        })
        val builder = ProcessBuilder(args)
        builder.directory(File(OS.home.absolutePath))
        return builder.start()
    }

    fun startAndPrint(): Process {
        val process = start()
        Threads.runTaskThread("cmd($args):error") { readLines(process.errorStream, true) }
        Threads.runTaskThread("cmd($args):input") { readLines(process.inputStream, false) }
        return process
    }

    companion object {

        fun readLines(input: InputStream, error: Boolean) {
            val reader = input.bufferedReader()
            while (!Engine.shutdown) {
                val line = reader.readLine() ?: break
                if (line.isNotEmpty()) {
                    if (error) LOGGER.warn(line)
                    else LOGGER.info(line)
                }
            }
            reader.close()
        }

        @JvmStatic
        private val LOGGER = LogManager.getLogger(BetterProcessBuilder::class)
    }
}