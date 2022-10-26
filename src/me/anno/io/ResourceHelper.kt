package me.anno.io

import me.anno.io.Streams.readText
import java.io.FileNotFoundException
import java.io.InputStream

object ResourceHelper {

    fun loadResource(name: String): InputStream {
        // needs to be the same jar
        return ResourceHelper.javaClass.classLoader.getResourceAsStream(name)
            ?: throw FileNotFoundException("res://$name")
    }

    fun loadText(name: String) = loadResource(name).readText()

}