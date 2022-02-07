package me.anno.remsstudio.video

import me.anno.io.files.FileReference
import me.anno.remsstudio.animation.AnimatedProperty
import me.anno.remsstudio.audio.AudioCreatorV2
import me.anno.remsstudio.objects.Audio
import me.anno.remsstudio.objects.Camera
import me.anno.remsstudio.objects.Transform
import me.anno.video.VideoAudioCreator
import me.anno.video.VideoCreator

class VideoAudioCreatorV2(
    videoCreator: VideoCreator,
    scene: Transform,
    camera: Camera,
    durationSeconds: Double,
    sampleRate: Int,
    audioSources: List<Audio>,
    motionBlurSteps: AnimatedProperty<Int>,
    shutterPercentage: AnimatedProperty<Float>,
    output: FileReference
) : VideoAudioCreator(
    videoCreator,
    VideoBackgroundTaskV2(videoCreator, scene, camera, motionBlurSteps, shutterPercentage),
    AudioCreatorV2(scene, camera, audioSources, durationSeconds, sampleRate),
    output
)