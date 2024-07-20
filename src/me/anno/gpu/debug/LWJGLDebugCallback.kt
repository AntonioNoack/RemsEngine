package me.anno.gpu.debug

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.OutputStream
import java.io.PrintStream

object LWJGLDebugCallback : PrintStream(object : OutputStream() {
    // parse the message instead
    // [LWJGL] OpenGL debug message
    // ID: 0x1
    // Source: compiler
    // Type: other
    // Severity: notification
    // Message: ...
    private val LOGGER: Logger = LogManager.getLogger("LWJGL")
    private var id: String? = null
    private var source: String? = null
    private var type: String? = null
    private var severity: String? = null
    private var line = StringBuilder()
    override fun write(i: Int) {
        when (i) {
            '\r'.code -> {}
            '\n'.code -> {
                val info = line.toString().trim { it <= ' ' }
                if (!info.startsWith("[LWJGL]")) {
                    val index = info.indexOf(':')
                    if (index > 0) {
                        val key = info.substring(0, index).trim { it <= ' ' }.lowercase()
                        val value = info.substring(index + 1).trim { it <= ' ' }
                        when (key) {
                            "id" -> id = value
                            "source" -> source = value
                            "type" -> type = value
                            "severity" -> severity = value
                            "message" -> {
                                // ignored message, because it spams my logs
                                if ("will use VIDEO memory as the source for buffer object operations" !in value &&
                                    // idk about this one...
                                    "Pixel-path performance warning: Pixel transfer is synchronized with 3D rendering." !in value
                                ) {
                                    var printedMessage = "$value ID: $id Source: $source"
                                    if ("NOTIFICATION" != severity) printedMessage += " Severity: $severity"
                                    when (if (type == null) "" else type!!.lowercase()) {
                                        "error" -> LOGGER.error(printedMessage)
                                        "other" -> LOGGER.info(printedMessage)
                                        else -> {
                                            printedMessage += " Type: $type"
                                            LOGGER.info(printedMessage)
                                        }
                                    }
                                }
                                id = null
                                source = null
                                type = null
                                severity = null
                            }
                        }
                    } else if (info.isNotEmpty()) {
                        // awkward...
                        LOGGER.info(info)
                    }
                } // else idc
                // LOGGER.info(line.toString());
                line = StringBuilder()
            }
            else -> {
                val maxLength = 500 - 3
                val length = line.length
                if (length < maxLength) {
                    line.append(i.toChar())
                } else if (length == maxLength) {
                    line.append("...")
                } // else too many chars, we don't care ;)
            }
        }
    }
})