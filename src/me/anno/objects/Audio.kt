package me.anno.objects

import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.AudioInput
import me.anno.ui.style.Style
import org.joml.Matrix4fStack
import org.joml.Vector4f
import java.io.File

// todo openal to ffmpeg?


// todo flat playback vs 3D playback
// todo use the align-with-camera param for that? :)
class Audio(var file: File, parent: Transform?): GFXTransform(parent){

    data class AudioKey(val file: File, val slot: Int)

    // todo we need a flag, whether we draw in editor mode or not
    // todo or a separate function???
    // todo a separate mode, where resource availability is enforced?

    override fun onDraw(stack: Matrix4fStack, time: Float, color: Vector4f) {
        // todo ensure, that the correct buffer is being generated
        // todo we need to invalidate buffers, if we touch the custom timeline mode, or accelerate/decelerate audio...
        // todo how should we generate left/right audio?

    }

    override fun createInspector(list: PanelListY, style: Style) {
        super.createInspector(list, style)
        list += AudioInput(file, style)
    }

    override fun getClassName() = "Audio"

}