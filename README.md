# Game Engine: Rem's Engine

Parallel to my video editor "Rem's Engine", I am developing my own game engine. I have often written the beginnings of small games,
but they always had much in common, so I decided to write my own engine.

- direct Java/Kotlin support
- usable in my favourite IDE: Intellij Idea
- completely Open Source
- no fees
- hopefully in the future fewer annoyances with skeletons than Unreal Engine
- support for all kinds of formats
- working/loading from inside compressed folders
- I get to learn even more about game engines <3

This engine does not have the target to offer the best graphics, or be the best performant. Its goal is to be nice and
quick to develop in, like a tool box.

## [Official Website](https://remsengine.phychi.com/) (WIP)

Once finished, the website will contain everything from this readme, plus tutorials on how to get started, and maybe/probably a link
to a custom discord as well :). (I'm not sure yet whether I'll create one discord server for all projects, or seperate ones)

## Project Plan

I was writing the game engine in this same repository as [Rem's Studio](https://github.com/AntonioNoack/RemsStudio), because

- they share a lot of code,
- I wanted to base the video studio on the engine in the future
- the engine needed to be based on the video editor
- when I started the engine, the video studio already was a huge project

To develop them properly, I ofc had to split them. I finished the split in Q1 2022.

<!-- the following graph should work starting Jan-Mar 2022 -->
<!-- TB = top-bottom, LR = left-right -->
```mermaid
graph TB
    A(Older Kotlin / Java Projects)
    B(Rem's Studio)
    B2(Studio relatively stable / New target: GameDev)
    C(Rem's Studio/Engine)
    X1(Windows/Linux Export)
    X2(Engine relatively stable)
    X3(Android Export)
    W(Engine is production ready)
    W1(Rem's Engine)
    W2(Rem's Studio ''Game'')
    A -- Q1 2020 --> B
    B -- Q2 2021 --> B2
    B2 -- Q2 2021 --> C
    C -- Q1 2022 --> W1
    C -- Q1 2022 --> W2
    W1 -- TODO --> X1
    W1 -- TODO --> X2
    W1 --  TBD --> X3
    X2 -- TODO --> W
```

## Features

- entity - component based system
  - because I like the system from Unity more than that of Godot (still imperfect)
- 64 bit fp transformations for universe simulations
- supports loading all kinds of formats
  - Image formats (ffmpeg, ImageIO, Image4j, custom): png, jpg, tga, ico, dds, exr, hdr, svg, pcx, xcf (Gimp)
  - Video formats (ffmpeg): wav, mp3, mp4, avi, flv, gif
  - Mesh formats (Assimp, custom) obj, fbx, gltf, dae, blend, vox, md2, md5mesh, mitsuba
  - Package formats (Apache Compress, JUnRar): zip, tar, tar.gz, rar, 7z, bz2, lz4, xar, oar
  - Metadata formats (custom): json, csv, yaml, xml
  - Others (Apache PDFBox, custom): pdf, tar/text-based Unity-packages
  - Note: not all are fully supported
- can load files from compressed folders (recursively as well)
- pbr workflow
- Bullet as 3d physics engine
    - running on a separate thread for uninterrupted graphics (optional)
    - running with 64 bit floating precision for accuracy / universe simulations
- Box2d as 2d physics engine
    - currently a prototype
- mods / plugins from the start: your game is a mod for the engine
- event based
- simple switch between forward- and deferred rendering
    - for beautiful MSAA with a few lights,
    - or thousands of lights without performance hit
    - Note: forward rendering does not support SSR nor SSAO
- depth-edge-detection based anti-aliasing (like FXAA)
- different light types, with shadow support: directional, spot, point
- shadows with cascades (directional)
- planar reflections
- screen space reflections (SSR)
- screen space ambient occlusion (SSAO)
- static, animated and procedural meshes
- static and animated meshes can be instanced
- signed distance functions as mesh replacement
- aabb optimized scene hierarchy
- bloom to convey brightness
- AMD FSR: dynamic upscaling and sharpening
    - is used to upscale images in the file explorer as well
- controller support
    - works for UI automatically
    - you can even use your controller in other programs as a mouse, while Rem's Engine/Studio is running :3
- automatic file reload, on file change
- Android-inspired UI with integrated spellchecking
- very basic Lua scripting
- utilities like path-finding
- spellchecking in most input fields

## Planned Features
- easy local multiplayer
- usable Multiplayer, local and tcp/udp
- environment maps as lights
- compute environment maps from the scene for reflections
- animation trees (animations can be played and mixed, just not in tree form yet)
- shader graphs (currently shaders are only writable in GLSL)
- transparent meshes (possible right now, just needs a bit more logic)
- export to Windows/Linux (currently manual)
- usable Gizmos 😅
- fully supported Lua scripting
  - [LuaAnimTextPanel](src/me/anno/ui/anim/LuaAnimTextPanel.kt)
  - [QuickScriptComponent](src/me/anno/ecs/components/script/QuickScriptComponent.kt)

## Maybe Later Features
- automatic export to Web
- automatic export to Android
- light baking for realistic graphics
- trees for much stuff: animations, shaders
- visual coding? in work already :)
- support separate mice / keyboards for local multiplayer?
- Vulkan backend to support hardware raytracing

