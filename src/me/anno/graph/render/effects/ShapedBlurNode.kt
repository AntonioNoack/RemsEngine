package me.anno.graph.render.effects

import me.anno.gpu.shader.effects.ShapedBlur.applyFilter
import me.anno.gpu.shader.effects.ShapedBlur.filters
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.graph.render.Texture
import me.anno.graph.types.flow.actions.ActionNode
import me.anno.graph.ui.GraphEditor
import me.anno.language.translation.NameDesc
import me.anno.ui.base.groups.PanelList
import me.anno.ui.input.EnumInput
import me.anno.ui.style.Style
import me.anno.utils.Sleep.waitUntil

class ShapedBlurNode : ActionNode(
    "heart_5x32",
    listOf("Texture", "Input", "Float", "Scale"),
    listOf("Texture", "Blurred")
) {

    init {
        setInput(1, whiteTexture)
        setInput(2, 1f)
    }

    override fun createUI(g: GraphEditor, list: PanelList, style: Style) {
        super.createUI(g, list, style)
        // ensure all types are loaded
        waitUntil(true) { filters.isNotEmpty() }
        list.add(
            EnumInput(
                "Type", "", type,
                filters.keys.sorted().map { NameDesc(it) }, style
            ).setChangeListener { value, _, _ ->
                type = value
            }
        )
    }

    // todo serialize?
    var type = "heart_5x32"
        set(value) {
            field = value
            name = value
        }

    override fun executeAction() {
        // todo a formula could be connected, and this would break the texture-thing...
        val tex0 = getInput(1) as? Texture
        val tex1 = tex0?.tex ?: whiteTexture
        val scale = getInput(2) as Float
        val output = if (scale > 0f && tex1 != whiteTexture) {
            val filter = filters[type]?.value
            if (filter != null) {
                val (shader, stages) = filter
                Texture(applyFilter(tex1, shader, stages, tex1.isHDR, scale))
            } else tex0
        } else tex0
        setOutput(output, 1)
    }
}