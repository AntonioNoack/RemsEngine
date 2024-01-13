package me.anno.gpu.debug

import me.anno.Time
import me.anno.config.DefaultConfig.style
import me.anno.gpu.GFX
import me.anno.gpu.buffer.OpenGLBuffer
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.DrawTexts.monospaceFont
import me.anno.gpu.drawing.DrawTextures
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.texture.CubemapTexture
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture2DArray
import me.anno.gpu.texture.Texture3D
import me.anno.image.ImageScale.scaleMaxPreview
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.fract
import me.anno.maths.Maths.max
import me.anno.ui.Panel
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.groups.PanelList2D
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.base.text.TextPanel
import me.anno.utils.files.Files.formatFileSize
import me.anno.utils.structures.Compare.ifSame
import org.lwjgl.opengl.GL46C.*
import kotlin.math.min

/**
 * remember all alive instances for debugging
 *
 * only active, if Build.isDebug
 * */
object DebugGPUStorage {

    val tex2d = HashSet<Texture2D>(512)
    val tex3d = HashSet<Texture3D>(64)
    val tex2da = HashSet<Texture2DArray>(64)
    val tex3dCs = HashSet<CubemapTexture>(64)

    val fbs = HashSet<Framebuffer>(256)

    val buffers = HashSet<OpenGLBuffer>(256)

    val fontSize get() = monospaceFont.sizeInt

    abstract class TexturePanelBase(val title: String) : Panel(style) {

        abstract fun getTexW(): Int
        abstract fun getTexH(): Int
        abstract fun isFine(): Boolean

        override fun calculateSize(w: Int, h: Int) {
            super.calculateSize(w, h)
            val ws = windowStack
            val (sw, sh) = scaleMaxPreview(getTexW(), getTexH(), min(w, ws.width), min(h, ws.height) - fontSize)
            minW = sw
            minH = sh + fontSize
        }

        override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
            super.onDraw(x0, y0, x1, y1)
            if (isFine()) {
                val (w, h) = scaleMaxPreview(getTexW(), getTexH(), width, height - fontSize)
                val xi = x + (this.width - w) / 2
                val yi = y + fontSize + (this.height - fontSize - h) / 2
                // transparency-showing background
                DrawTextures.drawTransparentBackground(xi, yi, w, h)
                drawTexture(xi, yi, w, h)
                DrawTexts.drawSimpleTextCharByChar(x, y, 2, title)
            } else isVisible = false
        }

