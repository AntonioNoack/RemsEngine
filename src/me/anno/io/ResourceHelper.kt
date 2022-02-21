package me.anno.io

import me.anno.io.Streams.readText
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

object ResourceHelper {

    @Throws(IOException::class)
    fun loadResource(name: String): InputStream {
        // needs to be the same package
        return ResourceHelper.javaClass.classLoader.getResourceAsStream(name)
            ?: throw FileNotFoundException("res://$name")
    }

    @Throws(IOException::class)
    fun loadText(name: String) = loadResource(name).readText()

}