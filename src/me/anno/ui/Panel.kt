package me.anno.ui

import me.anno.config.DefaultConfig
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.Type
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.gpu.Cursor
import me.anno.gpu.GFX
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.drawing.DrawRounded.drawRoundedRect
import me.anno.gpu.drawing.GFXx2D.getSize
import me.anno.gpu.drawing.GFXx2D.getSizeX
import me.anno.gpu.drawing.GFXx2D.getSizeY
import me.anno.input.Input
import me.anno.input.Key
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.maths.Maths
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.length
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.PanelGroup
import me.anno.ui.base.scrolling.ScrollableX
import me.anno.ui.base.scrolling.ScrollableY
import me.anno.utils.Color.a
import me.anno.utils.Color.black
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Floats.roundToIntOr
import me.anno.utils.types.Strings
import me.anno.utils.types.Strings.isBlank2
import me.anno.utils.types.Strings.shorten
import org.apache.logging.log4j.LogManager
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

open class Panel(val style: Style) : PrefabSaveable() {

    /**
     * Actual size and placement
     * */
    var x = 0
    var y = 0
    var width = 258
    var height = 259

    /**
     * Wished minimum size, calculated in calculateSize();
     * */
    var minW = 1
    var minH = 1

    /**
     * which space the element will take up horizontally after size calculation:
     * min = left, max = right, center = centered, fill = all
     * */
    var alignmentX = AxisAlignment.FILL
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }

    /**
     * which space the element will take up horizontally after size calculation:
     * min = top, max = bottom, center = centered, fill = all
     * */
    var alignmentY = AxisAlignment.FILL
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }

    /**
     * this weight is used inside some layouts:
     * if there is enough space, weighted panels will use up that space proportionally to their weight,
     * and with disregard for the minimum size (because equally sized text panels would be tough to implement otherwise);
     *
     * like on Android
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
     * */
    var weight2 = 0f
        set(value) {
            if (value.isFinite() && field != value) {
                field = value
                invalidateLayout()
            }
        }

    /**
     * Spreads this panel with the given weight
     * */
    fun fill(weight: Float): Panel {
        this.weight = weight
        this.weight2 = weight2
        alignmentX = AxisAlignment.FILL
        alignmentY = AxisAlignment.FILL
        return this
    }

    var isVisible: Boolean
        get() = isEnabled
        set(value) {
            val wasEnabled = isEnabled
            isEnabled = value
            if (wasEnabled != value) {
                invalidateLayout()
            }
        }

    @NotSerializedProperty
    var window: Window? = null
        get() = uiParent?.window ?: field

    val windowStack: WindowStack
        get() = window?.windowStack ?: GFX.someWindow.windowStack

    val depth: Int get() = 1 + (uiParent?.depth ?: 0)

    fun toggleVisibility(): Panel {
        isVisible = !isVisible
        return this
    }

    fun makeBackgroundTransparent(): Panel {
        backgroundColor = backgroundColor and 0xffffff
        return this
    }

    fun makeBackgroundOpaque(): Panel {
        backgroundColor = backgroundColor or black
        return this
    }

    fun hide() {
        isVisible = false
    }

    fun show() {
        isVisible = true
    }

    @DebugAction
    open fun invalidateLayout() {
        val parent = uiParent
        if (parent == null) {
            window?.addNeedsLayout(this)
        } else parent.invalidateLayout()
    }

    @DebugAction
    open fun invalidateDrawing() {
        invalidateDrawing(lx0, ly0, lx1, ly1)
    }

    fun invalidateDrawing(x0: Int, y0: Int, x1: Int, y1: Int) {
        if (x1 > x0 && y1 > y0 && canBeSeen) {
            window?.addNeedsRedraw(this, x0, y0, x1, y1)
        }
    }

    open fun onUpdate() {
        if (wasInFocus != isInFocus) {
            invalidateDrawing()
        }
        wasInFocus = isInFocus
        wasHovered = isHovered
    }

    @NotSerializedProperty
    var oldLayoutState: Any? = null

    @NotSerializedProperty
    var oldVisualState: Any? = null

    @NotSerializedProperty
    var oldStateInt = 0

    // old task: mesh or image backgrounds for panels
    //  -> use a PanelStack, and an ImagePanel below your main panel

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

    val rootPanel: Panel get() = getRoot(Panel::class)

    open val onMovementHideTooltip = true

    @Type("String")
    var tooltip: String = ""

    @Type("Panel?/SameSceneRef")
    var tooltipPanel: Panel? = null

    open fun getLayoutState(): Any? = null
    open fun getVisualState(): Any? = null

    fun tick() {
        val newLayoutState = getLayoutState()
        val newVisualState = getVisualState()
        val newStateInt = isInFocus.toInt(1) + isHovered.toInt(2) + canBeSeen.toInt(4)
        if (newLayoutState != oldLayoutState) {
            invalidateLayout()
        } else if (oldStateInt != newStateInt || oldVisualState != newVisualState) {
            invalidateDrawing()
        }
        oldLayoutState = newLayoutState
        oldVisualState = newVisualState
        oldStateInt = newStateInt
    }

    open fun requestFocus(exclusive: Boolean = true) {
        windowStack.requestFocus(this, exclusive)
    }

    val hasRoundedCorners get() = backgroundRadius > 0f && backgroundRadiusCorners != 0

    val siblings get() = uiParent?.children ?: emptyList()

    open val canDrawOverBorders get() = hasRoundedCorners

    open fun drawBackground(x0: Int, y0: Int, x1: Int, y1: Int, dx: Int = 0, dy: Int = dx) {
        // if the children are overlapping, this is incorrect
        // this however, should rarely happen...
        if (backgroundColor.a() > 0 || hasRoundedCorners &&
            backgroundOutlineThickness > 0f &&
            backgroundOutlineColor.a() > 0
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

    @NotSerializedProperty
    private var lp0 = 0

    @NotSerializedProperty
    private var lp1 = 0

    val lx0: Int get() = getSizeX(lp0)
    val ly0: Int get() = getSizeY(lp0)
    val lx1: Int get() = getSizeX(lp1)
    val ly1: Int get() = getSizeY(lp1)

    fun updateVisibility(
        mx: Int, my: Int, canBeHovered: Boolean,
        x0: Int, y0: Int, x1: Int, y1: Int
    ) {
        isInFocus = false
        isAnyChildInFocus = false
        val lx0 = max(x0, x)
        val lx1 = min(x1, x + width)
        val ly0 = max(y0, y)
        val ly1 = min(y1, y + height)
        lp0 = getSize(lx0, ly0)
        lp1 = getSize(lx1, ly1)
        val parent = uiParent
        canBeSeen = (parent == null || parent.canBeSeen) && isVisible && lx1 > lx0 && ly1 > ly0
        isHovered = canBeHovered && canBeSeen && mx in lx0 until lx1 && my in ly0 until ly1
        if (this is PanelGroup) {
            updateChildrenVisibility(mx, my, isHovered, lx0, ly0, lx1, ly1)
        }
    }

    /**
     * draw the panel inside the rectangle (x0 until x1, y0 until y1)
     * more does not need to be drawn;
     * the area is already clipped with useFrame(x0,y0,x1-x0,y1-y0)
     * */
    open fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        drawBackground(x0, y0, x1, y1)
    }

    fun setPosSizeAligned(x: Int, y: Int, availableW: Int, availableH: Int) {
        val minW = min(availableW, minW)
        val minH = min(availableH, minH)
        val dx = alignmentX.getOffset(availableW, minW)
        val dy = alignmentY.getOffset(availableH, minH)
        val nw = alignmentX.getSize(availableW, minW)
        val nh = alignmentY.getSize(availableH, minH)
        setPosSize(x + dx, y + dy, nw, nh)
    }

    fun setPosSize(x: Int, y: Int, w: Int, h: Int) {
        setSize(w, h)
        setPosition(x, y)
    }

    open fun setPosition(x: Int, y: Int) {
        this.x = x
        this.y = y
    }

    fun setSize(w: Int, h: Int) {
        this.width = w
        this.height = h
    }

    /**
     * sets minW & minH to the minimum size, this panel would like, given the available space
     * */
    open fun calculateSize(w: Int, h: Int) {
        minW = 1
        minH = 1
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

    open fun getCursor(): Cursor? = uiParent?.getCursor()

    open fun getTooltipPanel(x: Float, y: Float): Panel? = tooltipPanel
    open fun getTooltipText(x: Float, y: Float): String? = tooltip.ifEmpty { null }
    fun getTooltipToP(x: Float, y: Float): Any? =
        getTooltipPanel(x, y)
            ?: getTooltipText(x, y)
            ?: uiParent?.getTooltipToP(x, y)

    fun setTooltip(tooltipText: String): Panel {
        tooltip = tooltipText
        tooltipPanel = null
        return this
    }

    open fun printLayout(tabDepth: Int) {
        val tooltip = tooltip
        println(
            "${Strings.spaces(tabDepth * 2)}$className(${(weight * 10).roundToIntOr()}, " +
                    "${if (isVisible) "v" else "_"}${if (isHovered) "h" else ""}${if (isInFocus) "F" else ""})) " +
                    "$x-${x + width}, $y-${y + height} ($minW $minH) ${
                        if (tooltip.isBlank2()) "" else "'${tooltip.shorten(20)}' "
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

    fun getOverlayParent(x0: Int, y0: Int, x1: Int, y1: Int): Panel? {
        return uiParent?.getOverlayParent(x0, y0, x1, y1) ?: (
                if (drawsOverlayOverChildren(x0, y0, x1, y1)) this
                else if (backgroundColor.a() < 255) uiParent
                else null)
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
        // if panel is larger than window, ignore mouse position, and just center it
        // clamp mouse position to window bounds
        val padding = 50
        scrollTo(
            if (width >= window.width) window.x + window.width / 2 else
                clamp(window.mouseXi, window.x + padding, window.x + window.width - padding),
            if (height >= window.height) window.y + window.height / 2 else
                clamp(window.mouseYi, window.y + padding, window.y + window.height - padding)
        )
    }

    override val listOfAll: List<Panel>
        get() = listOfChildren(null)

    val listOfVisible: List<Panel>
        get() = listOfChildren(Panel::isVisible)

    open fun forAllPanels(callback: (Panel) -> Unit) {
        callback(this)
        val children = (this as? PanelGroup)?.children ?: return
        for (i in children.indices) {
            val child = children[i]
            child.parent = this // meh :/, but we sometimes forget to set it...
            child.forAllPanels(callback)
        }
    }

    open fun forAllVisiblePanels(callback: (Panel) -> Unit) {
        if (!isVisible) return
        callback(this)
        val children = (this as? PanelGroup)?.children ?: return
        for (i in children.indices) {
            val child = children[i]
            child.parent = this // meh :/, but we sometimes forget to set it...
            child.forAllVisiblePanels(callback)
        }
    }

    fun firstOfAll(predicate: (Panel) -> Boolean): Panel? {
        return firstInChildren(null, predicate)
    }

    fun any(predicate: (Panel) -> Boolean): Boolean {
        return firstOfAll(predicate) != null
    }

    fun firstInChildren(filter: ((Panel) -> Boolean)?, predicate: (Panel) -> Boolean): Panel? {
        if (filter != null && !filter(this)) return null
        if (predicate(this)) return this
        val children = (this as? PanelGroup)?.children ?: return null
        // recursion could be made linear
        for (i in children.indices) {
            val child = children[i]
            val hit = child.firstInChildren(filter, predicate)
            if (hit != null) return hit
        }
        return null
    }

    fun listOfChildren(hierarchicalFilter: ((Panel) -> Boolean)?): List<Panel> {
        return if (hierarchicalFilter != null && !hierarchicalFilter(this)) {
            return emptyList()
        } else if (this is PanelGroup) {
            val dst = ArrayList<Panel>()
            dst.add(this)
            var i = 0
            while (i < dst.size) {
                val pi = dst[i++]
                val children = (pi as? PanelGroup)?.children ?: continue
                if (hierarchicalFilter == null) { // faster version
                    dst.addAll(children)
                } else {
                    addChildrenIf(children, dst, hierarchicalFilter)
                }
            }
            dst
        } else listOf(this)
    }

    private fun addChildrenIf(
        children: List<Panel>, dst: ArrayList<Panel>,
        filter: (Panel) -> Boolean
    ): List<Panel> {
        for (ci in children.indices) {
            val ch = children[ci]
            if (filter(ch)) {
                dst.add(ch)
            }
        }
        return dst
    }

    fun anyInChildren(filter: (Panel) -> Boolean, predicate: (Panel) -> Boolean): Boolean {
        return firstInChildren(filter, predicate) != null
    }

    /**
     * does this panel contain the coordinate (x, y)?
     * does not consider overlap
     * */
    fun contains(x: Int, y: Int, margin: Int = 0): Boolean =
        (x - this.x) in -margin until width + margin && (y - this.y) in -margin until height + margin

    /**
     * does this panel contain the coordinate (x, y)?
     * does not consider overlap
     *
     * panels are aligned on full coordinates, so there is no advantage in calling this function
     * */
    fun contains(x: Float, y: Float, margin: Int = 0): Boolean = contains(x.toInt(), y.toInt(), margin)

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
        if (dst !is Panel) return
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
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeInt("x", x)
        writer.writeInt("y", y)
        writer.writeInt("w", width)
        writer.writeInt("h", height)
        writer.writeEnum("alignmentX", alignmentX)
        writer.writeEnum("alignmentY", alignmentY)
        // to do save this stuff somehow, maybe...
        // writer.writeObjectList(this, "clickListeners", clickListeners)
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

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "weight" -> weight = value as? Float ?: return
            "backgroundRadius" -> backgroundRadius = value as? Float ?: return
            "backgroundOutlineThickness" -> backgroundOutlineThickness = value as? Float ?: return
            "tooltip" -> tooltip = (value as? String)?.ifEmpty { null } ?: return
            "tooltipPanel" -> tooltipPanel = value as? Panel
            "x" -> x = value as? Int ?: return
            "y" -> y = value as? Int ?: return
            "w" -> width = value as? Int ?: return
            "h" -> height = value as? Int ?: return
            "visibility" -> isVisible = value == true
            "alignmentX" -> alignmentX = AxisAlignment.find(value as? Int ?: return) ?: alignmentX
            "alignmentY" -> alignmentY = AxisAlignment.find(value as? Int ?: return) ?: alignmentY
            "background" -> backgroundColor = value as? Int ?: return
            "backgroundOutline" -> backgroundOutlineColor = value as? Int ?: return
            else -> super.setProperty(name, value)
        }
    }

    companion object {

        const val CORNER_TOP_RIGHT = 1
        const val CORNER_BOTTOM_RIGHT = 2
        const val CORNER_TOP_LEFT = 4
        const val CORNER_BOTTOM_LEFT = 8

        var setLX = true

        private val LOGGER = LogManager.getLogger(Panel::class)
        val interactionPadding get() = Maths.max(0, DefaultConfig["ui.interactionPadding", 6])
        val minOpaqueAlpha = 63
    }
}