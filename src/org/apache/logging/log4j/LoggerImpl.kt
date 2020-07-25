package org.apache.logging.log4j

class LoggerImpl(prefix: String?): Logger {

    fun interleave(msg: String, args: Array<Object>): String {
        val parts = msg.split("{}")
        return parts.take(parts.size-1).mapIndexed { index, s -> "$s${args.getOrNull(index)}" }.joinToString("") + parts.last()
    }

    val suffix = if(prefix == null) "" else ":$prefix"

    fun print(prefix: String, msg: String){
        msg.split('\n').forEach { line ->
            println("[$prefix$suffix] $line")
        }
    }

    override fun info(msg: String) {
        print("INFO", msg)
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
        print("ERR!", msg)
    }

    override fun error(msg: String, obj: Array<Object>) {
        error(interleave(msg, obj))
    }

    override fun error(msg: String, thrown: Throwable) {
        error(msg)
        thrown.printStackTrace()
    }

    override fun severe(msg: String) {
        print("SEVERE", msg)
    }

    override fun severe(msg: String, obj: Array<Object>) {
        error(interleave(msg, obj))
    }

    override fun severe(msg: String, thrown: Throwable) {
        error(msg)
        thrown.printStackTrace()
    }

    override fun fatal(msg: String) {
        print("FATAL", msg)
    }

    override fun fatal(msg: String, obj: Array<Object>) {
        fatal(interleave(msg, obj))
    }

    override fun fatal(msg: String, thrown: Throwable) {
        fatal(msg)
        thrown.printStackTrace()
    }

    override fun warn(msg: String) {
        print("WARN", msg)
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