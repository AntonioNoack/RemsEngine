package me.anno.ui.editor.cutting

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.input.Input
import me.anno.input.Input.isControlDown
import me.anno.input.Input.keysDown
import me.anno.input.Input.mouseKeysDown
import me.anno.input.Input.mouseX
import me.anno.input.Input.mouseY
import me.anno.input.Input.needsLayoutUpdate
import me.anno.input.MouseButton
import me.anno.io.text.TextReader
import me.anno.objects.Transform
import me.anno.studio.RemsStudio.onLargeChange
import me.anno.studio.RemsStudio.onSmallChange
import me.anno.studio.Studio
import me.anno.studio.Studio.root
import me.anno.studio.Studio.selectedTransform
import me.anno.studio.Studio.shiftSlowdown
import me.anno.ui.dragging.Draggable
import me.anno.ui.editor.TimelinePanel
import me.anno.ui.editor.files.addChildFromFile
import me.anno.ui.editor.sceneTabs.SceneTabs
import me.anno.ui.style.Style
import me.anno.utils.*
import org.joml.Vector4f
import java.io.File
import java.lang.Exception
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.roundToInt

class LayerView(style: Style) : TimelinePanel(style) {

    // todo select multiple elements to move them around together
    // todo they shouldn't be parent and children, because that would have awkward results...

    var timelineSlot = 0

    val height = 50

    lateinit var calculated: List<Transform>
    lateinit var drawn: List<Transform>

    val alphaMultiplier = 0.7f

    var draggedTransform: Transform? = null

    init {
        // not working :/
        // renderOnRequestOnly = true
    }

    // performance is very low... fix that...
    // especially, if it's not changing
    // two ideas:
    //  kind of done - render only every x frames + on request
    // actually done - calculation async
    // todo instanced arrays, because we have soo many stripes?
    // we could optimize simple, not manipulated stripes... -> we optimize with linear approximations

    companion object {
        val minAlpha = 1f / 255f
        val minDistSq = sq(3f / 255f)
        val maxStripes = 5
    }

    var needsUpdate = false
    var isCalculating = false

    var solution: Solution? = null

    class Solution(val x0: Int, val y0: Int, val x1: Int, val y1: Int) {
        val w = x1 - x0
        val stripes = Array(maxStripes) {
            ArrayList<Gradient>(w / 2)
        }

        fun draw() {
            val y = y0
            val h = y1 - y0
            stripes.forEachIndexed { index, gradients ->
                val y0 = y + 3 + index * 3
                val h0 = h - 10
                // val random = Random(15132132L + index * 15631L)
                gradients.forEach {
                    // val l = random.nextFloat()
                    // val v = Vector4f(l, l, l, 1f)
                    GFX.drawRectGradient(it.x0, y0, it.w, h0, it.c0, it.c2)
                    // GFX.drawRect(it.x0, y0, it.w, h0, v)
                }
            }
        }
    }

