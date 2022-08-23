package me.anno.ecs.components.audio

import me.anno.Engine
import me.anno.animation.LoopingState
import me.anno.audio.openal.AudioTasks.addTask
import me.anno.audio.streams.AudioFileStreamOpenAL
import me.anno.ecs.Component
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.video.ffmpeg.FFMPEGMetadata
import org.joml.Vector3d

// todo some kind of event system for when music changed

// todo there need to be multiple types: procedural & loaded

// todo effects maybe? mmh... procedural audio!
// todo what's the best way to compute distance to the main listener? world position

// todo what's the best way to combine audio for local multiplayer? we could use stereo a little for that :)
// (should be disable-able)

abstract class AudioComponentBase : Component() {

    var playMode = PlayMode.ONCE
    var attenuation = Attenuation.CONSTANT

    var isRunning = false
    var isFinished = false

    var startTime = 0L

    var volume = 1f

    enum class PlayMode {
        ONCE,
        LOOP
    }

    enum class Attenuation {
        LINEAR, EXPONENTIAL,
        CONSTANT,
    }

    // todo option to pause audio
    // todo start function with offset
    // todo end offset? (pre-ending)

    private var stream: AudioFileStreamOpenAL? = null

    @DebugAction
    open fun start() {
        addTask("start", 1) {
            stream?.stop()
            startTime = Engine.nanoTime
            // todo allow different methods of audio creation, e.g. procedural audio :3
            this as AudioComponent
            // todo wait for meta async
            val meta = FFMPEGMetadata.getMeta(source, false)
            if (meta != null) {
                // todo how can we control attenuation and such?
                val stream = AudioFileStreamOpenAL(
                    source, if (playMode == PlayMode.LOOP) LoopingState.PLAY_LOOP else LoopingState.PLAY_ONCE,
                    0.0, meta, 1.0
                )
                stream.start()
                this.stream = stream
            } else {
                lastWarning = "Meta could not be loaded"
                this.stream = null
            }
        }
    }

    override fun onUpdate(): Int {
        val stream = stream
        if (stream != null) {
            // todo it might be playing:
            // todo update distance and such
            addTask("Update", 1) {
                if (attenuation != Attenuation.CONSTANT) {
                    val transform = transform
                    if (transform != null) {
                        val pos = transform.globalPosition
                        val time = Engine.nanoTime
                        val deltaTime = (time - lastTime) * 1e-9
                        lastTime = time
                        stream.alSource.setPosition(pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat())
                        stream.alSource.setVelocity(
                            ((pos.x - lastPosition.x) / deltaTime).toFloat(),
                            ((pos.y - lastPosition.y) / deltaTime).toFloat(),
                            ((pos.z - lastPosition.z) / deltaTime).toFloat()
                        )
                        lastPosition.set(pos)
                    } else {
                        stream.alSource.setPosition(0f, 0f, 0f)
                        stream.alSource.setVelocity(0f, 0f, 0f)
                    }
                }
                stream.alSource.setGain(volume)
            }
        }
        return 1
    }

    private val lastPosition = Vector3d()
    private var lastTime = 0L

    @DebugAction
    open fun stop() {
        addTask("stop", 1) {
            stream?.stop()
            stream = null
        }
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as AudioComponentBase
        clone.isRunning = isRunning
        clone.isFinished = isFinished
        clone.playMode = playMode
        clone.attenuation = attenuation
        clone.volume = volume
    }

}