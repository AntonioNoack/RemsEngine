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
import me.anno.image.ImageScale.scaleMaxPreview
import me.anno.input.Input
import me.anno.language.translation.NameDesc
import me.anno.studio.StudioBase.Companion.defaultWindowStack
import me.anno.ui.Panel
import me.anno.ui.base.Visibility
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.groups.PanelList2D
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.menu.MenuOption
import org.apache.logging.log4j.LogManager
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

    val fontSize get() = monospaceFont.sizeInt

    abstract class TexturePanelBase(val title: String) : Panel(style) {

        abstract fun getTexW(): Int
        abstract fun getTexH(): Int
        abstract fun isFine(): Boolean

        override fun calculateSize(w: Int, h: Int) {
            super.calculateSize(w, h)
            val (sw, sh) = scaleMaxPreview(getTexW(), getTexH(), min(w, GFX.width), min(h, GFX.height) - fontSize)
            minW = sw
            minH = sh + fontSize
        }

        override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
            super.onDraw(x0, y0, x1, y1)
            if (isFine()) {
                val (w, h) = scaleMaxPreview(getTexW(), getTexH(), w, h - fontSize)
                val xi = x + (this.w - w) / 2
                val yi = y + fontSize + (this.h - fontSize - h) / 2
                // transparency-showing background
                DrawTextures.drawTransparentBackground(xi, yi, w, h)
                drawTexture(xi, yi, w, h)
                DrawTexts.drawSimpleTextCharByChar(x, y, 2, title)
            } else visibility = Visibility.GONE
        }

        abstract fun drawTexture(x: Int, y: Int, w: Int, h: Int)

    }

    class TexturePanel(name: String, val tex: Texture2D) : TexturePanelBase("$name, ${tex.w} x ${tex.h}") {

        override fun getTexW(): Int = tex.w
        override fun getTexH(): Int = tex.h
        override fun isFine(): Boolean = tex.isCreated && !tex.isDestroyed

        override fun drawTexture(x: Int, y: Int, w: Int, h: Int) {
            DrawTextures.drawTexture(x, y, w, h, tex, -1, null)
        }

    }

    class TexturePanel3D(name: String, val tex: CubemapTexture) : TexturePanelBase("$name, ${tex.w} x ${tex.h}") {

        override fun getTexW(): Int = tex.w
        override fun getTexH(): Int = tex.h
        override fun isFine(): Boolean = tex.isCreated && !tex.isDestroyed

        override fun drawTexture(x: Int, y: Int, w: Int, h: Int) {
            DrawTextures.drawProjection(x, y, w, h, tex, false, -1)
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