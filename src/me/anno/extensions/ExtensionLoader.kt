package me.anno.extensions

import me.anno.config.DefaultConfig
import me.anno.extensions.mods.Mod
import me.anno.extensions.mods.ModManager
import me.anno.extensions.plugins.Plugin
import me.anno.extensions.plugins.PluginManager
import me.anno.io.config.ConfigBasics.configFolder
import me.anno.studio.StudioBase
import me.anno.utils.hpc.HeavyProcessing.processStage
import org.apache.logging.log4j.LogManager
import java.io.BufferedReader
import java.io.File
import java.net.URLClassLoader
import java.util.zip.ZipInputStream
import kotlin.concurrent.thread

object ExtensionLoader {

    lateinit var pluginsFolder: File
    lateinit var modsFolder: File

    val managers = listOf(
        ModManager, PluginManager
    )

    fun unload() {
        // plugins may depend on mods -> first disable them
        PluginManager.disable()
        ModManager.disable()
    }

    fun getInfos(): List<ExtensionInfo> {
        val extInfos0 = ArrayList<ExtensionInfo>()
        val threads = ArrayList<Thread>()
        fun add(folder: File?) {
            for (it in folder?.listFiles() ?: emptyArray()) {
                if (!it.isDirectory) {
                    val name = it.name
                    if (!name.startsWith(".") && name.endsWith(".jar")) {
                        threads += thread {
                            val info = loadInfo(it)
                            // (check if compatible???)
                            if (info != null && checkExtensionRequirements(info)) {
                                synchronized(extInfos0) {
                                    extInfos0 += info
                                }
                            }
                        }
                    }
                }
            }
        }
        add(pluginsFolder)
        add(modsFolder)
        threads.forEach { it.join() }
        return extInfos0
    }

    fun checkExtensionRequirements(info: ExtensionInfo): Boolean {
        val instance = StudioBase.instance
        if(instance.versionNumber in info.minVersion until info.maxVersion){
            // check if the mod was not disabled
            val isDisabled = DefaultConfig["extensions.isDisabled.${info.uuid}",false]
            if(isDisabled){
                LOGGER.info("Ignored extension \"${info.name}\", because it was disabled")
            } else return true
        } else {
            LOGGER.warn("Extension \"${info.name}\" is incompatible " +
                    "with ${instance.title} version ${instance.versionName}!, " +
                    "${info.minVersion} <= ${instance.versionNumber} < ${info.maxVersion}")
        }
        return false
    }

    private fun warnOfMissingDependencies(extInfos: Collection<ExtensionInfo>, extInfos0: Collection<ExtensionInfo>) {
        if (extInfos.size != extInfos0.size) {
            val ids = extInfos.map { it.uuid }.toHashSet()
            extInfos0.filter { it !in extInfos }.forEach { ex ->
                LOGGER.warn("Discarded extension ${ex.name}, because of missing dependencies ${ex.dependencies.filter { it !in ids }}")
            }
        }
    }

    fun loadExtensions(extInfos: Collection<ExtensionInfo>): List<Extension> {
        val extensions = ArrayList<Extension>()
        processStage(extInfos.toList(), true) { ex ->
            val ext = load(ex)
            if (ext != null) {
                synchronized(extensions) {
                    extensions += ext
                }
            } else LOGGER.warn("Could not load ${ex.name}!")
        }
        return extensions
    }

    fun load() {

        unload()

        pluginsFolder = File(configFolder, "plugins")
        modsFolder = File(configFolder, "mods")

        fun tryCreate(file: File){
            try {
                file.mkdirs()
            } catch (e: Exception){
                LOGGER.warn("Failed to create $file")
            }
        }

        tryCreate(modsFolder)
        tryCreate(pluginsFolder)

        val extInfos0 = getInfos()
        val extInfos = checkDependencies(extInfos0)
        warnOfMissingDependencies(extInfos, extInfos)

        // load all extensions
        val extensions = loadExtensions(extInfos)
        val mods = extensions.filterIsInstance<Mod>()
        val plugins = extensions.filterIsInstance<Plugin>()

        // init all mods and plugins...
        ModManager.enable(mods)

        // first mods, then plugins
        PluginManager.enable(plugins)

    }

