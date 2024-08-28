package me.anno.engine

import me.anno.config.DefaultConfig.style
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.ui.control.PlayControls
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderView1
import me.anno.gpu.GFX
import me.anno.input.ActionManager
import me.anno.installer.Installer
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.saveable.Saveable
import me.anno.io.utils.StringMap
import me.anno.language.translation.NameDesc
import me.anno.ui.Window
import me.anno.ui.base.groups.PanelStack
import me.anno.utils.OS.res
import me.anno.utils.assertions.assertNotNull

class ExportedGame(val config: StringMap) : EngineBase(
    NameDesc(config["gameTitle", "Rem's Engine"]),
    config["configName", "config"],
    config["versionNumber", 1],
    true // it would be very rare, that we didn't need it
) {
    override fun createUI() {
        workspace = res // ok so?
        val loaded = PrefabCache[config["firstScenePath", InvalidRef]]
        val prefab = assertNotNull(loaded, "Missing first scene")
        val scene = prefab.createInstance()
        val windowStack = GFX.someWindow.windowStack
        val stack = PanelStack(style)
        val renderView = RenderView1(PlayMode.PLAYING, scene, style)
        stack.add(renderView)
        stack.add(PlayControls(renderView))
        windowStack.push(Window(stack, false, windowStack))

        Installer.checkFFMPEGInstall()

        EngineActions.register()
        ActionManager.init()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Saveable.registerCustomClass(StringMap())
            val configText = res.getChild("export.json").readTextSync()
            val config = JsonStringReader.readFirst(configText, InvalidRef, StringMap::class)
            ExportedGame(config).run()
        }
    }
}