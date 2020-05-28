/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package me.anno.gpu;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import static me.anno.gpu.GFX.flatShader;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;
import java.nio.*;

/**
 * Showcases how you can use multithreading in a GLFW application in order to
 * separate the (blocking) winproc handling from the render loop.
 * 
 * @author Kai Burjack
 */
public class GFXBase0 {

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
        glfwSetErrorCallback(errorCallback = GLFWErrorCallback.createPrint(System.err));
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

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

    public void addCallbacks(){
        glfwSetKeyCallback(window, keyCallback = new GLFWKeyCallback() {
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                    glfwSetWindowShouldClose(window, true);
            }
        });
        glfwSetFramebufferSizeCallback(window, fsCallback = new GLFWFramebufferSizeCallback() {
            public void invoke(long window, int w, int h) {
                if (w > 0 && h > 0) {
                    width = w;
                    height = h;
                }
            }
        });
    }

    void renderLoop() {

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);

        GL.createCapabilities();

        debugProc = GLUtil.setupDebugMessageCallback();

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
        glClearColor(0.3f, 0.5f, 0.7f, 0.0f);
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