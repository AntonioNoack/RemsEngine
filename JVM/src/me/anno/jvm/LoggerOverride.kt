package me.anno.jvm

import me.anno.utils.Logging
import java.io.OutputStream
import java.io.PrintStream

object LoggerOverride {

    @JvmStatic
    private val originalOut = System.out!!

    @JvmStatic
    private val originalErr = System.err!!

    @JvmField
    var maxConsoleLineLength = 500

    class OutputPipe(val prefix: String, val output: OutputStream) : OutputStream() {
        val line = StringBuilder(prefix)

        override fun write(b: Int) {
            when {
                b == '\n'.code -> {
                    // only accept non-empty lines?
                    Logging.push(line.toString())
                    line.clear().append(prefix)
                }
                line.length < maxConsoleLineLength -> {
                    // can be uncommented to find which class is calling println() instead of using a logger
                    /*if(line.isEmpty() && b != '['.toInt()){
                        throw RuntimeException("Please use the LogManager.getLogger(YourClass)!")
                    }*/
                    line.append(b.toChar())
                }
                line.length == maxConsoleLineLength -> {
                    line.append("...")
                }
            }
            output.write(b)
        }
    }

    @JvmStatic
    fun setup() {
        System.setOut(PrintStream(OutputPipe("", originalOut)))
        System.setErr(PrintStream(OutputPipe("[ERR] ", originalErr)))
    }
}