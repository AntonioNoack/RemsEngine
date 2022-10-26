package me.anno.utils

import java.io.OutputStream
import java.io.PrintStream
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import kotlin.math.max

// todo also print stuff to a file for debugging errors on users computers? :)
object Logging {

    private val originalOut = System.out!!
    private val originalErr = System.err!!

    // Web isn't supporting multithreading yet
    val lastConsoleLines: Queue<String> =
        if (OS.isWeb) LinkedList()
        else LinkedBlockingQueue()

    var lastConsoleLineCount = 500
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
                    lines.offer(line)
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

    fun setup() {
        System.setOut(PrintStream(OutputPipe(originalOut)))
        System.setErr(PrintStream(object : OutputPipe(originalErr) {
            override fun processMessage(str: String) = "[ERR] $str"
        }))
    }
}