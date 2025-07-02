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
