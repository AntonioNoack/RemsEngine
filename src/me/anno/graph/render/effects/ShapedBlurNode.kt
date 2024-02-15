package me.anno.graph.render.effects

import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.effects.ShapedBlur.applyFilter
import me.anno.gpu.shader.effects.ShapedBlur.filters
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.graph.render.Texture
import me.anno.graph.types.flow.actions.ActionNode
import me.anno.graph.ui.GraphEditor
import me.anno.graph.ui.GraphPanel
import me.anno.io.base.BaseWriter
import me.anno.language.translation.NameDesc
import me.anno.ui.Style
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.text.TextPanel
import me.anno.ui.input.EnumInput
import me.anno.utils.Sleep.waitUntil

class ShapedBlurNode() : ActionNode(
    "heart_5x32",
    listOf("Texture", "Input", "Float", "Scale", "Float", "Gamma"),
    listOf("Texture", "Blurred")
) {

    constructor(type: String) : this() {
        this.type = type
    }

    init {
        setInput(1, whiteTexture)
        setInput(2, 1f)
        setInput(3, 2.2f)
    }

    override fun createUI(g: GraphPanel, list: PanelList, style: Style) {
        // ensure all types are loaded
        if (g is GraphEditor) {
            waitUntil(true) { filters.isNotEmpty() }
            list.add(
                EnumInput(
                    "Type", "", type,
                    filters.keys.sorted().map { NameDesc(it) }, style
                ).setChangeListener { value, _, _ ->
                    type = value
                    g.onChange(false)
                }
            )
        } else list.add(TextPanel("Type: $type", style))
    }

    var type = "heart_5x32"
        set(value) {
            field = value
            name = value
        }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeString("type", type)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "type" -> type = value as? String ?: return
            else -> super.setProperty(name, value)
        }
    }

    override fun executeAction() {
        // todo a formula could be connected, and this would break the texture-thing...
        val tex0 = getInput(1) as? Texture
        val tex1 = tex0?.tex ?: whiteTexture
        val scale = getInput(2) as Float
        val gamma = getInput(3) as Float
        val output = if (scale > 0f && tex1 != whiteTexture && gamma > 0f) {
            val filter = filters[type]?.value
            if (filter != null) {
                val (shader, stages) = filter
                val tmpType = if (tex1.isHDR || gamma > 1.4f) TargetType.Float32x3
                else TargetType.UInt8x3
                Texture(applyFilter(tex1, shader, stages, tmpType, scale, gamma))
            } else tex0
        } else tex0
        setOutput(1, output)
    }
}