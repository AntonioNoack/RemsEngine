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
  , [Apache VFS](https://search.maven.org/artifact/org.apache.commons/commons-vfs2/2.8.0/jar) (reading RAR files)
* [Image4j](https://github.com/imcdonagh/image4j) (reading ICO images)

<!-- * [jGLTF](https://github.com/AntonioNoack/jGLTF) (jGLTF for glTF files, modified), included, but only used for their PBR shader -->
<!-- * [Caliko](https://github.com/FedUni/caliko) (FABRIK IK), not yet actively used -->

## Download

**[Download on the official website](https://remsstudio.phychi.com/?s=download)**

## Build

To build Rem's Studio, I am using Intellij Idea (the community edition is free). It should work in other IDEs as well,
you just need to add all libraries. In Intellij Idea set the memory of the compiler (Settings/Build/Compiler) to more
than 700 MB, as it becomes awfully slow with that amount, or even crashes.

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

