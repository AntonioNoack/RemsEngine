# Rem's Studio <!-- 's is correct, because it's owned; I thought it may be only for abbreviations of is -->

**[Please visit the website for all the info you need about Rem's Studio.](https://remsstudio.phychi.com)**

This studio is a **video editor**, which is intended for starters in the video industry, or YouTubers, and was inspired
by the channel [YDS](https://www.youtube.com/user/YutsuraidanceStudios). I wanted to be able to make those transitions
and motions easily myself. Without needing to buy some expensive software.

![Screenshot of working in the program](https://remsstudio.phychi.com/img/mask%20gs%20add%20to%20ron.png)

That's why Rem's Studio will always be sold **low-priced** to individuals, and may stay free, if you build it yourself (
proof of work, that you really can't afford it ;)). In the alpha times, you all are early testers, so you get it for **
free** anyways <3 :D.

The name is from Rem, an Anime character, I like; inspired by YDS, too, because they use the Anime/Manga style. If you
have issues with that, you can imagine it's named after Rembrandt.

The project is developed with Kotlin, JVM, so plugins should be possible, and are a goal at some point. It is
additionally developed with Java, so it works with Windows and Linux.

## Currently used libraries

* [LWJGL](https://www.lwjgl.org/) (Graphics and Audio; OpenGL + GLFW + OpenAL + stb + jemalloc)
* [Assimp](https://github.com/assimp/assimp) (loading 3d meshes, from LWJGL)
* [JOML](https://github.com/JOML-CI/JOML) (Matrix calculations and transforms for rendering)
* [FFMpeg](https://ffmpeg.org/) (Video/Image/Audio Import & Export)
* [HSLuv](https://github.com/hsluv/hsluv-java) (HSL alternative with constant brightness)
* [OpenSimplexNoise](https://gist.github.com/KdotJPG/b1270127455a94ac5d19) (Noise Generator)
* [LanguageTool](https://languagetool.org/) (Spellchecking)
* [JTransforms](https://sites.google.com/site/piotrwendykier/software/jtransforms) (Fast Fourier Transform)
* [Apache Imaging](https://commons.apache.org/proper/commons-imaging/) (More supported image formats, like .ico)
* [Apache CLI](https://commons.apache.org/proper/commons-cli/) (Basics of Command Line Interface)
* [Metadata Extractor](https://github.com/drewnoakes/metadata-extractor) (Detecting rotated JPEG files)
* [JNA Platform](https://github.com/java-native-access/jna) (Moving files to trash)
* [Apache PDFBox](https://pdfbox.apache.org/) (Rendering PDFs)
* [JAI ImageIO Core](https://github.com/jai-imageio/jai-imageio-core) (More image formats for PDFs)
* [JAI ImageIO JPEG2000](https://github.com/jai-imageio/jai-imageio-jpeg2000) (JPEG 2000 support for PDFs)
* [jUnRAR](https://github.com/edmund-wagner/junrar)
  , [Apache VFS](https://search.maven.org/artifact/org.apache.commons/commons-vfs2/2.8.0/jar) (Reading RAR files)
* [Image4j](https://github.com/imcdonagh/image4j) (Reading ICO images)

<!-- * [jGLTF](https://github.com/AntonioNoack/jGLTF) (jGLTF for glTF files, modified), included, but only used for their PBR shader -->
<!-- * [Caliko](https://github.com/FedUni/caliko) (FABRIK IK), not yet actively used -->

## Download

**[Download on the official website](https://remsstudio.phychi.com/?s=download)**

## Build

To build Rem's Studio, I am using Intellij Idea (the community edition is free). It should work in other IDEs as well,
you just need to add all libraries. In Intellij Idea set the memory of the compiler (Settings/Build/Compiler) to more
than 700 MB, as it becomes awfully slow with that amount, or even crashes.

## Game Engine: Rem's Engine

Parallel to this video editor, I am developing my own game engine. I have often written the beginnings of small games,
but they always had much in common, so I decided to write my own engine.

- direct Java/Kotlin support
- usable in my favourite IDE: Intellij Idea
- completely Open Source
- no fees
- hopefully in the future fewer annoyances with skeletons than Unreal Engine
- support for all kinds of formats
- working/loading from inside compressed folders
- hopefully fewer files than Unity, with their one-meta-for-every-file-strategy
- I get to learn even more about game engines <3

This engine does not have the target to offer the best graphics, or be the best performant. Its goal is to be nice and
quick to develop in, like a tool box.

I am writing the game engine in this same repository as Rem's Studio, because

- they share a lot of code,
- I want to base the video studio on the engine in the future
- currently, the engine needs to be based on the video editor
- when I started the engine, the video studio already was a huge project

### Features

- entity - component based system
  - because I like the system from Unity more than that of Godot (still imperfect)
- 64 bit fp transformations for universe simulations 
- supports loading all kinds of formats
- can load files from compressed folders
- pbr workflow
- Bullet as physics engine
  - running on a separate thread for uninterrupted graphics
  - running with 64 bit floating precision for accuracy / universe simulations
- mods / plugins from the start: your game is a mod for the engine
- event based
- auto switch between forward- and deferred rendering
  - for beautiful MSAA with a few lights,
  - or thousands of lights without performance hit
- depth-edge-detection based anti-aliasing (like FXAA) 
- shadows with cascades (directional, spot, point)
- planar reflections
- static meshes
- animated meshes
- aabb optimized scene hierarchy
- bloom to convey brightness
- AMD FSR: dynamic upscaling and sharpening
    - is used to upscale images in the file explorer as well
- controller support
  - works for UI automatically
  - you can even use your controller in other programs as a mouse, while Rem's Engine/Studio is running :3

### Planned Features
- nice UI system
- easy local multiplayer
- environment maps as lights
- compute env maps from the scene  
- animation trees
- shader graphs
- automatic mesh reload + basic Blender file support
  - so you could use Blender as a superb terrain editor

### Maybe Later Features
- light baking for realistic graphics
- trees for much stuff: animations, shaders
- visual coding?
- path finding algorithms
- block based library?
- save files
- multiplayer system?
- support separate mice / keyboards for local multiplayer?
- support controller + mouse/keyboard for one more local player

<!--
## Supported Formats (Import)

### Images:
- png
- jpg 
- jpeg2000
- ico
- bmp
- psd
- hdr
- tga

- (very basic support: svg)

### Videos:
- mp4
- flv
- practically everything else from FFMPEG

### Audio:
- mp3
- wav
- practically everything else from FFMPEG

### 3D Meshes: 
- obj/mtl
- fbx
- dae
- gltf/glb
- md2
- md5mesh
- vox

### Documents
- pdf

### Containers
- zip
- rar
- 7z
- tar.gz
- unity packages xD

-->

