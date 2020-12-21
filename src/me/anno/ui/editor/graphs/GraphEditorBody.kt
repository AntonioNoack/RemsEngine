package me.anno.ui.editor.graphs

import me.anno.config.DefaultStyle.black
import me.anno.config.DefaultStyle.white
import me.anno.gpu.GFX
import me.anno.gpu.GFX.loadTexturesSync
import me.anno.gpu.GFXx2D.drawRect
import me.anno.gpu.GFXx2D.drawText
import me.anno.gpu.GFXx2D.drawTexture
import me.anno.gpu.GFXx2D.getTextSize
import me.anno.gpu.TextureLib.whiteTexture
import me.anno.input.Input.isControlDown
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
import me.anno.objects.animation.Interpolation
import me.anno.objects.animation.Keyframe
import me.anno.objects.animation.Type
import me.anno.studio.rems.RemsStudio
import me.anno.studio.rems.RemsStudio.editorTime
import me.anno.studio.rems.RemsStudio.isPaused
import me.anno.studio.rems.RemsStudio.selectedProperty
import me.anno.studio.StudioBase.Companion.updateAudio
import me.anno.ui.editor.TimelinePanel
import me.anno.ui.editor.sceneView.Grid.drawSmoothLine
import me.anno.ui.style.Style
import me.anno.utils.*
import me.anno.utils.Maths.clamp
import me.anno.utils.Maths.length
import me.anno.utils.Maths.mix
import me.anno.utils.Maths.mixARGB
import me.anno.utils.Maths.pow
import org.joml.Vector2f
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// todo make music x times calmer, if another audio line (voice) is on as an optional feature

class GraphEditorBody(style: Style) : TimelinePanel(style.getChild("deep")) {

    var draggedKeyframe: Keyframe<*>? = null
    var draggedChannel = 0

    var lastUnitScale = 1f

    // style
    var dotSize = style.getSize("dotSize", 8)

    val selectedKeyframes = HashSet<Keyframe<*>>()

    var isSelecting = false
    val select0 = Vector2f()

    var activeChannels = -1

    override fun getVisualState() = Quad(
        super.getVisualState(), centralValue, dvHalfHeight, selectedProperty
    )

    fun normValue01(value: Float) = 0.5f - (value - centralValue) / dvHalfHeight * 0.5f

