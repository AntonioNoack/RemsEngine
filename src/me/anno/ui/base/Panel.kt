package me.anno.ui.base

import me.anno.gpu.GFX
import me.anno.gpu.TextureLib.whiteTexture
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.texture.Texture2D
import me.anno.input.Input
import me.anno.input.MouseButton
import me.anno.io.Saveable
import me.anno.ui.base.groups.PanelGroup
import me.anno.ui.style.Style
import me.anno.utils.Tabs
import org.lwjgl.opengl.GL11.*
import java.io.File
import java.lang.RuntimeException

// todo select any group of elements similar to control+click by shift+drag

open class Panel(val style: Style) : Saveable() {

    var minW = 1
    var minH = 1

    var visibility = Visibility.VISIBLE

    // todo make layout become valid/invalid, so we can save some comp. resources
    var hasValidLayout = false

    var cachedVisuals = Framebuffer("panel", 1, 1, 1, 1, false,
        Framebuffer.DepthBufferType.NONE)
    var renderOnRequestOnly = false

    /**
     * this weight is used inside some layouts
     * it allows layout by percentages and such
     * */
    var weight = 0f

    var backgroundColor = style.getColor("background", -1)
    val originalBGColor = backgroundColor

    var parent: PanelGroup? = null
    val layoutConstraints = ArrayList<Constraint>()

    var w = 258
    var h = 259
    var x = 0
    var y = 0

    val isInFocus get() = this in GFX.inFocus
    val canBeSeen get() = canBeSeen(0, 0, GFX.width, GFX.height)
    val canBeSeenCurrently get() = canBeSeen(GFX.windowX, GFX.windowY, GFX.windowWidth, GFX.windowHeight)
    val isHovered get() = Input.mouseX.toInt() - x in 0 until w && Input.mouseY.toInt() - y in 0 until h
    val rootPanel: Panel get() = parent?.rootPanel ?: this
    fun canBeSeen(x0: Int, y0: Int, w0: Int, h0: Int): Boolean {
        return x + w > x0 && y + h > y0 && x < x0 + w0 && y < y0 + h0
    }

    fun invalidateLayout() {
        if (hasValidLayout) {
            hasValidLayout = false
            parent?.invalidateLayout()
        }
    }

    var tooltip: String? = null
    val isVisible get() = visibility == Visibility.VISIBLE && canBeSeen

    fun requestFocus() = GFX.requestFocus(this, true)

    fun drawBackground() {
        GFX.drawRect(x, y, w, h, backgroundColor)
    }

