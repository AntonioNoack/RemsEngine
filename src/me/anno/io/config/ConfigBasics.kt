package me.anno.io.config

import me.anno.io.Saveable
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.json.generic.JsonFormatter
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.io.utils.StringMap
import me.anno.utils.OS.home
import org.apache.logging.log4j.LogManager
import java.io.IOException

object ConfigBasics {

    private val LOGGER = LogManager.getLogger(ConfigBasics::class)

    var configFolder = home.getChild(".config/RemsEngine")
    var cacheFolder = configFolder

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

    @Suppress("unused")
    fun loadJsonArray(
        localFileName: String,
        workspace: FileReference,
        defaultValue: List<ConfigEntry>,
        saveIfMissing: Boolean
    ): List<ConfigEntry> {

        val data = load(localFileName, saveIfMissing) { JsonStringWriter.toText(defaultValue, workspace) }

        val loaded = JsonStringReader.read(data, workspace, localFileName, true)
        val newestEntries = HashMap<String, ConfigEntry>(loaded.size + 10)

        fun addIfNewest(entry: ConfigEntry): Boolean {
            val id = entry.id
            return if (id != null) {
                val old = newestEntries[id]
                if (old != null) {
                    if (entry.version > old.version) {
                        newestEntries[id] = entry
                        true
                    } else false
                } else {
                    newestEntries[id] = entry
                    true
                }
            } else false
        }

        for (entry in loaded) {
            if (entry is ConfigEntry) {
                addIfNewest(entry)
            }
        }

        var wasAugmentedByDefault = false
        for (entry in defaultValue) {
            if (addIfNewest(entry)) {
                wasAugmentedByDefault = true
            }
        }

        val result = newestEntries.values.toList()

        if (wasAugmentedByDefault) {
            save(localFileName, JsonStringWriter.toText(result, workspace))
        }

        return result
    }
}