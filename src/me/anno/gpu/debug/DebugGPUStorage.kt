package me.anno.gpu.debug

import me.anno.config.DefaultConfig.style
import me.anno.gpu.GFX
import me.anno.gpu.buffer.Buffer
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.DrawTexts.monospaceFont
import me.anno.gpu.drawing.DrawTextures
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.texture.CubemapTexture
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture3D
import me.anno.gpu.texture.TextureLib
import me.anno.input.Input
import me.anno.language.translation.NameDesc
import me.anno.studio.StudioBase.Companion.defaultWindowStack
import me.anno.ui.base.Panel
import me.anno.ui.base.Visibility
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.groups.PanelList2D
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.menu.MenuOption
import org.apache.logging.log4j.LogManager
import org.joml.Vector2f
import org.joml.Vector4f
import kotlin.math.min

/**
 * remember all alive instances for debugging
 * */
object DebugGPUStorage {

    private val LOGGER = LogManager.getLogger(DebugGPUStorage::class)

    val tex2d = HashSet<Texture2D>()
    val tex3d = HashSet<Texture3D>()
    val texCd = HashSet<CubemapTexture>()

    val fbs = HashSet<Framebuffer>()

    val buffers = HashSet<Buffer>()

    val fontSize get() = monospaceFont.value.sizeInt

    class TexturePanel(name: String, val tex: Texture2D) : Panel(style) {

        init {
            this.name = name
            tooltip = name
        }

        override fun calculateSize(w: Int, h: Int) {
            super.calculateSize(w, h)
            val sx = min(w, GFX.width).toFloat() / tex.w
            val sy = min(h, GFX.height).toFloat() / tex.h
            val scale = min(sx, sy)
            minW = (tex.w * scale).toInt()
            minH = (tex.h * scale).toInt() + fontSize
        }

        override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
            super.onDraw(x0, y0, x1, y1)
            if (tex.isCreated && !tex.isDestroyed) {
                val scale = min(w.toFloat() / tex.w, (h - fontSize).toFloat() / tex.h)
                val w = (tex.w * scale).toInt()
                val h = (tex.h * scale).toInt()
                // transparency-showing background
                DrawTextures. drawTransparentBackground(x, y + fontSize, w, h - fontSize)
                DrawTextures.drawTexture(x, y + fontSize, w, h, tex, -1, null)
                DrawTexts.drawSimpleTextCharByChar(x, y, 2, "$name, ${tex.w} x ${tex.h}")
            } else visibility = Visibility.GONE
        }
    }

    class TexturePanel3D(name: String, val tex: CubemapTexture) : Panel(style) {

        init {
            this.name = name
            tooltip = name
        }

        val rotation = Vector2f()

        override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
            if (Input.isLeftDown) {
                val scale = 5f / min(w, h)
                rotation.x += scale * dx
                rotation.y += scale * dy
            } else super.onMouseMoved(x, y, dx, dy)
        }

        override fun calculateSize(w: Int, h: Int) {
            super.calculateSize(w, h)
            val sx = min(w, GFX.width).toFloat() / tex.w
            val sy = min(h, GFX.height).toFloat() / tex.h
            val scale = min(sx, sy)
            minW = (tex.w * scale).toInt()
            minH = (tex.h * scale).toInt() + fontSize
        }

        override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
            super.onDraw(x0, y0, x1, y1)
            if (tex.isCreated && !tex.isDestroyed) {
                val scale = min(w.toFloat() / tex.w, (h - fontSize).toFloat() / tex.h)
                val w = (tex.w * scale).toInt()
                val h = (tex.h * scale).toInt()
                DrawTextures.drawTransparentBackground(x, y + fontSize, w, h - fontSize)
                DrawTextures.drawProjection(x, y + fontSize, w, h, tex, false, -1)
                DrawTexts.drawSimpleTextCharByChar(x, y, 2, "$name, ${tex.w} x ${tex.h}")
            } else visibility = Visibility.GONE
        }
    }

    fun openMenu() {
        Menu.openMenu(defaultWindowStack!!, listOf(
            MenuOption(NameDesc("Texture2Ds")) {
                createListOfPanels("Texture2Ds") { list ->
                    for (tex in tex2d) {
                        list.add(TexturePanel(tex.name, tex))
                    }
                }
            },
            MenuOption(NameDesc("Texture3Ds")) {
                // how can we display them?
                LOGGER.warn("Not yet implemented")
            },
            MenuOption(NameDesc("CubemapTextures")) {
                // todo test this in Rem's Studio
                createListOfPanels("CubemapTextures") { list ->
                    for (tex in texCd) {
                        list.add(TexturePanel3D("", tex))
                    }
                }
            },
            MenuOption(NameDesc("Framebuffers")) {
                createListOfPanels("Framebuffers") { list ->
                    for (fb in fbs) {
                        for (tex in fb.textures) {
                            list.add(TexturePanel(tex.name, tex))
                        }
                        val dt = fb.depthTexture
                        if (dt != null) list.add(TexturePanel(dt.name, dt))
                    }
                }
            },
            MenuOption(NameDesc("Buffers")) {
                // how can we display them?
                // maybe like in RenderDoc, or as plain list with attributes, vertex count and such
                LOGGER.warn("Not yet implemented")
            }
        ))
    }

    private fun createListOfPanels(title: String, fillList: (PanelList) -> Unit) {
        val list = PanelList2D(style)
        list.childWidth *= 2
        list.childHeight *= 2
        fillList(list)
        Menu.openMenuByPanels(
            defaultWindowStack!!,
            Input.mouseX.toInt(),
            Input.mouseY.toInt(),
            NameDesc(title),
            listOf(list)
        )
    }

}