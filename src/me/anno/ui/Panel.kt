package me.anno.ui

import me.anno.config.DefaultConfig
import me.anno.config.DefaultStyle.black
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.Type
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.GFX
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.input.MouseButton
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.serialization.NotSerializedProperty
import me.anno.maths.Maths
import me.anno.ui.base.Visibility
import me.anno.ui.base.components.Corner.drawRoundedRect
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.constraints.Constraint
import me.anno.ui.base.groups.PanelContainer
import me.anno.ui.base.groups.PanelGroup
import me.anno.ui.base.scrolling.ScrollableX
import me.anno.ui.base.scrolling.ScrollableY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.editor.files.Search
import me.anno.ui.style.Style
import me.anno.utils.Color.a
import me.anno.utils.Tabs
import me.anno.utils.strings.StringHelper.shorten
import me.anno.utils.structures.arrays.ExpandingGenericArray
import me.anno.utils.types.Booleans.toInt
import org.apache.logging.log4j.LogManager
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

open class Panel(val style: Style) : PrefabSaveable() {

    // wished size and placement
    var minX = 0
    var minY = 0
    var minW = 1
    var minH = 1

    val depth: Int get() = 1 + (uiParent?.depth ?: 0)

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
    open var weight = 0f
        set(value) {
            if (value.isFinite() && field != value) {
                field = value
                invalidateLayout()
            }
        }

    open var visibility = Visibility.VISIBLE
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

    fun toggleVisibility() {
        visibility = if (visibility == Visibility.VISIBLE) Visibility.GONE else Visibility.VISIBLE
        invalidateLayout()
    }

    fun makeBackgroundTransparent() {
        backgroundColor = backgroundColor and 0xffffff
    }

    fun makeBackgroundOpaque() {
        backgroundColor = backgroundColor or black
    }

    fun hide() {
        if (visibility != Visibility.GONE) {
            visibility = Visibility.GONE
            invalidateLayout()
        }
    }

    fun show() {
        if (visibility != Visibility.VISIBLE) {
            visibility = Visibility.VISIBLE
            invalidateLayout()
        }
    }

    fun withPadding(l: Int, t: Int, r: Int, b: Int) = PanelContainer(this, Padding(l, t, r, b), style)

    // layout
    open fun invalidateLayout() {
        val parent = uiParent
        if (parent == null) {
            val window = window
            window?.needsLayout?.add(this)
        } else parent.invalidateLayout()
    }

    open fun invalidateDrawing() {
        val window = window
        window?.addNeedsRedraw(this)
    }

    open fun tickUpdate() {
        if (wasInFocus != isInFocus) {
            invalidateDrawing()
        }
        wasInFocus = isInFocus
    }

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

    var backgroundOutlineColor = 0
    var backgroundOutlineThickness = 0f
    var backgroundRadius = style.getSize("background.radius", 0f)
    var backgroundRadiusCorners = style.getInt("background.radiusSides", 15)

    var backgroundColor = style.getColor("background", -1)
        set(value) {
            if (field != value) {
                field = value
                invalidateDrawing()
            }
        }

    val originalBGColor = backgroundColor

    val uiParent: PanelGroup?
        get() = parent as? PanelGroup

    val layoutConstraints = ArrayList<Constraint>()

    var w = 258
    var h = 259
    var x = 0
    var y = 0

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
    var isAnyChildInFocus = false

    @DebugProperty
    @NotSerializedProperty
    var isHovered = false

    @DebugProperty
    @NotSerializedProperty
    var wasInFocus = false

    val rootPanel: Panel get() = uiParent?.rootPanel ?: this

    open val onMovementHideTooltip = true

    @Type("String?")
    var tooltip: String? = null

    @Type("Panel?/PrefabSaveable")
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

