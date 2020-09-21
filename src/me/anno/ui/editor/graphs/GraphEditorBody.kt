package me.anno.ui.editor.graphs

import me.anno.config.DefaultStyle.black
import me.anno.config.DefaultStyle.white
import me.anno.gpu.GFX
import me.anno.gpu.GFX.loadTexturesSync
import me.anno.gpu.TextureLib.whiteTexture
import me.anno.input.Input
import me.anno.input.Input.isShiftDown
import me.anno.input.Input.mouseDownX
import me.anno.input.Input.mouseDownY
import me.anno.input.Input.mouseKeysDown
import me.anno.input.Input.mouseX
import me.anno.input.Input.mouseY
import me.anno.input.MouseButton
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.animation.Keyframe
import me.anno.studio.RemsStudio.onSmallChange
import me.anno.studio.Studio
import me.anno.studio.Studio.editorTime
import me.anno.studio.Studio.editorTimeDilation
import me.anno.studio.Studio.selectedProperty
import me.anno.ui.editor.TimelinePanel
import me.anno.ui.style.Style
import me.anno.utils.*
import org.joml.Vector2f
import java.lang.Exception
import kotlin.math.*
import me.anno.input.Input.isControlDown as isControlDown


// todo make music x times calmer, if another audio line (voice) is on as an optional feature

// todo a method to easily create curves? splines?...

class GraphEditorBody(style: Style): TimelinePanel(style.getChild("deep")){

    var centralValue = 0f
    var dvHalfHeight = 1f

    var draggedKeyframe: Keyframe<*>? = null
    var draggedChannel = 0

    var lastUnitScale = 1f

    // style
    var dotSize = style.getSize("dotSize", 8)

    val selectedKeyframes = HashSet<Keyframe<*>>()

    var isSelecting = false
    val select0 = Vector2f()

    var activeChannels = -1


    fun normValue01(value: Float) = 0.5f - (value-centralValue)/dvHalfHeight * 0.5f