        abstract fun drawTexture(x: Int, y: Int, w: Int, h: Int)

    }

    fun isDepthFormat(format: Int) = when (format) {
        GL_DEPTH_COMPONENT16,
        GL_DEPTH_COMPONENT24,
        GL_DEPTH24_STENCIL8,
        GL_DEPTH32F_STENCIL8,
        GL_DEPTH_COMPONENT32,
        GL_DEPTH_COMPONENT32F -> true

        else -> false
    }

    class TexturePanel(name: String, val tex: Texture2D, val flipY: Boolean) :
        TexturePanelBase("$name, ${tex.width} x ${tex.height}") {

        override fun getTexW(): Int = tex.width
        override fun getTexH(): Int = tex.height
        override fun isFine(): Boolean = tex.wasCreated && !tex.isDestroyed

        override fun drawTexture(x: Int, y: Int, w: Int, h: Int) {
            if (isDepthFormat(tex.internalFormat)) {
                DrawTextures.drawDepthTexture(x, y, w, h, tex) // flipped automatically
            } else {
                var y2 = y
                var h2 = h
                if (flipY) {
                    y2 = y + h
                    h2 = -h
                }
                DrawTextures.drawTexture(x, y2, w, h2, tex, -1, null)
            }
        }

        override fun onUpdate() {
            super.onUpdate()
            invalidateDrawing()
        }

        override fun getTooltipText(x: Float, y: Float) =
            "${tex.width} x ${tex.height} x ${tex.samples}, ${GFX.getName(tex.internalFormat)}"

    }

    class TexturePanel3D(name: String, val tex: Texture3D) :
        TexturePanelBase("$name, ${tex.width} x ${tex.height} x ${tex.depth}") {

        override fun getTexW(): Int = tex.width
        override fun getTexH(): Int = tex.height
        override fun isFine(): Boolean = tex.wasCreated && !tex.isDestroyed
        val isDepth get() = isDepthFormat(tex.internalFormat)

        // animated
        override fun onUpdate() {
            super.onUpdate()
            invalidateDrawing()
        }

        override fun drawTexture(x: Int, y: Int, w: Int, h: Int) {
            // how can we display them? as slices...
            // slide through slices
            // a) automatically
            // b) when hovering
            // todo calculate min/max?
            // todo better test? currently only black...
            val z = if (isHovered) clamp((window!!.mouseX - x) / w)
            else fract(Time.nanoTime / 5.0).toFloat()
            DrawTextures.draw3dSlice(x, y, w, h, z, tex, true, -1, false, isDepth)
        }

    }

    class TexturePanel2DA(name: String, val tex: Texture2DArray) :
        TexturePanelBase("$name, ${tex.width} x ${tex.height} x ${tex.layers}") {

        override fun getTexW(): Int = tex.width
        override fun getTexH(): Int = tex.height
        override fun isFine(): Boolean = tex.wasCreated && !tex.isDestroyed
        val isDepth get() = isDepthFormat(tex.internalFormat)

        // animated
        override fun onUpdate() {
            super.onUpdate()
            invalidateDrawing()
        }

        override fun drawTexture(x: Int, y: Int, w: Int, h: Int) {
            // how can we display them? as slices...
            // slide through slices
            // a) automatically
            // b) when hovering
            // todo calculate min/max?
            val z = if (isHovered) clamp((window!!.mouseX - x) / w)
            else fract(Time.nanoTime / max(5f, tex.layers / 3f)).toFloat()
            val zi = (z * (tex.layers + 1)).toInt()
            // todo why is every 2nd slice missing??
            DrawTextures.draw2dArraySlice(
                x, y, w, h, zi,
                tex, true, -1, false, isDepth
            )
        }

    }

    class TexturePanel3DC(name: String, val tex: CubemapTexture) :
        TexturePanelBase("$name, ${tex.width} x ${tex.height}") {

        override fun getTexW(): Int = tex.width * 2 // 360°
        override fun getTexH(): Int = tex.width // 180°
        override fun isFine(): Boolean = tex.wasCreated && !tex.isDestroyed
        val isDepth get() = isDepthFormat(tex.internalFormat)

        override fun drawTexture(x: Int, y: Int, w: Int, h: Int) {
            DrawTextures.drawProjection(x, y, w, h, tex, false, -1, false, isDepth)
        }

    }

    fun openMenu() {
        val window = GFX.someWindow
        val window1 = Menu.openMenu(window.windowStack, listOf(
            MenuOption(
                NameDesc(
                    "Texture2Ds (${tex2d.size}, ${
                        tex2d.sumOf { it.locallyAllocated }.formatFileSize()
                    })"
                )
            ) {
                create2DListOfPanels("Texture2Ds") { list ->
                    for (tex in tex2d.sortedWith { a, b ->
                        val sa = a.width * a.height
                        val sb = b.width * b.height
                        sa.compareTo(sb).ifSame {
                            a.name.compareTo(b.name)
                        }
                    }) {
                        list.add(TexturePanel(tex.name, tex, false))
                    }
                }
            },
            MenuOption(
                NameDesc(
                    "Texture3Ds (${tex3d.size}, ${
                        tex3d.sumOf { it.locallyAllocated }.formatFileSize()
                    })"
                )
            ) {
                // todo test this
                create2DListOfPanels("Texture3Ds") { list ->
                    for (tex in tex3d.sortedBy { it.width * it.height * it.depth }) {
                        list.add(TexturePanel3D(tex.name, tex))
                    }
                }
            },
            MenuOption(
                NameDesc(
                    "Texture2D[]s (${tex2da.size}, ${
                        tex2da.sumOf { it.locallyAllocated }.formatFileSize()
                    })"
                )
            ) {
                create2DListOfPanels("Texture2D[]s") { list ->
                    for (tex in tex2da.sortedBy { it.width * it.height * it.layers }) {
                        list.add(TexturePanel2DA(tex.name, tex))
                    }
                }
            },
            MenuOption(
                NameDesc(
                    "CubemapTextures (${tex3dCs.size}, ${
                        tex3dCs.sumOf { it.locallyAllocated }.formatFileSize()
                    })"
                )
            ) {
                create2DListOfPanels("CubemapTextures") { list ->
                    for (tex in tex3dCs.sortedBy { it.width }) {
                        list.add(TexturePanel3DC("", tex))
                    }
                }
            },
            MenuOption(
                NameDesc(
                    "Framebuffers (${fbs.size}, ${
                        fbs.sumOf { it.renderBufferAllocated }.formatFileSize()
                    })"
                )
            ) {
                create2DListOfPanels("Framebuffers") { list ->
                    for (fb in fbs.sortedBy { it.width * it.height }) {
                        for (tex in fb.textures!!) {
                            list.add(TexturePanel(tex.name, tex, true))
                        }
                        val dt = fb.depthTexture
                        if (dt != null) list.add(TexturePanel(dt.name, dt, true))
                    }
                }
            },
            MenuOption(
                NameDesc(
                    "Buffers (${buffers.size}, ${
                        buffers.sumOf { it.locallyAllocated }.formatFileSize()
                    })"
                )
            ) {
                // how can we display them?
                // to do maybe like in RenderDoc, or as plain list with attributes, vertex count and such
                // we have name data, so we could show colors, uvs, coordinates and such :)
                // first, easy way:
                createListOfPanels("Buffers") { list ->
                    for (buff in buffers.sortedBy { it.locallyAllocated }) {
                        list.add(TextPanel(
                            "${GFX.getName(buff.type)}, " +
                                    "${buff.elementCount} x ${buff.attributes}, " +
                                    "total: ${
                                        (buff.nioBuffer?.capacity()?.toLong() ?: buff.locallyAllocated)
                                            .formatFileSize()
                                    }", style
                        ).apply { breaksIntoMultiline = true })
                    }
                }
            }
        ))
        window1?.drawDirectly = true
    }

    private fun create2DListOfPanels(title: String, fillList: (PanelList) -> Unit) {
        val list = PanelList2D(style)
        list.childWidth *= 2
        list.childHeight *= 2
        fillList(list)
        val window = GFX.someWindow
        val window1 = Menu.openMenuByPanels(
            window.windowStack,
            window.mouseX.toInt(),
            window.mouseY.toInt(),
            NameDesc(title),
            listOf(list)
        )
        window1?.drawDirectly = true
    }

    private fun createListOfPanels(title: String, fillList: (PanelList) -> Unit) {
        val list = PanelListY(style)
        fillList(list)
        val window = GFX.someWindow
        val window1 = Menu.openMenuByPanels(
            window.windowStack,
            window.mouseX.toInt(),
            window.mouseY.toInt(),
            NameDesc(title),
            listOf(list)
        )
        window1?.drawDirectly = true
    }

}