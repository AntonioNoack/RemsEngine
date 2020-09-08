package me.anno.ui.editor.cutting

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.input.Input
import me.anno.input.Input.isControlDown
import me.anno.input.Input.mouseKeysDown
import me.anno.input.Input.mouseX
import me.anno.input.Input.mouseY
import me.anno.input.MouseButton
import me.anno.io.text.TextReader
import me.anno.objects.Audio
import me.anno.objects.Transform
import me.anno.studio.RemsStudio.onLargeChange
import me.anno.studio.RemsStudio.onSmallChange
import me.anno.studio.Studio
import me.anno.studio.Studio.root
import me.anno.studio.Studio.selectedTransform
import me.anno.studio.Studio.shiftSlowdown
import me.anno.ui.dragging.Draggable
import me.anno.ui.editor.TimelinePanel
import me.anno.ui.editor.treeView.TreeView
import me.anno.ui.style.Style
import me.anno.utils.clamp
import me.anno.utils.incrementName
import org.joml.Vector4f
import java.io.File
import java.lang.Exception
import kotlin.math.roundToInt

class LayerView(style: Style): TimelinePanel(style) {

    // todo select multiple elements to move them around together
    // todo they shouldn't be parent and children, because that would have awkward results...

    var timelineSlot = 0

    val height = 50

    lateinit var calculated: List<Transform>
    lateinit var drawn: List<Transform>

    val alphaMultiplier = 0.7f
    val minAlpha = 0.001f

