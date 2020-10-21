package me.anno.ui.base

import me.anno.gpu.GFX
import me.anno.gpu.Window
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.input.MouseButton
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.Constraint
import me.anno.ui.base.groups.PanelContainer
import me.anno.ui.base.groups.PanelGroup
import me.anno.ui.state.Rect
import me.anno.ui.style.Style
import me.anno.utils.LOGGER
import me.anno.utils.Tabs
import java.io.File

open class Panel(val style: Style) {

    var minW = 1
    var minH = 1

    val depth: Int get() = 1 + (parent?.depth ?: 0)

    var visibility = Visibility.VISIBLE
    var window: Window? = null
        get() {
            if (field != null) return field
            field = parent?.window
            return field
        }

    fun toggleVisibility() {
        visibility = if (visibility == Visibility.VISIBLE) Visibility.GONE else Visibility.VISIBLE
    }

    fun hide() {
        visibility = Visibility.GONE
    }

    fun show() {
        visibility = Visibility.VISIBLE
    }

    fun withPadding(l: Int, t: Int, r: Int, b: Int) = PanelContainer(this, Padding(l, t, r, b), style)

    // layout
    open fun invalidateLayout() {
        parent?.invalidateLayout() ?: {
            val window = window
                ?: throw RuntimeException("${javaClass.simpleName} is missing parent, state: $oldLayoutState/$oldVisualState")
            window.needsLayout += this
        }()
    }

    open fun invalidateDrawing() {
        val window = window
            ?: throw RuntimeException("${javaClass.simpleName} is missing parent, state: $oldLayoutState/$oldVisualState")
        window.needsRedraw += this
    }

    open fun tickUpdate() {}
    open fun getLayoutState(): Any? = Rect(lx0, ly0, lx1, ly1)
    open fun getVisualState(): Any? = backgroundColor

    var oldLayoutState: Any? = null
    var oldVisualState: Any? = null

    fun tick() {
        val newLayoutState = getLayoutState()
        if (newLayoutState != oldLayoutState) {
            oldLayoutState = newLayoutState
            oldVisualState = getVisualState()
            invalidateLayout()
        } else {
            val newVisualState = getVisualState()
            if (newVisualState != oldVisualState) {
                oldVisualState = newVisualState
                invalidateDrawing()
            }
        }
    }

    var cachedVisuals = Framebuffer(
        "panel", 1, 1, 1, 1, false,
        Framebuffer.DepthBufferType.NONE
    )
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

    // is updated by StudioBase
    // should make some computations easier :)
    var canBeSeen = true
    var isInFocus = false
    var isHovered = false

    val rootPanel: Panel get() = parent?.rootPanel ?: this

    var tooltip: String? = null

    fun requestFocus() = GFX.requestFocus(this, true)

    fun drawBackground() {
        // if the children are overlapping, this is incorrect
        // this however, should rarely happen...
        GFX.drawRect(x, y, w, h, backgroundColor)
        /*if(parent?.backgroundColor != backgroundColor){
            GFX.drawRect(x, y, w, h, backgroundColor)
        }*/
    }

    var lx0 = 0
    var ly0 = 0
    var lx1 = 0
    var ly1 = 0

    /**
     * draw the panel at its last location and size
     * */
    fun redraw() {
        draw(lx0, ly0, lx1, ly1)
    }

    /**
     * draw the panel inside the rectangle (x0 until x1, y0 until y1)
     * more does not need to be drawn;
     * the area is already clipped with glViewport(x0,y0,x1-x0,y1-y0)
     * */
    fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        lx0 = x0
        ly0 = y0
        lx1 = x1
        ly1 = y1
        onDraw(x0, y0, x1, y1)
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

    fun place(x: Int, y: Int, w: Int, h: Int) {
        placeInParent(x, y)
        applyPlacement(w, h)
        placeInParent(this.x, this.y)
    }

    open fun placeInParent(x: Int, y: Int) {
        this.x = x
        this.y = y
    }

    open fun applyPlacement(w: Int, h: Int) {
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
        parent?.onPasteFiles(x, y, files) ?: LOGGER.warn("Paste Ignored! $files, ${javaClass.simpleName}")
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

    open fun onEscapeKey(x: Float, y: Float){
        parent?.onEscapeKey(x, y)
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

    open fun getCursor(): Long? = parent?.getCursor() ?: 0L

    open fun getTooltipText(x: Float, y: Float): String? = tooltip ?: parent?.getTooltipText(x, y)

    fun setTooltip(tooltipText: String?): Panel {
        tooltip = tooltipText
        return this
    }

    open fun printLayout(tabDepth: Int) {
        println("${Tabs.spaces(tabDepth * 2)}${javaClass.simpleName}($weight, ${if (visibility == Visibility.VISIBLE) "v" else "_"})) $x $y += $w $h ($minW $minH) ${style.prefix}")
    }

    open fun drawsOverlaysOverChildren(lx0: Int, ly0: Int, lx1: Int, ly1: Int) = false

    // first or null would be correct, however our overlays are all the same
    // (the small cross, which should be part of the ui instead)
    //, so we can use the last one
    open fun getOverlayParent() = listOfHierarchy.lastOrNull { it.drawsOverlaysOverChildren(lx0, ly0, lx1, ly1) }

    /**
     * if this element is in focus,
     * low-priority global events wont be fired (e.g. space for start/stop vs typing a space)
     * */
    open fun isKeyInput() = false
    open fun acceptsChar(char: Int) = true

    val listOfHierarchy: Sequence<Panel>
        get() = sequence {
            parent?.apply {
                yieldAll(listOfHierarchy)
            }
            yield(this@Panel)
        }

    val listOfVisible: Sequence<Panel>
        get() = sequence {
            if (canBeSeen) {
                yield(this@Panel)
                if (this@Panel is PanelGroup) {
                    this@Panel.children.forEach { child ->
                        yieldAll(child.listOfAll)
                    }
                }
            }
        }

    val listOfAll: Sequence<Panel>
        get() = sequence {
            yield(this@Panel)
            if (this@Panel is PanelGroup) {
                this@Panel.children.forEach { child ->
                    yieldAll(child.listOfAll)
                }
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

    open fun getClassName() = javaClass.simpleName

    /*companion object {
        private val LOGGER = LogManager.getLogger(Panel::class)
    }*/

}