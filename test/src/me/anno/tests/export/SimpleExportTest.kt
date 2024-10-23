package me.anno.tests.export

import me.anno.Engine
import me.anno.Time
import me.anno.engine.projects.GameEngineProject
import me.anno.export.ExportProcess
import me.anno.export.ExportSettings
import me.anno.io.files.FileFileRef
import me.anno.io.files.FileReference
import me.anno.jvm.utils.BetterProcessBuilder
import me.anno.maths.Maths.SECONDS_TO_NANOS
import me.anno.ui.base.progress.ProgressBar
import me.anno.utils.Clock
import me.anno.utils.OS
import me.anno.utils.OS.res
import me.anno.utils.Sleep
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.InputStream
import kotlin.concurrent.thread

class SimpleExportTest {

    private fun createEngineBuild(): FileReference {
        val tmp = FileFileRef.createTempFile("project", "test")
        tmp.delete(); tmp.mkdirs()
        val project = GameEngineProject()
        project.location = tmp
        val settings = ExportSettings()
        if (!OS.isMacOS) {
            settings.macosPlatforms.x64 = false
            settings.macosPlatforms.arm64 = false
        }
        if (!OS.isWindows) {
            settings.windowsPlatforms.x64 = false
            settings.windowsPlatforms.x86 = false
            settings.windowsPlatforms.arm64 = false
        }
        if (!OS.isLinux) {
            settings.linuxPlatforms.arm32 = false
            settings.linuxPlatforms.arm64 = false
            settings.linuxPlatforms.x64 = false
        }
        // test whether it still runs with most UI and Kotlin reflection removed/replaced
        settings.minimalUI = true
        settings.useKotlynReflect = true
        // optimization
        settings.excludedModules.addAll(
            ("Network,OpenXR,PDF,SDF,Recast,Image,Lua," +
                    "Bullet,BulletJME,Box2d,Export,test,Tests,Unpack,Video")
                .split(',')
        )
        settings.gameTitle = "SimpleExportTest"
        settings.configName = "test"
        settings.dstFile = tmp.getChild("testJar.jar")
        settings.firstSceneRef = res.getChild("icon.obj")
        val progress = ProgressBar("Test", "%", 1.0)
        ExportProcess.execute(project, settings, progress)
        return settings.dstFile
    }

    /**
     * create an engine build, and run it, and listener whether
     * the first frame can be rendered successfully
     * */
    @Test
    fun testEngineBuildIsRunning() {
        Engine.cancelShutdown()
        val clock = Clock("SimpleExportTest")
        val executableFile = createEngineBuild()
        clock.stop("Creating Export")
        val process = BetterProcessBuilder("java", 2, false)
            .addAll(listOf("-jar", executableFile.absolutePath))
            .start()
        clock.stop("Creating Process")
        var done = false
        val timeLimit = Time.nanoTime + 5 * SECONDS_TO_NANOS
        waitUntilOutput(process.inputStream, "for First frame finished") { done = true }
        logErrors(process.errorStream)
        Sleep.waitUntil(false) { done || Time.nanoTime > timeLimit }
        process.destroyForcibly()
        assertTrue(done)
        clock.stop("Rendering first frame")
    }

    private fun logErrors(stream: InputStream) {
        thread(name = "logErrors") {
            val reader = stream.bufferedReader()
            while (true) {
                val nextLine = reader.readLine() ?: break
                System.err.println("[Build] $nextLine")
            }
        }
    }

    private fun waitUntilOutput(stream: InputStream, keyPhrase: String, whenDone: () -> Unit) {
        thread(name = "logInfos") {
            val reader = stream.bufferedReader()
            while (true) {
                val nextLine = reader.readLine() ?: break
                println("[Build] $nextLine")
                if (keyPhrase in nextLine) {
                    whenDone()
                    break
                }
            }
        }
    }
}