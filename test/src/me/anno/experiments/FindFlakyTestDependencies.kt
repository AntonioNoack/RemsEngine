package me.anno.experiments

import me.anno.io.files.FileReference
import me.anno.utils.OS.documents
import me.anno.utils.algorithms.Recursion
import org.apache.logging.log4j.LogManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.lang.reflect.Method
import java.lang.reflect.Modifier

private val LOGGER = LogManager.getLogger("FindFlakyTestDependencies")

fun main() {

    // todo there is flaky tests...
    //  find which tests cause flakiness by running subsets of them
    // todo provide a seed and a percentage, and it will create a flakiness report

    // find all tests and run them
    // exclude long running tests
    // todo when everything is repaired, implement multi-threaded tests :3
    // todo also run Bullet tests :3

    val src = documents.getChild("IdeaProjects/RemsEngine/test/src")
    val testClasses = findTestClasses(src)

    val testMethods = testClasses.map { clazz ->
        findTestMethods(clazz)
    }

    if (false) {
        println("Test Classes [${testClasses.size}, ${testMethods.sumOf { it.size }}]:")
        for (i in testClasses.indices) {
            if (testMethods[i].isEmpty()) continue
            println("- ${testClasses[i].name}")
            for (method in testMethods[i]) {
                println("  - ${method.methodName}()")
            }
        }
    }

    val skippableClasses = listOf<String>(
        "me.anno.tests.animation.InterpolationTests",
        "me.anno.tests.animation.LoopingStateTests",
        "me.anno.tests.assimp.BoneByBoneTest",

        "me.anno.tests.export.SimpleExportTest",
        "me.anno.tests.gfx.gpuframes.BlankFrameDetectorTest",
        "me.anno.tests.gfx.gpuframes.GPUFrameTest",
        "me.anno.tests.gfx.textures.TextureReadPixelTests",
        "me.anno.tests.graph.hdb.AllocationManagerTest",
        "me.anno.tests.graph.hdb.HierarchicalDatabaseTest",
        "me.anno.tests.graph.octtree.KdTreePairsTests",
        "me.anno.tests.graph.octtree.OctTreeQueryTests",
        "me.anno.tests.graph.octtree.OctTreeTest",
        "me.anno.tests.graph.visual.CombineVectorNodeTest",
        "me.anno.tests.graph.visual.CompareNodeCloneTest",
        "me.anno.tests.graph.visual.DotProductNodeTest",
        "me.anno.tests.graph.visual.RotateNodeTest",
        "me.anno.tests.graph.visual.SeparateVectorNodeTest",
        "me.anno.tests.graph.visual.VectorLengthNodeTest",

        "me.anno.tests.audio.AmplitudeRangeTest",
        "me.anno.tests.audio.AudioReaderTest",
        "me.anno.tests.audio.AudioTimeIndexTest",
        "me.anno.tests.engine.BlockTracingTest",
        "me.anno.tests.engine.BoundsTest",
        "me.anno.tests.engine.CameraPropertyTest",
        "me.anno.tests.engine.ChangeHistoryTests",
        "me.anno.tests.engine.DefaultAssetsTest",
        "me.anno.tests.engine.DestroyOnUpdateTest",
        "me.anno.tests.engine.EventTest",
        "me.anno.tests.engine.InspectorTest",
        "me.anno.tests.engine.OnUpdateTest",
        "me.anno.tests.engine.PropertySetterTests",
        "me.anno.tests.engine.TextComponentImplTests",
        "me.anno.tests.engine.TransformTests",
        "me.anno.tests.engine.collider.ColliderTest",
        "me.anno.tests.engine.collider.MeshColliderTests",
        "me.anno.tests.engine.collider.SphereColliderSDFTest",
        "me.anno.tests.engine.prefab.AutoRefTest",
        "me.anno.tests.engine.prefab.PathTest",

        "me.anno.tests.engine.projects.FileEncodingTests",
        "me.anno.tests.engine.projects.ProjectsTest",
        "me.anno.tests.engine.raycast.ProjectionTests",
        "me.anno.tests.engine.sprites.SpriteLayerTests",
        "me.anno.tests.fonts.SignedDistanceFontsTests",
        "me.anno.tests.geometry.AStarTest",
        "me.anno.tests.geometry.MarchingCubesTest",
        "me.anno.tests.geometry.MarchingSquaresTest",
        "me.anno.tests.geometry.TriangleTest",
        "me.anno.tests.gfx.AccumulationTest",
        "me.anno.tests.gfx.CullTest",
        "me.anno.tests.gfx.DrawShaderCompileTest",
        "me.anno.tests.gfx.FBCreationTest",
        "me.anno.tests.gfx.RasterizerTest",
        "me.anno.tests.image.ResizeImageTest",
        "me.anno.tests.image.ImageMetadataTest",
        "me.anno.tests.image.ImageTests",
        "me.anno.tests.image.ImageWriterTest",
        "me.anno.tests.image.ImageAsFolderTest",

        "me.anno.tests.joml.AABBdTests",
        "me.anno.tests.joml.AABBfTests",
        "me.anno.tests.joml.AABBiTests",
        "me.anno.tests.joml.AxisAngleTest",
        "me.anno.tests.joml.Matrix2dTests",
        "me.anno.tests.joml.Matrix2fTests",
        "me.anno.tests.joml.MatrixArrayListTest",
        "me.anno.tests.joml.MatrixElementWiseTests",
        "me.anno.tests.joml.MatrixEquivalenceTests",
        "me.anno.tests.joml.MatrixGetSetTests",
        "me.anno.tests.joml.MatrixTransformTests",
        "me.anno.tests.joml.PerspectiveTests",
        "me.anno.tests.joml.PlaneTests",
        "me.anno.tests.joml.QuaternionTests",
        "me.anno.tests.joml.SimpleMathTest",
        "me.anno.tests.joml.Vector2dTests",
        "me.anno.tests.joml.Vector2fTests",
        "me.anno.tests.joml.Vector2iTests",
        "me.anno.tests.joml.Vector3dTests",
        "me.anno.tests.joml.Vector3fTests",
        "me.anno.tests.joml.Vector3iTests",
        "me.anno.tests.joml.Vector4dTests",
        "me.anno.tests.joml.Vector4fTests",
        "me.anno.tests.joml.Vector4iTests",
        "me.anno.tests.joml.VectorElementWiseTests",

        "me.anno.tests.io.BinaryWriterPathTest",
        "me.anno.tests.io.CSVReaderTest",
        "me.anno.tests.io.CloneTest",
        "me.anno.tests.io.CompleteFileEncodingTest",
        "me.anno.tests.io.CountingInputStreamTest",
        "me.anno.tests.io.EnumIdTests",
        "me.anno.tests.io.FileCacheTests",
        "me.anno.tests.io.GeneralFileEncodingTest",
        "me.anno.tests.io.HeavyAccessTest",
        "me.anno.tests.io.JsonWriterPathTest",
        "me.anno.tests.io.PropertyFinderTest",
        "me.anno.tests.io.StringFileEncodingTest",
        "me.anno.tests.io.StringHistoryTest",
        "me.anno.tests.io.UnknownSaveableTest",
        "me.anno.tests.io.XmlArray2DTest",
        "me.anno.tests.io.YamlArray2DTest",
        "me.anno.tests.io.files.EnumSerializationTest",
        "me.anno.tests.io.files.FilesTest",
        "me.anno.tests.io.files.JsonFormatterTest",
        "me.anno.tests.io.files.SanitizePathTest",
        "me.anno.tests.io.files.XMLTest",
        "me.anno.tests.io.files.YAMLTest",
        "me.anno.tests.io.xml.XMLReaderTest",
        "me.anno.tests.lua.QuickInputScriptTest",

        "me.anno.tests.image.raw.ByteImageFormatTest",
        "me.anno.tests.image.tga.TgaImageReaderTest",
        "me.anno.tests.image.utils.BoxBlurTest",
        "me.anno.tests.image.utils.GaussianBlurTest",
        "me.anno.tests.maths.Base64Test",
        "me.anno.tests.maths.EquationSolverTest",
        "me.anno.tests.maths.HalfFloatTest",
        "me.anno.tests.maths.PackingTests",
        "me.anno.tests.maths.DistancesTest",
        "me.anno.tests.maths.SimpleExpressionParserTest",

        "me.anno.tests.image.MediaMetadataOrderTest",
        "me.anno.tests.image.bmp.BMPDecoderTest",
        "me.anno.tests.image.bmp.BMPWriterTest",
        "me.anno.tests.image.gimp.GimpImageReaderTest",

        // todo this crashes TLASRaycastTests.testRaycastingSphereAnyHit
        // "me.anno.tests.image.svg.SVGThumbnailTest",

        /* "me.anno.tests.image.MediaMetadataOrderTest",
         "me.anno.tests.image.bmp.BMPDecoderTest",
         "me.anno.tests.image.bmp.BMPWriterTest",
         "me.anno.tests.image.gimp.GimpImageReaderTest",
         "me.anno.tests.image.raw.ByteImageFormatTest",
         "me.anno.tests.image.svg.SVGThumbnailTest",
         "me.anno.tests.image.tga.TgaImageReaderTest",
         "me.anno.tests.image.utils.BoxBlurTest",
         "me.anno.tests.image.utils.GaussianBlurTest",*/

    )

    val skippableMethods = listOf<String>(
        //"me.anno.tests.audio.AmplitudeRangeTest.testAmplitudeRange",
    )

    // todo reproduce flakiness
    val runTests = ArrayList<String>()

    val allTestMethods = testMethods.flatten()
    for (i in allTestMethods.indices) {
        val method = allTestMethods[i]
        if (method.clazzName in skippableClasses) continue
        if (method.fullName in skippableMethods) continue
        val name = "${method.fullName}($i/${allTestMethods.size})"
        try {
            LOGGER.warn("Starting $name")
            method.execute()
        } catch (e: Throwable) {
            e.printStackTrace()
            LOGGER.warn("Crashed in $name")
            for (test in runTests) {
                println("  \"$test\",")
            }
            break
        }

        val clazzName = method.clazzName
        if (runTests.lastOrNull() != clazzName) {
            runTests.add(clazzName)
        }
    }
}

