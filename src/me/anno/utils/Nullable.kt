package me.anno.utils

object Nullable {

    fun <V> tryOrNull(func: () -> V): V? {
        return try {
            func()
        } catch (e: Exception){
            null
        }
    }

    fun <V> tryOrException(func: () -> V): Any {
        return try {
            func()
        } catch (e: Exception){
            e
        } as Any
    }

}