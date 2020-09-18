package me.anno.studio.project

import me.anno.io.Saveable
import me.anno.io.config.ConfigBasics
import me.anno.io.utils.StringMap
import me.anno.studio.Studio
import me.anno.ui.editor.sceneTabs.SceneTabs
import java.io.File
import kotlin.concurrent.thread

class Project(var name: String, val file: File): Saveable(){

    val configFile = File(file, "config.json")

    val config: StringMap
    init {
        val defaultConfig = StringMap()
        defaultConfig["general.name"] = name
        defaultConfig["target.width"] = 1920
        defaultConfig["target.height"] = 1080
        defaultConfig["target.fps"] = 30f
        config = ConfigBasics.loadConfig(configFile, defaultConfig, true)
    }

    val scenes = File(file, "scenes")
    init { scenes.mkdirs() }

    // do we need multiple targets per project? maybe... todo overlays!
    // do we need a target at all? -> yes
    // todo include the scene? or do we store it in different files?
    // todo a project always is a folder
    // zip this folder all the time to not waste SSD life time? -> no, we don't have that many files
    // -> we need to be able to show contents of zip files then

    var targetDuration = config["target.duration", 5.0]
    var targetSizePercentage = config["target.sizePercentage", 100f]
    var targetWidth = config["target.width", 1920]
    var targetHeight = config["target.height", 1080]
    var targetFPS = config["target.fps", 30.0]
    var targetOutputFile = config["target.file", File(file, "output.mp4")]

    override fun getClassName() = "Project"
    override fun getApproxSize() = 1000
    override fun isDefaultValue() = false

    fun open(){
        thread {
            // todo open all recently opened files as tabs
            // todo lazy loading??? it's just config anyways, so it shouldn't be THAT bad...
            // todo if no project files are found, create a default file...
            config["recent.files", ""].split('\n').forEach {
                val name = it.trim()
                if(name.isNotEmpty()){
                    SceneTabs.open(File(name))
                }
            }
        }
    }

    // todo even save not saved parts? :)
    fun saveConfig(){
        config["general.name"] = name
        config["target.duration"] = targetDuration
        config["target.sizePercentage"] = targetSizePercentage
        config["target.width"] = targetWidth
        config["target.height"] = targetHeight
        config["target.fps"] = targetFPS
        config["recent.files"] = SceneTabs.children3
            .filter { it.file != null }
            .joinToString("\n"){ it.file.toString() }
        ConfigBasics.save(configFile, config.toString())
    }

    fun save(){
        saveConfig()
        SceneTabs.currentTab?.save {}
    }

}