    var draggedTransform: Transform? = null

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.draw(x0, y0, x1, y1)
        drawTimeAxis(x0, y0, x1, y1, timelineSlot == 0)
        calculated = findElements()
        drawn = calculated.filter { it.timelineSlot == timelineSlot }.reversed()
        val draggedTransform = draggedTransform
        if(drawn.isNotEmpty()){
            val selectedTransform = if(isHovered && mouseKeysDown.isEmpty()){
                getTransformAt(mouseX, mouseY)
            } else null
            val leftTime = getTimeAt(x0.toFloat())
            val dt = dtHalfLength * 2.0 / w
            val white = Vector4f(1f, 1f, 1f, 1f)
            val y = y
            val h = h
            for(x in x0 until x1){
                val i = x-x0
                var ctr = 0
                val globalTime = leftTime + i * dt
                root.lastLocalTime = root.getLocalTime(globalTime)
                root.lastLocalColor = root.getLocalColor(white, root.lastLocalTime)
                calculated.forEach { tr ->
                    if(tr !== root){
                        val p = tr.parent!!
                        val localTime = tr.getLocalTime(p.lastLocalTime)
                        tr.lastLocalTime = localTime
                        tr.lastLocalColor = tr.getLocalColor(p.lastLocalColor, localTime)
                    }
                }
                // smooth transition of ctr???
                // stripes by index to make visible, that there are multiple objects
                drawn.forEach { tr ->
                    val color = tr.lastLocalColor
                    val alpha = color.w * alphaMultiplier
                    if(alpha >= minAlpha && tr.isVisible(tr.lastLocalTime)){
                        // todo draw a stripe of the current image, or a symbol or sth...
                        color.w = alpha
                        // show stripes on the selected/hovered element
                        if(x % 5 == 0 && (selectedTransform === tr || draggedTransform === tr)){
                            color.w *= 1.5f
                        }
                        GFX.drawRect(x, y+3+ctr*3, 1, h-10, color)
                        ctr++
                    }
                }
            }
        }
    }

    fun getTransformAt(x: Float, y: Float): Transform? {
        var bestTransform: Transform? = null
        val yInt = y.toInt()
        if(drawn.isNotEmpty()){
            val white = Vector4f(1f, 1f, 1f, 1f)
            var ctr = 0
            val globalTime = getTimeAt(x)
            root.lastLocalTime = root.getLocalTime(globalTime)
            root.lastLocalColor = root.getLocalColor(white, root.lastLocalTime)
            calculated.forEach { tr ->
                if(tr !== root){
                    val p = tr.parent!!
                    val localTime = tr.getLocalTime(p.lastLocalTime)
                    tr.lastLocalTime = localTime
                    tr.lastLocalColor = tr.getLocalColor(p.lastLocalColor, localTime)
                }
            }
            drawn.forEach { tr ->
                val color = tr.lastLocalColor
                val alpha = color.w * alphaMultiplier
                if(alpha >= minAlpha && tr.isVisible(tr.lastLocalTime)){
                    if(yInt-(this.y+3+ctr*3) in 0 .. h-10){
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
        if(button.isLeft){
            draggedTransform = getTransformAt(x, y)
            if(draggedTransform != null) GFX.select(draggedTransform)
        }
    }

    override fun onDeleteKey(x: Float, y: Float) {
        selectedTransform?.destroy()
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        draggedTransform?.apply {
            val thisSlot = this@LayerView.timelineSlot
            if(dx != 0f){
                var dilation = 1.0
                var parent = parent
                while(parent != null){
                    dilation *= parent.timeDilation
                    parent = parent.parent
                }
                if(isControlDown){
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
            if(sumDY < 0) sumDY += 0.5f
            else sumDY -= 0.5f
            val newSlot = thisSlot + sumDY.roundToInt()
            if(newSlot != timelineSlot){
                timelineSlot = newSlot
                Studio.updateInspector()
                onSmallChange("layer-slot")
            }
        }
    }

    override fun onMouseUp(x: Float, y: Float, button: MouseButton) {
        draggedTransform = null
    }

    override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
        if(button.isRight){
            val transform = getTransformAt(x, y)
            if(transform != null){
                val localTime = transform.lastLocalTime
                // todo get the options for this transform
                val options = ArrayList<Pair<String, () -> Unit>>()
                options += "Split Here" to {
                    // todo ask user for split time?... todo rather add fadeout- / fadein-effects
                    // todo 100% transparency on both in the middle??
                    val fadingTime = 0.2
                    val fadingHalf = fadingTime / 2
                    transform.color.isAnimated = true
                    val lTime = localTime - fadingHalf
                    val rTime = localTime + fadingHalf
                    val color = transform.color[localTime]
                    val lColor = transform.color[lTime]
                    val lTransparent = Vector4f(lColor.x, lColor.y, lColor.z, 0f)
                    val rColor = transform.color[rTime]
                    val rTransparent = Vector4f(rColor.x, rColor.y, rColor.z, 0f)
                    val second = transform.clone()
                    second.name = incrementName(transform.name)
                    transform.addAfter(second)
                    // transform.color.addKeyframe(localTime-fadingTime/2, color)
                    transform.color.keyframes.removeIf { it.time >= localTime }
                    transform.color.addKeyframe(localTime, color)
                    transform.color.addKeyframe(rTime, rTransparent)
                    second.color.keyframes.removeIf { it.time <= localTime }
                    second.color.addKeyframe(lTime, lTransparent)
                    second.color.addKeyframe(localTime, color)
                    onLargeChange()
                }
                if(options.isNotEmpty()){
                    GFX.openMenu(x, y, "", options)
                }
            } else super.onMouseClicked(x, y, button, long)
        } else super.onMouseClicked(x, y, button, long)
    }

    fun findElements(): List<Transform> {
        val list = ArrayList<Transform>()
        fun inspect(parent: Transform): Boolean {
            val isRequired = parent.children.count {  child ->
                inspect(child)
            } > 0 || parent.timelineSlot == timelineSlot
            if(isRequired){ list += parent }
            return isRequired
        }
        inspect(root)
        return list.reversed()
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        if(!data.startsWith("[")) return super.onPaste(x, y, data, type)
        try {
            val child = TextReader.fromText(data).firstOrNull { it is Transform } as? Transform ?: return super.onPaste(x, y, data, type)
            val original = (Studio.dragged as? Draggable)?.getOriginal() as? Transform
            if(original != null){
                original.timelineSlot = timelineSlot
                onSmallChange("layer-paste")
            } else {
                root.addChild(child)
                root.timelineSlot = timelineSlot
                GFX.select(child)
                onLargeChange()
            }
        } catch (e: Exception){
            e.printStackTrace()
            super.onPaste(x, y, data, type)
        }
    }

    override fun onPasteFiles(x: Float, y: Float, files: List<File>) {
        val time = getTimeAt(x)
        files.forEach { file ->
            TreeView.addChildFromFile(root, file, {
                it.timeOffset = time
                it.timelineSlot = timelineSlot
                // fade-in? is better for stuff xD
                if(DefaultConfig["import.files.fade", true]){
                    val fadingTime = 0.2
                    if(it.color.isDefaultValue()){
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