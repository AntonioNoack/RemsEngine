/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package me.anno.gpu;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import me.anno.input.Input;
import me.anno.studio.Studio;
import me.anno.studio.project.Project;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.LWJGLUtil;
import org.lwjgl.Version;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;
import java.nio.*;
import java.util.Locale;

/**
 * Showcases how you can use multithreading in a GLFW application in order to
 * separate the (blocking) winproc handling from the render loop.
 * 
 * @author Kai Burjack
 *
 * modified by Antonio Noack
 * including all os natives has luckily only very few overhead :) (&lt; 1 MiB)
 */
public class GFXBase0 {

    static Logger LOGGER = LogManager.getLogger(GFXBase0.class);

    GLFWErrorCallback errorCallback;
    GLFWKeyCallback keyCallback;
    GLFWFramebufferSizeCallback fsCallback;
    Callback debugProc;

    public long window;
    public int width = 800;
    public int height = 700;
    final Object lock = new Object();
    final Object lock2 = new Object();
    boolean destroyed;

    public void run() {
        try {

            init();
            winProcLoop();

            synchronized (lock) {
                destroyed = true;
                glfwDestroyWindow(window);
            }
            if (debugProc != null)
                debugProc.free();
            keyCallback.free();
            fsCallback.free();
        } finally {
            glfwTerminate();
            errorCallback.free();
        }
    }

    void init() {

        LOGGER.info("Using LWJGL Version " + Version.getVersion());

        long t0 = System.nanoTime();
        glfwSetErrorCallback(errorCallback = GLFWErrorCallback.createPrint(System.err));
        long t1 = System.nanoTime();
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        long t2 = System.nanoTime();
        LOGGER.info(String.format(Locale.ENGLISH, "Used %.3fs for error callback + %.3fs for glfwInit", ((t1-t0)*1e-9f), ((t2-t1)*1e-9f)));

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        window = glfwCreateWindow(width, height, "Hello World!", NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        addCallbacks();

        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowPos(window, (vidmode.width() - width) / 2, (vidmode.height() - height) / 2);
        try (MemoryStack frame = MemoryStack.stackPush()) {
            IntBuffer framebufferSize = frame.mallocInt(2);
            nglfwGetFramebufferSize(window, memAddress(framebufferSize), memAddress(framebufferSize) + 4);
            width = framebufferSize.get(0);
            height = framebufferSize.get(1);
        }

        glfwSetWindowTitle(window, "Rem's Studio");
        glfwShowWindow(window);

    }

    public void updateTitle(){
        Project project = Studio.INSTANCE.getProject();
        glfwSetWindowTitle(window, project == null ? "Rem's Studio" : "Rem's Studio: "+project.getFile().getName());
    }

    public void addCallbacks(){
        glfwSetKeyCallback(window, keyCallback = new GLFWKeyCallback() {
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                    glfwSetWindowShouldClose(window, true);
            }
        });
        glfwSetFramebufferSizeCallback(window, fsCallback = new GLFWFramebufferSizeCallback() {
            public void invoke(long window, int w, int h) {
                if (w > 0 && h > 0 && (w != width || h != height)) {
                    Studio.INSTANCE.addEvent(() -> {
                        width = w;
                        height = h;
                        Input.INSTANCE.setFramesSinceLastInteraction(0);
                        return Unit.INSTANCE;
                    });
                }
            }
        });
    }

    void renderLoop() {

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);

        GL.createCapabilities();

        debugProc = GLUtil.setupDebugMessageCallback();

        GFX.gameInit.invoke();
        renderStep0();

        while (!destroyed) {
            synchronized (lock2){
                renderStep();

                synchronized (lock) {
                    if (!destroyed) {
                        glfwSwapBuffers(window);
                    }
                }
            }
        }

        GFX.shutdown.invoke();

    }

    public void renderStep0(){
    }

    public void renderStep(){

        glClear(GL_COLOR_BUFFER_BIT);
        // glViewport(0, 0, width, height);

        float elapsed = 0.001667f;

        float aspect = (float) width / height;
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(-1.0f * aspect, +1.0f * aspect, -1.0f, +1.0f, -1.0f, +1.0f);

        glMatrixMode(GL_MODELVIEW);
        glRotatef(elapsed * 10.0f, 0, 0, 1);
        glBegin(GL_QUADS);
        glVertex2f(-0.5f, -0.5f);
        glVertex2f(+0.5f, -0.5f);
        glVertex2f(+0.5f, +0.5f);
        glVertex2f(-0.5f, +0.5f);
        glEnd();

    }

    void winProcLoop() {
        /*
         * Start new thread to have the OpenGL context current in and which does
         * the rendering.
         */
        new Thread(new Runnable() {
            public void run() {
                renderLoop();
            }
        }).start();

        while (!glfwWindowShouldClose(window)) {
            glfwWaitEvents();
        }
    }

    public static void main(String[] args) {
        new GFXBase0().run();
    }

}