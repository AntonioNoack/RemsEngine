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

    // todo find all tests and run them
    // todo exclude long running tests
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

    val skippableClasses = listOf(
        // "me.anno.tests.utils.WebRefTest", // is required for time failing
        "me.anno.tests.utils.types.AnyToLongTests",
        "me.anno.tests.utils.VectorTests",
        "me.anno.tests.utils.TransformTest",
        "me.anno.tests.physics.shapes.SDFCubeTest",


        "me.anno.tests.utils.SpellcheckTest",
        "me.anno.tests.utils.SimpleParserTest",
        "me.anno.tests.utils.SearchTest",
        "me.anno.tests.utils.search.PartitionTests",
        "me.anno.tests.utils.search.MedianTest",
        "me.anno.tests.utils.search.BinarySearchTests",
        "me.anno.tests.utils.search.BinarySearchTest",
        "me.anno.tests.utils.QuaternionTest",
        "me.anno.tests.utils.PromiseTest",
        "me.anno.tests.utils.algorithms.SortingTests",
        "me.anno.tests.utils.algorithms.ForLoopTests",
        "me.anno.tests.structures.WeightedRandomTest",
        "me.anno.tests.structures.WeightedListTest",
        "me.anno.tests.structures.TopologicalSortTest",
        "me.anno.tests.structures.FastIteratorSetTest",
        "me.anno.tests.physics.shapes.SDFSphereTest",
        "me.anno.tests.physics.shapes.SDFCylinderTest",
        "me.anno.tests.structures.ListTransposeTest",
        "me.anno.tests.structures.LineSequenceTest",
        "me.anno.tests.structures.LinearRegressionTest",
        "me.anno.tests.structures.IntArrayListTest",
        "me.anno.tests.structures.HeapTest",

        "me.anno.tests.ui.HoverTest",
        "me.anno.tests.ui.HideTest",
        "me.anno.tests.utils.AnyToDoubleTests",
        "me.anno.tests.ui.groups.ListAlignmentTest",
        "me.anno.tests.ui.input.ClearTextInputTest",
        "me.anno.tests.utils.WebRefTest",
        "me.anno.tests.utils.TimeTest",
        "me.anno.tests.utils.structures.lists.SimpleListTests",
        "me.anno.tests.utils.structures.lists.SegmentListTests",
        "me.anno.tests.utils.structures.arrays.ArrayListTests",
        "me.anno.tests.utils.StringHistorySerializationTest",
        "me.anno.tests.utils.SphericalHierarchyTest",
        "me.anno.tests.utils.ProcessingGroupTest",
        "me.anno.tests.utils.PrefabTest",
        "me.anno.tests.utils.OklabTest",
        "me.anno.tests.utils.NodeSerializationTests",
        "me.anno.tests.utils.LineIntersectionTests",
        "me.anno.tests.utils.LevenshteinTest",
        "me.anno.tests.utils.HSITest",
        "me.anno.tests.utils.hpc.ProcessingGroupTests",
        "me.anno.tests.utils.HeavyProcessingTest",
        "me.anno.tests.utils.FindNextNameTests",
        "me.anno.tests.utils.ConfigTest",
        "me.anno.tests.utils.ColorParsingTest",
        "me.anno.tests.utils.CodepointsTest",
        "me.anno.tests.utils.CallbackMappingTest",
        "me.anno.tests.utils.ByteImageFormatFileSizeTests",
        "me.anno.tests.ui.inspector.AutoInspectorTest",
        "me.anno.tests.ui.input.VectorInputTabTest",
        "me.anno.tests.ui.groups.SizeLimitingContainerTest",
        "me.anno.tests.ui.FocusTest",
        "me.anno.tests.ui.editor.LanguageThemeLibTest",
        "me.anno.tests.ui.editor.AllowedFileNamesTest",
        "me.anno.tests.ui.dragging.DraggingTest",
        "me.anno.tests.ui.CodeEditorTest",
        "me.anno.tests.structures.StringMapTest",
        "me.anno.tests.structures.SortedAddTest",
        "me.anno.tests.structures.SerializablePropertiesTest",
        "me.anno.tests.structures.SecureStackTest",
        "me.anno.tests.structures.MeshSerializationTest",
        "me.anno.tests.structures.FloatArray2DSerializationTest",
        "me.anno.tests.shader.CompileTest",
        "me.anno.tests.rtrt.engine.BVHBuilderTest",
        "me.anno.tests.recast.RecastTests",
        "me.anno.tests.recast.NavMeshBuilderGenTests",
        "me.anno.tests.recast.FindStraightPathTests",

        "me.anno.tests.physics.shapes.MeshSphereTest",
        "me.anno.tests.physics.CompoundColliderTest",
        "me.anno.tests.physics.CenterOfMassTests",
        "me.anno.tests.physics.BulletVehicleTest",
    )

    val skippableMethods = listOf(
        "me.anno.tests.utils.TimeTest.testProgressBackwards",
        "me.anno.tests.utils.WebRefTest.testURLParsing",
        "me.anno.tests.utils.TimeTest.testProgressFast",
        "me.anno.tests.utils.TimeTest.testProgress",
        "me.anno.tests.utils.TimeTest.testProgressSlow",
        "me.anno.tests.utils.TimeTest.testProgressStopped"
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
            LOGGER.warn("Finished $name")
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
    }
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
