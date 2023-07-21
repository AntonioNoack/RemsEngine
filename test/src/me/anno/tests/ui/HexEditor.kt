package me.anno.tests.ui

import me.anno.config.DefaultConfig
import me.anno.gpu.GFXBase.disableRenderDoc
import me.anno.input.ActionManager
import me.anno.studio.StudioBase
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.scrolling.ScrollPanelXY
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.ui.editor.code.HexEditor
import me.anno.utils.OS

fun main() {
    disableRenderDoc()
    testUI("Hex Editor") {
        StudioBase.instance?.enableVSync = false
        ActionManager.register("HexEditor.s.t.c", "Save")
        val list = PanelListX(DefaultConfig.style)
        val files = listOf(
            OS.desktop.getChild("SM_Prop_Gem_03.prefab"),
            OS.desktop.getChild("SM_Env_Minetrack_Bridge_Broken_01.prefab"),
            OS.desktop.getChild("Character_Ghost_01.prefab"),
            OS.desktop.getChild("Character_Ghost_02.prefab"),
            OS.desktop.getChild("FX_Sword_Fire.prefab")
        )
        for (file1 in files) {
            list.add(HexEditor(DefaultConfig.style).apply {
                file = file1
                compareTo.addAll(files)
                compareTo.remove(file1)
                showAddress = list.children.isEmpty()
            })
        }
        ScrollPanelXY(list, Padding.Zero, DefaultConfig.style)
    }
}