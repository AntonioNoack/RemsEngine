package me.anno.extensions

import me.anno.config.DefaultConfig
import me.anno.extensions.mods.Mod
import me.anno.extensions.mods.ModManager
import me.anno.extensions.plugins.Plugin
import me.anno.extensions.plugins.PluginManager
import me.anno.io.config.ConfigBasics.configFolder
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.InvalidRef
import me.anno.studio.StudioBase
import me.anno.utils.hpc.HeavyProcessing.processStage
import me.anno.utils.hpc.Threads.threadWithName
import org.apache.logging.log4j.LogManager
import java.io.InputStream
import java.net.URLClassLoader
import java.util.zip.ZipInputStream

object ExtensionLoader {

    // todo do linux soft links work in mods/plugins folder?

    lateinit var pluginsFolder: FileReference
    lateinit var modsFolder: FileReference

    val managers = listOf(
        ModManager, PluginManager
    )

    val internally = ArrayList<ExtensionInfo>()

    fun load() {

        unload()

        pluginsFolder = getReference(configFolder, "plugins")
        modsFolder = getReference(configFolder, "mods")

        modsFolder.tryMkdirs()
        pluginsFolder.tryMkdirs()

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

    fun unload() {
        // plugins may depend on mods -> first disable them
        PluginManager.disable()
        ModManager.disable()
    }

    private fun addAllFromFolder(
        folder: FileReference,
        threads: MutableList<Thread>,
        extInfos0: MutableList<ExtensionInfo>,
        maxDepth: Int = 5
    ) {
        if (!folder.exists) return
        for (it in folder.listChildren() ?: return) {
            if (!it.name.startsWith(".")) {
                if (it.isDirectory) {
                    if (maxDepth > 0) {
                        addAllFromFolder(it, threads, extInfos0, maxDepth - 1)
                    } else LOGGER.warn("Ignored $it, because it's too deep")
                } else {
                    val windowsLink = it.windowsLnk.value
                    if (windowsLink != null) {
                        if (maxDepth > 0) {
                            addAllFromFolder(getReference(windowsLink.absolutePath), threads, extInfos0, maxDepth - 1)
                        } else LOGGER.warn("Ignored $it by $it, because it's too deep")
                    } else {
                        val name = it.name
                        if (!name.startsWith(".") && it.lcExtension == "jar") {
                            threads += threadWithName("ExtensionLoader::getInfos($it)") {
                                val info = loadInfoFromZip(it)
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
        }
    }

    private fun getInfos(): List<ExtensionInfo> {
        val result = ArrayList<ExtensionInfo>()
        val threads = ArrayList<Thread>()
        addAllFromFolder(pluginsFolder, threads, result)
        addAllFromFolder(modsFolder, threads, result)
        threads.forEach { it.join() }
        for (internal in internally) {
            result.removeIf { it.uuid == internal.uuid }
            result.add(internal)
        }
        return result
    }

    private fun checkExtensionRequirements(info: ExtensionInfo): Boolean {
        val instance = StudioBase.instance
        if (instance.versionNumber in info.minVersion until info.maxVersion) {
            // check if the mod was not disabled
            val isDisabled = DefaultConfig["extensions.isDisabled.${info.uuid}", false]
            if (isDisabled) {
                LOGGER.info("Ignored extension \"${info.name}\", because it was disabled")
            } else return true
        } else {
            LOGGER.warn(
                "Extension \"${info.name}\" is incompatible " +
                        "with ${instance.title} version ${instance.versionName}!, " +
                        "${info.minVersion} <= ${instance.versionNumber} < ${info.maxVersion}"
            )
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

    private fun loadExtensions(extInfos: Collection<ExtensionInfo>): List<Extension> {
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
        try {
            // create the main extension instance
            // load the classes
            val exFile = ex.file
            val urlClassLoader =
                if (exFile.exists) URLClassLoader(arrayOf(exFile.toUri().toURL()), javaClass.classLoader)
                else ClassLoader.getSystemClassLoader()
            Thread.currentThread().contextClassLoader = urlClassLoader
            val classToLoad = Class.forName(ex.mainClass, true, urlClassLoader)
            // call with arguments??..., e.g. config or StudioBase or sth...
            val ext = classToLoad.newInstance() as? Extension
            ext?.setInfo(ex)
            ext?.isRunning = true
            return ext
        } catch (e: Exception) {
            LOGGER.error("Error while loading ${ex.file}", e)
        }
        return null
    }

    /**
     * removes all extensions, which have missing dependencies
     * */
    private fun checkDependencies(extensions: Collection<ExtensionInfo>): HashSet<ExtensionInfo> {
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

    fun loadInfoFromZip(file: FileReference): ExtensionInfo? {
        LOGGER.info("Loading info about $file")
        ZipInputStream(file.inputStream()).use { zis ->
            while (true) {
                val entry = zis.nextEntry ?: break
                if (entry.name == "extension.info") {
                    return loadInfoFromTxt(file, zis)
                }
            }
        }
        return null
    }

    fun loadMainInfo() {
        val extensionSource = getReference("res://extension.info")
        val info = loadInfoFromTxt(InvalidRef, extensionSource)!!
        internally.add(info)
    }

    fun loadInfoFromTxt(modFile: FileReference, infoFile: FileReference = modFile): ExtensionInfo? {
        return infoFile.inputStream().use { loadInfoFromTxt(modFile, it) }
    }

    fun loadInfoFromTxt(file: FileReference, input: InputStream): ExtensionInfo? {
        val reader = input.bufferedReader()
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
                when (key.lowercase()) {
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
        uuid = uuid.lowercase()
        if (name.isNotEmpty()) {
            val dependencyList =
                dependencies.lowercase().split(',')
                    .map { it.trim() }.filter { it.isNotEmpty() }
            return ExtensionInfo(
                uuid, file,
                name, description, version, authors,
                minVersion, maxVersion,
                mainClass, isPluginNotMod, dependencyList
            )
        }
        return null
    }

    private val LOGGER = LogManager.getLogger(ExtensionLoader::class)

}