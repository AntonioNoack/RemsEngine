package me.anno.studio.project

import me.anno.config.DefaultConfig.style
import me.anno.gpu.GFX
import me.anno.io.Saveable
import me.anno.io.config.ConfigBasics
import me.anno.io.json.JsonArray
import me.anno.io.json.JsonReader
import me.anno.io.json.JsonWriter
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import me.anno.io.utils.StringMap
import me.anno.language.Language
import me.anno.objects.Camera
import me.anno.objects.Transform
import me.anno.studio.history.History
import me.anno.studio.rems.RemsStudio.editorTime
import me.anno.ui.base.Panel
import me.anno.ui.custom.*
import me.anno.ui.editor.UILayouts.createDefaultMainUI
import me.anno.ui.editor.sceneTabs.SceneTab
import me.anno.ui.editor.sceneTabs.SceneTabs
import me.anno.ui.editor.sceneView.SceneTabData
import me.anno.utils.types.Casting.castToFloat
import me.anno.utils.files.Files.use
import me.anno.utils.types.Lists.sumByFloat
import me.anno.video.FFMPEGEncodingBalance
import me.anno.video.FFMPEGEncodingType
import org.apache.logging.log4j.LogManager
import java.io.File
import kotlin.math.roundToInt

class Project(var name: String, val file: File) : Saveable() {

    val configFile = File(file, "config.json")
    val uiFile = File(file, "ui.json")
    val tabsFile = File(file, "tabs.json")

    val config: StringMap

    init {
        val defaultConfig = StringMap()
        defaultConfig["general.name"] = name
        defaultConfig["target.width"] = 1920
        defaultConfig["target.height"] = 1080
        defaultConfig["target.fps"] = 30f
        config = ConfigBasics.loadConfig(configFile, defaultConfig, true)
    }

    val scenes = File(file, "Scenes")

    init {
        scenes.mkdirs()
    }

    lateinit var mainUI: Panel

    fun resetUIToDefault() {
        mainUI = createDefaultMainUI(style)
    }

    fun loadUI() {

        fun tabsDefault() {
            val tab =
                SceneTab(
                    File(scenes, "Root.json"),
                    Transform().run {
                        name = "Root"
                        Camera(this)
                        this
                    }, History()
                )
            tab.save {}
            GFX.addGPUTask(1) {
                SceneTabs.closeAll()
                SceneTabs.open(tab)
                saveTabs()
            }
        }


        // tabs
        try {
            if (tabsFile.exists()) {
                val loadedUIData = TextReader
                    .fromText(tabsFile.readText())
                val sceneTabs = loadedUIData
                    .filterIsInstance<SceneTabData>()
                if (sceneTabs.isEmpty()) {
                    tabsDefault()
                } else {
                    GFX.addGPUTask(1) {
                        SceneTabs.closeAll()
                        sceneTabs.forEach { tabData ->
                            val tab = SceneTab(null, Transform(), null)
                            tabData.apply(tab)
                            SceneTabs.open(tab)
                        }
                    }
                }
            } else tabsDefault()
        } catch (e: Exception) {
            e.printStackTrace()
            tabsDefault()
        }

        // main ui
        try {
            if (uiFile.exists()) {
                val loadedUIData = loadUI2()
                if (loadedUIData != null) {
                    mainUI = loadedUIData
                } else resetUIToDefault()
            } else resetUIToDefault()
        } catch (e: Exception) {
            e.printStackTrace()
            resetUIToDefault()
        }

        (config["editor.time"] as? Double)?.apply {
            editorTime = this
        }

    }

    fun saveTabs() {
        val writer = TextWriter(false)
        SceneTabs.save(writer)
        writer.writeAllInList()
        tabsFile.writeText(writer.data.toString())
    }

