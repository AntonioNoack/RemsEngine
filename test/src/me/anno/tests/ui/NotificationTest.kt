package me.anno.tests.ui

import me.anno.Engine
import me.anno.engine.OfficialExtensions
import me.anno.image.ImageCache
import me.anno.jvm.images.BIImage.createBufferedImage
import me.anno.utils.OS.res
import me.anno.utils.assertions.assertTrue
import java.awt.SystemTray
import java.awt.TrayIcon
import kotlin.system.exitProcess

/**
 * Just testing a feature in AWT: showing Windows notifications.
 * Those would be useful for Rem's Engine to show that rendering is done :)
 * */
fun main() {
    OfficialExtensions.initForTests()
    assertTrue(SystemTray.isSupported())
    val tray = SystemTray.getSystemTray()
    val image = ImageCache[res.getChild("icon.png"), false]!!.createBufferedImage()
    val icon = TrayIcon(image, "Noti-Demo ✉")
    icon.isImageAutoSize = true
    icon.toolTip = "Hi, I'm some ttt with emojis 😃"
    tray.add(icon)
    // none/icon/warn/error
    icon.displayMessage("Lö Title", "You got a message, check it out!", TrayIcon.MessageType.INFO)
    Engine.requestShutdown()
    exitProcess(0) // todo BulletJME isn't shutting down properly :(
}