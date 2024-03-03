package me.anno.utils

import me.anno.utils.Color.hex32
import java.io.OutputStream
import java.io.PrintStream
import java.util.Queue
import java.util.concurrent.LinkedBlockingQueue
import kotlin.math.max

object Logging {

    @JvmStatic
    private val originalOut = System.out!!

    @JvmStatic
    private val originalErr = System.err!!

    // Web isn't supporting multithreading yet
    @JvmField
    val lastConsoleLines: Queue<String> = LinkedBlockingQueue()

    @JvmField
    var lastConsoleLineCount = 500

    @JvmField
    var maxConsoleLineLength = 500

    open class OutputPipe(val output: OutputStream) : OutputStream() {
        var line = ""
        open fun processMessage(str: String) = str
        override fun write(b: Int) {
            when {
                b == '\n'.code -> {
                    // only accept non-empty lines?
                    val lines = lastConsoleLines
                    if (lines.size > max(0, lastConsoleLineCount)) {
                        lines.poll()
                    }
                    line = processMessage(line)
                    lines.add(line)
                    line = ""
                }
                line.length < maxConsoleLineLength -> {
                    // enable for
                    /*if(line.isEmpty() && b != '['.toInt()){
                        throw RuntimeException("Please use the LogManager.getLogger(YourClass)!")
                    }*/
                    line += b.toChar()
                }
                line.length == maxConsoleLineLength -> {
                    line += "..."
                }
            }
            output.write(b)
        }
    }

    @JvmStatic
    fun setup() {
        System.setOut(PrintStream(OutputPipe(originalOut)))
        System.setErr(PrintStream(object : OutputPipe(originalErr) {
            override fun processMessage(str: String) = "[ERR] $str"
        }))
    }

    /**
     * returns a short random string with letters and numbers,
     * such that most instances have different codes, and it won't ever change over the lifetime of an instance
     * */
    @JvmStatic
    fun hash32(instance: Any?): String {
        return hex32(hash32raw(instance))
    }

    /**
     * returns a random i32,
     * such that most instances have different codes, and it won't ever change over the lifetime of an instance
     * */
    @JvmStatic
    fun hash32raw(instance: Any?): Int {
        return System.identityHashCode(instance)
    }
}