    fun getValueAt(my: Float) = centralValue - dvHalfHeight * normAxis11(my, y, h)
    fun getYAt(value: Float) = y + h * normValue01(value)

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        val size = 5
        minW = size
        minH = size
    }

    fun getValueString(value: Float, step: Float) = getValueString(abs(value), step, if(value < 0) '-' else '+')

    fun getValueString(value: Float, step: Float, sign: Char): String {
        val int = value.toInt()
        if(step >= 1f) return "$sign$int"
        val float = value % 1
        if(step >= 0.1f) return "$sign$int.${(float*10).roundToInt()}"
        if(step >= 0.01f) return "$sign$int.${get0XString((float*100).roundToInt())}"
        return "$sign$int.${get00XString((float*1000).roundToInt())}"
    }

    fun getValueStep(value: Float): Float {
        return valueFractions.minBy { abs(it - value) }!!
    }

    val valueFractions = listOf(
        0.1f, 0.2f, 0.5f, 1f,
        2f, 5f, 10f, 15f, 30f, 45f,
        90f, 120f, 180f, 360f, 720f
    )


    fun drawValueAxis(x0: Int, y0: Int, x1: Int, y1: Int){

        val minValue = centralValue - dvHalfHeight
        val maxValue = centralValue + dvHalfHeight

        val deltaValue = 2 * dvHalfHeight
        val valueStep = getValueStep(deltaValue * 0.2f)

        val minStepIndex = (minValue / valueStep).toInt() - 1
        val maxStepIndex = (maxValue / valueStep).toInt() + 1

        for(stepIndex in maxStepIndex downTo minStepIndex){
            val value = stepIndex * valueStep
            val y = getYAt(value).roundToInt()
            if(y > y0+1 && y+2 < y1){
                val text = getValueString(value, valueStep)
                val size = GFX.getTextSize(fontName, tinyFontSize, isBold, isItalic, text, -1)
                val h = size.second
                GFX.drawRect(x0 + size.first + 2, y, x1-x0-size.first, 1, fontColor and 0x3fffffff)
                GFX.drawText(x0 + 2, y - h/2, fontName, tinyFontSize, isBold, isItalic,
                    text, fontColor, backgroundColor, -1)
            }
        }

    }

    fun drawCurrentTime(){
        loadTexturesSync.push(true)
        val timeFontSize = 20
        val text = getTimeString(Studio.editorTime, 0.0)
        val (tw, th) = GFX.getTextSize(fontName, timeFontSize, isBold, isItalic, text, -1)
        val color = mixARGB(fontColor, backgroundColor, 0.8f)
        GFX.drawText(x+(w-tw)/2, y+(h-th)/2, fontName, timeFontSize, isBold, isItalic, text, color, backgroundColor, -1)
        loadTexturesSync.pop()
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {

        drawBackground()

        val targetUnitScale = Studio.selectedProperty?.type?.unitScale ?: lastUnitScale
        if(lastUnitScale != targetUnitScale){
            val scale = targetUnitScale / lastUnitScale
            centralValue *= scale
            dvHalfHeight *= scale
            lastUnitScale = targetUnitScale
            clampValues()
        }

        drawCurrentTime()

        drawTimeAxis(x0, y0, x1, y1, true)

        val property = selectedProperty ?: return
        if(!property.isAnimated) return

        // only required, if there are values
        drawValueAxis(x0, y0, x1, y1)

        val type = property.type
        val halfSize = dotSize/2

        val blueish = 0x7799ff
        val red = 0xff0000
        val green = 0x00ff00
        val blue = 0x0000ff

        val valueColors = intArrayOf(
            red, green, blue, white
        )

        when(type){
            AnimatedProperty.Type.FLOAT -> {
                valueColors[0] = blueish
            }
            else -> {}
        }

        for(i in 0 until 4){
            valueColors[i] = (valueColors[i] or black) and 0x7fffffff
        }

        val channelCount = property.type.components
        // val values = FloatArray(channelCount)

        val minSelectX = min(mouseDownX, mouseX).toInt()
        val maxSelectX = max(mouseDownX, mouseX).toInt()
        val minSelectY = min(mouseDownY, mouseY).toInt()
        val maxSelectY = max(mouseDownY, mouseY).toInt()
        val selectX = minSelectX-halfSize .. maxSelectX+halfSize
        val selectY = minSelectY-halfSize .. maxSelectY+halfSize

        fun drawDot(x: Int, y: Int, color: Int, willBeSelected: Boolean){
            if(willBeSelected){// draw outline, if point is selected
                GFX.drawTexture(x-halfSize-1, clamp(y-halfSize-1, y0-1, y1),
                    dotSize+2, dotSize+2,
                    whiteTexture, -1, null)
            }
            GFX.drawTexture(x-halfSize, clamp(y-halfSize, y0-1, y1),
                dotSize, dotSize,
                whiteTexture, color, null)
        }

        // draw selection box
        if(isSelecting){

            // draw borders
            GFX.drawTexture(minSelectX, minSelectY,
                maxSelectX-minSelectX, 1,
                whiteTexture, black, null)
            GFX.drawTexture(minSelectX, minSelectY,
                1, maxSelectY-minSelectY,
                whiteTexture, black, null)
            GFX.drawTexture(minSelectX, maxSelectY,
                maxSelectX-minSelectX, 1,
                whiteTexture, black, null)
            GFX.drawTexture(maxSelectX, minSelectY,
                1, maxSelectY-minSelectY,
                whiteTexture, black, null)

            // draw inner
            if(minSelectX+1 < maxSelectX && minSelectY+1 < maxSelectY) GFX.drawTexture(minSelectX+1, minSelectY+1,
                maxSelectX-minSelectX-2, maxSelectY-minSelectY-2,
                whiteTexture, black and 0x77000000, null)

        }


        // draw all data points
        val yValues = IntArray(type.components)
        property.keyframes.forEach { kf ->

            val keyTime = kf.time
            val keyValue = kf.value
            val x = getXAt(keyTime).roundToInt()

            for(i in 0 until channelCount){
                val value = keyValue!![i]
                yValues[i] = getYAt(value).roundToInt()
            }

            var willBeSelected = kf in selectedKeyframes
            if(!willBeSelected && isSelecting && x in selectX){
                for(i in 0 until channelCount){
                    if(yValues[i] in selectY && i.isChannelActive()){
                        willBeSelected = true
                        break
                    }
                }
            }

            for(i in 0 until channelCount){
                drawDot(x, yValues[i], valueColors[i], willBeSelected && (draggedKeyframe !== kf || draggedChannel == i))
            }

            // GFX.drawRect(x.toInt()-1, y+h/2, 2,2, black or 0xff0000)
        }

    }

    fun Int.isChannelActive() = ((1 shl this) and activeChannels) != 0

    fun getKeyframeAt(x: Float, y: Float): Pair<Keyframe<*>, Int>? {
        val property = selectedProperty ?: return null
        var bestDragged: Keyframe<*>? = null
        var bestChannel = 0
        val maxMargin = dotSize*2f/3f + 1f
        var bestDistance = maxMargin
        property.keyframes.forEach { keyframe ->
            val dx = x - getXAt(keyframe.time)
            if(abs(dx) < maxMargin){
                for(channel in 0 until property.type.components){
                    if(channel.isChannelActive()){
                        val dy = y - getYAt(keyframe.getValue(channel))
                        if(abs(dy) < maxMargin){
                            val distance = length(dx.toFloat(), dy)
                            if(distance < bestDistance){
                                bestDragged = keyframe
                                bestChannel = channel
                                bestDistance = distance
                            }
                        }
                    }
                }
            }
        }
        return bestDragged?.to(bestChannel)
    }

    // todo scale a group of selected keyframes
    // todo move a group of selected keyframes
    // todo select full keyframes, or partial keyframes?
    fun getAllKeyframes(minX: Float, maxX: Float, minY: Float, maxY: Float): List<Keyframe<*>> {
        if(minX > maxX || minY > maxY) return getAllKeyframes(min(minX, maxX), max(minX, maxX), min(minY, maxY), max(minY, maxY))
        val halfSize = dotSize/2
        val property = selectedProperty ?: return emptyList()
        val keyframes = ArrayList<Keyframe<*>>()
        keyframes@for(keyframe in property.keyframes){
            if(getXAt(keyframe.time) in minX-halfSize .. maxX+halfSize){
                for(channel in 0 until property.type.components){
                    if(channel.isChannelActive()){
                        if(getYAt(keyframe.getValue(channel)) in minY-halfSize .. maxY+halfSize){
                            keyframes += keyframe
                            continue@keyframes
                        }
                    }
                }
            }
        }
        return keyframes
    }

    // todo only work on one channel, vs working on all?
    // todo this would allow us to copy only z for example

    // todo if there are multiples selected, allow them to be moved (?)

    override fun onMouseDown(x: Float, y: Float, button: MouseButton) {
        // find the dragged element
        draggedKeyframe = null
        if(button.isLeft){
            isSelecting = isShiftDown
            if(!isSelecting){ selectedKeyframes.clear() }
            val keyframeChannel = getKeyframeAt(x, y)
            if(keyframeChannel != null){
                val (keyframe, channel) = keyframeChannel
                draggedKeyframe = keyframe
                draggedChannel = channel
                selectedKeyframes.add(keyframe) // was not found -> add it
            } else {
                select0.x = x
                select0.y = y
            }
        }
    }

    // todo always show the other properties, too???
    override fun onMouseUp(x: Float, y: Float, button: MouseButton) {
        draggedKeyframe = null
        if(isSelecting){
            // add all keyframes in that area
            selectedKeyframes += getAllKeyframes(select0.x, x, select0.y, y)
            isSelecting = false
        }
    }

    override fun onDeleteKey(x: Float, y: Float) {
        var wasChanged = false
        selectedKeyframes.forEach {
            if(selectedProperty?.remove(it) == true){
                wasChanged = true
            }
        }
        if(selectedProperty == null){
            wasChanged = wasChanged || selectedKeyframes.isNotEmpty()
            selectedKeyframes.clear()
        }
        if(wasChanged){
            onSmallChange("graph-delete")
        }
    }

    fun moveUp(sign: Float){
        val delta = sign * dvHalfHeight * movementSpeed / h
        centralValue += delta
        clampTime()
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        when(action){
            "MoveUp" -> moveUp(1f)
            "MoveDown" -> moveUp(-1f)
            else -> return super.onGotAction(x, y, dx, dy, action, isContinuous)
        }
        return true
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        GFX.editorHoverTime = getTimeAt(x)
        val draggedKeyframe = draggedKeyframe
        if(isSelecting){
            // select new elements, update the selected keyframes?
        } else if(draggedKeyframe != null){
            // dragging
            val time = getTimeAt(x)
            draggedKeyframe.time = time
            Studio.editorTime = time
            Studio.updateAudio()
            draggedKeyframe.setValue(draggedChannel, getValueAt(y))
            selectedProperty?.sort()
            onSmallChange("graph-drag")
        } else {
            if(0 in mouseKeysDown){
                if((isShiftDown || isControlDown) && editorTimeDilation == 0.0){
                    // scrubbing
                    editorTime = getTimeAt(x)
                } else {
                    // move left/right/up/down
                    centralTime -= dx * dtHalfLength / (w/2)
                    centralValue += dy * dvHalfHeight / (h/2)
                    clampTime()
                    clampValues()
                }
            }
        }
    }

    override fun onDoubleClick(x: Float, y: Float, button: MouseButton) {
        val property = selectedProperty
        property?.apply {
            val time = getTimeAt(x)
            property.addKeyframe(time, property[time]!!, propertyDt)
            onSmallChange("graph-add")
        } ?: println("Please select a property first!")
    }

    fun clampValues(){
        dvHalfHeight = clamp(dvHalfHeight, 0.001f * lastUnitScale, 1000f * lastUnitScale)
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        // paste keyframes
        // todo convert the values, if required
        // move them? :)
        // paste float/vector values at the mouse position?
        try {
            val time0 = getTimeAt(x)
            val target = selectedProperty ?: return super.onPaste(x, y, data, type)
            val targetType = target.type
            val parsedKeyframes = TextReader.fromText(data)
            parsedKeyframes.forEach { sth ->
                (sth as? Keyframe<*>)?.apply {
                    if(targetType.accepts(value)){
                        target.addKeyframe(time + time0, value!!)
                    } else println("$targetType doesn't accept $value")
                }
            }
            onSmallChange("graph-paste")
        } catch (e: Exception){
            e.printStackTrace()
            super.onPaste(x, y, data, type)
        }
    }

    // todo scale and move a selection of keyframes xD
    override fun onCopyRequested(x: Float, y: Float): String? {
        // copy keyframes
        // left anker or center? left for now
        val time0 = selectedKeyframes.minBy { it.time }?.time ?: 0.0
        return TextWriter.toText(
            selectedKeyframes
            .map { Keyframe(it.time - time0, it.value) }
            .toList(), false)
    }

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float) {
        val delta = dx-dy
        val scale = pow(1.05f, delta)
        if(isShiftDown){
            dvHalfHeight *= scale
            clampValues()
        } else {// time
            super.onMouseWheel(x, y, dx, dy)
        }
    }

}