## Projects using Rem's Engine
- [Rem's Studio](https://github.com/AntonioNoack/RemsStudio)
- [Cellular Automata](https://github.com/AntonioNoack/CellularAutomata)
- [Tsunami Simulation](https://github.com/AntonioNoack/RemsTsunamis)
- [Voxel World](https://github.com/AntonioNoack/RemsEngine-VoxelWorld)
- [Monte-Carlo Map Optimization](https://github.com/AntonioNoack/MonteCarloMapOptimization)
- a few more, non-published (yet?)

## Getting Started
- Download an IDE of your choice. I prefer IntelliJ IDEA, but have worked with Eclipse in the past.
- Download either the engine source code, and compile it yourself, or download a pre-compiled jar.
  - If there is no release available yet, just ask for a build, and I'll create one :)
- Add the engine jar as a dependency to your project.
- Either create an extension (mod), or work directly with the engine.

## Samples
Besides my personal projects that use Rem's Engine, there is also quite a few samples within the engine source code.
They also function as tests for me.

Some tests, starting the whole engine, like a small game:
- [GFX: MarchingCubes.kt](test/src/me/anno/tests/geometry/MarchingCubes.kt)
- [Math: PathfindingAccTest.kt](test/src/me/anno/tests/geometry/PathFindingAccTest.kt)

Some feature tests:
- [Math: SDFTest.kt](test/src/me/anno/tests/collider/SDFColliderTest.kt)
- [Internal: Hierarchy.kt](test/src/me/anno/tests/ecs/Hierarchy.kt)
- [GFX: MarchingSquares.kt](test/src/me/anno/tests/geometry/MarchingSquares.kt)
- [GFX: Reduction.kt](test/src/me/anno/tests/shader/Reduction.kt)
- [GFX: FSR.kt](src/me/anno/ecs/components/shaders/effects/FSR.kt)
- [Debug: JsonFormatter.kt](test/src/me/anno/tests/files/JsonFormatter.kt)
- [UI: DrawCurves.kt](test/src/me/anno/tests/shader/DrawCurves.kt)
- [UI: AnimTest.kt](test/src/me/anno/tests/ui/AnimTest.kt)
- [Snake Game](test/src/me/anno/tests/game/Snake.kt), [Running Web Demo](https://remsengine.phychi.com/jvm2wasm/snake/)

You can find most examples in the "tests" folder. In total, there are more than 200 handwritten tests for you to see how the engine works :).

## Ports
- Linux, Windows by default
- MacOS should be simple as long as Java is working there
- [Android](https://github.com/AntonioNoack/RemsEngine-Android)
- Web (WASM) WIP, not published yet;

## Build It

All libraries are shipped with the engine :).
Use any IDE you want, with Java and Kotlin support. Best use IntelliJ IDEA.

First build the KOML module, and compile the artifact.
Then build the main module :).

If you want PDF support, compile the PDF module, and add it to your project, or add it to the plugins or mods folder.

## Ship It

### Linux, Windows

Shipping is easy on Windows and Linux: just export your projects will all dependencies into a .jar file, or download  dependencies on the first start (as I do with FFMPEG and spellchecking).
To load plugins from your built jar, because your jar won't be scanned at runtime, register them from your main class as internal :).

### Android

Shipping to Android is a bit more complicated:
- download the [Android fork](https://github.com/AntonioNoack/RemsEngine-Android) as an Android Studio project
- modify it to your needs
- libraries can be added just like in IntelliJ IDEA

### Web (HTML5, WASM, WebGL)

Shipping to Web hasn't been published yet.<br>
Contact me to get pre-release access 😊.<br>
First demo: [Snake Game](https://remsengine.phychi.com/jvm2wasm/snake/) from [tests](test/src/me/anno/tests/game/Snake.kt).

## Just Try/Use It

If you don't want to compile the engine yourself, and just want to focus on developing games, plugins or mods, you can use pre-built versions.
There isn't an official release yet, but you can use the in-official build from my [Cellular Automata Demo](https://github.com/AntonioNoack/CellularAutomata/tree/main/out/artifacts/Demo). 
A release of [Rem's Studio](https://github.com/AntonioNoack/RemsStudio) would work as well.

Until I create a release, they might be a bit out of date 😅.

## Used libraries

* [LWJGL](https://www.lwjgl.org/) (Graphics and Audio; OpenGL + GLFW + OpenAL + stb (audio))
* [Assimp](https://github.com/assimp/assimp) (loading 3d meshes; from LWJGL)
* [JOML](https://github.com/JOML-CI/JOML) (Matrix calculations and transforms for rendering)
* [FFMpeg](https://ffmpeg.org/) (Video/Image/Audio Import & Export)
* [HSLuv](https://github.com/hsluv/hsluv-java) (HSL alternative with constant brightness)
* [LanguageTool](https://languagetool.org/) (Spellchecking)
* [JTransforms](https://sites.google.com/site/piotrwendykier/software/jtransforms) (Fast Fourier Transform)
* [Apache Imaging](https://commons.apache.org/proper/commons-imaging/) (More supported image formats, like .ico)
* [Apache CLI](https://commons.apache.org/proper/commons-cli/) (Basics of Command Line Interface)
* [Thumbnailator](https://github.com/coobird/thumbnailator/) (Only EXIF related code, detecting rotated JPEG files)
* [JNA Platform](https://github.com/java-native-access/jna) (Moving files to trash)
* [Apache PDFBox](https://pdfbox.apache.org/) (Rendering PDFs)
* [JAI ImageIO Core](https://github.com/jai-imageio/jai-imageio-core) (More image formats for PDFs)
* [JAI ImageIO JPEG2000](https://github.com/jai-imageio/jai-imageio-jpeg2000) (JPEG 2000 support for PDFs)
* [jUnRAR](https://github.com/edmund-wagner/junrar), [Apache VFS](https://search.maven.org/artifact/org.apache.commons/commons-vfs2/2.8.0/jar) (Reading RAR files)
* [Image4j](https://github.com/imcdonagh/image4j) (Reading ICO images)
* [Bullet](http://jbullet.advel.cz/) (3d Physics, adjusted to be FP64 instead of FP32)
* [Box2d](https://github.com/jbox2d/jbox2d) (2d Physics, still FP32)
* [LuaJ](https://github.com/luaj/luaj) (Lua scripting)
* [QOI-Java](https://github.com/saharNooby/qoi-java) (QOI image format)
* [Recast4j](https://github.com/AntonioNoack/recast4j/) (NavMesh generation; adjusted for JOML; not yet integrated)

## Documentation

The code is mainly documented within itself, so I recommend you to download the engine source code.
Using it, you can easily look up how and where pieces of engine code are being used.

You can also take a look at the Wiki. There, I describe the serialization system, how to create a mod, and supported file formats.

In the future, I might create a website for it :).

## Wiki

Since both projects originate from the same base, the [Wiki](https://github.com/AntonioNoack/RemsStudio/wiki) currently is located at Rem's Studio's repository.
If you have any in-depth questions, just ask me on YouTube, MeWe, GitHub, Discord, or via Email.