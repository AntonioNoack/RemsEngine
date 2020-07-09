package me.anno.audio

import org.lwjgl.BufferUtils
import org.lwjgl.openal.AL10.AL_FORMAT_STEREO16
import org.lwjgl.openal.ALC10
import org.lwjgl.openal.ALC11.*
import java.nio.ByteBuffer

// alc = audio library context
class AudioRecording {

    val sampleRate = 44100
    val sampleSize = 1024

    val buffer = BufferUtils.createByteBuffer(sampleRate / 2)

    init {

        ALBase.check()

        val device = alcCaptureOpenDevice(null as ByteBuffer?, sampleRate, AL_FORMAT_STEREO16, sampleSize)

        ALBase.check()

        alcCaptureStart(device)

        val infoBuffer = BufferUtils.createIntBuffer(1)
        while(true){

            // todo parallely somehow play the audio...
            // todo this is kind of awkward...

            ALC10.alcGetIntegerv(device, ALC_CAPTURE_SAMPLES, infoBuffer)
            alcCaptureSamples(device, buffer, infoBuffer[0])

            // todo save the samples
            // todo alias save them to a file
            // todo or just send them to ffmpeg when rendering

        }

        alcCaptureStop(device)
        alcCaptureCloseDevice(device)

        ALBase.check()


    }

}

/**
#include <OpenAL/al.h>
#include <OpenAL/alc.h>
#include <iostream>
using namespace std;

const int SRATE = 44100;
const int SSIZE = 1024;

ALbyte buffer[22050];
ALint sample;

int main(int argc, char *argv[]) {
alGetError();
ALCdevice *device = alcCaptureOpenDevice(NULL, SRATE, AL_FORMAT_STEREO16, SSIZE);
if (alGetError() != AL_NO_ERROR) {
return 0;
}
alcCaptureStart(device);

while (true) {
alcGetIntegerv(device, ALC_CAPTURE_SAMPLES, (ALCsizei)sizeof(ALint), &sample);
alcCaptureSamples(device, (ALCvoid *)buffer, sample);

// ... do something with the buffer
}

alcCaptureStop(device);
alcCaptureCloseDevice(device);

return 0;
}
 *
 * */