val testAnnotationClass = Test::class.java
val beforeEachAnnotationClass = BeforeEach::class.java

fun findTestMethods(testClass: Class<*>): List<RunnableTest> {
    val methods = testClass.declaredMethods
    val beforeEach = methods.firstOrNull { method ->
        method.parameterCount == 0 &&
                method.getAnnotation(beforeEachAnnotationClass) != null &&
                !Modifier.isStatic(method.modifiers)
    }
    return methods.filter { method ->
        method.parameterCount == 0 &&
                method.getAnnotation(testAnnotationClass) != null &&
                !Modifier.isStatic(method.modifiers)
    }.map { method ->
        RunnableTest(method, beforeEach)
    }.sortedBy { it.methodName }
}

fun findTestClasses(src: FileReference): List<Class<*>> {
    val testClasses = ArrayList<Class<*>>()
    Recursion.processRecursive(src) { folder, remaining ->
        if (folder.isDirectory) {
            remaining.addAll(folder.listChildren())
        } else {
            val file = folder
            if (isTestClass(file.name) && fileHasClassHeader(file)) {
                // check whether class can easily be instantiated
                val classPath = file.relativePathTo(src, 0)!!
                    .replace('/', '.')
                    .removeSuffix(".kt")
                val clazz = Class.forName(classPath)
                if (clazz != null && clazz.constructors.any { it.parameterCount == 0 }) {
                    testClasses.add(clazz)
                }
            }
        }
    }
    testClasses.sortBy { it.name }
    return testClasses
}

fun fileHasClassHeader(file: FileReference): Boolean {
    val text = file.readTextSync()
    val className = file.nameWithoutExtension
    val expectedCode = "class $className "
    return expectedCode in text
}

fun isTestClass(name: String): Boolean {
    return (name.length > 7 && name.endsWith("Test.kt")) ||
            (name.length > 8 && name.endsWith("Tests.kt"))
}

class RunnableTest(val method: Method, val beforeEach: Method?) {

    val clazzName = method.declaringClass.name
    val methodName = method.name
    val fullName = "$clazzName.$methodName"

    fun execute() {
        val instance = method.declaringClass.newInstance()
        beforeEach?.invoke(instance)
        method.invoke(instance)
    }
}
