package me.anno.ui.base

import me.anno.gpu.Cursor
import me.anno.gpu.GFX
import me.anno.io.Saveable
import me.anno.ui.base.constraints.Margin
import me.anno.ui.style.Style
import me.anno.utils.Tabs
import java.io.File
import java.lang.RuntimeException

open class Panel(val style: Style): Saveable(){

    var visibility = Visibility.VISIBLE

    var weight = 0f

    var backgroundColor = style.getColor("background", -1)

    var parent: Panel? = null
    val alignmentConstraints = ArrayList<Constraint>()

    var w = 258
    var h = 259
    var x = 0
    var y = 0

    var minW = 10
    var minH = 10

    val isInFocus get() = GFX.inFocus === this
    val canBeSeen get() = canBeSeen(0,0,GFX.width,GFX.height)
    val canBeSeenCurrently get() = canBeSeen(GFX.windowX, GFX.windowY, GFX.windowWidth, GFX.windowHeight)
    fun canBeSeen(x0: Int, y0: Int, w0: Int, h0: Int): Boolean {
        return x + w > x0 && y + h > y0 && x < x0+w0 && y < y0+h0
    }

    val isVisible get() = visibility == Visibility.VISIBLE && canBeSeen

    fun requestFocus() = GFX.requestFocus(this)

    fun drawBackground(){
        GFX.drawRect(x,y,w,h,backgroundColor)
    }

    open fun draw(x0: Int, y0: Int, x1: Int, y1: Int){
        drawBackground()
    }

    fun setWeight(w: Float): Panel {
        weight = w
        return this
    }

    operator fun plusAssign(c: Constraint){
        alignmentConstraints += c
        alignmentConstraints.sortBy { it.order }
    }

    fun addPadding(left: Int, top: Int, right: Int, bottom: Int){
        alignmentConstraints.add(Margin(left, top, right, bottom))
        alignmentConstraints.sortBy { it.order }
    }

    fun addPadding(x: Int, y: Int) = addPadding(x,y,x,y)
    fun addPadding(p: Int) = addPadding(p,p,p,p)

    fun assert(b: Boolean, msg: String?){
        if(!b) throw RuntimeException(msg)
    }

    open fun placeInParent(x: Int, y: Int){
        this.x = x
        this.y = y
    }

    fun applyConstraints(){
        for(c in alignmentConstraints){
            c.apply(this)
        }
    }

    open fun calculateSize(w: Int, h: Int){
        minW = 10
        minH = 10
        this.w = w
        this.h = h
    }

    fun add(c: Constraint): Panel {
        this += c
        return this
    }

    private var onClickListener: ((Float, Float, Int, Boolean) -> Unit)? = null

    fun setOnClickListener(onClickListener: ((x: Float, y: Float, button: Int, long: Boolean) -> Unit)): Panel {
        this.onClickListener = onClickListener
        return this
    }

    open fun onMouseDown(x: Float, y: Float, button: Int){ parent?.onMouseDown(x,y,button) }
    open fun onMouseUp(x: Float, y: Float, button: Int){ parent?.onMouseUp(x,y,button) }
    open fun onMouseClicked(x: Float, y: Float, button: Int, long: Boolean){
        onClickListener?.invoke(x,y,button,long) ?: parent?.onMouseClicked(x,y,button,long)
    }

    open fun onDoubleClick(x: Float, y: Float, button: Int){ parent?.onDoubleClick(x,y,button)}
    open fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float){ parent?.onMouseMoved(x,y,dx,dy) }
    open fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float){ parent?.onMouseWheel(x,y,dx,dy) }

    open fun onKeyDown(x: Float, y: Float, key: Int){ parent?.onKeyDown(x,y,key) }
    open fun onKeyUp(x: Float, y: Float, key: Int){ parent?.onKeyUp(x,y,key) }
    open fun onKeyTyped(x: Float, y: Float, key: Int){ parent?.onKeyTyped(x,y,key) }
    open fun onCharTyped(x: Float, y: Float, key: Int){ parent?.onCharTyped(x,y,key) }

    open fun onEmpty(x: Float, y: Float) { parent?.onEmpty(x,y) }
    open fun onPaste(x: Float, y: Float, pasted: String){ parent?.onPaste(x,y,pasted) }
    open fun onPasteFiles(x: Float, y: Float, files: List<File>){ parent?.onPasteFiles(x,y,files) ?: println("Paste Ignored! $files") }
    open fun onCopyRequested(x: Float, y: Float): String? = parent?.onCopyRequested(x,y)

    open fun onSelectAll(x: Float, y: Float){ parent?.onSelectAll(x,y) }

    open fun onGotAction(x: Float, y: Float, action: String){ parent?.onGotAction(x, y, action) }

    open fun onBackKey(x: Float, y: Float){ parent?.onBackKey(x,y) }
    open fun onEnterKey(x: Float, y: Float){ parent?.onEnterKey(x,y) }
    open fun onDeleteKey(x: Float, y: Float){ parent?.onDeleteKey(x,y) }

    override fun getClassName(): String = "Panel"
    override fun getApproxSize(): Int = 1

    open fun getCursor(): Long? = parent?.getCursor() ?: 0L

    open fun printLayout(depth: Int){
        println("${Tabs.spaces(depth*2)}${javaClass.simpleName}($weight) $x $y += $w $h ($minW $minH)")
    }

}