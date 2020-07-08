package me.anno.utils.test

import me.anno.gpu.GFX
import org.lwjgl.Version
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL30.GL_R8
import java.nio.ByteBuffer

object OpenGLCrash {

    // solution: glPixelStorei(GL_UNPACK_ALIGNMENT, 1)

    fun test(){

        // monochrome 1590 2244 3567960
        /*
#
# A fatal error has been detected by the Java Runtime Environment:
#
#  EXCEPTION_ACCESS_VIOLATION (0xc0000005) at pc=0x00007ff98593509c, pid=2100, tid=8244
#
# JRE version: OpenJDK Runtime Environment (12.0.1+12) (build 12.0.1+12)
# Java VM: OpenJDK 64-Bit Server VM (12.0.1+12, mixed mode, sharing, tiered, compressed oops, g1 gc, windows-amd64)
# Problematic frame:
# C  [atio6axx.dll+0x1bb509c]
#
# No core dump will be written. Minidumps are not enabled by default on client versions of Windows
#
# An error report file with more information is saved as:
# C:\Users\Antonio\Documents\IdeaProjects\VideoStudio\hs_err_pid2100.log
#
# If you would like to submit a bug report, please visit:
#   http://bugreport.java.com/bugreport/crash.jsp
# The crash happened outside the Java Virtual Machine in native code.
# See problematic frame for where to report the bug.
#

        * */

        // 18 x 552

        println(Version.getVersion())
        println(glGetString(GL_VERSION))

        // 242 x 901
        // 337 x 396

        for(i in 0 until 64){

            val w = 1590//(Math.random()*3000+10).toInt()
            val h = 2246//(Math.random()*3000+10).toInt()

            println("[GLCrashTest] $w x $h")

            val texture = glGenTextures()
            glBindTexture(GL_TEXTURE_2D, texture)
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1)

            // val w = 1590
            // val h = 2246

            val byteBuffer = ByteBuffer
                .allocateDirect(w*h + 2*h)
                .position(0)

            glTexImage2D(GL_TEXTURE_2D, 0, GL_R8, w, h, 0, GL_RED, GL_UNSIGNED_BYTE, byteBuffer)

            glDeleteTextures(texture)

        }



    }
}
