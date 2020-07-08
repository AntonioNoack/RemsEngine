package me.anno.studio.project

import me.anno.io.Saveable
import me.anno.io.config.ConfigBasics
import me.anno.io.utils.StringMap
import me.anno.studio.Studio
import java.io.File

class Project(val file: File): Saveable(){

    val configFile = File(file, "config.txt")

    val config: StringMap
    init {
        val defaultConfig = StringMap()
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

    var targetWidth = config["target.width", 1920]
    var targetHeight = config["target.height", 1080]
    var targetFPS = config["target.fps", 30f]
    var targetOutputFile = config["target.file", File(file, "output.mp4")]

    override fun getClassName() = "Project"
    override fun getApproxSize() = 1000
    override fun isDefaultValue() = false

    fun saveConfig(){
        config["target.width"] = targetWidth
        config["target.height"] = targetHeight
        config["target.fps"] = targetFPS
        ConfigBasics.save(configFile, config.toString())
    }

    fun saveScenes(){
        // todo save the scene(s)
    }

    fun save(){
        saveConfig()
        saveScenes()
    }

    fun loadIntoUI(){
        Studio.project = this
    }

}