    fun loadUI2(): Panel? {
        return use(uiFile.inputStream()) { fis ->
            val types = TypeLibrary.types
            val notFound = HashSet<String>()
            val style = style
            fun load(arr: JsonArray?): Panel? {
                arr ?: return null
                return try {
                    val type = arr[0] as? String ?: return null
                    val obj = when (type) {
                        "CustomListX" -> CustomListX(style)
                        "CustomListY" -> CustomListY(style)
                        else -> types[type]?.constructor?.invoke()
                    }
                    if (obj == null) {
                        notFound += type
                        return null
                    }
                    val weight = castToFloat(arr[1]) ?: 1f
                    if (obj is CustomList) {
                        for (i in 2 until arr.size) {
                            val child = load(arr[i] as? JsonArray) ?: continue
                            obj.addChild(child)
                        }
                        obj.setWeight(weight)
                    } else CustomContainer(obj, style).setWeight(weight)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            val obj = load(JsonReader(fis).readArray())
            if (notFound.isNotEmpty()) LOGGER.warn("UI-Types $notFound not found!")
            obj
        }
    }

    fun saveUI() {
        use(uiFile.outputStream()) { fos ->
            val writer = JsonWriter(fos)
            val cdc = mainUI as CustomListY
            fun write(c: Panel, w: Float) {
                if (c is CustomContainer) {
                    return write(c.child, w)
                }
                writer.open(true)
                writer.write(c.getClassName())
                writer.write((w * 1000f).roundToInt())
                if (c is CustomList) {
                    val weightSum = c.customChildren.sumByFloat { it.weight }
                    for (chi in c.customChildren) {
                        write(chi, chi.weight / weightSum)
                    }
                }
                writer.close(true)
            }
            write(cdc, 1f)
        }
    }

    // do we need multiple targets per project? maybe... soft links!
    // do we need a target at all? -> yes
    // a project always is a folder
    // zip this folder all the time to not waste SSD life time? -> no, we don't have that many files
    // -> we need to be able to show contents of zip files then

    var targetDuration = config["target.duration", 5.0]
    var targetSizePercentage = config["target.sizePercentage", 100f]
    var targetWidth = config["target.width", 1920]
    var targetHeight = config["target.height", 1080]
    var targetFPS = config["target.fps", 30.0]
    var targetOutputFile = config["target.output", File(file, "output.mp4")]
    var targetVideoQuality = config["target.quality", 23]
    var motionBlurSteps = config["target.motionBlur.steps", 8]
    var shutterPercentage = config["target.motionBlur.shutterPercentage", 1f]
    var nullCamera = createNullCamera(config["camera.null"] as? Camera)
    var language = Language.get(config["language", Language.AmericanEnglish.code])
    var ffmpegFlags = FFMPEGEncodingType[config["target.ffmpegFlags.id", FFMPEGEncodingType.DEFAULT.id]]
    var ffmpegBalance = FFMPEGEncodingBalance[config["target.encodingBalance", 0.5f]]

    override fun getClassName() = "Project"
    override fun getApproxSize() = 1000
    override fun isDefaultValue() = false

    fun open() {}

    fun saveConfig() {
        config["general.name"] = name
        config["target.duration"] = targetDuration
        config["target.sizePercentage"] = targetSizePercentage
        config["target.width"] = targetWidth
        config["target.height"] = targetHeight
        config["target.fps"] = targetFPS
        config["target.quality"] = targetVideoQuality
        config["target.motionBlur.steps"] = motionBlurSteps
        config["target.motionBlur.shutterPercentage"] = shutterPercentage
        config["target.output"] = targetOutputFile.toString()
        config["recent.files"] = SceneTabs.children3
            .filter { it.file != null }
            .joinToString("\n") { it.file.toString() }
        config["camera.null"] = nullCamera
        config["editor.time"] = editorTime
        config["language"] = language.code
        config["target.ffmpegFlags.id"] = ffmpegFlags.id
        config["target.encodingBalance"] = ffmpegBalance.value
        ConfigBasics.save(configFile, config.toString())
    }

    fun save() {
        saveConfig()
        SceneTabs.currentTab?.save {}
        saveUI()
    }

    fun createNullCamera(camera: Camera?): Camera {
        return (camera ?: Camera(null).apply {
            name = "Inspector Camera"
            onlyShowTarget = false
        })
            .apply {
                // higher far value to allow other far values to be seen
                farZ.defaultValue = 5000f
                timeDilation = 0.0 // the camera has no time, so no motion can be recorded
            }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(Project::class)
    }

}