package me.anno.utils

object Warning {
    /**
     * gets rid of the "unused variable" warning
     * */
    @JvmStatic
    fun unused(x: Any?): Any? {
        return x
    }
}