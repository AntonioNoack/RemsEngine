package me.anno.ui.input

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.EngineBase.Companion.dragged
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.gpu.GFX
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.Screenshots
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.Texture2D
import me.anno.image.raw.IntImage
import me.anno.input.Clipboard
import me.anno.input.Input
import me.anno.input.Input.shiftSlowdown
import me.anno.input.Key
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.min
import me.anno.maths.Maths.pow
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.WindowStack
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.SizeLimitingContainer
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.base.text.TextStyleable
import me.anno.ui.editor.color.ColorChooser
import me.anno.ui.editor.color.ColorPreviewField
import me.anno.ui.input.components.ColorPalette
import me.anno.ui.input.components.ColorPicker
import me.anno.ui.input.components.TitlePanel
import me.anno.utils.Color.black4
import me.anno.utils.Color.rgba
import me.anno.utils.Color.toARGB
import me.anno.utils.Color.toHexColor
import me.anno.utils.Color.withAlpha
import org.apache.logging.log4j.LogManager
import org.joml.Vector4f
import kotlin.math.max

open class ColorInput(
    nameDesc: NameDesc,
    @Suppress("unused_parameter")
    visibilityKey: String,
    oldValue: Vector4f,
    val withAlpha: Boolean,
    style: Style,
    val base: ColorChooser = ColorChooser(style, withAlpha, ColorPalette(8, 4, style))
) : PanelListX(style), InputPanel<Vector4f>, TextStyleable {

    constructor(style: Style) : this(NameDesc.EMPTY, "", Vector4f(), true, style)

    val titleView = TitlePanel(nameDesc, base, style)
    private val previewField = ColorPreviewField(titleView, 2, style)
        .apply {
            addLeftClickListener { if (isInputAllowed) openColorChooser() }
            val oldValue1 = oldValue.toARGB()
            color = if (withAlpha) oldValue1 else oldValue1.withAlpha(255)
        }

    @NotSerializedProperty
    private var mouseIsDown = false

    override var isInputAllowed = true
        set(value) {
            if (field != value) {
                field = value
                val alpha = if (value) 255 else 160
                titleView.textColor = titleView.textColor.withAlpha(alpha)
                titleView.focusTextColor = titleView.focusTextColor.withAlpha(alpha)
            }
        }

    override val value: Vector4f
        get() = base.getColor()

    override fun setValue(newValue: Vector4f, mask: Int, notify: Boolean): Panel {
        val newValue1 = newValue.toARGB()
        previewField.color = if (withAlpha) newValue1
        else newValue1.withAlpha(255)
        base.setRGBA(newValue, mask, false)
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
        if (nameDesc.name.isNotEmpty()) this += titleView
        titleView.enableHoverColor = true
        titleView.disableFocusColors()
        base.setRGBA(oldValue, -1, false)
        base.setChangeRGBListener { r, g, b, a, mask ->
            setValue(Vector4f(r, g, b, a), mask, true)
        }
        tooltip = nameDesc.desc
        name = nameDesc.name
        description = nameDesc.desc
    }

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        if (nameDesc.name.isEmpty()) titleView.calculateSize(w, h)
    }

    fun openColorChooser() {
        base.colorSpace = ColorChooser.getDefaultColorSpace()
        val window = window!!
        val title = NameDesc(nameDesc.name.ifEmpty { "Choose Color" })
        val width = min(max(windowStack.width / 5, 200), windowStack.width)
        Menu.openMenuByPanels(
            window.windowStack, window.mouseXi, window.mouseYi, title, listOf(
                SizeLimitingContainer(base, width, -1, style)
            )
        )
    }

    override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
        when {
            isInputAllowed && button == Key.BUTTON_LEFT -> {
                if (long) openColorChooser()
                else super.onMouseClicked(x, y, button, false)
            }
            isInputAllowed && button == Key.BUTTON_RIGHT -> {
                val window = GFX.activeWindow!!
                Menu.openMenu(
                    windowStack, listOf(
                        MenuOption(NameDesc("Copy")) {
                            Input.copy(window, base)
                        }, MenuOption(NameDesc("Copy #rrggbb")) {
                            Clipboard.setClipboardContent(value.toARGB().toHexColor())
                        }, MenuOption(NameDesc("Copy Vector")) {
                            Clipboard.setClipboardContent(value.toString())
                        }, MenuOption(NameDesc("Paste")) {
                            Input.paste(window, base)
                        }.checkEnabled(),
                        MenuOption(NameDesc("Pick Color")) {
                            pickColor(windowStack, style) { color ->
                                base.setARGB(color, -1, true)
                            }
                        }.checkEnabled(),
                        MenuOption(NameDesc("Reset")) {
                            val newValue = base.resetListener?.invoke() ?: black4
                            setValue(newValue, -1, true)
                        }.checkEnabled()
                    )
                )
            }
            else -> super.onMouseClicked(x, y, button, long)
        }
    }

    private fun MenuOption.checkEnabled(): MenuOption {
        return setEnabled(isInputAllowed, "Input disabled")
    }

    override fun onCopyRequested(x: Float, y: Float): String? {
        return base.onCopyRequested(x, y)
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        return base.onPaste(x, y, data, type)
    }

    override fun onEmpty(x: Float, y: Float) {
        if (isInputAllowed) base.onEmpty(x, y)
        else super.onEmpty(x, y)
    }

    override fun onKeyDown(x: Float, y: Float, key: Key) {
        if (key == Key.BUTTON_LEFT) mouseIsDown = true
        super.onKeyDown(x, y, key)
    }

    override fun onKeyUp(x: Float, y: Float, key: Key) {
        if (key == Key.BUTTON_LEFT) mouseIsDown = false
        super.onKeyUp(x, y, key)
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        super.onMouseMoved(x, y, dx, dy)
        if (mouseIsDown && dragged == null && isInputAllowed) {
            val ws = windowStack
            val speed = 20f * shiftSlowdown / max(ws.width, ws.height)
            val delta = (dx - dy) * speed
            val scaleFactor = 1.10f
            val scale = pow(scaleFactor, delta)
            base.apply {
                if (Input.isControlDown) {
                    setHSL(hue, saturation, lightness * scale, opacity, colorSpace, 4, true)
                } else {
                    setHSL(hue, saturation, lightness, clamp(opacity + delta, 0f, 1f), colorSpace, 8, true)
                }
            }
        }
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        val focused1 = titleView.isInFocus || base.any { it.isInFocus }
        if (focused1) isSelectedListener?.invoke()
        super.draw(x0, y0, x1, y1)
    }

    fun setChangeListener(listener: (r: Float, g: Float, b: Float, a: Float, mask: Int) -> Unit): ColorInput {
        base.setChangeRGBListener { r, g, b, a, mask ->
            if (isInputAllowed) {
                previewField.color = rgba(r, g, b, if (withAlpha) a else 1f)
                listener(r, g, b, a, mask)
            } else LOGGER.warn("Cannot change color")
        }
        return this
    }

    private var isSelectedListener: (() -> Unit)? = null
    fun setIsSelectedListener(listener: () -> Unit): ColorInput {
        isSelectedListener = listener
        return this
    }

    fun setResetListener(listener: (() -> Vector4f)?): ColorInput {
        base.setResetListener(listener)
        return this
    }

    override fun clone(): ColorInput {
        val clone = ColorInput(style)
        copyInto(clone)
        return clone
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyIntoExceptChildren(dst)
        if (dst !is ColorInput) return
        // only works, if there is no references
        dst.isSelectedListener = isSelectedListener
        dst.setResetListener(base.resetListener)
    }

    companion object {
        private val LOGGER = LogManager.getLogger(ColorInput::class)
        fun pickColor(windowStack: WindowStack, style: Style, colorCallback: (Int) -> Unit) {
            // color picker
            // - take screenshot of full screen; all screens? could be hard with multiples in non-regular config...
            // - open (new?) window in fullscreen
            // - add controls on the bottom, or somewhere..., with a preview of the color
            // - select on click, or when dragging + enter then
            val windowX = GFX.someWindow
            addGPUTask("ColorInput.pickColor()", 1) {// delay, so the original menu can disappear
                val screenshot = Screenshots.takeSystemScreenshot()
                val colorPicker = if (screenshot == null) {
                    // correct way up
                    val fb = Framebuffer(
                        "colorPicker", windowStack.width, windowStack.height,
                        1, TargetType.UInt8x4, DepthBufferType.INTERNAL
                    )
                    fb.ensure()
                    useFrame(fb) {
                        windowStack.draw(0, 0, fb.width, fb.height)
                    }
                    val imageData = fb.createImage(flipY = true, withAlpha = false) ?: IntImage(1, 1, false)
                    ColorPicker(fb, fb.getTexture0() as Texture2D, imageData, true, flipTexture = false, style)
                } else {
                    val texture = Texture2D("screenshot", screenshot, false)
                    ColorPicker(null, texture, screenshot, true, flipTexture = true, style)
                }
                var wasFullscreen = false
                fun resetFullscreen() {
                    if (!wasFullscreen && windowX.isFullscreen()) {
                        windowX.toggleFullscreen()
                    }
                }
                colorPicker.callback = { color ->
                    colorCallback(color)
                    resetFullscreen()
                }
                colorPicker.enableControls()
                windowStack.push(colorPicker)
                    .addClosingListener(::resetFullscreen)

                wasFullscreen = windowX.isFullscreen()
                if (!wasFullscreen) windowX.toggleFullscreen()
            }
        }
    }
}