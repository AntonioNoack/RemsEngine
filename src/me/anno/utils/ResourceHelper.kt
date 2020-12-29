package me.anno.utils

import me.anno.utils.Streams.readText
import java.io.FileNotFoundException
import java.io.IOException

object ResourceHelper {

    @Throws(IOException::class)
    fun loadResource(name: String) = (
        // needs to be the same package
        ResourceHelper.javaClass.classLoader.getResourceAsStream(name)
            ?: throw FileNotFoundException(name)
    )

    @Throws(IOException::class)
    fun loadText(name: String) = loadResource(name).readText()

}