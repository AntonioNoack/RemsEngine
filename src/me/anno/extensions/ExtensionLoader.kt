package me.anno.extensions

import me.anno.config.DefaultConfig
import me.anno.engine.EngineBase
import me.anno.extensions.mods.Mod
import me.anno.extensions.mods.ModManager
import me.anno.extensions.plugins.Plugin
import me.anno.extensions.plugins.PluginManager
import me.anno.io.config.ConfigBasics.configFolder
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.Reference.getReference
import me.anno.io.yaml.generic.SimpleYAMLReader
import me.anno.utils.hpc.HeavyProcessing.processStage
import me.anno.utils.types.Ints.toIntOrDefault
import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.net.URL
import java.net.URLClassLoader
import java.util.zip.ZipInputStream
import kotlin.concurrent.thread
import kotlin.reflect.KClass

object ExtensionLoader {

    @JvmStatic
    lateinit var pluginsFolder: FileReference

    @JvmStatic
    lateinit var modsFolder: FileReference

    @JvmField
    val managers = listOf(ModManager, PluginManager)

    private val internally = ArrayList<ExtensionInfo>()

    @JvmStatic
    fun load() {

        unload()

        pluginsFolder = configFolder.getChild("plugins")
        modsFolder = configFolder.getChild("mods")

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

    @JvmStatic
    fun unload() {
        // plugins may depend on mods -> first disable them
        PluginManager.disable()
        ModManager.disable()
    }

    @JvmStatic
    private fun addAllFromFolder(
        folder: FileReference,
        threads: MutableList<Thread>,
        extInfos0: MutableList<ExtensionInfo>,
        maxDepth: Int = 5
    ) {
        if (!folder.exists) return
        for (it in folder.listChildren()) {
            if (!it.name.startsWith(".")) {
                if (it.isDirectory) {
                    if (maxDepth > 0) {
                        addAllFromFolder(it, threads, extInfos0, maxDepth - 1)
                    } else LOGGER.warn("Ignored $it, because it's too deep")
                } else {
                    val name = it.name
                    if (!name.startsWith(".") && it.lcExtension == "jar") {
                        threads += thread(name = "ExtensionLoader::getInfos($it)") {
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

    @JvmStatic
    private fun getInfos(): List<ExtensionInfo> {
        val result = ArrayList<ExtensionInfo>()
        val threads = ArrayList<Thread>()
        addAllFromFolder(pluginsFolder, threads, result)
        addAllFromFolder(modsFolder, threads, result)
        for (it in threads) {
            it.join()
        }
        for (internal in internally) {
            result.removeAll { it.uuid == internal.uuid }
            result.add(internal)
        }
        return result
    }

    @JvmStatic
    private fun checkExtensionRequirements(info: ExtensionInfo): Boolean {
        val instance = EngineBase.instance
        val instanceVersion = instance?.versionNumber ?: 0
        if (instanceVersion in info.minVersion until info.maxVersion) {
            // check if the mod was not disabled
            val isDisabled = DefaultConfig["extensions.isDisabled.${info.uuid}", false]
            if (isDisabled) {
                LOGGER.info("Ignored extension \"${info.name}\", because it was disabled")
            } else return true
        } else {
            LOGGER.warn(
                "Extension \"${info.name}\" is incompatible " +
                        "with ${instance?.title} version ${instance?.versionName}!, " +
                        "${info.minVersion} <= $instanceVersion < ${info.maxVersion}"
            )
        }
        return false
    }

    @JvmStatic
    private fun warnOfMissingDependencies(extInfos: Collection<ExtensionInfo>, extInfos0: Collection<ExtensionInfo>) {
        if (extInfos.size != extInfos0.size) {
            val ids = HashSet(extInfos.map { it.uuid })
            for (ex in extInfos0) {
                if (ex !in extInfos) {
                    LOGGER.warn("Discarded extension ${ex.name}, because of missing dependencies ${ex.dependencies.filter { it !in ids }}")
                }
            }
        }
    }

    @JvmStatic
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

    @JvmStatic
    @Suppress("unused")
    fun reloadPlugins() {
        PluginManager.disable()
        val extInfos0 = getInfos()
        val extInfos = checkDependencies(extInfos0)
            .filter { it.isPluginNotMod }
        val loaded = loadExtensions(extInfos)
        val plugins = loaded.filterIsInstance<Plugin>()
        PluginManager.enable(plugins)
    }

    @JvmStatic
    fun load(ex: ExtensionInfo): Extension? {
        // create the main extension instance
        val clazz = ex.clazz
        if (clazz != null) {
            try {
                // call with arguments??..., e.g. config or StudioBase or sth...
                val ext = clazz.java.newInstance() as? Extension
                ext?.setInfo(ex)
                ext?.isRunning = true
                return ext
            } catch (e: Exception) {
                LOGGER.error("Error while loading ${ex.file}, class '$clazz'", e)
            }
            return null
        } else {
            val className = ex.mainClass
                .replace('\\', '.')
                .replace('/', '.')
            try {
                // create the main extension instance
                // load the classes
                val exFile = ex.file
                val urlClassLoader =
                    if (exFile.exists) {
                        URLClassLoader(
                            arrayOf(URL("file://${exFile.absolutePath}")),
                            javaClass.classLoader
                        )
                    } else ClassLoader.getSystemClassLoader()
                Thread.currentThread().contextClassLoader = urlClassLoader
                val classToLoad = Class.forName(className, true, urlClassLoader)
                // call with arguments??..., e.g. config or StudioBase or sth...
                val ext = classToLoad.newInstance() as? Extension
                ext?.setInfo(ex)
                ext?.isRunning = true
                return ext
            } catch (e: Exception) {
                LOGGER.error("Error while loading ${ex.file}, class '$className'", e)
            }
            return null
        }
    }

    /**
     * removes all extensions, which have missing dependencies
     * */
    @JvmStatic
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
                remaining.removeAll(toRemove.toSet())
                remainingUUIDs.removeAll(toRemove.map { it.uuid }.toSet())
                toRemove.clear()
            }
        }
        return remaining
    }

    @JvmStatic
    fun loadInfoFromZip(file: FileReference): ExtensionInfo? {
        LOGGER.info("Loading info about $file")
        ZipInputStream(file.inputStreamSync()).use { zis: ZipInputStream ->
            while (true) {
                val entry = zis.nextEntry ?: break
                val name = entry.name
                if (name.endsWith("extension.info") || name.endsWith("ext.info")) {
                    return loadInfoFromTxt(file)
                }
            }
        }
        return null
    }

    /**
     * loads the extension within the current mod project;
     * very useful for setting up a quick project
     * */
    @JvmStatic
    fun loadMainInfo(fileName: String = "res://extension.info") {
        val extensionSource = getReference(fileName)
        loadInternally(loadInfoFromTxt(InvalidRef, extensionSource)!!)
    }

    /**
     * loads the extension within the current mod project;
     * very useful for setting up a quick project;
     *
     * fails without exception
     * */
    @JvmStatic
    fun tryLoadMainInfo(fileName: String = "res://extension.info") {
        try {
            loadMainInfo(fileName)
        } catch (e: IOException) {
            LOGGER.warn("$fileName could not be loaded")
        }
    }

    @JvmStatic
    fun loadInternally(info: ExtensionInfo) {
        internally.add(info)
    }

    @JvmStatic
    @Suppress("unused")
    fun loadInternally(clazz: KClass<*>) {
        loadInternally(ExtensionInfo(clazz))
    }

    @JvmStatic
    fun loadInfoFromTxt(modFile: FileReference, infoFile: FileReference = modFile): ExtensionInfo? {
        val properties = SimpleYAMLReader.read(infoFile.readLinesSync(1024))
        val dependencies = ArrayList<String>()
        val info = ExtensionInfo()
        for ((key, value) in properties) {
            @Suppress("SpellCheckingInspection")
            when (key.lowercase()) {
                "plugin-name", "pluginname", "name" -> {
                    info.name = value
                    info.isPluginNotMod = true
                }
                "plugin-class", "pluginclass" -> {
                    info.mainClass = value
                    info.isPluginNotMod = true
                }
                "mod-class", "modclass" -> {
                    info.mainClass = value
                    info.isPluginNotMod = false
                }
                "mainclass", "main-class" -> {
                    info.mainClass = value
                }
                "mod-name", "modname" -> {
                    info.name = value
                    info.isPluginNotMod = false
                }
                "plugin-version", "mod-version", "pluginversion", "modversion" -> info.version = value
                "desc", "description", "mod-description", "moddescription",
                "plugin-description", "plugindescription" -> info.description = value
                "plugin-author", "plugin-authors",
                "mod-author", "mod-authors",
                "author", "authors" -> info.authors = value
                "moddependencies", "mod-dependencies",
                "plugindependencies", "plugin-dependencies",
                "dependencies" -> dependencies += value.split(',').filter { it.isNotBlank() }.map { it.trim() }
                "plugin-uuid", "mod-uuid", "plugin-id", "mod-id", "uuid" -> info.uuid = value
                "minversion", "min-version" -> info.minVersion = value.toIntOrDefault(info.minVersion)
                "maxversion", "max-version" -> info.maxVersion = value.toIntOrDefault(info.maxVersion)
                "priority" -> info.priority = value.toDoubleOrNull() ?: info.priority
            }
        }
        if (info.uuid.isEmpty()) info.uuid = info.name.trim()
        return if (info.name.isNotEmpty()) info else null
    }

    @JvmStatic
    private val LOGGER = LogManager.getLogger(ExtensionLoader::class)
}