# Rem's Studio <!-- 's is correct, because it's owned; I thought it may be only for abbreviations of is -->

This studio is intended for starters in the video industry, or YouTubers,
and was inspired by the channel [YDS](https://www.youtube.com/user/YutsuraidanceStudios). I wanted to be able to make those transitions and motions easily myself. Without needing to buy some expensive software.

That's why Rem's Studio will always be sold **low-priced** to individuals, and may stay free, if you build it yourself (proof of work, that you really can't afford it ;)).
In the alpha times, you all are early testers, so you get it for **free** anyways <3 :D.


The name is from Rem, an Anime character, I like; inspired by YDS, too, because they use the Anime/Manga style. If you have issues with that, you can imagine it's named after Rembrandt.

The project is developed with Kotlin,JVM, so plugins should be possible, and are a goal at some point.
It is additionally developed with Java, so it works with Windows and Linux.

## Currently used libraries

* LWJGL (OpenGL + GLFW + OpenAL + stb)
* JOML (Matrix calculations and transforms for rendering)
* FFMpeg (Video/Image/Audio Import & Export)
* HSLuv (HSL alternative with constant brightness)
* OpenSimplexNoise (Noise Generator)
* Apache Imaging (More supported image formats, like .ico)

## Download

The version still is a pre-alpha.

Missing, important features:
- project management
- redo/undo
- linear video editor
- bug-free input fields ;)

### Linux

FFmpeg needs to be installed.

You can download the [universal build here](https://github.com/AntonioNoack/RemsStudio/raw/master/out/artifacts/Universal/VideoStudio.jar).
Later, I'll probably create specific builds depending on your OS, so they are smaller.

My FFmpeg install on Linux Mint 19 somehow misses features, that FFmpeg for Windows has. You might encounter issues as well.
Observed missing features:
- wav file can't be exported without extra INFO-header; I built a work-around for that
- I have a gif file, which can't be played on Linux

### Windows

You probably have to copy the files for ffmpeg (the program will tell you in the console). This is because of a bug in Java 8 (but Java 8 is the default download for Win10), and a slight config issue on my webserver.

You can download the [universal build here](https://github.com/AntonioNoack/RemsStudio/raw/master/out/artifacts/Universal/VideoStudio.jar).
Later, I'll probably create specific builds depending on your OS, so they are smaller.

### iOS

I own no device to test Rem's Studio on iOS. I read that installing Java on iOS isn't that easy either.
I am sure, you could make it work, if you are a developer. I have included the FFmpeg files for iOS in the [universal build, which you can download here](https://github.com/AntonioNoack/RemsStudio/raw/master/out/artifacts/Universal/VideoStudio.jar).
