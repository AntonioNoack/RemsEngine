package me.anno.objects.rendering

import me.anno.objects.Transform
import me.anno.objects.animation.AnimatedProperty
import me.anno.studio.Studio.addEvent
import me.anno.studio.Studio.project
import me.anno.studio.Studio.targetDuration
import me.anno.ui.base.TextPanel
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.frames.FrameSizeInput
import me.anno.ui.input.EnumInput
import me.anno.ui.style.Style
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.max

object RenderSettings : Transform(){

    override fun getDefaultDisplayName(): String = "Render Settings"

    override fun createInspector(list: PanelListY, style: Style) {
        super.createInspector(list, style)
        val project = project!!
        list.clear()
        list += TextPanel(getDefaultDisplayName(), style)
        list += VI("Duration", "Video length in seconds", AnimatedProperty.Type.FLOAT_PLUS, targetDuration, style){
            project.targetDuration = it
            save()
        }
        list += VI("Relative Frame Size (%)", "For rendering tests, in percent", AnimatedProperty.Type.FLOAT_PERCENT, project.targetSizePercentage, style){
            project.targetSizePercentage = it
            save()
        }
        list += FrameSizeInput("Frame Size", "${project.targetWidth}x${project.targetHeight}", style)
            .setChangeListener { w, h ->
                project.targetWidth = max(1, w)
                project.targetHeight = max(1, h)
                save()
            }
            .setTooltip("Size of resulting video")
        list += EnumInput("Framerate", true, project.targetFPS.toString(), setOf(
            project.targetFPS, 30.0, 24.0, 60.0, 120.0, 144.0, 240.0).sorted().toList().map { it.toString() }, style)
            .setChangeListener { value, _, _ ->
                project.targetFPS = value.toDouble()
                save()
            }
            .setTooltip("The fps of the video, or how many frame are shown per second")
    }

    var lastSavePoint = 0L
    var wasChanged = false
    fun save(){
        val time = System.nanoTime()
        if(abs(time-lastSavePoint) > 500_000_000L){// 500ms, saving 2/s
            actuallySave()
        } else {
            wasChanged = true
            thread {
                Thread.sleep(500) // save
                if(wasChanged){
                    addEvent {
                        if(wasChanged){// yes, checking twice makes sense
                            actuallySave()
                        }
                    }
                }
            }
        }
    }

    fun actuallySave(){
        wasChanged = false
        project!!.saveConfig()
    }

}