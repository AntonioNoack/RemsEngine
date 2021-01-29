package me.anno.ui.editor.cutting

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.GFXx2D.drawRect
import me.anno.input.Input
import me.anno.input.Input.isControlDown
import me.anno.input.Input.keysDown
import me.anno.input.Input.mouseKeysDown
import me.anno.input.Input.mouseX
import me.anno.input.Input.mouseY
import me.anno.input.Input.needsLayoutUpdate
import me.anno.input.MouseButton
import me.anno.io.text.TextReader
import me.anno.language.translation.NameDesc
import me.anno.objects.Transform
import me.anno.objects.animation.Keyframe
import me.anno.studio.StudioBase
import me.anno.studio.StudioBase.Companion.shiftSlowdown
import me.anno.studio.rems.RemsStudio
import me.anno.studio.rems.RemsStudio.isPlaying
import me.anno.studio.rems.RemsStudio.root
import me.anno.studio.rems.Selection.select
import me.anno.studio.rems.Selection.selectTransform
import me.anno.studio.rems.Selection.selectedTransform
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.dragging.Draggable
import me.anno.ui.editor.TimelinePanel
import me.anno.ui.editor.files.ImportFromFile.addChildFromFile
import me.anno.ui.editor.sceneTabs.SceneTabs
import me.anno.ui.style.Style
import me.anno.utils.Maths.clamp
import me.anno.utils.Maths.mix
import me.anno.utils.Maths.sq
import me.anno.utils.files.Naming.incrementName
import org.joml.Vector4f
import java.io.File
import kotlin.collections.set
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.roundToInt

class LayerView(style: Style) : TimelinePanel(style) {

    // todo select multiple elements to move them around together
    // todo they shouldn't be parent and children, because that would have awkward results...

    var timelineSlot = 0

    val height = 50

    lateinit var calculated: List<Transform>
    var drawn: List<Transform>? = null

    lateinit var cuttingView: CuttingView

    val alphaMultiplier = 0.7f

    var draggedTransform: Transform? = null
    var draggedKeyframes: List<Keyframe<*>>? = null

    var hoveredTransform: Transform? = null
    var hoveredKeyframes: List<Keyframe<*>>? = null

    // performance is very low... fix that...
    // especially, if it's not changing
    // two ideas:
    //  kind of done - render only every x frames + on request
    // actually done - calculation async
    // instanced arrays, because we have soo many stripes?
    // we could optimize simple, not manipulated stripes... -> we optimize with linear approximations

    companion object {
        val minAlpha = 1f / 255f
        val minDistSq = sq(3f / 255f)
        val maxLines = 5
        val defaultLayerCount = 8
    }

    var needsUpdate = false
    var isCalculating = false

    var solution: Solution? = null

