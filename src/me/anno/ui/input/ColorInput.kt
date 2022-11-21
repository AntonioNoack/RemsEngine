package me.anno.ui.input

import me.anno.config.DefaultConfig.style
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.GFX
import me.anno.gpu.GFXBase
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.Screenshots
import me.anno.gpu.texture.Texture2D
import me.anno.input.Input
import me.anno.input.MouseButton
import me.anno.io.serialization.NotSerializedProperty
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.pow
import me.anno.studio.StudioBase.Companion.dragged
import me.anno.studio.StudioBase.Companion.shiftSlowdown
import me.anno.ui.Window
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.constraints.SizeLimitingContainer
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.base.text.TextStyleable
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.ui.editor.color.ColorChooser
import me.anno.ui.editor.color.PreviewField
import me.anno.ui.input.components.ColorPalette
import me.anno.ui.input.components.ColorPicker
import me.anno.ui.input.components.TitlePanel
import me.anno.ui.style.Style
import me.anno.utils.Color.black
import me.anno.utils.Color.toARGB
import org.joml.Vector4f
import kotlin.math.max

open class ColorInput(
    style: Style,
    val title: String,
    @Suppress("unused_parameter")
    visibilityKey: String,
    oldValue: Vector4f,
    withAlpha: Boolean,
    val contentView: ColorChooser = ColorChooser(style, withAlpha, ColorPalette(8, 4, style))
) : PanelListX(style), InputPanel<Vector4f>, TextStyleable {

    constructor(style: Style) : this(style, "", "", Vector4f(), true)

    val titleView = TitlePanel(title, contentView, style)
    private val previewField = PreviewField(titleView, 2, style)
        .apply {
            addLeftClickListener { if (isInputAllowed) openColorChooser() }
            color = oldValue.toARGB() or black
        }

    @NotSerializedProperty
    private var mouseIsDown = false

    // todo drawing & ignoring inputs
    override var isInputAllowed = true
        set(value) {
            if (field != value) {
                field = value
                invalidateDrawing()
            }
        }

    fun getValue() = contentView.getColor()
    override val lastValue: Vector4f get() = getValue()

    override fun setValue(value: Vector4f, notify: Boolean): ColorInput {
        previewField.color = value.toARGB()
        previewField.invalidateDrawing()
        contentView.setRGBA(value, notify)
        return this
    }

    override var textSize: Float
        get() = titleView.textSize
        set(value) {
            titleView.textSize = value
        }

    override var textColor: Int
        get() = titleView.textColor
        set(value) {
            titleView.textColor = value
        }

    override var isBold: Boolean
        get() = titleView.isBold
        set(value) {
            titleView.isBold = value
        }

    override var isItalic: Boolean
        get() = titleView.isItalic
        set(value) {
            titleView.isItalic = value
        }

    init {
        // switched order for consistent alignment
        this += previewField
        this += titleView
        titleView.enableHoverColor = true
        titleView.disableFocusColors()
        contentView.setRGBA(oldValue, false)
    }

    fun openColorChooser() {
        contentView.colorSpace = ColorChooser.getDefaultColorSpace()
        val window = window!!
        Menu.openMenuByPanels(
            window.windowStack, window.mouseXi, window.mouseYi, NameDesc(title), listOf(
                SizeLimitingContainer(contentView, windowStack.width / 5, -1, style)
            )
        )
    }

    override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
        when {
            isInputAllowed && button.isLeft -> {
                if (long) openColorChooser()
                else super.onMouseClicked(x, y, button, false)
            }
            isInputAllowed && button.isRight -> {
                val window = GFX.activeWindow!!
                Menu.openMenu(windowStack, listOf(
                    MenuOption(NameDesc("Copy")) { Input.copy(window, contentView) },
                    MenuOption(NameDesc("Paste")) { Input.paste(window, contentView) },
                    MenuOption(NameDesc("Pick Color")) { pickColor() },
                    MenuOption(NameDesc("Reset")) { setValue(contentView.resetListener(), true) }
                ))
            }
            else -> super.onMouseClicked(x, y, button, long)
        }
    }

    // todo button for color picker.. but where?
    fun pickColor() {
        // color picker
        // - take screenshot of full screen; all screens? could be hard with multiples in non-regular config...
        // - open (new?) window in fullscreen
        // - add controls on the bottom, or somewhere..., with a preview of the color
        // - select on click, or when dragging + enter then
        val windowX = GFX.activeWindow ?: GFX.someWindow
        GFX.addGPUTask("ColorInput.pickColor()", 1) {// delay, so the original menu can disappear
            val screenshot = Screenshots.takeSystemScreenshot()
            val colorPicker = if (screenshot == null) {
                val ws = windowStack
                val fb = Framebuffer("colorPicker", ws.width, ws.height, 1, 1, false, DepthBufferType.INTERNAL)
                fb.ensure()
                useFrame(fb) {
                    windowStack.draw(fb.w, fb.h, didSomething0 = true, forceRedraw = true)
                }
                val imageData = fb.createImage(true, withAlpha = false)
                ColorPicker(fb, fb.getTexture0() as Texture2D, imageData, true, style)
            } else {
                val texture = Texture2D("screenshot", screenshot, false)
                ColorPicker(null, texture, screenshot, true, style)
            }
            var wasFullscreen = false
            fun resetFullscreen() {
                if (!wasFullscreen && windowX.isFullscreen()) {
                    windowX.toggleFullscreen()
                }
            }
            colorPicker.callback = { color ->
                contentView.setARGB(color, true)
                this@ColorInput.invalidateDrawing()
                resetFullscreen()
            }
            colorPicker.enableControls()
            val windowStack = windowStack
            windowStack.push(object : Window(colorPicker, true, windowStack) {
                override fun destroy() {
                    super.destroy()
                    resetFullscreen()
                }
            })
            wasFullscreen = windowX.isFullscreen()
            if (!wasFullscreen) windowX.toggleFullscreen()
        }
    }

    override fun onCopyRequested(x: Float, y: Float): String? {
        return contentView.onCopyRequested(x, y)
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        return contentView.onPaste(x, y, data, type)
    }

    override fun onMouseDown(x: Float, y: Float, button: MouseButton) {
        super.onMouseDown(x, y, button)
        mouseIsDown = true
    }

    override fun onMouseUp(x: Float, y: Float, button: MouseButton) {
        super.onMouseUp(x, y, button)
        mouseIsDown = false
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        super.onMouseMoved(x, y, dx, dy)
        if (mouseIsDown && dragged == null) {
            val ws = windowStack
            val speed = 20f * shiftSlowdown / max(ws.width, ws.height)
            val delta = (dx - dy) * speed
            val scaleFactor = 1.10f
            val scale = pow(scaleFactor, delta)
            contentView.apply {
                if (Input.isControlDown) {
                    setHSL(hue, saturation, lightness * scale, opacity, colorSpace, true)
                } else {
                    setHSL(hue, saturation, lightness, clamp(opacity + delta, 0f, 1f), colorSpace, true)
                }
            }
        }
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        val focused1 = titleView.isInFocus || contentView.any { it.isInFocus }
        if (focused1) isSelectedListener?.invoke()
        super.onDraw(x0, y0, x1, y1)
    }

    fun setChangeListener(listener: (r: Float, g: Float, b: Float, a: Float) -> Unit): ColorInput {
        contentView.setChangeRGBListener { r, g, b, a ->
            previewField.color = Vector4f(r, g, b, a).toARGB()
            listener(r, g, b, a)
        }
        return this
    }

    private var isSelectedListener: (() -> Unit)? = null
    fun setIsSelectedListener(listener: () -> Unit): ColorInput {
        isSelectedListener = listener
        return this
    }

    fun setResetListener(listener: () -> Vector4f): ColorInput {
        contentView.setResetListener(listener)
        return this
    }

    override fun clone(): ColorInput {
        val clone = ColorInput(style)
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as ColorInput
        // only works, if there is no references
        clone.isSelectedListener = isSelectedListener
        clone.setResetListener(contentView.resetListener)
    }

    override val className = "ColorInput"

    companion object {
        // test the UI
        @JvmStatic
        fun main(args: Array<String>) {
            GFXBase.disableRenderDoc()
            testUI {
                ColorInput(style)
                    .apply {
                        alignmentX = AxisAlignment.CENTER
                        alignmentY = AxisAlignment.CENTER
                    }
            }
        }
    }

}