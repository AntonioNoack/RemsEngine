package me.anno.tests.io.files

import me.anno.Engine
import me.anno.config.DefaultConfig
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.ECSRegistry
import me.anno.gpu.texture.Texture2D
import me.anno.graph.hdb.HDBKey.Companion.InvalidKey
import me.anno.image.ImageCache
import me.anno.image.raw.GPUImage
import me.anno.image.thumbs.AssetThumbnails
import me.anno.image.thumbs.Thumbs
import me.anno.io.files.FileReference
import me.anno.io.files.Reference.getReference
import me.anno.io.files.inner.InnerLinkFile
import me.anno.io.json.generic.JsonFormatter
import me.anno.jvm.HiddenOpenGLContext
import me.anno.tests.LOGGER
import me.anno.ui.debug.TestEngine
import me.anno.ui.editor.files.FileExplorer
import me.anno.ui.editor.files.FileExplorerOption
import me.anno.utils.OS
import me.anno.utils.files.Files.formatFileSize
import me.anno.utils.types.Strings

fun inspectAsset(asset: FileReference) {
    for (file in asset.listChildren().filterIsInstance<InnerLinkFile>().sortedBy { -it.length() }) {
        LOGGER.info(file.name + " links to " + file.link)
    }
    for (file in asset.listChildren().filter { it !is InnerLinkFile }.sortedBy { -it.length() }) {
        LOGGER.info(file.name + ", " + file.length().formatFileSize())
        LOGGER.info(JsonFormatter.format(file.readTextSync()))
    }
}

fun FileReference.printTree(depth: Int, maxDepth: Int) {
    if (!isHidden) {
        LOGGER.debug(Strings.spaces(depth * 2) + name)
        if (depth + 1 < maxDepth) {
            for (child in listChildren()) {
                child.printTree(depth + 1, maxDepth)
            }
        }
    }
}

fun testRendering(file: FileReference, size: Int = 512, index: Int) {
    val prefab = PrefabCache[file]!!
    LOGGER.debug(JsonFormatter.format(prefab.toString()))
    val sample = prefab.createInstance()
    LOGGER.debug(sample)
    AssetThumbnails.generateAssetFrame(
        prefab, file,
        InvalidKey, size
    ) { result, exc ->
        if (result is Texture2D) {
            GPUImage(result).write(OS.desktop.getChild("$index.png"))
        }
        exc?.printStackTrace()
    }
}

fun smallRenderTest() {

    val projectPath = OS.downloads.getChild("up/PolygonSciFiCity_Unity_Project_2017_4.unitypackage")
    val colliderComponent = projectPath.getChild("f9a80be48a6254344b5f885cfff4bbb0/64472554668277586.json")
    val meshComponent = projectPath.getChild("f9a80be48a6254344b5f885cfff4bbb0/33053279949580010.json")
    val entityOfComponent = projectPath.getChild("f9a80be48a6254344b5f885cfff4bbb0/1661159153272266.json")
    val bbox =
        getReference("E:/Assets/Polygon_Street_Racer_Unity_Package_2018_4_Update_01.unitypackage/Assets/PolygonStreetRacer/Prefabs/Generic/SM_Generic_TreeStump_01.prefab")

    HiddenOpenGLContext.createOpenGL()

    ECSRegistry.init()

    Thumbs.useCacheFolder = true
    for ((index, file) in listOf(meshComponent, colliderComponent, entityOfComponent, bbox).withIndex()) {
        testRendering(file, 512, index)
    }

    Engine.requestShutdown()
}

fun sceneRenderTest() {

    // todo analyse triplanar scene & recreate complete tree structure from it
    // todo -> the object with fileId 0 as parent is root

    /*
--- !u!1 &752095191
GameObject:
m_ObjectHideFlags: 0
m_PrefabParentObject: {fileID: 0}
m_PrefabInternal: {fileID: 0}
serializedVersion: 5
m_Component:
- component: {fileID: 752095192}
m_Layer: 0
m_Name: Demo_Scene
m_TagString: Untagged
m_Icon: {fileID: 0}
m_NavMeshLayer: 0
m_StaticEditorFlags: 0
m_IsActive: 1
--- !u!4 &752095192
Transform:
m_ObjectHideFlags: 0
m_PrefabParentObject: {fileID: 0}
m_PrefabInternal: {fileID: 0}
m_GameObject: {fileID: 752095191}
m_LocalRotation: {x: 0, y: 0, z: 0, w: 1}
m_LocalPosition: {x: -0.066163585, y: 5.009288, z: 15.072274}
m_LocalScale: {x: 1, y: 1, z: 1}
m_Children:
- {fileID: 1958462913}
- {fileID: 1721309181}
- {fileID: 1492832708}
...
m_Father: {fileID: 0}
m_RootOrder: 0
m_LocalEulerAnglesHint: {x: 0, y: 0, z: 0}
    * */

    val projectPath = OS.downloads.getChild("up/PolygonSciFiCity_Unity_Project_2017_4.unitypackage")
    val scene = projectPath.getChild("Assets/PolygonSciFiCity/Scenes/Demo_TriplanarDirt.unity/2130288114.json")

    HiddenOpenGLContext.createOpenGL()
    ECSRegistry.init()

    Thumbs.useCacheFolder = true
    for ((index, file) in listOf(scene).withIndex()) {
        testRendering(file, 1024, index)
    }

    Engine.requestShutdown()
}