    fun getValueAt(my: Float) = centralValue - dvHalfHeight * normAxis11(my, y, h)
    fun getYAt(value: Float) = y + h * normValue01(value)

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        val size = 5
        minW = size
        minH = size
    }

    fun getValueString(value: Float, step: Float) = getValueString(abs(value), step, if (value < 0) '-' else '+')

    fun getValueString(value: Float, step: Float, sign: Char): String {
        val int = value.toInt()
        if (step >= 1f) return "$sign$int"
        val float = value % 1
        if (step >= 0.1f) return "$sign$int.${(float * 10).roundToInt()}"
        if (step >= 0.01f) return "$sign$int.${get0XString((float * 100).roundToInt())}"
        return "$sign$int.${get00XString((float * 1000).roundToInt())}"
    }

    fun getValueStep(value: Float): Float {
        return valueFractions.minBy { abs(it - value) }!!
    }

    fun drawValueAxis(x0: Int, y0: Int, x1: Int, y1: Int) {

        val minValue = centralValue - dvHalfHeight
        val maxValue = centralValue + dvHalfHeight

        val deltaValue = 2 * dvHalfHeight
        val valueStep = getValueStep(deltaValue * 0.2f)

        val minStepIndex = (minValue / valueStep).toInt() - 1
        val maxStepIndex = (maxValue / valueStep).toInt() + 1

        val fontSize = font.size
        val fontColor = fontColor
        val backgroundColor = backgroundColor

        for (stepIndex in maxStepIndex downTo minStepIndex) {
            val value = stepIndex * valueStep
            val y = getYAt(value).roundToInt()
            if (y > y0 + 1 && y + 2 < y1) {

                val text = getValueString(value, valueStep)

                // to keep it loaded
                drawnStrings.add(text)

                val size = getTextSize(font, text, -1)
                val h = size.second
                drawRect(x0 + size.first + 2, y, x1 - x0 - size.first, 1, fontColor and 0x3fffffff)
                drawText(
                    x0 + 2, y - h / 2, font,
                    text, fontColor, backgroundColor, -1
                )

            }
        }

    }

    val timeFont = font.withSize(20f)

    fun drawCurrentTime() {
        loadTexturesSync.push(true)
        val text = getTimeString(editorTime, 0.0)
        val (tw, th) = getTextSize(font, text, -1)
        val color = mixARGB(fontColor, backgroundColor, 0.8f)
        drawText(x + (w - tw) / 2, y + (h - th) / 2, timeFont, text, color, backgroundColor, -1)
        loadTexturesSync.pop()
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {

        drawnStrings.clear()

        drawBackground()

        val targetUnitScale = selectedProperty?.type?.unitScale ?: lastUnitScale
        if (lastUnitScale != targetUnitScale) {
            val scale = targetUnitScale / lastUnitScale
            centralValue *= scale
            dvHalfHeight *= scale
            lastUnitScale = targetUnitScale
            clampValues()
        }

        drawCurrentTime()

        drawTimeAxis(x0, y0, x1, y1, true)

        updateLocalTime()

        val property = selectedProperty ?: return
        if (!property.isAnimated) return

        // only required, if there are values
        drawValueAxis(x0, y0, x1, y1)

        val type = property.type
        val halfSize = dotSize / 2

        val blueish = 0x7799ff
        val red = 0xff0000
        val green = 0x00ff00
        val blue = 0x0000ff

        val channelCount = property.type.components
        val valueColors = intArrayOf(
            if (channelCount == 1) white else red,
            green, blue, white
        )

        when (type) {
            Type.FLOAT -> {
                valueColors[0] = blueish
            }
            else -> {
            }
        }

        for (i in 0 until 4) {
            valueColors[i] = (valueColors[i] or black) and 0x7fffffff
        }

        // val values = FloatArray(channelCount)


        fun drawDot(x: Int, y: Int, color: Int, willBeSelected: Boolean) {
            if (willBeSelected) {// draw outline, if point is selected
                drawTexture(
                    x - halfSize - 1, clamp(y - halfSize - 1, y0 - 1, y1),
                    dotSize + 2, dotSize + 2,
                    whiteTexture, -1, null
                )
            }
            drawTexture(
                x - halfSize, clamp(y - halfSize, y0 - 1, y1),
                dotSize, dotSize,
                whiteTexture, color, null
            )
        }

        val minSelectX = min(mouseDownX, mouseX).toInt()
        val maxSelectX = max(mouseDownX, mouseX).toInt()
        val minSelectY = min(mouseDownY, mouseY).toInt()
        val maxSelectY = max(mouseDownY, mouseY).toInt()
        val selectX = minSelectX - halfSize..maxSelectX + halfSize
        val selectY = minSelectY - halfSize..maxSelectY + halfSize

        // draw selection box
        if (isSelecting) {

            // draw borders
            drawTexture(
                minSelectX, minSelectY,
                maxSelectX - minSelectX, 1,
                whiteTexture, black, null
            )
            drawTexture(
                minSelectX, minSelectY,
                1, maxSelectY - minSelectY,
                whiteTexture, black, null
            )
            drawTexture(
                minSelectX, maxSelectY,
                maxSelectX - minSelectX, 1,
                whiteTexture, black, null
            )
            drawTexture(
                maxSelectX, minSelectY,
                1, maxSelectY - minSelectY,
                whiteTexture, black, null
            )

            // draw inner
            if (minSelectX + 1 < maxSelectX && minSelectY + 1 < maxSelectY) drawTexture(
                minSelectX + 1, minSelectY + 1,
                maxSelectX - minSelectX - 2, maxSelectY - minSelectY - 2,
                whiteTexture, black and 0x77000000, null
            )

        }


        // draw all data points
        val yValues = IntArray(type.components)
        val prevYValues = IntArray(type.components)
        val kfs = property.keyframes

        if(kfs.isNotEmpty()){
            val first = kfs[0]
            when(first.interpolation){
                Interpolation.LINEAR_BOUNDED, Interpolation.STEP, Interpolation.SPLINE -> {
                    if(getXAt(kf2Global(first.time)) > x0+1){// a line is needed from left to 1st point
                        val startValue = property[global2Kf(getTimeAt(x0.toFloat()))]!!
                        val endX = getXAt(kf2Global(first.time)).toFloat()
                        val endValue = first.value!!
                        for (i in 0 until channelCount) {
                            val startY = getYAt(startValue[i])
                            val endY = getYAt(endValue[i])
                            drawSmoothLine(
                                x0.toFloat(), startY, endX, endY,
                                this.x, this.y, this.w, this.h, valueColors[i], 0.5f
                            )
                        }
                    }
                }
                else -> {
                    // ...
                }
            }
        }

        for ((j, kf) in kfs.withIndex()) {

            val tGlobal = kf2Global(kf.time)
            val keyValue = kf.value
            val x = getXAt(tGlobal).roundToInt()

            if (j > 0) {
                System.arraycopy(yValues, 0, prevYValues, 0, yValues.size)
            }

            for (i in 0 until channelCount) {
                val value = keyValue!![i]
                yValues[i] = getYAt(value).roundToInt()
            }

            if (j > 0) {// draw all lines
                drawLines(property, x, x0, x1, j, tGlobal, channelCount, valueColors)
            }

            var willBeSelected = kf in selectedKeyframes
            if (!willBeSelected && isSelecting && x in selectX) {
                for (i in 0 until channelCount) {
                    if (yValues[i] in selectY && i.isChannelActive()) {
                        willBeSelected = true
                        break
                    }
                }
            }

            for (i in 0 until channelCount) {
                drawDot(
                    x, yValues[i], valueColors[i],
                    willBeSelected && (draggedKeyframe !== kf || draggedChannel == i)
                )
            }

        }

        // todo not correct, because of clamping...
        // start is either
        // todo we need to calculate the correct cutting point with min or max...
        // todo sine is crazy as well ->
        // todo we need a function similar to drawLines()
        if(kfs.isNotEmpty()){
            val last = kfs.last()
            when(last.interpolation){
                Interpolation.SPLINE, Interpolation.STEP, Interpolation.LINEAR_BOUNDED -> {
                    if(getXAt(kf2Global(last.time)) < x1){// a line is needed from last pt to end
                        val endValue = property[global2Kf(getTimeAt(x1.toFloat()))]!!
                        val startX = getXAt(kf2Global(last.time)).toFloat()
                        val startValue = last.value!!
                        for (i in 0 until channelCount) {
                            val endY = getYAt(endValue[i])
                            val startY = getYAt(startValue[i])
                            drawSmoothLine(
                                startX, startY, x1.toFloat(), endY,
                                this.x, this.y, this.w, this.h, valueColors[i], 0.5f
                            )
                        }
                    }
                }
                else -> {
                    // ...
                }
            }
        }

    }

    // todo draw curve of animation-drivers :)
    // todo input (animated values) and output (calculated values)?

    fun drawLines(property: AnimatedProperty<*>,
                  x: Int, x0: Int, x1: Int,
                  j: Int, endTime: Double,
                  channelCount: Int, valueColors: IntArray){
        val kfs = property.keyframes
        val previous = kfs[j - 1]
        val tGlobalPrev = mix(0.0, 1.0, kf2Global(previous.time))
        val prevX = getXAt(tGlobalPrev).roundToInt()
        val minX = max(prevX, x0)
        val maxX = min(x, x1)
        val stepSize = max(1, (maxX - minX) / 30)
        val t0 = getTimeAt(minX.toFloat())
        for (i in 0 until channelCount) {
            var lastX = minX
            var lastY = getYAt(property[global2Kf(t0)]!![i])
            fun addLine(xHere: Int, tGlobalHere: Double) {
                val value = property[global2Kf(tGlobalHere)]!!
                val yHere = getYAt(value[i])
                if (xHere > lastX && xHere >= x0 && lastX < x1) {
                    drawSmoothLine(
                        lastX.toFloat(), lastY, xHere.toFloat(), yHere,
                        this.x, this.y, this.w, this.h, valueColors[i], 0.5f
                    )
                }
                lastX = xHere
                lastY = yHere
            }
            // draw differently depending on section mode
            when(previous.interpolation){
                Interpolation.LINEAR_BOUNDED, Interpolation.LINEAR_UNBOUNDED -> {
                    addLine(x, endTime) // done
                }
                Interpolation.STEP -> {
                    val startTime = kf2Global(previous.time)
                    var time: Double
                    time = mix(startTime, endTime, 0.4999)
                    addLine(getXAt(time).toInt(), time)
                    time = mix(startTime, endTime, 0.5001)
                    addLine(getXAt(time).toInt(), time)
                    addLine(x, endTime)
                }
                // Interpolation.SPLINE,
                else -> {
                    // steps in between are required
                    for (xHere in minX until maxX step stepSize) {
                        val tGlobalHere = getTimeAt(xHere.toFloat())
                        addLine(xHere, tGlobalHere)
                    }
                    addLine(x, endTime)
                }
            }
        }
    }

    fun Int.isChannelActive() = ((1 shl this) and activeChannels) != 0

    fun getKeyframeAt(x: Float, y: Float): Pair<Keyframe<*>, Int>? {
        val property = selectedProperty ?: return null
        var bestDragged: Keyframe<*>? = null
        var bestChannel = 0
        val maxMargin = dotSize * 2f / 3f + 1f
        var bestDistance = maxMargin
        property.keyframes.forEach { kf ->
            val globalT = mix(0.0, 1.0, kf2Global(kf.time))
            val dx = x - getXAt(globalT)
            if (abs(dx) < maxMargin) {
                for (channel in 0 until property.type.components) {
                    if (channel.isChannelActive()) {
                        val dy = y - getYAt(kf.getValue(channel))
                        if (abs(dy) < maxMargin) {
                            val distance = length(dx.toFloat(), dy)
                            if (distance < bestDistance) {
                                bestDragged = kf
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
        if (minX > maxX || minY > maxY) return getAllKeyframes(
            min(minX, maxX),
            max(minX, maxX),
            min(minY, maxY),
            max(minY, maxY)
        )
        val halfSize = dotSize / 2
        val property = selectedProperty ?: return emptyList()
        val keyframes = ArrayList<Keyframe<*>>()
        keyframes@ for (kf in property.keyframes) {
            val globalT = mix(0.0, 1.0, kf2Global(kf.time))
            if (getXAt(globalT) in minX - halfSize..maxX + halfSize) {
                for (channel in 0 until property.type.components) {
                    if (channel.isChannelActive()) {
                        if (getYAt(kf.getValue(channel)) in minY - halfSize..maxY + halfSize) {
                            keyframes += kf
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
        invalidateDrawing()
        draggedKeyframe = null
        if (button.isLeft) {
            isSelecting = isShiftDown
            if (!isSelecting) {
                selectedKeyframes.clear()
            }
            val keyframeChannel = getKeyframeAt(x, y)
            if (keyframeChannel != null) {
                val (keyframe, channel) = keyframeChannel
                draggedKeyframe = keyframe
                draggedChannel = channel
                selectedKeyframes.add(keyframe) // was not found -> add it
            } else {
                select0.x = x
                select0.y = y
            }
        }
        invalidateDrawing()
    }

    // todo always show the other properties, too???
    // todo maybe add a list of all animated properties?
    override fun onMouseUp(x: Float, y: Float, button: MouseButton) {
        draggedKeyframe = null
        if (isSelecting) {
            // add all keyframes in that area
            selectedKeyframes += getAllKeyframes(select0.x, x, select0.y, y)
            isSelecting = false
        }
        invalidateDrawing()
    }

    override fun onDeleteKey(x: Float, y: Float) {
        RemsStudio.largeChange("Deleted Keyframes"){
            selectedKeyframes.forEach {
                selectedProperty?.remove(it)
            }
            if (selectedProperty == null) {
                selectedKeyframes.clear()
            }
        }
    }

    fun moveUp(sign: Float) {
        val delta = sign * dvHalfHeight * movementSpeed / h
        centralValue += delta
        clampTime()
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        when (action) {
            "MoveUp" -> moveUp(1f)
            "MoveDown" -> moveUp(-1f)
            else -> return super.onGotAction(x, y, dx, dy, action, isContinuous)
        }
        return true
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        GFX.editorHoverTime = getTimeAt(x)
        val draggedKeyframe = draggedKeyframe
        val selectedProperty = selectedProperty
        if (isSelecting) {
            // select new elements, update the selected keyframes?
            invalidateDrawing()
        } else if (draggedKeyframe != null && selectedProperty != null) {
            // dragging
            val time = getTimeAt(x)
            RemsStudio.incrementalChange("dragging keyframe"){
                draggedKeyframe.time = global2Kf(time) // global -> local
                editorTime = time
                updateAudio()
                draggedKeyframe.setValue(draggedChannel, getValueAt(y), selectedProperty.type)
                selectedProperty.sort()
            }
            invalidateDrawing()
        } else {
            if (0 in mouseKeysDown) {
                if ((isShiftDown || isControlDown) && isPaused) {
                    // scrubbing
                    editorTime = getTimeAt(x)
                } else {
                    // move left/right/up/down
                    centralTime -= dx * dtHalfLength / (w / 2)
                    centralValue += dy * dvHalfHeight / (h / 2)
                    clampTime()
                    clampValues()
                }
                invalidateDrawing()
            }
        }
    }

    override fun onDoubleClick(x: Float, y: Float, button: MouseButton) {
        val property = selectedProperty
        property?.apply {
            val time = global2Kf(getTimeAt(x))
            RemsStudio.largeChange("Created keyframe at ${time}s"){
                property.addKeyframe(time, property[time]!!, propertyDt)
            }
        } ?: println("Please select a property first!")
    }

    fun clampValues() {
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
            val parsedKeyframes = TextReader.fromText(data).filterIsInstance<Keyframe<*>>()
            if(parsedKeyframes.isNotEmpty()){
                RemsStudio.largeChange("Pasted Keyframes") {
                    parsedKeyframes.forEach { sth ->
                        sth.apply {
                            if (targetType.accepts(value)) {
                                target.addKeyframe(time + time0, value!!)
                            } else LOGGER.warn("$targetType doesn't accept $value")
                        }
                    }
                }
            }
        } catch (e: Exception) {
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
        val delta = dx - dy
        val scale = pow(1.05f, delta)
        if (isShiftDown) {
            dvHalfHeight *= scale
            clampValues()
        } else {// time
            super.onMouseWheel(x, y, dx, dy)
        }
    }

    override fun onSelectAll(x: Float, y: Float) {
        val kf = selectedProperty?.keyframes
        if(kf != null){
            selectedKeyframes.clear()
            selectedKeyframes.addAll(kf)
        }
        invalidateDrawing()
    }

    override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
        when {
            button.isRight -> {
                if (selectedKeyframes.isEmpty()) {
                    super.onMouseClicked(x, y, button, long)
                } else {
                    GFX.openMenu("Interpolation", Interpolation.values().map { mode ->
                        mode.displayName to {
                            selectedKeyframes.forEach {
                                it.interpolation = mode
                            }
                            invalidateDrawing()
                        }
                    })
                }
            }
            else -> super.onMouseClicked(x, y, button, long)
        }
    }

    companion object {
        val valueFractions = listOf(
            0.1f, 0.2f, 0.5f, 1f,
            2f, 5f, 10f, 15f, 30f, 45f,
            90f, 120f, 180f, 360f, 720f
        )
    }

}