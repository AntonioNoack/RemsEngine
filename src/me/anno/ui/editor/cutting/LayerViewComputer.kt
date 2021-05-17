package me.anno.ui.editor.cutting

import me.anno.config.DefaultStyle
import me.anno.objects.Transform
import me.anno.objects.Video
import me.anno.studio.rems.RemsStudio
import me.anno.ui.editor.TimelinePanel
import org.joml.Vector4f

class LayerViewComputer(val view: LayerView) {

    var isCalculating = false
    lateinit var calculated: List<Transform>

    fun calculateSolution(x0: Int, y0: Int, x1: Int, y1: Int, asnyc: Boolean) {

        isCalculating = true
        view.needsUpdate = false

        if (asnyc) {
            LayerView.taskQueue += {
                calculateSolution(x0, y0, x1, y1, false)
            }
            return
        }

        val solution = LayerStripeSolution(x0, y0, x1, y1, TimelinePanel.centralTime)
        val stripes = solution.lines

        val root = RemsStudio.root

        val transforms = view.findElements()

        // load all metas
        for(transform in transforms){
            if (transform is Video) {
                transform.update()
                transform.forcedMeta
            }
        }

        this.calculated = transforms

        val timelineSlot = view.timelineSlot
        val drawn = transforms.filter { it.timelineSlot.value == timelineSlot }.reversed()
        view.drawn = drawn

        val stepSize = 1

        if (drawn.isNotEmpty()) {

            val leftTime = view.getTimeAt(x0.toFloat())
            val dt = TimelinePanel.dtHalfLength * 2.0 / view.w
            val white = DefaultStyle.white4

            val size = transforms.size

            // hashmaps are slower, but thread safe
            val localTime = DoubleArray(size)
            val localColor = Array(size) { Vector4f() }

            val parentIndices = IntArray(size)
            val transformMap = HashMap<Transform, Int>()
            for (i in transforms.indices) {
                transformMap[transforms[i]] = i
            }

            for (i in 1 until transforms.size) {
                parentIndices[i] = transformMap[transforms[i].parent]!!
            }

            val drawnIndices = drawn.map { transformMap[it]!! }

            for (x in x0 until x1 step stepSize) {

                val i = x - x0
                var lineIndex = 0
                val globalTime = leftTime + i * dt

                val rootTime = root.getLocalTime(globalTime)
                localTime[0] = rootTime
                localColor[0] = root.getLocalColor(white, rootTime)

                for (index in 1 until size) {
                    val parent = parentIndices[index]
                    val transform = transforms[index]
                    val parentTime = localTime[parent]
                    val localTimeI = transform.getLocalTime(parentTime)
                    localTime[index] = localTimeI
                    localColor[index] = transform.getLocalColor(localColor[parent], localTimeI, localColor[index])
                }

                trs@ for (index in drawnIndices) {

                    val tr = transforms[index]

                    val color = localColor[index]
                    val time = localTime[index]
                    val alpha = color.w * view.alphaMultiplier

                    if (!tr.isVisible(time)) continue

                    if (alpha >= LayerView.minAlpha) {

                        color.w = alpha

                        val list = stripes[lineIndex]
                        if (list.isEmpty()) {
                            if (alpha > LayerView.minAlpha) {
                                list += Gradient(tr, x, x, color, color)
                            } // else not worth it
                        } else {
                            val last = list.last()
                            if (last.owner === tr && last.x1 + stepSize >= x && last.isLinear(x, stepSize, color)) {
                                last.setEnd(x, stepSize, color)
                            } else {
                                list += Gradient(tr, x - stepSize + 1, x, color, color)
                            }
                        }

                        if (++lineIndex >= LayerView.maxLines) {
                            break@trs
                        }

                    }

                }
            }
        }

        stripes.forEach { list ->
            list.removeIf { !it.needsDrawn() }
        }

        view.solution = solution
        isCalculating = false
        view.invalidateDrawing()

    }

}