    /**
     * draw the panel inside the rectangle (x0 until x1, y0 until y1)
     * more does not need to be drawn;
     * the area is already clipped with glViewport(x0,y0,x1-x0,y1-y0)
     * */
    fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        if (renderOnRequestOnly) {
            // todo this somehow is not working :/
            val w = w
            val h = h
            if (cachedVisuals.w != w || cachedVisuals.h != h || true) {
                // update drawn stuff
                cachedVisuals.bind(w, h)
                val x = y
                val y = y
                this.x = 0
                this.y = 0
                GFX.clip(0, 0, w, h)
                // glClearColor(Math.random().toFloat(), 0f, 0f, 1f)
                // glClear(GL_COLOR_BUFFER_BIT)
                onDraw(0, 0, w - 1, h - 1)
                this.x = x
                this.y = y
                cachedVisuals.unbind()
                GFX.clip2(x0, y0, x1, y1)
            }
            // draw cached result
            GFX.drawTexture(x, y, w, h, cachedVisuals.textures[0], -1, null)
        } else {
            onDraw(x0, y0, x1, y1)
        }
    }

    open fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        drawBackground()
    }

    /**
     * weight used in layouts, that work with weights
     * */
    fun setWeight(w: Float): Panel {
        weight = w
        return this
    }

    /**
     * add a layout constraint
     * may not be fulfilled by container
     * */
    operator fun plusAssign(c: Constraint) {
        layoutConstraints += c
        layoutConstraints.sortBy { it.order }
    }

    fun assert(b: Boolean, msg: String?) {
        if (!b) throw RuntimeException(msg)
    }

    open fun placeInParent(x: Int, y: Int) {
        this.x = x
        this.y = y
    }

    /*open fun applyConstraints(){
        for(c in layoutConstraints){
            c.apply(this)
        }
    }*/

    open fun applyPlacement(w: Int, h: Int) {
        //this.minW = w
        //this.minH = h
        this.w = w
        this.h = h
        for (c in layoutConstraints) {
            c.apply(this)
            if (this.w > w || this.h > h) throw RuntimeException("${c.javaClass} isn't working properly: $w -> ${this.w}, $h -> ${this.h}")
        }
    }

    open fun calculateSize(w: Int, h: Int) {
        this.w = w
        this.h = h
    }

    fun add(c: Constraint): Panel {
        this += c
        return this
    }

    fun removeFromParent() {
        parent?.remove(this)
    }

    /**
     * event listeners;
     * as functions to be able to cancel parent listeners
     * */

    var onClickListener: ((Float, Float, MouseButton, Boolean) -> Unit)? = null
    fun setOnClickListener(onClickListener: ((x: Float, y: Float, button: MouseButton, long: Boolean) -> Unit)): Panel {
        this.onClickListener = onClickListener
        return this
    }

    fun setSimpleClickListener(onClick: () -> Unit): Panel {
        onClickListener = { _, _, b, _ ->
            if (b.isLeft) onClick()
        }
        return this
    }

    open fun onMouseDown(x: Float, y: Float, button: MouseButton) {
        parent?.onMouseDown(x, y, button)
    }

    open fun onMouseUp(x: Float, y: Float, button: MouseButton) {
        parent?.onMouseUp(x, y, button)
    }

    open fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
        onClickListener?.invoke(x, y, button, long) ?: parent?.onMouseClicked(x, y, button, long)
    }

    open fun onDoubleClick(x: Float, y: Float, button: MouseButton) {
        parent?.onDoubleClick(x, y, button)
    }

    open fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        parent?.onMouseMoved(x, y, dx, dy)
    }

    open fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float) {
        parent?.onMouseWheel(x, y, dx, dy)
    }

    open fun onKeyDown(x: Float, y: Float, key: Int) {
        parent?.onKeyDown(x, y, key)
    }

    open fun onKeyUp(x: Float, y: Float, key: Int) {
        parent?.onKeyUp(x, y, key)
    }

    open fun onKeyTyped(x: Float, y: Float, key: Int) {
        parent?.onKeyTyped(x, y, key)
    }

    open fun onCharTyped(x: Float, y: Float, key: Int) {
        parent?.onCharTyped(x, y, key)
    }

    open fun onEmpty(x: Float, y: Float) {
        parent?.onEmpty(x, y)
    }

    open fun onPaste(x: Float, y: Float, data: String, type: String) {
        parent?.onPaste(x, y, data, type)
    }

    open fun onPasteFiles(x: Float, y: Float, files: List<File>) {
        parent?.onPasteFiles(x, y, files) ?: println("Paste Ignored! $files")
    }

    open fun onCopyRequested(x: Float, y: Float): String? = parent?.onCopyRequested(x, y)

    open fun onSelectAll(x: Float, y: Float) {
        parent?.onSelectAll(x, y)
    }

    open fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean =
        false

    open fun onBackSpaceKey(x: Float, y: Float) {
        parent?.onBackSpaceKey(x, y)
    }

    open fun onEnterKey(x: Float, y: Float) {
        parent?.onEnterKey(x, y)
    }

    open fun onDeleteKey(x: Float, y: Float) {
        parent?.onDeleteKey(x, y)
    }

    /**
     * must not be used for important actions, because not all mice have these
     * not all users know about the keys -> default reroute?
     * I like these keys ;)
     * */
    open fun onMouseForwardKey(x: Float, y: Float) {
        parent?.onMouseForwardKey(x, y)
    }

    open fun onMouseBackKey(x: Float, y: Float) {
        parent?.onMouseBackKey(x, y)
    }

    /**
     * for serialization and easier runtime debugging
     * (print key -> prints layout with Panel.printLayout())
     * */
    override fun getClassName(): String = "Panel"
    override fun getApproxSize(): Int = 1

    open fun getCursor(): Long? = parent?.getCursor() ?: 0L

    open fun getTooltipText(x: Float, y: Float): String? = tooltip ?: parent?.getTooltipText(x, y)

    fun setTooltip(tooltipText: String?): Panel {
        tooltip = tooltipText
        return this
    }

    open fun printLayout(tabDepth: Int) {
        println("${Tabs.spaces(tabDepth * 2)}${javaClass.simpleName}($weight) $x $y += $w $h ($minW $minH)")
    }

    /**
     * part of serialization; isn't used currently
     * */
    override fun isDefaultValue() = false

    /**
     * if this element is in focus,
     * low-priority global events wont be fired (e.g. space for start/stop vs typing a space)
     * */
    open fun isKeyInput() = false

    val listOfAll: Sequence<Panel>
        get() = sequence {
            yield(this@Panel)
            (this@Panel as? PanelGroup)?.children?.forEach { child ->
                yieldAll(child.listOfAll)
            }
        }

    /**
     * does this panel contain the coordinate (x,y)?
     * does not consider overlap
     * */
    fun contains(x: Int, y: Int, margin: Int = 0) =
        (x - this.x) in -margin until w + margin && (y - this.y) in -margin until h + margin

    /**
     * does this panel contain the coordinate (x,y)?
     * does not consider overlap
     *
     * panels are aligned on full coordinates, so there is no advantange in calling this function
     * */
    fun contains(x: Float, y: Float, margin: Int = 0) = contains(x.toInt(), y.toInt(), margin)

    /**
     * when shift/ctrl clicking on items to select multiples...
     * which parent is the common parent?
     *
     * isn't really meant to be concatenated as a function
     * (multiselect inside multiselect)
     * */
    open fun getMultiSelectablePanel(): Panel? = parent?.getMultiSelectablePanel()

    /**
     * get the index in our parent; or -1, if we have no parent (are the root element)
     * */
    val indexInParent get() = parent?.children?.indexOf(this) ?: -1
    val isRootElement get() = parent == null

}