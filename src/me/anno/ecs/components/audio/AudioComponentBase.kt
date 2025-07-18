package me.anno.ecs.components.audio

import me.anno.Time
import me.anno.animation.LoopingState
import me.anno.audio.openal.AudioTasks.addAudioTask
import me.anno.audio.openal.SoundListener
import me.anno.audio.streams.AudioFileStreamOpenAL
import me.anno.ecs.Component
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Range
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.ui.render.RenderState
import me.anno.io.MediaMetadata
import me.anno.utils.types.Floats.toLongOr
import org.joml.Vector3f
import org.lwjgl.openal.AL11.AL_EXPONENT_DISTANCE_CLAMPED
import org.lwjgl.openal.AL11.AL_INVERSE_DISTANCE_CLAMPED
import org.lwjgl.openal.AL11.AL_LINEAR_DISTANCE_CLAMPED
import org.lwjgl.openal.AL11.alDistanceModel
import kotlin.math.max

// todo some kind of event system for when music changed

// todo what's the best way to combine audio for local multiplayer? we could use stereo a little for that :)
// (should be disable-able)

/**
 * base class for components that shall play audio;
 * audio can be loaded from files, or be procedurally generated
 * */
abstract class AudioComponentBase : Component(), OnUpdate {

    companion object {

        private var lastCameraUpdate = 0L

        var model = AttenuationModel.INVERSE
            set(value) {
                if (field != value) {
                    field = value
                    addAudioTask("dm", 1) {
                        updateGlobalDistanceModel()
                    }
                }
            }

        val prevCamPos = Vector3f()
        val currCamPos = Vector3f()
        val currCamDirZ = Vector3f()
        val currCamDirY = Vector3f()

        enum class AttenuationModel(val id: Int) {
            LINEAR(0), INVERSE(1), EXPONENTIAL(2)
        }

        fun updateGlobalDistanceModel() {
            alDistanceModel(
                when (model) {
                    AttenuationModel.LINEAR -> AL_LINEAR_DISTANCE_CLAMPED
                    AttenuationModel.INVERSE -> AL_INVERSE_DISTANCE_CLAMPED
                    AttenuationModel.EXPONENTIAL -> AL_EXPONENT_DISTANCE_CLAMPED
                }
            )
        }
    }

    var playMode = PlayMode.ONCE

    // serialized??
    var globalAttenuationModel
        get() = model
        set(value) {
            model = value
        }

    // todo detect that...
    var hasFinished = false

    var startTime = 0L

    @Docs("How loud the audio is")
    @Range(0.0, Double.POSITIVE_INFINITY)
    var volume = 1f
        set(value) {
            val v = max(0f, value)
            if (field != v) {
                field = v
                if (stream0 != null) addAudioTask("volume", 1) {
                    stream0?.alSource?.setVolume(volume)
                    stream1?.alSource?.setVolume(volume)
                }
            }
        }

    @Docs("How fast the audio is played; equivalent to 'pitch'")
    @Range(0.0, Double.POSITIVE_INFINITY)
    var speed = 1f
        set(value) {
            val v = max(0f, value) // less than 0 is not supported
            if (field != v) {
                field = v
                if (stream0 != null) addAudioTask("speed", 1) {
                    stream0?.alSource?.setSpeed(speed)
                    stream1?.alSource?.setSpeed(speed)
                }
            }
        }

    enum class PlayMode(val id: Int) { ONCE(0), LOOP(1) }

    // todo end offset? (pre-ending)

    private var stream0: AudioFileStreamOpenAL? = null
    private var stream1: AudioFileStreamOpenAL? = null

    // if this is > 0, play audio as two sources
    var stereoSeparation = 0f

    private var stopTime = 0.0

    @DebugAction
    fun start() {
        start(0.0)
    }

    open fun start(startTime0: Double) {
        addAudioTask("start", 1) {
            stream0?.stop()
            stream1?.stop()
            startTime = Time.nanoTime - (startTime0 * 1e9).toLongOr()
            this as AudioComponent
            // todo wait for meta async
            val meta = MediaMetadata.getMeta(source).waitFor()
            if (meta != null && meta.hasAudio) {
                updateGlobalDistanceModel()
                // todo loop is not working :/
                if (meta.audioChannels >= 2 && rollOffFactor > 0f && stereoSeparation > 0f) {
                    // create dual channel
                    // OpenAL somehow doesn't support to play stereo sources in 3d... why ever...
                    val stream0 = AudioFileStreamOpenAL(
                        source, if (playMode == PlayMode.LOOP) LoopingState.PLAY_LOOP else LoopingState.PLAY_ONCE,
                        startTime0, false, meta, 1.0, left = true, center = false, right = false
                    )
                    val stream1 = AudioFileStreamOpenAL(
                        source, if (playMode == PlayMode.LOOP) LoopingState.PLAY_LOOP else LoopingState.PLAY_ONCE,
                        startTime0, false, meta, 1.0, left = false, center = false, right = true
                    )
                    this.stream0 = stream0
                    this.stream1 = stream1
                    updateDistanceModel()
                    updatePosition()
                    val src0 = stream0.alSource
                    val src1 = stream1.alSource
                    src0.setVolume(volume)
                    src0.setSpeed(speed)
                    src0.setPosition(lastPosition0)
                    src0.setVelocity(lastVelocity0)
                    src1.setVolume(volume)
                    src1.setSpeed(speed)
                    src1.setPosition(lastPosition1)
                    src1.setVelocity(lastVelocity1)
                    stream0.start()
                    stream1.start()
                } else {
                    val stream = AudioFileStreamOpenAL(
                        source, if (playMode == PlayMode.LOOP) LoopingState.PLAY_LOOP else LoopingState.PLAY_ONCE,
                        startTime0, rollOffFactor <= 0f, meta, 1.0, left = false, center = true, right = false
                    )
                    stream0 = stream
                    stream1 = null
                    updateDistanceModel()
                    updatePosition()
                    stream.alSource.setVolume(volume)
                    stream.alSource.setSpeed(speed)
                    stream.alSource.setPosition(lastPosition0)
                    stream.alSource.setVelocity(lastVelocity0)
                    stream.start()
                }
            } else {
                lastWarning = "Meta could not be loaded"
                stream0 = null
                stream1 = null
            }
        }
    }