    fun drawBackground(x0: Int, y0: Int, x1: Int, y1: Int, dx: Int = 0, dy: Int = dx) {
        // if the children are overlapping, this is incorrect
        // this however, should rarely happen...
        if (backgroundColor.a() > 0) {
            if (hasRoundedCorners) {
                val uip = uiParent
                val bg = if (uip == null) 0 else uip.backgroundColor and 0xffffff
                val radius = backgroundRadius
                drawRoundedRect(
                    x + dx, y + dy, w - 2 * dx, h - 2 * dy,
                    if (backgroundRadiusCorners and 1 != 0) radius else 0f,
                    if (backgroundRadiusCorners and 2 != 0) radius else 0f,
                    if (backgroundRadiusCorners and 4 != 0) radius else 0f,
                    if (backgroundRadiusCorners and 8 != 0) radius else 0f,
                    backgroundOutlineThickness,
                    backgroundColor, backgroundOutlineColor, bg,
                    1f
                )
            } else {
                val x2 = max(x0, x + dx)
                val y2 = max(y0, y + dy)
                val x3 = min(x1, x + w - dx)
                val y3 = min(y1, y + h - dy)
                drawRect(x2, y2, x3 - x2, y3 - y2, backgroundColor)
            }
        }
    }

    fun updateVisibility(mx: Int, my: Int) {
        isInFocus = false
        isAnyChildInFocus = false
        canBeSeen = (uiParent?.canBeSeen != false) &&
                visibility == Visibility.VISIBLE &&
                lx1 > lx0 && ly1 > ly0
        isHovered = mx in lx0 until lx1 && my in ly0 until ly1
        if (this is PanelGroup) {
            val children = children
            for (i in children.indices) {
                children[i].updateVisibility(mx, my)
            }
        }
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
        layoutConstraints.add(c)
        layoutConstraints.sortBy { it.order }
    }

    fun assert(b: Boolean, msg: String?) {
        if (!b) throw RuntimeException(msg)
    }

    fun setPosSize(x: Int, y: Int, w: Int, h: Int) {
        setPosition(x, y)
        setSize(w, h)
        // todo why is the menu-window depending on this?
        if (true || this.x != x || this.y != y) {
            setPosition(this.x, this.y)
        } // else is already placed
    }

    open fun setPosition(x: Int, y: Int) {
        this.x = x
        this.y = y
    }

    open fun setSize(w: Int, h: Int) {
        this.w = w
        this.h = h
        val constraints = layoutConstraints
        for (i in constraints.indices) {
            val c = constraints[i]
            c.apply(this)
            if (this.w > w || this.h > h)
                throw RuntimeException("Constraint ${c.javaClass} isn't working properly: $w -> ${this.w}, $h -> ${this.h}")
        }
    }

    /**
     * sets minW & minH to the minimum size, this panel would like, given the available space
     * */
    open fun calculateSize(w: Int, h: Int) {
        minW = 1
        minH = 1
        // todo why is this required? this should not be needed
        this.w = w
        this.h = h
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
            else -> x + alignmentX.getOffset(w, this.minW)
        }
        when (alignmentY) {
            AxisAlignment.FILL -> {
                minY = 0
                minH = h
            }
            else -> y + alignmentY.getOffset(h, this.minH)
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

    val onClickListeners = ArrayList<(Float, Float, MouseButton, Boolean) -> Boolean>()

    fun addOnClickListener(onClickListener: ((x: Float, y: Float, button: MouseButton, long: Boolean) -> Boolean)): Panel {
        onClickListeners.add(onClickListener)
        return this
    }

    fun addLeftClickListener(onClick: () -> Unit): Panel {
        addOnClickListener { _, _, mouseButton, isLong ->
            if (mouseButton.isLeft && !isLong) {
                onClick()
                true
            } else false
        }
        return this
    }

    fun addRightClickListener(onClick: () -> Unit): Panel {
        addOnClickListener { _, _, b, isLong ->
            if (b.isRight || isLong) {
                onClick()
                true
            } else false
        }
        return this
    }

    open fun onMouseDown(x: Float, y: Float, button: MouseButton) {
        uiParent?.onMouseDown(x, y, button)
    }

    open fun onMouseUp(x: Float, y: Float, button: MouseButton) {
        uiParent?.onMouseUp(x, y, button)
    }

    open fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
        for (listener in onClickListeners) {
            if (listener(x, y, button, long)) return
        }
        uiParent?.onMouseClicked(x, y, button, long)
    }

