package me.anno.io.config

import me.anno.io.saveable.Saveable
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.json.generic.JsonFormatter
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.io.utils.StringMap
import me.anno.utils.OS.home
import org.apache.logging.log4j.LogManager
import java.io.IOException

/**
 * Our configuration files are JSON for now.
 * They probably could be something like YAML, too.
 * */
object ConfigBasics {

    private val LOGGER = LogManager.getLogger(ConfigBasics::class)

    var configFolder = home.getChild(".config/RemsEngine")
    var cacheFolder = home.getChild(".cache/RemsEngine")

    fun getConfigFile(localFileName: String): FileReference {
        return configFolder.getChild(localFileName)
    }

    fun save(file: FileReference, data: String): String {
        if (file == InvalidRef) {
            LOGGER.warn("Skipping writing to InvalidRef")
            return data
        }
        val parentFile = file.getParent()
        if (!parentFile.exists) parentFile.tryMkdirs()
        val formatted = JsonFormatter.format(data)
        file.writeText(formatted)
        return data
    }

    fun save(localFileName: String, data: String) = save(getConfigFile(localFileName), data)

    inline fun load(file: FileReference, saveIfMissing: Boolean, getDefault: () -> String): String {
        val value = if (file.exists) {
            try {
                file.readTextSync()
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        } else null
        return if (value == null) {
            val default = getDefault()
            if (saveIfMissing) save(file, default)
            else default
        } else value
    }

    inline fun load(localFileName: String, saveIfMissing: Boolean, getDefault: () -> String) =
        load(getConfigFile(localFileName), saveIfMissing, getDefault)

    fun loadConfig(
        file: FileReference,
        workspace: FileReference,
        defaultValue: StringMap,
        saveIfMissing: Boolean
    ): StringMap {
        Saveable.registerCustomClass(StringMap())
        val read = load(file, saveIfMissing) {
            LOGGER.info("Didn't find $file, using default values")
            JsonStringWriter.toText(defaultValue, workspace)
        }
        val readData = JsonStringReader.read(read, workspace, file.absolutePath, true)
        val map = readData.firstOrNull { it is StringMap } as? StringMap
        return if (map == null) {
            LOGGER.info("Config was corrupted, didn't find a config, in $file, got $readData")
            save(file, JsonStringWriter.toText(defaultValue, workspace))
            defaultValue
        } else map
    }

    fun loadConfig(localFileName: String, workspace: FileReference, defaultValue: StringMap, saveIfMissing: Boolean) =
        loadConfig(getConfigFile(localFileName), workspace, defaultValue, saveIfMissing)
}