fun main() {

    // testSceneWithUI(getReference("E:/Assets/Unity/POLYGON_Nature_Unity_2017_4.zip/PolygonNature/Prefabs/Plants/SM_Plant_01.prefab"))
    // return

    Prefab.maxPrefabDepth = 7

    // to do support for submeshes:
    // MeshFilters reference submeshes by changing the fileId
    // 4300002 is the 2nd (probably) submesh

    /*sceneRenderTest()
    return*/

    val projectPath = OS.downloads.getChild("up/PolygonSciFiCity_Unity_Project_2017_4.unitypackage")

    /*
    return*/

    /*val file =
        getReference("E:/Assets/POLYGON_Pirates_Pack_Unity_5_6_0.zip/PolygonPirates/Assets/PolygonPirates/Materials")
    LOGGER.debug("file exists? ${file.exists}, children: ${file.listChildren()}")
    val projectPath2 = findUnityProject(file)
    LOGGER.debug("project from file? $projectPath2")*/

    /*val file2 = file.getParent().getChild("Prefabs/Vehicles/SM_Flag_British_01.prefab")
    LOGGER.debug("file2 exists? ${file2.exists}")
    LOGGER.debug(PrefabCache.getPrefab(file2))*/

    // return

    /*val colliderComponent = getReference(projectPath, "f9a80be48a6254344b5f885cfff4bbb0/64472554668277586.json")
    val meshComponent = getReference(projectPath, "f9a80be48a6254344b5f885cfff4bbb0/33053279949580010.json")
    val entityOfComponent = getReference(projectPath, "f9a80be48a6254344b5f885cfff4bbb0/1661159153272266.json")

    HiddenOpenGLContext.createOpenGL()*/

    ECSRegistry.init()

    /*val circularDependencies = listOf(
        "6e7e49849c96318418dbd28b88bc6d06/100100000.json",
        "cae9881699f289945baf66e9c9958a45/100100000.json",
        "9baabcdff9f934e4f93321577d7858e5/1637145686889916.json",
        "6924b6055d3b89c49be5c9d309e8e14c/100100000.json",
        "32713ca9df7701740ab3e8677019c63a/1656211306410468.json",
        "32713ca9df7701740ab3e8677019c63a/1656211306410468.json"
    )
    for(sample in circularDependencies){
        val prefab = PrefabCache.getPrefab(getReference(projectPath, sample))
        LOGGER.info(sample)
        LOGGER.info(JsonFormatter.format(prefab.toString()))
        LOGGER.info(prefab!!.getSampleInstance())
    }*/

    /*Thumbs.useCacheFolder = true
    for (file in listOf(meshComponent, colliderComponent, entityOfComponent)) {
        val prefab = PrefabCache.loadPrefab(file)!!
        LOGGER.debug(JsonFormatter.format(prefab.toString()))
        val sample = prefab.createInstance()
        LOGGER.debug(sample)
        Thumbs.generateSomething(prefab, file,
            desktop.getChild(sample::class.simpleName + ".png"), 512
        ) {}
    }

    Engine.requestShutdown()

    return*/

    ImageCache[projectPath.getChild("Assets/PolygonSciFiCity/Textures/LineTex 4.png"), false]!!
        .write(OS.desktop.getChild("LineTex4.png"))

    // circular sample
    // inspectAsset(getReference(projectPath, "f3ffd5a2a26fdf04e93bfde173e2b50d"))

    // blank sample
    /*val blankSample =
        getReference(projectPath, "Assets/PolygonSciFiCity/Prefabs/Buildings/SM_Building_Shack_01.prefab")
    inspectAsset(blankSample)
    ECSRegistry.initWithGFX(512)
    testEntityMeshFrame(blankSample)
    Engine.shutdown()*/


    val assets = projectPath.getChild("Assets")
    val main = assets.getChild("PolygonSciFiCity")

    //parseYAML(getReference(main, "Materials/Alternates/PolygonScifi_03_B.mat").readText())
    //parseYAML(getReference(main, "Scenes/Demo.unity"))

    // val project = findUnityProject(assets)!!
    // val path = decodePath("", "{fileID: 2800000, guid: fff3796fd3630f64890b09296fcb8f85, type: 3}", project)
    // LOGGER.info("Path: $path")
    // correct solution: main, Textures/PolygonSciFiCity_Texture_Normal.png

    // parseYAML(getReference(downloads, "up/SM_Prop_DiningTable_01.fbx.meta"))


    // val meshMeta = getReference(main, "Models/Flame_Mesh.fbx")
    // val material = getReference(main, "Materials/PolygonSciFi_01_A.mat")
    // val scene = getReference(main, "Scenes/Demo.unity")
    // LOGGER.info(readAsAsset(material).readText())

    // val sampleMaterial = getReference(downloads, "up/mat/PolygonScifi_03_A.mat")
    // LOGGER.info(readAsAsset(sampleMaterial).readText())

    // ECSRegistry.init()

    val testScene = main.getChild("Scenes/Demo_TriplanarDirt.unity")
    for (fileName in listOf("2130288114", "668974552")) {
        val file = testScene.getChild("$fileName.json")
        LOGGER.debug("$fileName: " + PrefabCache.printDependencyGraph(file))
    }
    //Engine.requestShutdown()
    //return

    TestEngine.testUI("Unity") {
        object : FileExplorer(testScene, true, DefaultConfig.style) {

            override fun getFolderOptions(): List<FileExplorerOption> = emptyList()

            override fun onDoubleClick(file: FileReference) {
                switchTo(file)
            }

            override fun onPaste(x: Float, y: Float, data: String, type: String) {
            }
        }
    }
}
