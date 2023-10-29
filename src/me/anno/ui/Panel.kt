package me.anno.ui

import me.anno.config.DefaultConfig
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.Type
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.GFX
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.drawing.DrawRounded.drawRoundedRect
import me.anno.input.Input
import me.anno.input.Key
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.serialization.NotSerializedProperty
import me.anno.maths.Maths
import me.anno.maths.Maths.length
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.constraints.Constraint
import me.anno.ui.base.groups.PanelContainer
import me.anno.ui.base.groups.PanelGroup
import me.anno.ui.base.scrolling.ScrollableX
import me.anno.ui.base.scrolling.ScrollableY
import me.anno.utils.Color.a
import me.anno.utils.Color.black
import me.anno.utils.Tabs
import me.anno.utils.strings.StringHelper.shorten
import me.anno.utils.structures.arrays.ExpandingGenericArray
import me.anno.utils.types.Booleans.toInt
import org.apache.logging.log4j.LogManager
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

open class Panel(val style: Style) : PrefabSaveable() {

    /**
     * Actual size and placement
     * */
    var x = 0
    var y = 0
    var width = 258
    var height = 259

    /**
     * Wished size and placement
     * */
    var minX = 0
    var minY = 0
    var minW = 1
    var minH = 1

    var alignmentX = AxisAlignment.MIN
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }

    var alignmentY = AxisAlignment.MIN
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }

    /**
     * this weight is used inside some layouts
     * it allows layout by percentages and such
     *
     * alignment should be "fill"
     * */
    var weight = 0f
        set(value) {
            if (value.isFinite() && field != value) {
                field = value
                invalidateLayout()
            }
        }

    /**
     * this weight is used inside some 2d layouts
     * it allows layout by percentages and such
     *
     * alignment should be "fill"
     * */
    var weight2 = 0f
        set(value) {
            if (value.isFinite() && field != value) {
                field = value
                invalidateLayout()
            }
        }

    open var isVisible = true
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }

    @NotSerializedProperty
    var window: Window? = null
        get() {
            if (field != null) return field
            field = uiParent?.window
            return field
        }

    val windowStack get() = window!!.windowStack

    val depth: Int get() = 1 + (uiParent?.depth ?: 0)

    fun toggleVisibility() {
        isVisible = !isVisible
        invalidateLayout()
    }

    fun makeBackgroundTransparent() {
        backgroundColor = backgroundColor and 0xffffff
    }

    fun makeBackgroundOpaque() {
        backgroundColor = backgroundColor or black
    }

    fun hide() {
        isVisible = false
    }

    fun show() {
        isVisible = true
    }

    fun withPadding(l: Int, t: Int, r: Int, b: Int) = PanelContainer(this, Padding(l, t, r, b), style)

    // layout
    open fun invalidateLayout() {
        val parent = uiParent
        if (parent == null) {
            window?.addNeedsLayout(this)
        } else parent.invalidateLayout()
    }

    open fun invalidateDrawing() {
        window?.addNeedsRedraw(this)
    }

    open fun onUpdate() {
        if (wasInFocus != isInFocus) {
            invalidateDrawing()
        }
        wasInFocus = isInFocus
        wasHovered = isHovered
    }

    // the following is the last drawn size, clipped stuff clipped
    @NotSerializedProperty
    var lx0 = 0

    @NotSerializedProperty
    var ly0 = 0

    @NotSerializedProperty
    var lx1 = 0

    @NotSerializedProperty
    var ly1 = 0

    @NotSerializedProperty
    var llx0 = lx0

    @NotSerializedProperty
    var lly0 = ly0

    @NotSerializedProperty
    var llx1 = lx1

    @NotSerializedProperty
    var lly1 = ly1

    @NotSerializedProperty
    var oldLayoutState: Any? = null

    @NotSerializedProperty
    var oldVisualState: Any? = null

    @NotSerializedProperty
    var oldStateInt = 0

    // todo mesh or image backgrounds for panels
    // todo if we do it that complicated, maybe create a class for the background?...

    var backgroundOutlineColor = 0
    var backgroundOutlineThickness = 0f
    var backgroundRadius = style.getSize("background.radius", 0f)
    var backgroundRadiusCorners = style.getInt("background.radiusCorners", 15)

    var backgroundColor = style.getColor("background", -1)
        set(value) {
            if (field != value) {
                field = value
                invalidateDrawing()
            }
        }

    val originalBGColor = backgroundColor

    var uiParent: PanelGroup?
        get() = parent as? PanelGroup
        set(value) {
            parent = value
        }

    val layoutConstraints = ArrayList<Constraint>()

    // is updated by Window class
    // should make some computations easier :)
    @DebugProperty
    @NotSerializedProperty
    var canBeSeen = true

    @DebugProperty
    @NotSerializedProperty
    var isInFocus = false

    @DebugProperty
    @NotSerializedProperty
    var wasInFocus = false

    @DebugProperty
    @NotSerializedProperty
    var isAnyChildInFocus = false

    @DebugProperty
    @NotSerializedProperty
    var isHovered = false

    @DebugProperty
    @NotSerializedProperty
    var wasHovered = false

    val rootPanel: Panel get() = uiParent?.rootPanel ?: this

    open val onMovementHideTooltip = true

    @Type("String?")
    var tooltip: String? = null

    @Type("Panel?/SameSceneRef")
    var tooltipPanel: Panel? = null

    open fun getLayoutState(): Any? = null
    open fun getVisualState(): Any? = null

    fun tick() {
        val newLayoutState = getLayoutState()
        if (newLayoutState != oldLayoutState) {
            oldLayoutState = newLayoutState
            oldVisualState = getVisualState()
            invalidateLayout()
        } else {
            val newStateInt = isInFocus.toInt(1) + isHovered.toInt(2) + canBeSeen.toInt(4)
            if (oldStateInt != newStateInt || llx0 != lx0 || lly0 != ly0 || llx1 != lx1 || lly1 != ly1) {
                oldStateInt = newStateInt
                llx0 = lx0
                lly0 = ly0
                llx1 = lx1
                lly1 = ly1
                oldVisualState = getVisualState()
                invalidateDrawing()
            } else {
                val newVisualState = getVisualState()
                if (newVisualState != oldVisualState) {
                    oldVisualState = newVisualState
                    invalidateDrawing()
                }
            }
        }
    }

    open fun requestFocus(exclusive: Boolean = true) = windowStack.requestFocus(this, exclusive)

    val hasRoundedCorners get() = backgroundRadius > 0f && backgroundRadiusCorners != 0

    val siblings get() = uiParent?.children ?: emptyList()

    open val canDrawOverBorders get() = hasRoundedCorners

    open fun drawBackground(x0: Int, y0: Int, x1: Int, y1: Int, dx: Int = 0, dy: Int = dx) {
        // if the children are overlapping, this is incorrect
        // this however, should rarely happen...
        if (backgroundColor.a() > 0 ||
            hasRoundedCorners && backgroundOutlineThickness > 0f && backgroundOutlineColor.a() > 0
        ) {
            if (hasRoundedCorners) {
                val uip = uiParent
                val bg = if (uip == null) black else uip.backgroundColor and 0xffffff
                val th = backgroundOutlineThickness
                val radius = backgroundRadius + th
                val corners = backgroundRadiusCorners
                drawRoundedRect(
                    x + dx, y + dy, width - 2 * dx, height - 2 * dy,
                    if (corners and CORNER_TOP_RIGHT != 0) radius else th,
                    if (corners and CORNER_BOTTOM_RIGHT != 0) radius else th,
                    if (corners and CORNER_TOP_LEFT != 0) radius else th,
                    if (corners and CORNER_BOTTOM_LEFT != 0) radius else th, th,
                    backgroundColor, backgroundOutlineColor, bg,
                    1f
                )
            } else {
                val x2 = max(x0, x + dx)
                val y2 = max(y0, y + dy)
                val x3 = min(x1, x + width - dx)
                val y3 = min(y1, y + height - dy)
                drawRect(x2, y2, x3 - x2, y3 - y2, backgroundColor)
            }
        }
    }

    open fun updateVisibility(mx: Int, my: Int) {
        isInFocus = false
        isAnyChildInFocus = false
        canBeSeen = (uiParent?.canBeSeen != false) && isVisible && lx1 > lx0 && ly1 > ly0
        isHovered = canBeSeen && mx in lx0 until lx1 && my in ly0 until ly1
    }

    fun findMissingParents(parent: PanelGroup? = null) {
        if (parent != null) this.parent = parent
        if (this is PanelGroup) {
            val children = children
            for (i in children.indices) {
                children[i].findMissingParents(this)
            }
        }
    }

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
        GFX.check()
        onDraw(x0, y0, x1, y1)
        GFX.check()
        wasInFocus = isInFocus
    }

    open fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        drawBackground(x0, y0, x1, y1)
    }

    /**
     * add a layout constraint
     * may not be fulfilled by container
     * */
    operator fun plusAssign(c: Constraint) {
        layoutConstraints.add(c)
        layoutConstraints.sortBy { it.order }
    }

    fun setPosSize(x: Int, y: Int, w: Int, h: Int) {
        setSize(w, h)
        this.x = x
        this.y = y
        val constraints = layoutConstraints
        for (i in constraints.indices) {
            constraints[i].apply(this)
        }
        setPosition(this.x, this.y)
    }

    open fun setPosition(x: Int, y: Int) {
        this.x = x
        this.y = y
    }

    open fun setSize(w: Int, h: Int) {
        this.width = w
        this.height = h
    }

    /**
     * sets minW & minH to the minimum size, this panel would like, given the available space
     * */
    open fun calculateSize(w: Int, h: Int) {
        minW = 1
        minH = 1
        // todo why is this required? this should not be needed
        this.width = w
        this.height = h
    }

    /**
     * calculates the placement, given the available space
     * */
    open fun calculatePlacement(x: Int, y: Int, w: Int, h: Int) {
        when (alignmentX) {
            AxisAlignment.FILL -> {
                minX = 0
                minW = w
            }
            else -> {
                minX = x + alignmentX.getOffset(w, this.minW)
            }
        }
        when (alignmentY) {
            AxisAlignment.FILL -> {
                minY = 0
                minH = h
            }
            else -> {
                minY = y + alignmentY.getOffset(h, this.minH)
            }
        }
    }

    fun add(c: Constraint): Panel {
        this += c
        return this
    }

    override fun removeFromParent() {
        uiParent?.remove(this)
    }

    /**
     * event listeners;
     * as functions to be able to cancel parent listeners
     * */

    val onClickListeners = ArrayList<(Panel, Float, Float, Key, Boolean) -> Boolean>()

    fun addOnClickListener(onClickListener: ((panel: Panel, x: Float, y: Float, button: Key, long: Boolean) -> Boolean)): Panel {
        onClickListeners.add(onClickListener)
        return this
    }

    fun addLeftClickListener(onClick: (Panel) -> Unit): Panel {
        addOnClickListener { panel, _, _, mouseButton, isLong ->
            if (mouseButton == Key.BUTTON_LEFT && !isLong) {
                onClick(panel)
                true
            } else false
        }
        return this
    }

    fun addRightClickListener(onClick: (Panel) -> Unit): Panel {
        addOnClickListener { panel, _, _, b, isLong ->
            if (b == Key.BUTTON_RIGHT || isLong) {
                onClick(panel)
                true
            } else false
        }
        return this
    }

    open fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
        for (listener in onClickListeners) {
            if (listener(this, x, y, button, long)) return
        }
        uiParent?.onMouseClicked(x, y, button, long)
    }

    open fun onDoubleClick(x: Float, y: Float, button: Key) {
        for (listener in onClickListeners) {
            if (listener(this, x, y, button, false)) return
        }
        uiParent?.onDoubleClick(x, y, button)
    }

    open fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        uiParent?.onMouseMoved(x, y, dx, dy)
    }

    open fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean) {
        uiParent?.onMouseWheel(x, y, dx, dy, byMouse)
    }

    open fun onKeyDown(x: Float, y: Float, key: Key) {
        uiParent?.onKeyDown(x, y, key)
    }

    open fun onKeyUp(x: Float, y: Float, key: Key) {
        uiParent?.onKeyUp(x, y, key)
    }

    open fun onKeyTyped(x: Float, y: Float, key: Key) {
        uiParent?.onKeyTyped(x, y, key)
    }

    open fun onCharTyped(x: Float, y: Float, codepoint: Int) {
        uiParent?.onCharTyped(x, y, codepoint)
    }

    open fun onEmpty(x: Float, y: Float) {
        uiParent?.onEmpty(x, y)
    }

    open fun onPaste(x: Float, y: Float, data: String, type: String) {
        uiParent?.onPaste(x, y, data, type)
    }

    open fun onPasteFiles(x: Float, y: Float, files: List<FileReference>) {
        uiParent?.onPasteFiles(x, y, files) ?: LOGGER.warn("Paste Ignored! $files, ${this::class.simpleName}")
    }

    open fun onCopyRequested(x: Float, y: Float): Any? = uiParent?.onCopyRequested(x, y)

    open fun onSelectAll(x: Float, y: Float) {
        uiParent?.onSelectAll(x, y)
    }

    /**
     * returns whether the action was handled;
     * actions are handled by me.anno.input.ActionManager, and typically registered on startup
     * */
    open fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean =
        false

    open fun onBackSpaceKey(x: Float, y: Float) {
        uiParent?.onBackSpaceKey(x, y)
    }

    open fun onEnterKey(x: Float, y: Float) {
        uiParent?.onEnterKey(x, y)
    }

    open fun onDeleteKey(x: Float, y: Float) {
        uiParent?.onDeleteKey(x, y)
    }

    open fun onEscapeKey(x: Float, y: Float) {
        uiParent?.onEscapeKey(x, y)
    }

    /**
     * must not be used for important actions, because not all mice have these
     * not all users know about the keys -> default reroute?
     * I like these keys ;)
     * */
    open fun onMouseForwardKey(x: Float, y: Float) {
        uiParent?.onMouseForwardKey(x, y)
    }

    open fun onMouseBackKey(x: Float, y: Float) {
        uiParent?.onMouseBackKey(x, y)
    }

    open fun getCursor(): Long? = uiParent?.getCursor() ?: 0L

    open fun getTooltipPanel(x: Float, y: Float): Panel? = tooltipPanel
    open fun getTooltipText(x: Float, y: Float): String? = tooltip
    fun getTooltipToP(x: Float, y: Float): Any? =
        getTooltipPanel(x, y) ?: getTooltipText(x, y) ?: uiParent?.getTooltipToP(x, y)

    fun setTooltip(tooltipText: String?): Panel {
        tooltip = tooltipText
        tooltipPanel = null
        return this
    }

    open fun printLayout(tabDepth: Int) {
        val tooltip = tooltip
        println(
            "${Tabs.spaces(tabDepth * 2)}$className(${(weight * 10).roundToInt()}, " +
                    "${if (isVisible) "v" else "_"}${if (isHovered) "h" else ""}${if (isInFocus) "F" else ""})) " +
                    "$x-${x + width}, $y-${y + height} ($minW $minH) ${
                        if (tooltip == null) "" else "'${tooltip.shorten(20)}' "
                    }${getPrintSuffix()}"
        )
    }

    open fun getPrintSuffix(): String = "${style.prefix}"

    /**
     * if this returns true, the parent must be drawn for the child to look correct
     * */
    open fun drawsOverlayOverChildren(lx0: Int, ly0: Int, lx1: Int, ly1: Int) =
        capturesChildEvents(lx0, ly0, lx1, ly1) // the default behaviour

    /**
     * if this returns true, the parent must be drawn for the child to look correct
     * */
    fun drawsOverlayOverChildren(x: Int, y: Int) = drawsOverlayOverChildren(x, y, x + 1, y + 1)
    fun drawsOverlayOverChildren() = drawsOverlayOverChildren(lx0, ly0, lx1, ly1)

    open fun capturesChildEvents(lx0: Int, ly0: Int, lx1: Int, ly1: Int) = false

    fun capturesChildEvents(x: Int, y: Int) = capturesChildEvents(x, y, x + 1, y + 1)

    fun getOverlayParent() = getOverlayParent(lx0, ly0, lx1, ly1)
    open fun getOverlayParent(x0: Int, y0: Int, x1: Int, y1: Int): Panel? {
        return uiParent?.getOverlayParent(x0, y0, x1, y1) ?: (
                if (drawsOverlayOverChildren(x0, y0, x1, y1)
                ) this else null)
    }

    /**
     * if this element is in focus,
     * low-priority global events won't be fired (e.g., space for start/stop vs typing a space)
     * */
    open fun isKeyInput() = false
    open fun acceptsChar(char: Int) = true

    open fun scrollTo(x: Int, y: Int) {
        // find parent scroll lists, such that after scrolling, this panel has its center there
        var dx = (this.x + this.width / 2) - x
        var dy = (this.y + this.height / 2) - y
        var par = uiParent
        while (par != null && (dx != 0 || dy != 0)) {
            if (dx != 0 && par is ScrollableX) {
                par.scrollX(dx)
                par.invalidateLayout()
                dx = 0
            }
            if (dy != 0 && par is ScrollableY) {
                par.scrollY(dy)
                par.invalidateLayout()
                dy = 0
            }
            par = par.uiParent
        }
    }

    open fun scrollTo() {
        val window = window ?: return
        scrollTo(window.mouseXi, window.mouseYi)
    }

    fun listOfPanelHierarchy(callback: (Panel) -> Unit) {
        uiParent?.listOfPanelHierarchy(callback)
        callback(this)
    }

    fun listOfPanelHierarchyReversed(callback: (Panel) -> Unit) {
        callback(this)
        uiParent?.listOfPanelHierarchy(callback)
    }

    val listOfPanelHierarchy: Sequence<Panel>
        get() = sequence {
            val p = uiParent
            if (p != null) yieldAll(p.listOfPanelHierarchy)
            yield(this@Panel)
        }

    val listOfPanelHierarchyReversed: Sequence<Panel>
        get() = sequence {
            yield(this@Panel)
            val p = uiParent
            if (p != null) yieldAll(p.listOfPanelHierarchyReversed)
        }

    fun listOfVisible(callback: (Panel) -> Unit) {
        if (canBeSeen) {
            callback(this)
            if (this is PanelGroup) {
                for (child in children) {
                    child.forAllPanels(callback)
                }
            }
        }
    }

    val listOfVisible: Sequence<Panel>
        get() = sequence {
            if (canBeSeen) {
                yield(this@Panel)
                if (this@Panel is PanelGroup) {
                    for (child in this@Panel.children) {
                        yieldAll(child.listOfVisible)
                    }
                }
            }
        }

    fun forAllPanels(callback: (Panel) -> Unit) {
        callback(this)
        if (this is PanelGroup) {
            val children = children
            for (i in children.indices) {
                val child = children[i]
                child.parent = this
                child.forAllPanels(callback)
            }
        }
    }

    open fun forAllVisiblePanels(callback: (Panel) -> Unit) {
        if (canBeSeen) callback(this)
    }

    fun forAllPanels(array: ExpandingGenericArray<Panel>) {
        array += this
        if (this is PanelGroup) {
            for (child in children) {
                child.forAllPanels(array)
            }
        }
    }

    fun firstOfAll(predicate: (Panel) -> Boolean): Panel? {
        if (predicate(this)) return this
        if (this is PanelGroup) {
            for (child in children) {
                val first = child.firstOfAll(predicate)
                if (first != null) return first
            }
        }
        return null
    }

    fun any(predicate: (Panel) -> Boolean): Boolean {
        return firstOfAll(predicate) != null
    }

    fun firstOfHierarchical(filter: (Panel) -> Boolean, predicate: (Panel) -> Boolean): Panel? {
        if (!filter(this)) return null
        if (predicate(this)) return this
        if (this is PanelGroup) {
            for (child in children) {
                val first = child.firstOfAll(predicate)
                if (first != null) return first
            }
        }
        return null
    }

    fun anyHierarchical(filter: (Panel) -> Boolean, predicate: (Panel) -> Boolean): Boolean {
        return firstOfHierarchical(filter, predicate) != null
    }

    override val listOfAll: Sequence<Panel>
        get() = sequence {
            yield(this@Panel)
            if (this@Panel is PanelGroup) {
                for (child in this@Panel.children) {
                    yieldAll(child.listOfAll)
                }
            }
        }

    /**
     * does this panel contain the coordinate (x,y)?
     * does not consider overlap
     * */
    fun contains(x: Int, y: Int, margin: Int = 0) =
        (x - this.x) in -margin until width + margin && (y - this.y) in -margin until height + margin

    /**
     * does this panel contain the coordinate (x,y)?
     * does not consider overlap
     *
     * panels are aligned on full coordinates, so there is no advantage in calling this function
     * */
    fun contains(x: Float, y: Float, margin: Int = 0) = contains(x.toInt(), y.toInt(), margin)

    /**
     * when shift/ctrl clicking on items to select multiples...
     * whose parent is the common parent?
     *
     * isn't really meant to be concatenated as a function
     * (multiselect inside multiselect)
     *
     * return **this**, if your panel is multi-selectable
     * */
    open fun getMultiSelectablePanel(): Panel? = uiParent?.getMultiSelectablePanel()

    open fun isOpaqueAt(x: Int, y: Int): Boolean {
        return backgroundColor.a() >= minOpaqueAlpha && if (hasRoundedCorners) {
            val cornerMasks = ((x - this.x) * 2 < this.width).toInt(2) + ((y - this.y) * 2 > this.height).toInt()
            if ((1 shl cornerMasks) and backgroundRadiusCorners != 0) {
                val px = ((x - this.x) * 2 - this.width)
                val py = ((y - this.y) * 2 - this.height)
                val r = backgroundRadius * 2
                val qx = abs(px) - this.width + r
                val qy = abs(py) - this.height + r
                // println("$x,$y -> $px,$py -> $qx,$qy -> ${length(max(qx, 0f),max(qy, 0f)) + min(0f, max(qx, qy)) - r}")
                length(max(qx, 0f), max(qy, 0f)) + min(0f, max(qx, qy)) - r <= 0
            } else contains(x, y)
        } else contains(x, y)
    }

    open fun getPanelAt(x: Int, y: Int): Panel? {
        return if (canBeSeen && contains(x, y) && isOpaqueAt(x, y)) this else null
    }

    open fun onPropertiesChanged() {}

    val isRootElement get() = uiParent == null

    /**
     * traps/locks the cursor to the window;
     * don't forget to unlock it again
     *
     * Unity: Cursor.lockState
     * Unreal: Mouse Lock Mode
     *
     * @return true on success
     */
    fun lockMouse(): Boolean {
        val window = GFX.focusedWindow
        return if (window != null) {
            Input.mouseLockWindow = window
            Input.mouseLockPanel = this
            true
        } else false
    }

    fun unlockMouse() {
        Input.unlockMouse()
    }

    override fun clone(): Panel {
        val clone = Panel(style)
        copyInto(clone)
        return clone
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as Panel
        dst.minX = minX
        dst.minY = minY
        dst.x = x
        dst.y = y
        dst.width = width
        dst.height = height
        dst.tooltip = tooltip
        dst.tooltipPanel = dst.tooltipPanel // could create issues, should be found in parent or cloned
        dst.weight = weight
        dst.isVisible = isVisible
        dst.backgroundColor = backgroundColor
        dst.backgroundRadiusCorners = backgroundRadiusCorners
        dst.backgroundRadius = backgroundRadius
        dst.layoutConstraints.clear()
        dst.layoutConstraints.addAll(layoutConstraints.map { it.clone() })
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "x" -> x = value
            "y" -> y = value
            "w" -> width = value
            "h" -> height = value
            "minW" -> minW = value
            "minH" -> minH = value
            "visibility" -> isVisible = value != 0
            "alignmentX" -> alignmentX = AxisAlignment.find(value) ?: alignmentX
            "alignmentY" -> alignmentY = AxisAlignment.find(value) ?: alignmentY
            "background" -> backgroundColor = value
            "backgroundOutline" -> backgroundOutlineColor = value
            else -> super.readInt(name, value)
        }
    }

    override fun readFloat(name: String, value: Float) {
        when (name) {
            "weight" -> weight = value
            "backgroundRadius" -> backgroundRadius = value
            "backgroundOutlineThickness" -> backgroundOutlineThickness = value
            else -> super.readFloat(name, value)
        }
    }

    override fun readString(name: String, value: String?) {
        if (name == "tooltip") tooltip = value
        else super.readString(name, value)
    }

    override fun readObject(name: String, value: ISaveable?) {
        if (name == "tooltipPanel") tooltipPanel = value as? Panel
        else super.readObject(name, value)
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeInt("x", x)
        writer.writeInt("y", y)
        writer.writeInt("w", width)
        writer.writeInt("h", height)
        writer.writeInt("minW", minW)
        writer.writeInt("minH", minH)
        writer.writeEnum("alignmentX", alignmentX)
        writer.writeEnum("alignmentY", alignmentY)
        // to do save this stuff somehow, maybe...
        // writer.writeObjectList(this, "clickListeners", clickListeners)
        writer.writeObjectList(this, "layoutConstraints", layoutConstraints)
        writer.writeFloat("weight", weight)
        writer.writeBoolean("visibility", isVisible)
        writer.writeColor("background", backgroundColor)
        writer.writeInt("backgroundRadiusCorners", backgroundRadiusCorners)
        writer.writeColor("backgroundOutline", backgroundOutlineColor)
        writer.writeFloat("backgroundRadius", backgroundRadius)
        writer.writeFloat("backgroundOutlineThickness", backgroundOutlineThickness)
        writer.writeString("tooltip", tooltip)
        writer.writeObject(this, "tooltipPanel", tooltipPanel)
    }

    override val className: String get() = "Panel"

    companion object {

        const val CORNER_TOP_RIGHT = 1
        const val CORNER_BOTTOM_RIGHT = 2
        const val CORNER_TOP_LEFT = 4
        const val CORNER_BOTTOM_LEFT = 8

        private val LOGGER = LogManager.getLogger(Panel::class)
        val interactionPadding get() = Maths.max(0, DefaultConfig["ui.interactionPadding", 6])
        val minOpaqueAlpha = 63
    }
}