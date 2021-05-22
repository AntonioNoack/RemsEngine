package me.anno.utils.process

import me.anno.utils.OS
import org.apache.logging.log4j.LogManager

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
                    // todo test whether this works
                    // args += "nice"
                    // args += "-n"
                    // args += "10" // -20 .. +19, 0 = user_space, < 0 = super, > 0 = lower
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
        LOGGER.info(args.joinToString(" ") {
            if (' ' in it) "\"${it.replace("\"", "\\\"")}\""
            else it
        })
        val builder = ProcessBuilder(args)
        builder.directory(OS.home.file)
        return builder.start()
    }

    companion object {
        private val LOGGER = LogManager.getLogger(BetterProcessBuilder::class)
    }

}