    // todo al-native pause and unpause

    var rollOffFactor = 1f
        set(value) {
            if (field != value) {
                field = value
                if (stream0 != null) addAudioTask("distance", 1) { updateDistanceModel() }
            }
        }
    var referenceDistance = 1f
        set(value) {
            val f = max(value, 0f)
            if (field != f) {
                field = f
                if (stream0 != null) addAudioTask("distance", 1) { updateDistanceModel() }
            }
        }
    var maxDistance = 32f
        set(value) {
            if (field != value) {
                field = value
                if (stream0 != null) addAudioTask("distance", 1) { updateDistanceModel() }
            }
        }

    fun updateDistanceModel() {
        val rof = max(0f, rollOffFactor)
        stream0?.alSource?.setDistanceModel(rof, referenceDistance, maxDistance)
        stream1?.alSource?.setDistanceModel(rof, referenceDistance, maxDistance)
    }

    @DebugProperty
    val isPlaying get() = stream0?.isPlaying == true

    @DebugAction
    open fun resume() {
        if (stream0 != null) {
            stream0?.alSource?.play()
            stream1?.alSource?.play()
        } else start(stopTime)
    }

    private val lastPosition0 = Vector3f()
    private val lastVelocity0 = Vector3f()
    private val lastPosition1 = Vector3f()
    private val lastVelocity1 = Vector3f()
    private var lastTime = 0L

    @DebugAction
    open fun pause() {
        if (stream0 == null) return
        addAudioTask("pause", 1) {
            stream0?.alSource?.pause()
            stream1?.alSource?.pause()
        }
    }

    @DebugAction
    open fun stop() {
        if (stream0 == null) return
        (this as? AudioComponent)?.autoStart = false // else kind of pointless
        stopTime = (Time.nanoTime - startTime) * 1e-9
        addAudioTask("stop", 1) {
            stream0?.stop()
            stream1?.stop()
            stream0 = null
            stream1 = null
        }
    }

    override fun destroy() {
        super.destroy()
        stop()
    }

    fun updatePosition() {
        val transform = transform
        if (transform != null) {
            transform.validate()
            val pos = transform.globalPosition
            val time = Time.gameTimeN
            val deltaTime = time - lastTime
            if (deltaTime > 0) {
                lastTime = time
                if (stream1 != null && stereoSeparation > 0f) {
                    val right = transform.globalTransform.transformDirection(Vector3f(stereoSeparation, 0f, 0f))
                    lastVelocity0.set(pos).sub(right).sub(lastPosition0).mul(1e9f / deltaTime)
                    lastPosition0.set(pos)
                    lastVelocity1.set(pos).add(right).sub(lastPosition1).mul(1e9f / deltaTime)
                    lastPosition1.set(pos)
                } else {
                    lastVelocity0.set(pos).sub(lastPosition0).mul(1e9f / deltaTime)
                    lastPosition0.set(pos)
                    lastVelocity1.set(lastVelocity0)
                    lastPosition1.set(lastPosition0)
                }
            }
        }
    }

    override fun onUpdate() {
        val stream0 = stream0
        if (stream0 != null) {

            // set this here, because RenderState might be overridden, e.g., by thumbs,
            // when the audio task is being run
            currCamPos.set(RenderState.cameraPosition)
            currCamDirY.set(RenderState.cameraDirectionUp)
            currCamDirZ.set(RenderState.cameraDirection)

            addAudioTask("Update", 1) {

                // once per frame, also set the camera :3
                val time = Time.gameTimeN
                if (time != lastCameraUpdate) {
                    val dt = time - lastCameraUpdate
                    lastCameraUpdate = time
                    SoundListener.setPosition(currCamPos)
                    SoundListener.setVelocity(prevCamPos.sub(currCamPos).mul(-1e9f / dt))
                    SoundListener.setOrientation(currCamDirZ, currCamDirY)
                    prevCamPos.set(currCamPos)
                }

                val transform = transform
                if (rollOffFactor > 0f && transform != null) {
                    updatePosition()
                    stream0.alSource.setPosition(lastPosition0)
                    stream0.alSource.setVelocity(lastVelocity0)
                    val stream1 = stream1
                    if (stream1 != null) {
                        stream1.alSource.setPosition(lastPosition1)
                        stream1.alSource.setVelocity(lastVelocity1)
                    }
                }
            }
        }
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is AudioComponentBase) return
        dst.playMode = playMode
        dst.volume = volume
        dst.speed = speed
        dst.rollOffFactor = rollOffFactor
        dst.referenceDistance = referenceDistance
        dst.maxDistance = maxDistance
    }
}