    open fun onDoubleClick(x: Float, y: Float, button: MouseButton) {
        for (l in onClickListeners) {
            if (l(x, y, button, false)) return
        }
        uiParent?.onDoubleClick(x, y, button)
    }

    open fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        uiParent?.onMouseMoved(x, y, dx, dy)
    }

    open fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean) {
        uiParent?.onMouseWheel(x, y, dx, dy, byMouse)
    }

    open fun onKeyDown(x: Float, y: Float, key: Int) {
        uiParent?.onKeyDown(x, y, key)
    }

    open fun onKeyUp(x: Float, y: Float, key: Int) {
        uiParent?.onKeyUp(x, y, key)
    }

    open fun onKeyTyped(x: Float, y: Float, key: Int) {
        uiParent?.onKeyTyped(x, y, key)
    }

    open fun onCharTyped(x: Float, y: Float, key: Int) {
        uiParent?.onCharTyped(x, y, key)
    }

    open fun onEmpty(x: Float, y: Float) {
        uiParent?.onEmpty(x, y)
    }

    open fun onPaste(x: Float, y: Float, data: String, type: String) {
        uiParent?.onPaste(x, y, data, type)
    }

    open fun onPasteFiles(x: Float, y: Float, files: List<FileReference>) {
        uiParent?.onPasteFiles(x, y, files) ?: LOGGER.warn("Paste Ignored! $files, ${javaClass.simpleName}")
    }

    open fun onCopyRequested(x: Float, y: Float): Any? = uiParent?.onCopyRequested(x, y)

    open fun onSelectAll(x: Float, y: Float) {
        uiParent?.onSelectAll(x, y)
    }

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
    open fun getTooltipText(x: Float, y: Float): String? = tooltip ?: uiParent?.getTooltipText(x, y)

    fun setTooltip(tooltipText: String?): Panel {
        tooltip = tooltipText
        return this
    }

    open fun printLayout(tabDepth: Int) {
        val tooltip = tooltip
        println(
            "${Tabs.spaces(tabDepth * 2)}${javaClass.simpleName}(${(weight * 10).roundToInt()}, " +
                    "${if (visibility == Visibility.VISIBLE) "v" else "_"})) " +
                    "$x-${x + w}, $y-${y + h} ($minW $minH) ${
                        if (tooltip == null) "" else "'${tooltip.shorten(20)}' "
                    }${getPrintSuffix()}"
        )
    }

    open fun getPrintSuffix(): String = "${style.prefix}"

    open fun drawsOverlayOverChildren(lx0: Int, ly0: Int, lx1: Int, ly1: Int) =
        capturesChildEvents(lx0, ly0, lx1, ly1) // the default behaviour

    fun drawsOverlayOverChildren(x: Int, y: Int) =
        drawsOverlayOverChildren(x, y, x + 1, y + 1)

    open fun capturesChildEvents(lx0: Int, ly0: Int, lx1: Int, ly1: Int) = false

    fun capturesChildEvents(x: Int, y: Int) = capturesChildEvents(x, y, x + 1, y + 1)

    // todo overlays don't work perfectly: the cross over the scrollbar is blinking when regularly redrawing...
    // first or null would be correct, however our overlays are all the same
    // (the small cross, which should be part of the ui instead)
    //, so we can use the last one
    open fun getOverlayParent(): Panel? {
        return uiParent?.getOverlayParent() ?: (if (drawsOverlayOverChildren(lx0, ly0, lx1, ly1)) this else null)
    }

    open fun getOverlayParent(x0: Int, y0: Int, x1: Int, y1: Int): Panel? {
        return uiParent?.getOverlayParent(x0, y0, x1, y1) ?: (if (drawsOverlayOverChildren(
                x0,
                y0,
                x1,
                y1
            )
        ) this else null)
    }

    /**
     * if this element is in focus,
     * low-priority global events wont be fired (e.g. space for start/stop vs typing a space)
     * */
    open fun isKeyInput() = false
    open fun acceptsChar(char: Int) = true

    open fun scrollTo(x: Int, y: Int) {
        // find parent scroll lists, such that after scrolling, this panel has it's center there
        var dx = (this.x + this.w / 2) - x
        var dy = (this.y + this.h / 2) - y
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
        val window = window
        if (window == null) {
            LOGGER.warn("Window of $this is null")
        } else scrollTo(window.mouseXi, window.mouseYi)
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
                    this@Panel.children.forEach { child ->
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

    fun forAllVisiblePanels(callback: (Panel) -> Unit) {
        if (canBeSeen) {
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

    override val listOfAll: Sequence<Panel>
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
     * panels are aligned on full coordinates, so there is no advantage in calling this function
     * */
    fun contains(x: Float, y: Float, margin: Int = 0) = contains(x.toInt(), y.toInt(), margin)

    /**
     * when shift/ctrl clicking on items to select multiples...
     * which parent is the common parent?
     *
     * isn't really meant to be concatenated as a function
     * (multiselect inside multiselect)
     * */
    open fun getMultiSelectablePanel(): Panel? = uiParent?.getMultiSelectablePanel()

    open fun isOpaqueAt(x: Int, y: Int): Boolean {
        // todo check rounded corners
        return backgroundColor.a() >= minOpaqueAlpha
    }

    open fun getPanelAt(x: Int, y: Int): Panel? {
        return if (canBeSeen && contains(x, y) && isOpaqueAt(x, y)) this else null
    }

    open fun onPropertiesChanged() {}

    fun fulfillsSearch(search: Search): Boolean {
        // join all text (below a certain limit), and search that
        // could be done more efficient
        val joined = StringBuilder()
        forAllPanels { panel ->
            if (panel is TextPanel) {
                joined.append(panel.text)
                joined.append(' ')
            }
        }
        return search.matches(joined.toString())
    }

    val isRootElement get() = uiParent == null

    override fun clone(): Panel {
        val clone = Panel(style)
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as Panel
        clone.minX = minX
        clone.minY = minY
        clone.x = x
        clone.y = y
        clone.w = w
        clone.h = h
        clone.tooltip = tooltip
        clone.tooltipPanel = clone.tooltipPanel // could create issues, should be found in parent or cloned
        clone.weight = weight
        clone.visibility = visibility
        clone.backgroundColor = backgroundColor
        clone.backgroundRadiusCorners = backgroundRadiusCorners
        clone.backgroundRadius = backgroundRadius
        clone.layoutConstraints.clear()
        clone.layoutConstraints.addAll(layoutConstraints.map { it.clone() })
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "x" -> x = value
            "y" -> y = value
            "w" -> w = value
            "h" -> h = value
            "minW" -> minW = value
            "minH" -> minH = value
            "visibility" -> visibility = Visibility[value != 0]
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

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeInt("x", x)
        writer.writeInt("y", y)
        writer.writeInt("w", w)
        writer.writeInt("h", h)
        writer.writeInt("minW", minW)
        writer.writeInt("minH", minH)
        writer.writeEnum("alignmentX", alignmentX)
        writer.writeEnum("alignmentY", alignmentY)
        // maybe...
        // writer.writeObjectList(this, "layoutConstraints", layoutConstraints)
        // todo all other properties...
        writer.writeFloat("weight", weight)
        writer.writeEnum("visibility", visibility)
        writer.writeColor("background", backgroundColor)
        writer.writeInt("backgroundRadiusCorners", backgroundRadiusCorners)
        writer.writeColor("backgroundOutline", backgroundOutlineColor)
        writer.writeFloat("backgroundRadius", backgroundRadius)
        writer.writeFloat("backgroundOutlineThickness", backgroundOutlineThickness)
    }

    override val className: String = "Panel"

    companion object {
        private val LOGGER = LogManager.getLogger(Panel::class)
        val interactionPadding get() = Maths.max(0, DefaultConfig["ui.interactionPadding", 6])
        val minOpaqueAlpha = 63
    }

}