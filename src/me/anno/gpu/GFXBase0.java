/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package me.anno.gpu;

import kotlin.Unit;
import me.anno.config.DefaultConfig;
import me.anno.input.Input;
import me.anno.language.translation.NameDesc;
import me.anno.studio.StudioBase;
import me.anno.ui.base.menu.Menu;
import me.anno.utils.Clock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GLDebugMessageCallback;
import org.lwjgl.opengl.GLUtil;
import org.lwjgl.system.Callback;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeResource;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.IntBuffer;
import java.util.ArrayList;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL43.GL_DEBUG_OUTPUT;
import static org.lwjgl.opengl.GL43.glDebugMessageCallback;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memAddress;

/**
 * Showcases how you can use multithreading in a GLFW application in order to
 * separate the (blocking) winproc handling from the render loop.
 *
 * @author Kai Burjack
 * <p>
 * modified by Antonio Noack
 * including all os natives has luckily only very few overhead :) (&lt; 1 MiB)
 */
public class GFXBase0 {

    public static boolean enableVsync = true;

    public static void setVsyncEnabled(boolean enabled) {
        enableVsync = enabled;
        glfwSwapInterval(enableVsync ? 1 : 0);
    }

    public static void toggleVsync() {
        enableVsync = !enableVsync;
        glfwSwapInterval(enableVsync ? 1 : 0);
    }

    static Logger LOGGER = LogManager.getLogger(GFXBase0.class);

    GLFWErrorCallback errorCallback;
    GLFWKeyCallback keyCallback;
    GLFWFramebufferSizeCallback fsCallback;
    Callback debugProc;

    public String title = "Rem's Studio";

    public long window;
    public int width = 800;
    public int height = 700;
    final Object lock = new Object();
    final Object lock2 = new Object();
    boolean destroyed;

    public void run() {
        try {

            init();
            windowLoop();

            synchronized (lock) {
                destroyed = true;
                glfwDestroyWindow(window);
            }

            if (debugProc != null)
                debugProc.free();
            keyCallback.free();
            fsCallback.free();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            glfwTerminate();
            errorCallback.free();
        }
    }

    void init() {

        LOGGER.info("Using LWJGL Version " + Version.getVersion());

        Clock tick = new Clock();
        glfwSetErrorCallback(errorCallback = GLFWErrorCallback.createPrint(System.err));
        tick.stop("error callback");

        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        tick.stop("GLFW initialization");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        // removes scaling options -> how could we replace them?
        // glfwWindowHint(GLFW_DECORATED, GLFW_FALSE);

        // tick.stop("window hints");// 0s

        window = glfwCreateWindow(width, height, projectName, NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        tick.stop("create window");

        addCallbacks();

        tick.stop("adding callbacks");

        GLFWVidMode videoMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (videoMode != null)
            glfwSetWindowPos(window, (videoMode.width() - width) / 2, (videoMode.height() - height) / 2);
        try (MemoryStack frame = MemoryStack.stackPush()) {
            IntBuffer framebufferSize = frame.mallocInt(2);
            nglfwGetFramebufferSize(window, memAddress(framebufferSize), memAddress(framebufferSize) + 4);
            width = framebufferSize.get(0);
            height = framebufferSize.get(1);
        }

        tick.stop("window position");

        glfwSetWindowTitle(window, title);

        // tick.stop("window title"); // 0s

        glfwShowWindow(window);

        tick.stop("show window");

        GFXBase1.Companion.setIcon(window);

        tick.stop("setting icon");

    }

    public void setTitle(String title) {
        newTitle = title;
    }

    private void setNewTitle(String title){
        glfwSetWindowTitle(window, title);
    }

    public void addCallbacks() {
        glfwSetKeyCallback(window, keyCallback = new GLFWKeyCallback() {
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                    glfwSetWindowShouldClose(window, true);
            }
        });
        glfwSetFramebufferSizeCallback(window, fsCallback = new GLFWFramebufferSizeCallback() {
            public void invoke(long window, int w, int h) {
                if (w > 0 && h > 0 && (w != width || h != height)) {
                    StudioBase.Companion.addEvent(() -> {
                        width = w;
                        height = h;
                        Input.INSTANCE.invalidateLayout();
                        return Unit.INSTANCE;
                    });
                }
            }
        });
    }

