package me.anno.utils.io

import me.anno.utils.io.Streams.readText
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