package me.anno.tests.shader

import me.anno.animation.Type
import me.anno.config.DefaultConfig.style
import me.anno.ecs.components.camera.effects.DepthOfFieldEffect
import me.anno.ecs.prefab.PrefabInspector
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.debug.TestStudio.Companion.testUI3
import me.anno.ui.input.FloatInput
import me.anno.utils.OS.downloads

fun main() {
    // https://blog.voxagon.se/2018/05/04/bokeh-depth-of-field-in-single-pass.html
    val scene = downloads.getChild("ogldev-source/crytek_sponza/sponza.obj")
    testUI3 {
        EditorState.prefabSource = scene
        val sceneView = SceneView(EditorState, PlayMode.EDITING, style)
        sceneView.renderer.renderMode = RenderMode.DEPTH_OF_FIELD
        sceneView.weight = 1f
        PrefabInspector.currentInspector = PrefabInspector(scene)
        val list = PanelListY(style)
        val list2 = PanelListX(style)
        val effect = RenderMode.DEPTH_OF_FIELD.effect as DepthOfFieldEffect
        list2.add(FloatInput("Point", effect.focusPoint, Type.FLOAT_PLUS, style)
            .setChangeListener { effect.focusPoint = it.toFloat() })
        list2.add(FloatInput("Scale", effect.focusScale, Type.FLOAT_PLUS, style)
            .setChangeListener { effect.focusScale = it.toFloat() })
        list2.add(FloatInput("RadScale", effect.radScale, Type.LONG_PLUS, style)
            .setChangeListener { effect.radScale = it.toFloat() })
        list2.add(FloatInput("MaxSize", effect.maxBlurSize, Type.FLOAT_PLUS, style)
            .setChangeListener { effect.maxBlurSize = it.toFloat() })
        list2.add(FloatInput("Spherical", effect.spherical, Type.FLOAT_PLUS, style)
            .setChangeListener { effect.spherical = it.toFloat() })
        list.add(sceneView)
        list.add(list2)
        list
    }
}