    fun calculateSolution(x0: Int, y0: Int, x1: Int, y1: Int, asnyc: Boolean) {

        isCalculating = true
        needsUpdate = false

        if (asnyc) {
            thread {
                calculateSolution(x0, y0, x1, y1, false)
            }
            return
        }

        val solution = Solution(x0, y0, x1, y1)
        val stripes = solution.stripes
        // val t1 = System.nanoTime()
        val root = root
        calculated = findElements()
        drawn = calculated.filter { it.timelineSlot == timelineSlot }.reversed()
        // val t2 = System.nanoTime()
        val isHovered = isHovered
        val draggedTransform = draggedTransform
        val stripeDelay = 5
        val stepSize = 1
        val additionalStripes = ArrayList<Pair<Int, Gradient>>((x1 - x0) / stripeDelay + 5)
        if (drawn.isNotEmpty()) {
            val selectedTransform = if (isHovered && mouseKeysDown.isEmpty()) {
                getTransformAt(mouseX, mouseY)
            } else null
            val leftTime = getTimeAt(x0.toFloat())
            val dt = dtHalfLength * 2.0 / w
            val white = Vector4f(1f, 1f, 1f, 1f)
            for (x in x0 until x1 step stepSize) {
                val i = x - x0
                var ctr = 0
                val globalTime = leftTime + i * dt
                // hashmaps are slower, but thread safe
                val localTime = HashMap<Transform, Double>()
                val localColor = HashMap<Transform, Vector4f>()
                val rootTime = root.getLocalTime(globalTime)
                localTime[root] = rootTime
                localColor[root] = root.getLocalColor(white, rootTime)
                // root.lastLocalTime = root.getLocalTime(globalTime)
                // root.lastLocalColor = root.getLocalColor(white, root.lastLocalTime)
                for (tr in calculated){
                    if (tr !== root) {
                        val p = tr.parent ?: continue // was deleted
                        val parentTime = localTime[p] ?: continue // parent was deleted
                        val localTime0 = tr.getLocalTime(parentTime)
                        localTime[tr] = localTime0
                        localColor[tr] = tr.getLocalColor(localColor[p]!!, localTime0)
                    }
                }
                // smooth transition of ctr???
                // stripes by index to make visible, that there are multiple objects
                trs@ for (tr in drawn) {
                    val color = localColor[tr] ?: continue // was deleted
                    val time = localTime[tr]!!
                    var alpha = color.w * alphaMultiplier
                    if (!tr.isVisible(time)) alpha = 0f

                    // todo draw a stripe of the current image, or a symbol or sth...
                    color.w = alpha

                    // show stripes on the selected/hovered element
                    if (alpha >= minAlpha && x % stripeDelay == 0 && (selectedTransform === tr || draggedTransform === tr)) {
                        // additional stripes reduces the draw time from 400-600µs to 300µs :)
                        // more time reduction could be done with a specialized shader and an additional channel
                        val color2 = Vector4f(color)
                        color2.w *= 1.5f
                        additionalStripes += ctr to Gradient(tr, x, x, color2, color2)
                    }

                    if(alpha >= minAlpha){

                        if(ctr >= maxStripes) break@trs
                        val list = stripes[ctr]
                        if (list.isEmpty()) {
                            if(alpha > minAlpha){
                                list += Gradient(tr, x, x, color, color)
                            } // else not worth it
                        } else {
                            val last = list.last()
                            if (last.owner == tr && last.isLinear(x, stepSize, color) && last.x2 + stepSize >= x) {
                                last.set(x, color)
                            } else {
                                list += Gradient(tr, x - stepSize + 1, x, color, color)
                            }
                        }

                        if (ctr++ >= maxStripes) {
                            break@trs
                        }

                    }

                }
            }
            // val t3 = System.nanoTime()
            // the calculation is 3x slower, but still, it's now async, so it doesn't matter much
            // ("${((t2 - t1) * 1e-6).f3()}+${((t3 - t2) * 1e-6).f3()}")
        }
        stripes.forEach { list ->
            list.removeIf { !it.needsDrawn() }
        }
        additionalStripes.forEach { (index, stripe) ->
            stripes[index].add(stripe)
        }
        this.solution = solution
        isCalculating = false
    }

    var lastTime = GFX.lastTime

    // calculation is fast, drawing is slow
    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {

        val t0 = System.nanoTime()
        // 80-100µ for background and time axis
        drawBackground()
        drawTimeAxis(x0, y0, x1, y1, timelineSlot == 0)

        val t1 = System.nanoTime()
        val solution = solution
        val needsUpdate = needsUpdate ||
                solution == null ||
                x0 != solution.x0 ||
                x1 != solution.x1 || isHovered || mouseKeysDown.isNotEmpty() || keysDown.isNotEmpty() ||
                abs(this.lastTime - GFX.lastTime) > if (needsLayoutUpdate()) 5e7 else 1e9

        if (needsUpdate && !isCalculating) {
            lastTime = GFX.lastTime
            calculateSolution(x0, y0, x1, y1, true)
        }

        if (solution != null) {
            solution.draw()
            val t2 = System.nanoTime()
            // two circle example:
            // 11µs for two sections x 2
            // 300µs for the sections with stripes;
            // hardware accelerated stripes? -> we'd have to add a flag/flag color
            // ("${((t1-t0)*1e-6).f3()}+${((t2-t1)*1e-6).f3()}")
            return
        }

    }

    fun getTransformAt(x: Float, y: Float): Transform? {
        var bestTransform: Transform? = null
        val yInt = y.toInt()
        if (drawn.isNotEmpty()) {
            val white = Vector4f(1f, 1f, 1f, 1f)
            var ctr = 0
            val globalTime = getTimeAt(x)
            root.lastLocalTime = root.getLocalTime(globalTime)
            root.lastLocalColor = root.getLocalColor(white, root.lastLocalTime)
            for(tr in calculated){
                if (tr !== root) {
                    val p = tr.parent ?: continue
                    val localTime = tr.getLocalTime(p.lastLocalTime)
                    tr.lastLocalTime = localTime
                    tr.lastLocalColor = tr.getLocalColor(p.lastLocalColor, localTime)
                }
            }
            drawn.forEach { tr ->
                val color = tr.lastLocalColor
                val alpha = color.w * alphaMultiplier
                if (alpha >= minAlpha && tr.isVisible(tr.lastLocalTime)) {
                    if (yInt - (this.y + 3 + ctr * 3) in 0..h - 10) {
                        bestTransform = tr
                    }
                    ctr++
                }
            }
        }
        return bestTransform
    }

    // todo hold / move up/down / move sideways
    // todo right click cut
    // todo move start/end times
    // todo highlight the hovered panel?

    override fun onMouseDown(x: Float, y: Float, button: MouseButton) {
        if (button.isLeft) {
            draggedTransform = getTransformAt(x, y)
            if (draggedTransform != null) GFX.select(draggedTransform)
        }
    }

