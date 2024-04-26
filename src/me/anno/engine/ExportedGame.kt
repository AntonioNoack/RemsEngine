package me.anno.engine

import me.anno.config.DefaultConfig.style
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.ui.control.PlayControls
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderView1
import me.anno.gpu.GFX
import me.anno.gpu.shader.ShaderLib
import me.anno.input.ActionManager
import me.anno.installer.Installer
import me.anno.io.Saveable
import me.anno.io.files.InvalidRef
import me.anno.io.files.Reference
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.utils.StringMap
import me.anno.ui.Window
import me.anno.ui.base.groups.PanelStack

// todo register all classes that we need (not excluded ones)
class ExportedGame(val config: StringMap) : EngineBase(
    config["gameTitle", "Rem's Engine"],
    config["configName", "config"],
    config["versionNumber", 1],
    true // it would be very rare, that we didn't need it
) {
    override fun createUI() {
        workspace = Reference.getReference("res://") // ok so?
        val prefab = PrefabCache[config["firstScenePath", InvalidRef]]
            ?: throw IllegalStateException("Missing first scene")
        val scene = prefab.createInstance()
        val windowStack = GFX.someWindow.windowStack
        val stack = PanelStack(style)
        val renderView = RenderView1(PlayMode.PLAYING, scene, style)
        stack.add(renderView)
        stack.add(PlayControls(renderView))
        windowStack.add(Window(stack, false, windowStack))

        Installer.checkFFMPEGInstall()
        ShaderLib.init()

        EngineActions.register()
        ActionManager.init()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Saveable.registerCustomClass(StringMap())
            val configText = Reference.getReference("res://export.json").readTextSync()
            val config = JsonStringReader.readFirst<StringMap>(configText, InvalidRef)
            ExportedGame(config).run()
        }
    }
}