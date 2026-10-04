package me.anno.ecs.components.audio

import me.anno.audio.openal.AudioTasks.addAudioTask
import org.lwjgl.openal.AL10.alDistanceModel
import org.lwjgl.openal.AL11.AL_EXPONENT_DISTANCE_CLAMPED
import org.lwjgl.openal.AL11.AL_INVERSE_DISTANCE_CLAMPED
import org.lwjgl.openal.AL11.AL_LINEAR_DISTANCE_CLAMPED

enum class AttenuationModel(
    val id: Int, val alId: Int,
) {
    LINEAR(0, AL_LINEAR_DISTANCE_CLAMPED),
    INVERSE(1, AL_INVERSE_DISTANCE_CLAMPED),
    EXPONENTIAL(2, AL_EXPONENT_DISTANCE_CLAMPED);

    companion object {

        var model = INVERSE
            set(value) {
                if (field != value) {
                    field = value
                    addAudioTask("dm", 1) {
                        updateGlobalDistanceModel()
                    }
                }
            }

        fun updateGlobalDistanceModel() {
            alDistanceModel(model.alId)
        }
    }
}