    override fun onDeleteKey(x: Float, y: Float) {
        selectedTransform?.destroy()
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        draggedTransform?.apply {
            val thisSlot = this@LayerView.timelineSlot
            if (dx != 0f) {
                var dilation = 1.0
                var parent = parent
                while (parent != null) {
                    dilation *= parent.timeDilation
                    parent = parent.parent
                }
                if (isControlDown) {
                    // todo scale around the time=0 point?
                    // todo first find this point...
                    timeDilation *= clamp(1f - shiftSlowdown * dx / w, 0.01f, 100f)
                } else {
                    val dt = shiftSlowdown * dilation * dx * dtHalfLength * 2 / w
                    timeOffset += dt
                }
                Studio.updateInspector()
                onSmallChange("layer-dx")
            }
            var sumDY = (y - Input.mouseDownY) / height
            if (sumDY < 0) sumDY += 0.5f
            else sumDY -= 0.5f
            val newSlot = thisSlot + sumDY.roundToInt()
            if (newSlot != timelineSlot) {
                timelineSlot = newSlot
                Studio.updateInspector()
                onSmallChange("layer-slot")
            }
        } ?: super.onMouseMoved(x, y, dx, dy)
    }

    override fun onMouseUp(x: Float, y: Float, button: MouseButton) {
        draggedTransform = null
    }

    override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
        if (button.isRight) {
            val transform = getTransformAt(x, y)
            if (transform != null) {
                val cTime = transform.lastLocalTime
                // todo get the options for this transform
                val options = ArrayList<Pair<String, () -> Unit>>()
                options += "Split Here" to {
                    // todo ask user for split time?... todo rather add fadeout- / fadein-effects
                    // todo 100% transparency on both in the middle??
                    val fadingTime = 0.2
                    val fadingHalf = fadingTime / 2
                    transform.color.isAnimated = true
                    val lTime = cTime - fadingHalf
                    val rTime = cTime + fadingHalf
                    val color = transform.color[cTime]
                    val lColor = transform.color[lTime]
                    val lTransparent = Vector4f(lColor.x, lColor.y, lColor.z, 0f)
                    val rColor = transform.color[rTime]
                    val rTransparent = Vector4f(rColor.x, rColor.y, rColor.z, 0f)
                    val second = transform.clone()!!
                    second.name = incrementName(transform.name)
                    if(transform.parent != null) {
                        transform.addAfter(second)
                    } else {
                        // can't split directly,
                        // because we have no parent
                        val newRoot = Transform()
                        newRoot.addChild(transform)
                        newRoot.addChild(second)
                        root = newRoot
                        // needs to be updated
                        SceneTabs.currentTab?.root = newRoot
                    }
                    // transform.color.addKeyframe(localTime-fadingTime/2, color)
                    transform.color.keyframes.removeIf { it.time >= cTime }
                    transform.color.addKeyframe(cTime, color)
                    transform.color.addKeyframe(rTime, rTransparent)
                    second.color.keyframes.removeIf { it.time <= cTime }
                    second.color.addKeyframe(lTime, lTransparent)
                    second.color.addKeyframe(cTime, color)
                    onLargeChange()
                }
                GFX.openMenu(options)
            } else super.onMouseClicked(x, y, button, long)
        } else super.onMouseClicked(x, y, button, long)
    }

    fun findElements(): List<Transform> {
        val list = ArrayList<Transform>()
        fun inspect(parent: Transform): Boolean {
            val isRequired = parent.children.count { child ->
                inspect(child)
            } > 0 || parent.timelineSlot == timelineSlot
            if (isRequired) {
                list += parent
            }
            return isRequired
        }
        inspect(root)
        return list.reversed()
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        if (!data.startsWith("[")) return super.onPaste(x, y, data, type)
        try {
            val childMaybe = TextReader.fromText(data).firstOrNull { it is Transform } as? Transform
            val child = childMaybe ?: return super.onPaste(x, y, data, type)
            val original = (Studio.dragged as? Draggable)?.getOriginal() as? Transform
            if (original != null) {
                original.timelineSlot = timelineSlot
                onSmallChange("layer-paste")
            } else {
                root.addChild(child)
                root.timelineSlot = timelineSlot
                GFX.select(child)
                onLargeChange()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            super.onPaste(x, y, data, type)
        }
    }

    override fun onPasteFiles(x: Float, y: Float, files: List<File>) {
        val time = getTimeAt(x)
        files.forEach { file ->
            addChildFromFile(root, file, {
                it.timeOffset = time
                it.timelineSlot = timelineSlot
                // fade-in? is better for stuff xD
                if (DefaultConfig["import.files.fade", true]) {
                    val fadingTime = 0.2
                    if (it.color.isDefaultValue()) {
                        it.color.isAnimated = true
                        it.color.addKeyframe(0.0, Vector4f(1f, 1f, 1f, 0f))
                        it.color.addKeyframe(fadingTime, Vector4f(1f, 1f, 1f, 1f))
                    }
                }
            })
        }
    }

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        minW = w
        minH = height
    }

}