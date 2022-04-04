package me.anno.io.config

import me.anno.gpu.GFXBase.Companion.projectName
import me.anno.io.ISaveable
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.json.JsonFormatter
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import me.anno.io.utils.StringMap
import me.anno.utils.OS
import org.apache.logging.log4j.LogManager
import java.io.IOException

/**
 * max file size: 2GB, because of Javas internal String and array limitations
 * 2GB for a config file would be just insane
 * */
object ConfigBasics {

    val LOGGER = LogManager.getLogger(ConfigBasics::class)

    val configFolder get() = getReference(OS.home, ".config/$projectName")
    val cacheFolder get() = getReference(OS.home, ".cache/$projectName")

    fun getConfigFile(localFileName: String): FileReference {
        return getReference(configFolder, localFileName)
    }

    @Throws(IOException::class)
    fun save(file: FileReference, data: String): String {
        val parentFile = file.getParent() ?: return data
        if (!parentFile.exists) parentFile.tryMkdirs()
        file.writeText(JsonFormatter.format(data))
        return data
    }

    @Throws(IOException::class)
    fun save(localFileName: String, data: String) = save(getConfigFile(localFileName), data)

    @Throws(IOException::class)
    inline fun load(file: FileReference, saveIfMissing: Boolean, getDefault: () -> String): String {
        val value = if (file.exists) {
            try {
                file.readText()
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
        ISaveable.registerCustomClass(StringMap())
        val read = load(file, saveIfMissing) {
            LOGGER.info("Didn't find $file, using default values")
            TextWriter.toText(defaultValue, workspace)
        }
        val readData = TextReader.read(read, workspace, file.absolutePath, true)
        val map = readData.firstOrNull { it is StringMap } as? StringMap
        return if (map == null) {
            LOGGER.info("Config was corrupted, didn't find a config, in $file, got $readData")
            save(file, TextWriter.toText(defaultValue, workspace))
            defaultValue
        } else map
    }

    fun loadConfig(localFileName: String, workspace: FileReference, defaultValue: StringMap, saveIfMissing: Boolean) =
        loadConfig(getConfigFile(localFileName), workspace, defaultValue, saveIfMissing)

    fun loadJsonArray(
        localFileName: String,
        workspace: FileReference,
        defaultValue: List<ConfigEntry>,
        saveIfMissing: Boolean
    ): List<ConfigEntry> {

        val data = load(localFileName, saveIfMissing) { TextWriter.toText(defaultValue, workspace) }

        val loaded = TextReader.read(data, workspace, localFileName, true)
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
            save(localFileName, TextWriter.toText(result, workspace))
        }

        return result
    }

}