    GLCapabilities capabilities;
    private void runRenderLoop() {

        Clock tick = new Clock();

        glfwMakeContextCurrent(window);
        glfwSwapInterval(enableVsync ? 1 : 0);

        tick.stop("make context current + vsync");

        capabilities = GL.createCapabilities();

        tick.stop("OpenGL initialization");

        setupDebugging();

        GFX.gameInit.invoke();
        renderStep0();

        while (!destroyed) {
            synchronized (lock2) {
                renderStep();

                synchronized (lock) {
                    if (!destroyed) {
                        glfwSwapBuffers(window);
                    }
                }
            }
        }

        GFX.onShutdown.invoke();

    }

    private void setupDebugging() {
        debugProc = GLUtil.setupDebugMessageCallback(
                new PrintStream(new OutputStream() {
                    // parse the message instead
                    // [LWJGL] OpenGL debug message
                    // ID: 0x1
                    // Source: compiler
                    // Type: other
                    // Severity: notification
                    // Message: ...
                    private final Logger LOGGER = LogManager.getLogger("LWJGL");
                    private String id, source, type, severity;
                    private StringBuilder line = new StringBuilder();

                    @Override
                    public void write(int i) {
                        switch (i) {
                            case '\r':
                                break;// idc
                            case '\n':
                                String info = line.toString().trim();
                                if (info.startsWith("[LWJGL]")) {
                                    // idc...
                                } else {
                                    int index = info.indexOf(':');
                                    if (index > 0) {
                                        String key = info.substring(0, index).trim().toLowerCase();
                                        String value = info.substring(index + 1).trim();
                                        switch (key) {
                                            case "id":
                                                id = value;
                                                break;
                                            case "source":
                                                source = value;
                                                break;
                                            case "type":
                                                type = value;
                                                break;
                                            case "severity":
                                                severity = value;
                                                break;
                                            case "message":
                                                String printedMessage = value + " ID: " + id + " Source: " + source;
                                                if (!"NOTIFICATION".equals(severity))
                                                    printedMessage += " Severity: " + severity;
                                                switch (type == null ? "" : type.toLowerCase()) {
                                                    case "error":
                                                        LOGGER.error(printedMessage);
                                                        break;
                                                    case "other":
                                                        LOGGER.info(printedMessage);
                                                        break;
                                                    default:
                                                        printedMessage += " Type: " + type;
                                                        LOGGER.info(printedMessage);
                                                }
                                                id = null;
                                                source = null;
                                                type = null;
                                                severity = null;
                                                break;
                                        }
                                    } else if (!info.isEmpty()) {
                                        // awkward...
                                        LOGGER.info(info);
                                    }
                                }
                                // LOGGER.info(line.toString());
                                line = new StringBuilder();
                                break;
                            default:
                                final int maxLength = 500 - 3;
                                final int length = line.length();
                                if (length < maxLength) {
                                    line.append((char) i);
                                } else if (length == maxLength) {
                                    line.append("...");
                                }// else too many chars, we don't care ;)
                        }
                    }
                }));
    }

    public void renderStep0() {
    }

    public void renderStep() {

        glClear(GL_COLOR_BUFFER_BIT);

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

    public static String projectName = "Rem's Studio";
    private String newTitle = null;
    boolean shouldClose = false;

    void windowLoop() {
        /*
         * Start new thread to have the OpenGL context current in and which does
         * the rendering.
         */
        new Thread(() -> {
            runRenderLoop();
            cleanUp();
        }).start();

        while (!shouldClose) {
            while (!glfwWindowShouldClose(window) && !shouldClose) {
                if(newTitle != null){
                    setNewTitle(newTitle);
                    newTitle = null;
                }
                glfwWaitEvents();
            }
            if (DefaultConfig.INSTANCE.get("window.close.directly", false)) {
                break;
            } else {
                glfwSetWindowShouldClose(window, false);
                GFX.INSTANCE.addGPUTask(1, () -> {
                    Menu.INSTANCE.ask(new NameDesc("Close %1?", "", "ui.closeProgram")
                            .with("%1", projectName), () -> {
                        shouldClose = true;
                        glfwSetWindowShouldClose(window, true);
                        return null;
                    });
                    Input.INSTANCE.invalidateLayout();
                    StudioBase.instance.getWindowStack().peek().setAcceptsClickAway(false);
                    return null;
                });
            }
        }

    }

    public void requestExit() {
        glfwSetWindowShouldClose(window, true);
    }

    public void cleanUp() { }

    public static void main(String[] args) {
        new GFXBase0().run();
    }

}