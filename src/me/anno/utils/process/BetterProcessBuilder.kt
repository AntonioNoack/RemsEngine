package me.anno.utils.process

import me.anno.Engine
import me.anno.config.DefaultConfig
import me.anno.utils.OS
import me.anno.utils.maths.Maths.clamp
import org.apache.logging.log4j.LogManager
import java.io.InputStream
import kotlin.concurrent.thread

class BetterProcessBuilder(
    program: String?,
    ownArgumentCount: Int,
    isLowPriority: Boolean
) {

    val args = ArrayList<String>(ownArgumentCount + 1 + (if (isLowPriority) 3 else 0))

    init {
        if (isLowPriority) {
            when {
                OS.isLinux -> {
                    // task: test whether this works
                    // -> seems to work in WSL :)
                    // -20 .. +19, 0 = user_space, < 0 = super, > 0 = lower
                    val niceness = clamp(DefaultConfig["cpu.linux.subprocess-niceness", 10], -20, 19)
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

    fun add(argument: String) {
        args += argument
    }

    fun addAll(arguments: Collection<String>) {
        args += arguments
    }

    fun addAll(arguments: Array<out String>) {
        args += arguments
    }

    fun start(): Process {
        LOGGER.debug(args.joinToString(" ") {
            if (' ' in it) "\"${it.replace("\"", "\\\"")}\""
            else it
        })
        val builder = ProcessBuilder(args)
        builder.directory(OS.home.unsafeFile)
        return builder.start()
    }

    fun startAndPrint(): Process {
        val process = start()
        thread(name = "cmd($args):error") { readLines(process.errorStream, true) }
        thread(name = "cmd($args):input") { readLines(process.inputStream, false) }
        return process
    }

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

    companion object {
        private val LOGGER = LogManager.getLogger(BetterProcessBuilder::class)
    }

}