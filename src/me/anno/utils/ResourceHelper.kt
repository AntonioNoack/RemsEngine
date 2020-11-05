package me.anno.utils

import java.io.FileNotFoundException

object ResourceHelper {
    fun loadResource(name: String) = (
        // needs to be the same package
        ResourceHelper.javaClass.classLoader.getResourceAsStream(name)
            ?: throw FileNotFoundException(name)
    )
}