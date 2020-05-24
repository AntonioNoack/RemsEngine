package me.anno.io.config

import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import me.anno.io.utils.StringMap
import java.io.File
import java.nio.charset.Charset

object ConfigBasics {

    val configFolder = File(System.getProperty("user.home")+"/.config/RemsStudio")

    val beautify = true

    fun getConfigFile(localFileName: String): File {
        return File(configFolder, localFileName)
    }

    fun save(file: File, data: String){
        val parentFile = file.parentFile
        if(!parentFile.exists()) parentFile.mkdirs()
        file.writeText(data)
    }

    fun save(localFileName: String, data: String) = save(getConfigFile(localFileName), data)

    fun load(localFileName: String, saveIfMissing: Boolean, getDefault: () -> String): String {
        val file = getConfigFile(localFileName)
        return if(file.exists()){
            file.readText(Charset.forName("UTF-8"))
        } else {
            val default = getDefault()
            if(saveIfMissing) save(localFileName, default)
            default
        }
    }

    fun loadConfig(localFileName: String, defaultValue: StringMap, saveIfMissing: Boolean): StringMap {
        val read = load(localFileName, saveIfMissing){
            println("didn't find ${getConfigFile(localFileName)}, using default values")
            TextWriter.toText(defaultValue, beautify)
        }
        val readData = TextReader.fromText(read)
        val map = readData.firstOrNull { it is StringMap } as? StringMap
        return if(map == null){
            println("config was corrupted, didn't find a config, in ${getConfigFile(localFileName)}, got $readData")
            save(localFileName, TextWriter.toText(defaultValue, beautify))
            defaultValue
        } else map
    }

    fun loadJsonArray(localFileName: String, defaultValue: List<ConfigEntry>, saveIfMissing: Boolean): List<ConfigEntry> {

        val data = load(localFileName, saveIfMissing){ TextWriter.toText(defaultValue, beautify) }

        val loaded = TextReader.fromText(data)
        val newestEntries = HashMap<String, ConfigEntry>(loaded.size + 10)

        fun addIfNewest(entry: ConfigEntry): Boolean {
            val id = entry.id
            return if(id != null){
                val old = newestEntries[id]
                if(old != null){
                    if(entry.version > old.version){
                        newestEntries[id] = entry
                        true
                    } else false
                } else {
                    newestEntries[id] = entry
                    true
                }
            } else false
        }

        loaded.forEach {  entry ->
            (entry as? ConfigEntry)?.apply {
                addIfNewest(this)
            }
        }

        var wasAugmentedByDefault = false
        defaultValue.forEach { entry ->
            if(addIfNewest(entry)){
                wasAugmentedByDefault = true
            }
        }

        val result = newestEntries.values.toList()

        if(wasAugmentedByDefault){
            save(localFileName, TextWriter.toText(result, beautify))
        }

        return result
    }

}