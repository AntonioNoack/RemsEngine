package me.anno.remsstudio.ui.editor.cutting

import me.anno.remsstudio.objects.Transform
import me.anno.remsstudio.RemsStudio
import me.anno.remsstudio.ui.sceneTabs.SceneTabs
import me.anno.utils.files.Naming
import org.joml.Vector4f

object SplitTransform {

    fun split(transform: Transform, localTime: Double){
        val fadingTime = 0.2
        val fadingHalf = fadingTime / 2
        transform.color.isAnimated = true
        val lTime = localTime - fadingHalf
        val rTime = localTime + fadingHalf
        val color = transform.color[localTime]
        val lColor = transform.color[lTime]
        val lTransparent = Vector4f(lColor).apply { w = 0f }
        val rColor = transform.color[rTime]
        val rTransparent = Vector4f(rColor).apply { w = 0f }
        val second = transform.clone()
        second.name = Naming.incrementName(transform.name)
        if (transform.parent != null) {
            transform.addAfter(second)
        } else {
            // can't split directly,
            // because we have no parent
            val newRoot = Transform()
            newRoot.addChild(transform)
            newRoot.addChild(second)
            RemsStudio.root = newRoot
            // needs to be updated
            SceneTabs.currentTab?.scene = newRoot
        }
        // transform.color.addKeyframe(localTime-fadingTime/2, color)
        transform.color.checkThread()
        transform.color.keyframes.removeIf { it.time >= localTime }
        transform.color.addKeyframe(localTime, color)
        transform.color.addKeyframe(rTime, rTransparent)
        second.color.checkThread()
        second.color.keyframes.removeIf { it.time <= localTime }
        second.color.addKeyframe(lTime, lTransparent)
        second.color.addKeyframe(localTime, color)
    }

}