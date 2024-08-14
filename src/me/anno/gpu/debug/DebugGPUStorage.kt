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
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture2DArray
import me.anno.gpu.texture.Texture3D
import me.anno.image.ImageScale.scaleMaxPreview
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.fract
import me.anno.maths.Maths.max
import me.anno.ui.Panel
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.groups.PanelList2D
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.base.text.TextPanel
import me.anno.utils.files.Files.formatFileSize
import me.anno.utils.structures.Compare.ifSame
import org.lwjgl.opengl.GL46C
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
    val texCubes = HashSet<CubemapTexture>(64)

    val fbs = HashSet<Framebuffer>(256)

    val buffers = HashSet<OpenGLBuffer>(256)

    val fontSize get() = monospaceFont.sizeInt

    abstract class TexturePanel<V : ITexture2D>(val title: String, val tex: V) : Panel(style) {

        open fun getTexW(): Int = tex.width
        fun getTexH(): Int = tex.height
        fun isFine(): Boolean = tex.isCreated()

        override fun calculateSize(w: Int, h: Int) {
            super.calculateSize(w, h)
            val ws = windowStack
            val (sw, sh) = scaleMaxPreview(getTexW(), getTexH(), min(w, ws.width), min(h, ws.height) - fontSize, 5)
            minW = sw
            minH = sh + fontSize
        }

        override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
            super.onDraw(x0, y0, x1, y1)
            if (isFine()) {
                val (w, h) = scaleMaxPreview(getTexW(), getTexH(), width, height - fontSize, 5)
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
        GL46C.GL_DEPTH_COMPONENT16,
        GL46C.GL_DEPTH_COMPONENT24,
        GL46C.GL_DEPTH24_STENCIL8,
        GL46C.GL_DEPTH32F_STENCIL8,
        GL46C.GL_DEPTH_COMPONENT32,
        GL46C.GL_DEPTH_COMPONENT32F -> true
        else -> false
    }

    class TexturePanel2D(name: String, tex: Texture2D, val flipY: Boolean) :
        TexturePanel<Texture2D>("$name, ${tex.width} x ${tex.height}", tex) {

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

        override fun getTooltipText(x: Float, y: Float) =
            "${tex.width} x ${tex.height} x ${tex.samples}, ${GFX.getName(tex.internalFormat)}"
    }

    // todo test this
    class TexturePanel3D(tex: Texture3D) :
        TexturePanel<Texture3D>("${tex.name}, ${tex.width} x ${tex.height} x ${tex.depth}", tex) {

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
            val isDepth = isDepthFormat(tex.internalFormat)
            DrawTextures.draw3dSlice(x, y, w, h, z, tex, true, -1, false, isDepth)
        }
    }

    class TexturePanel2DA(tex: Texture2DArray) :
        TexturePanel<Texture2DArray>("${tex.name}, ${tex.width} x ${tex.height} x ${tex.layers}", tex) {

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
            else fract(Time.nanoTime / max(5f, tex.layers / 3f))
            val zi = (z * (tex.layers + 1)).toInt()
            // todo why is every 2nd slice missing??
            DrawTextures.draw2dArraySlice(
                x, y, w, h, zi,
                tex, true, -1, false, isDepth
            )
        }
    }

    class TexturePanelCubes(tex: CubemapTexture) :
        TexturePanel<CubemapTexture>("${tex.name}, ${tex.width} x ${tex.height}", tex) {

        override fun getTexW(): Int = tex.width * 2 // 360°
        val isDepth get() = isDepthFormat(tex.internalFormat)

        override fun drawTexture(x: Int, y: Int, w: Int, h: Int) {
            DrawTextures.drawProjection(x, y, w, h, tex, false, -1, false, isDepth)
        }
    }

    private fun <V : ITexture2D> createEntry(
        title: String, tex2d: Collection<V>,
        createPanel: (V) -> Panel
    ): MenuOption {
        val sizeSum = tex2d.sumOf(ITexture2D::locallyAllocated).formatFileSize()
        return MenuOption(NameDesc("$title (${tex2d.size}, $sizeSum)")) {
            create2DListOfPanels(title) { list ->
                for (tex in tex2d.sortedWith { a, b ->
                    val sa = a.width * a.height
                    val sb = b.width * b.height
                    sa.compareTo(sb).ifSame(a.name.compareTo(b.name))
                }) {
                    list.add(createPanel(tex))
                }
            }
        }
    }

    fun openMenu() {
        val fbsSum = fbs.sumOf { it.renderBufferAllocated }
        val bufferSum = buffers.sumOf { it.locallyAllocated }
        Menu.openMenu(GFX.someWindow.windowStack, listOf(
            createEntry("Texture2Ds", tex2d) { TexturePanel2D(it.name, it, false) },
            createEntry("Texture3Ds", tex3d) { TexturePanel3D(it) },
            createEntry("Texture2D[]s", tex2da) { TexturePanel2DA(it) },
            createEntry("CubemapTextures", texCubes) { TexturePanelCubes(it) },
            MenuOption(NameDesc("Framebuffers (${fbs.size}, ${fbsSum.formatFileSize()})")) {
                create2DListOfPanels("Framebuffers") { list ->
                    for (fb in fbs.sortedBy { it.width * it.height }) {
                        val textures = fb.textures ?: emptyList()
                        for (i in textures.indices) {
                            val tex = textures[i]
                            list.add(TexturePanel2D(tex.name, tex, true))
                        }
                        val dt = fb.depthTexture
                        if (dt != null) {
                            list.add(TexturePanel2D(dt.name, dt, true))
                        }
                    }
                }
            },
            MenuOption(NameDesc("Buffers (${buffers.size}, ${bufferSum.formatFileSize()})")) {
                // how can we display them?
                // to do maybe like in RenderDoc, or as plain list with attributes, vertex count and such
                // we have name data, so we could show colors, uvs, coordinates and such :)
                // first, easy way:
                openMenuOfPanels("Buffers", PanelListY(style)) { list ->
                    for (buffer in buffers.sortedBy { it.locallyAllocated }) {
                        list.add(TextPanel(
                            "\"${buffer.name}\", ${GFX.getName(buffer.type)}, " +
                                    "${buffer.elementCount} x ${buffer.attributes}, " +
                                    "total: ${
                                        (buffer.nioBuffer?.capacity()?.toLong() ?: buffer.locallyAllocated)
                                            .formatFileSize()
                                    }", style
                        ).apply { breaksIntoMultiline = true })
                    }
                }
            }
        ))
    }

    private fun create2DListOfPanels(title: String, fillList: (PanelList) -> Unit) {
        val list = PanelList2D(style)
        list.childWidth *= 2
        list.childHeight *= 2
        openMenuOfPanels(title, list, fillList)
    }

    private fun openMenuOfPanels(title: String, list: PanelList, fillList: (PanelList) -> Unit) {
        fillList(list)
        Menu.openMenuByPanels(
            GFX.someWindow.windowStack,
            NameDesc(title),
            listOf(list)
        )?.drawDirectly = true
    }
}