    private fun calculateSolution(x0: Int, y0: Int, x1: Int, y1: Int, asnyc: Boolean) {

        isCalculating = true
        needsUpdate = false

        if (asnyc) {
            thread { calculateSolution(x0, y0, x1, y1, false) }
            return
        }

        val solution = Solution(x0, y0, x1, y1, centralTime)
        val stripes = solution.lines
        // val t1 = System.nanoTime()
        val root = root
        calculated = findElements()
        val drawn = calculated.filter { it.timelineSlot.value == timelineSlot }.reversed()
        this.drawn = drawn
        // val t2 = System.nanoTime()
        /*val isHovered = isHovered
        val draggedTransform = draggedTransform
        val stripeDelay = 5*/
        val stepSize = 1
        // val additionalStripes = ArrayList<Pair<Int, Gradient>>((x1 - x0) / stripeDelay + 5)
        if (drawn.isNotEmpty()) {
            /*val selectedTransform = if (isHovered && mouseKeysDown.isEmpty()) {
                getTransformAt(mouseX, mouseY)
            } else null*/
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

                for (tr in calculated) {
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

                    color.w = alpha

                    if (alpha >= minAlpha) {

                        if (ctr >= maxLines) break@trs
                        val list = stripes[ctr]
                        if (list.isEmpty()) {
                            if (alpha > minAlpha) {
                                list += Gradient(tr, x, x, color, color)
                            } // else not worth it
                        } else {
                            val last = list.last()
                            // println("${last.owner === tr} && ${last.isLinear(x, stepSize, color)} && ${last.x1 + stepSize} >= $x")
                            if (last.owner === tr && last.x1 + stepSize >= x && last.isLinear(x, stepSize, color)) {
                                last.setEnd(x, stepSize, color)
                            } else {
                                list += Gradient(tr, x - stepSize + 1, x, color, color)
                            }
                        }

                        if (ctr++ >= maxLines) {
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
        this.solution = solution
        isCalculating = false
        invalidateDrawing()
    }

    var visualStateCtr = 0
    override fun getVisualState(): Any? =
        Pair(
            super.getVisualState(),
            if ((isHovered && mouseKeysDown.isNotEmpty()) || isPlaying) visualStateCtr++
            else if (isHovered) getTransformAt(mouseX, mouseY)
            else null
        )

    override fun tickUpdate() {
        super.tickUpdate()
        solution?.keepResourcesLoaded()
    }

    var lastTime = GFX.gameTime

    // calculation is fast, drawing is slow
    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {

        drawnStrings.clear()

        // val t0 = System.nanoTime()
        // 80-100µ for background and time axis
        drawBackground()
        drawTimeAxis(x0, y0, x1, y1, timelineSlot == 0)

        // val t1 = System.nanoTime()
        val solution = solution
        val needsUpdate = needsUpdate ||
                solution == null ||
                x0 != solution.x0 ||
                x1 != solution.x1 ||
                isHovered ||
                mouseKeysDown.isNotEmpty() ||
                keysDown.isNotEmpty() ||
                abs(this.lastTime - GFX.gameTime) > if (needsLayoutUpdate()) 5e7 else 1e9


        if (needsUpdate && !isCalculating) {
            lastTime = GFX.gameTime
            calculateSolution(x0, y0, x1, y1, true)
        }

        // if (solution != null) {
        solution?.draw(selectedTransform, draggedTransform)
        // val t2 = System.nanoTime()
        // two circle example:
        // 11µs for two sections x 2
        // 300µs for the sections with stripes;
        // hardware accelerated stripes? -> we'd have to add a flag/flag color
        // ("${((t1-t0)*1e-6).f3()}+${((t2-t1)*1e-6).f3()}")
        //}

        val draggedTransform = draggedTransform
        val draggedKeyframes = draggedKeyframes

        fun drawLines(transform: Transform) {
            val color = transform.color
            if (color.isAnimated) {
                var ht0 = getTimeAt(mouseX - 5f)
                var ht1 = getTimeAt(mouseX + 5f)
                val hx0 = getXAt(ht0)
                val hx1 = getXAt(ht1)
                val inheritance = transform.listOfInheritance.toList().reversed()
                inheritance.forEach {
                    ht0 = it.getLocalTime(ht0)
                    ht1 = it.getLocalTime(ht1)
                }
                val keyframes = draggedKeyframes ?: color[ht0, ht1]
                hoveredKeyframes = keyframes
                var x = x0 - 1
                keyframes.forEach {
                    val relativeTime = (it.time - ht0) / (ht1 - ht0)
                    val x2 = mix(hx0, hx1, relativeTime).toInt()
                    if (x2 > x) {
                        drawRect(x2, y0, 1, y1 - y0, accentColor)
                        x = x2
                    }
                }
            } else hoveredKeyframes = null
        }

        if (draggedTransform == null || draggedKeyframes == null) {
            if (isHovered) {
                val hovered = getTransformAt(mouseX, mouseY)
                    ?: if (selectedTransform?.timelineSlot?.value == timelineSlot) selectedTransform else null
                hoveredTransform = hovered
                if (hovered != null) {
                    drawLines(hovered)
                }
            }
        } else {
            drawLines(draggedTransform)
        }

    }

    private fun getTransformAt(x: Float, y: Float): Transform? {
        val drawn = drawn ?: return null
        var bestTransform: Transform? = null
        val yInt = y.toInt()
        if (drawn.isNotEmpty()) {
            val white = Vector4f(1f, 1f, 1f, 1f)
            var ctr = 0
            val globalTime = getTimeAt(x)
            root.lastLocalTime = root.getLocalTime(globalTime)
            root.lastLocalColor = root.getLocalColor(white, root.lastLocalTime)
            for (tr in calculated) {
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

    // done hold / move up/down / move sideways
    // done right click cut
    // done move start/end times
    // done highlight the hovered panel?

    override fun onMouseDown(x: Float, y: Float, button: MouseButton) {
        if (button.isLeft) {
            var draggedTransform = getTransformAt(x, y)
            this.draggedTransform = draggedTransform
            if (draggedTransform != null) {
                select(draggedTransform, draggedTransform.color)
                if (draggedTransform == hoveredTransform) {
                    val hoveredKeyframes = hoveredKeyframes
                    draggedKeyframes = if (hoveredKeyframes?.isNotEmpty() == true) {
                        hoveredKeyframes
                    } else null
                }
            } else {
                // move the keyframes of the last selected transform,
                // they may be invisible
                val hoveredTransform = hoveredTransform
                val hoveredKeyframes = hoveredKeyframes
                if (hoveredTransform != null && hoveredKeyframes?.isNotEmpty() == true) {
                    draggedTransform = hoveredTransform
                    this.draggedTransform = draggedTransform
                    select(draggedTransform, draggedTransform.color)
                    draggedKeyframes = hoveredKeyframes
                }
            }
        }
    }

    override fun onDeleteKey(x: Float, y: Float) {
        RemsStudio.largeChange("Deleted Component") {
            selectedTransform?.destroy()
        }
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        val transform = draggedTransform
        val draggedKeyframes = draggedKeyframes
        if (transform != null) {
            if (draggedKeyframes != null) {
                if (draggedKeyframes.isNotEmpty()) {
                    val dilation = transform.listOfInheritance
                        .fold(1.0) { t0, tx -> t0 * tx.timeDilation }
                    val dt = shiftSlowdown * dilation * dx * dtHalfLength * 2 / w
                    if (dt != 0.0) {
                        RemsStudio.incrementalChange("Move Keyframes") {
                            draggedKeyframes.forEach {
                                it.time += dt
                            }
                        }
                    }
                }
            } else {
                val thisSlot = this@LayerView.timelineSlot
                if (dx != 0f) {
                    val dilation = transform.listOfInheritance
                        .fold(1.0) { t0, tx -> t0 * tx.timeDilation }
                    RemsStudio.incrementalChange("Change Time Dilation / Offset") {
                        if (isControlDown) {
                            // todo scale around the time=0 point?
                            // todo first find this point...
                            val factor = clamp(1f - shiftSlowdown * dx / w, 0.01f, 100f)
                            transform.timeDilation *= factor
                        } else {
                            val dt = shiftSlowdown * dilation * dx * dtHalfLength * 2 / w
                            transform.timeOffset += dt
                        }
                    }
                }
                var sumDY = (y - Input.mouseDownY) / height
                if (sumDY < 0) sumDY += 0.5f
                else sumDY -= 0.5f
                // todo make sure the timeline slot doesn't have invalid values
                val newSlot = thisSlot + sumDY.roundToInt()
                if (newSlot != timelineSlot) {
                    RemsStudio.largeChange("Changed Timeline Slot") {
                        timelineSlot = newSlot
                    }
                }
            }
        } else super.onMouseMoved(x, y, dx, dy)
    }

    override fun onMouseUp(x: Float, y: Float, button: MouseButton) {
        draggedTransform = null
        draggedKeyframes = null
    }

    override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
        when {
            button.isRight -> {
                val transform = getTransformAt(x, y)
                if (transform != null) {
                    val cTime = transform.lastLocalTime
                    // get the options for this transform
                    val options = ArrayList<MenuOption>()
                    options += MenuOption(
                        NameDesc(
                            "Split Here",
                            "Cuts the element in two halves",
                            "ui.cutting.splitHere"
                        )
                    ) {
                        RemsStudio.largeChange("Split Component") {
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
                            if (transform.parent != null) {
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
                            transform.color.checkThread()
                            transform.color.keyframes.removeIf { it.time >= cTime }
                            transform.color.addKeyframe(cTime, color)
                            transform.color.addKeyframe(rTime, rTransparent)
                            second.color.checkThread()
                            second.color.keyframes.removeIf { it.time <= cTime }
                            second.color.addKeyframe(lTime, lTransparent)
                            second.color.addKeyframe(cTime, color)
                        }
                    }
                    openMenu(options)
                } else super.onMouseClicked(x, y, button, long)
            }
            else -> super.onMouseClicked(x, y, button, long)
        }
    }

    fun findElements(): List<Transform> {
        val list = ArrayList<Transform>()
        fun inspect(parent: Transform): Boolean {
            val isRequired = parent.children.count { child ->
                inspect(child)
            } > 0 || parent.timelineSlot.value == timelineSlot
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
            val original = (StudioBase.dragged as? Draggable)?.getOriginal() as? Transform
            RemsStudio.largeChange("Pasted Component / Changed Timeline Slot") {
                if (original != null) {
                    original.timelineSlot.value = timelineSlot
                } else {
                    root.addChild(child)
                    root.timelineSlot.value = timelineSlot
                    selectTransform(child)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            super.onPaste(x, y, data, type)
        }
    }

    override fun onPasteFiles(x: Float, y: Float, files: List<File>) {
        val time = getTimeAt(x)
        files.forEach { file ->
            addChildFromFile(root, file, null, true) {
                it.timeOffset = time
                it.timelineSlot.value = timelineSlot
                // fade-in? is better for stuff xD
                if (DefaultConfig["import.files.fade", true]) {
                    val fadingTime = 0.2
                    if (it.color.isDefaultValue()) {
                        it.color.isAnimated = true
                        it.color.addKeyframe(0.0, Vector4f(1f, 1f, 1f, 0f))
                        it.color.addKeyframe(fadingTime, Vector4f(1f, 1f, 1f, 1f))
                    }
                }
            }
        }
    }

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        minW = w
        minH = height
    }


}