    fun reloadPlugins() {
        PluginManager.disable()
        val extInfos0 = getInfos()
        val extInfos = checkDependencies(extInfos0)
            .filter { it.isPluginNotMod }
        val loaded = loadExtensions(extInfos)
        val plugins = loaded.filterIsInstance<Plugin>()
        PluginManager.enable(plugins)
    }

    fun load(ex: ExtensionInfo): Extension? {
        // create the main extension instance
        // load the classes
        val child = URLClassLoader(arrayOf(ex.file.toURI().toURL()), javaClass.classLoader)
        val classToLoad = Class.forName(ex.mainClass, true, child)
        // call with arguments??..., e.g. config or StudioBase or sth...
        val ext = classToLoad.newInstance() as? Extension
        ext?.setInfo(ex)
        ext?.isRunning = true
        return ext
    }

    /**
     * removes all extensions, which have missing dependencies
     * */
    fun checkDependencies(extensions: Collection<ExtensionInfo>): HashSet<ExtensionInfo> {
        val remaining = HashSet(extensions)
        val remainingUUIDs = HashSet(extensions.map { it.uuid })
        val toRemove = ArrayList<ExtensionInfo>()
        while (true) {
            for (extension in remaining) {
                if (!remainingUUIDs.containsAll(extension.dependencies)) {
                    toRemove += extension
                }
            }
            if (toRemove.isEmpty()) {
                break
            } else {
                remaining.removeAll(toRemove)
                remainingUUIDs.removeAll(toRemove.map { it.uuid })
                toRemove.clear()
            }
        }
        return remaining
    }

    fun loadInfo(file: File): ExtensionInfo? {
        ZipInputStream(file.inputStream().buffered()).use { zis ->
            while (true) {
                val entry = zis.nextEntry ?: break
                if (entry.name == "extension.info") {
                    val reader = BufferedReader(zis.reader())
                    var name = ""
                    var version = ""
                    var description = ""
                    var authors = ""
                    var mainClass = ""
                    var isPluginNotMod = true
                    var dependencies = ""
                    var uuid = ""
                    var minVersion = 0
                    var maxVersion = Integer.MAX_VALUE
                    while (true) {
                        val line = reader.readLine() ?: break
                        val index = line.indexOf(':')
                        if (index > 0) {
                            val key = line.substring(0, index).trim()
                            val value = line.substring(index + 1).trim()
                            when (key.toLowerCase()) {
                                "plugin-name", "pluginname", "name" -> {
                                    name = value
                                    isPluginNotMod = true
                                }
                                "plugin-class", "pluginclass" -> {
                                    mainClass = value
                                    isPluginNotMod = true
                                }
                                "mod-class", "modclass" -> {
                                    mainClass = value
                                    isPluginNotMod = false
                                }
                                "mainclass", "main-class" -> {
                                    mainClass = value
                                }
                                "mod-name", "modname" -> {
                                    name = value
                                    isPluginNotMod = false
                                }
                                "plugin-version", "mod-version", "pluginversion", "modversion" -> version = value
                                "desc", "description", "mod-description", "moddescription",
                                "plugin-description", "plugindescription" -> description = value
                                "plugin-author", "plugin-authors",
                                "mod-author", "mod-authors",
                                "author", "authors" -> authors = value
                                "moddependencies", "mod-dependencies",
                                "plugindependencies", "plugin-dependencies",
                                "dependencies" -> dependencies += value
                                "plugin-uuid", "mod-uuid", "plugin-id", "mod-id", "uuid" -> uuid = value
                                "minversion", "min-version" -> minVersion = value.toIntOrNull() ?: minVersion
                                "maxversion", "max-version" -> maxVersion = value.toIntOrNull() ?: maxVersion
                            }
                        }
                    }
                    uuid = uuid.trim()
                    if (uuid.isEmpty()) uuid = name.trim()
                    uuid = uuid.toLowerCase()
                    if (name.isNotEmpty()) {
                        val dependencyList =
                            dependencies.toLowerCase().split(',')
                                .map { it.trim() }.filter { it.isNotEmpty() }
                        return ExtensionInfo(
                            uuid, file,
                            name, description, version, authors,
                            minVersion, maxVersion,
                            mainClass, isPluginNotMod, dependencyList
                        )
                    }
                }
            }
        }
        return null
    }

    private val LOGGER = LogManager.getLogger(ExtensionLoader::class)

}