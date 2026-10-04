package me.anno.ecs.components.audio

import me.anno.Time
import me.anno.audio.openal.AudioTasks.addAudioTask
import me.anno.audio.openal.SoundListener
import me.anno.ecs.components.audio.AudioComponentBase.Companion.currCamDirY
import me.anno.ecs.components.audio.AudioComponentBase.Companion.currCamDirZ
import me.anno.ecs.components.audio.AudioComponentBase.Companion.currCamPos
import me.anno.ecs.components.audio.AudioComponentBase.Companion.prevCamPos
import me.anno.engine.ui.render.RenderState

object AudioCamera {

    private var lastCameraUpdate = 0L

    fun setCameraPosition() {
        // set this here, because RenderState might be overridden, e.g., by thumbs,
        // when the audio task is being run
        currCamPos.set(RenderState.cameraPosition)
        currCamDirY.set(RenderState.cameraDirectionUp)
        currCamDirZ.set(RenderState.cameraDirection)
    }

    fun updateCameraAsync() {
        setCameraPosition()
        addAudioTask("Update", 1) {
            updateCameraSync(setCameraPosition = false)
        }
    }

    fun updateCameraSync(setCameraPosition: Boolean) {

        if (setCameraPosition) setCameraPosition()

        // once per frame, also set the camera :3
        val time = Time.gameTimeNanos
        if (time != lastCameraUpdate) {
            val dt = time - lastCameraUpdate
            lastCameraUpdate = time
            SoundListener.setPosition(currCamPos)
            SoundListener.setVelocity(prevCamPos.sub(currCamPos).mul(-1e9f / dt))
            SoundListener.setOrientation(currCamDirZ, currCamDirY)
            prevCamPos.set(currCamPos)
        }
    }
}