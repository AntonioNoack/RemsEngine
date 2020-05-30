package org.apache.logging.log4j

class LoggerImpl: Logger {

    fun interleave(msg: String, args: Array<Object>): String {
        val parts = msg.split("{}")
        return parts.take(parts.size-1).mapIndexed { index, s -> "$s${args.getOrNull(index)}" }.joinToString("") + parts.last()
    }

    override fun info(msg: String) {
        println("[INFO] $msg")
    }

    override fun info(msg: String, obj: Array<Object>) {
        info(interleave(msg, obj))
    }

    override fun info(marker: Marker, msg: String) {
        info(msg)
    }

    override fun info(msg: String, thrown: Throwable) {
        info(msg)
        thrown.printStackTrace()
    }

    override fun error(msg: String) {
        println("[ERR] $msg")
    }

    override fun error(msg: String, obj: Array<Object>) {
        error(interleave(msg, obj))
    }

    override fun error(msg: String, thrown: Throwable) {
        error(msg)
        thrown.printStackTrace()
    }

    override fun fatal(msg: String) {
        println("[FATAL] $msg")
    }

    override fun fatal(msg: String, obj: Array<Object>) {
        fatal(interleave(msg, obj))
    }

    override fun fatal(msg: String, thrown: Throwable) {
        fatal(msg)
        thrown.printStackTrace()
    }

    override fun warn(msg: String) {
        println("[WARN] $msg")
    }

    override fun warn(msg: String, obj: Array<Object>) {
        warn(interleave(msg, obj))
    }

    override fun warn(msg: String, thrown: Throwable) {
        warn(msg)
        thrown.printStackTrace()
    }

    override fun warn(marker: Marker, msg: String, obj: Array<Object>) = warn(msg, obj)


}