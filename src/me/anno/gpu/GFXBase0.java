/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package me.anno.gpu;

import kotlin.Unit;
import me.anno.config.DefaultConfig;
import me.anno.input.Input;
import me.anno.language.translation.NameDesc;
import me.anno.studio.Build;
import me.anno.studio.StudioBase;
import me.anno.ui.base.Panel;
import me.anno.ui.base.menu.Menu;
import me.anno.utils.Clock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.Version;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GLUtil;
import org.lwjgl.system.Callback;
import org.lwjgl.system.MemoryStack;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.KHRDebug.GL_DEBUG_OUTPUT;
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
 *
 * todo rewrite this such that we can have multiple windows, which may be nice for the color picker, and maybe other stuff,
 * todo e.g. having multiple editor windows
 *
 * todo rebuild and recompile the glfw driver, which handles the touch input, so the input can be assigned to the window
 * (e.g. add 1 to the pointer)
 *
 */
public class GFXBase0 {

    public static boolean enableVsync = true;
    private static int lastVsyncInterval = -1;

    public static void setVsyncEnabled(boolean enabled) {
        enableVsync = enabled;
    }

    public static void toggleVsync() {
        enableVsync = !enableVsync;
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

    public void loadRenderDoc() {
        // must be executed before OpenGL-init
        if (Build.INSTANCE.isDebug()) {
            try {
                // todo this path should be customizable
                // if renderdoc is install on linux, or given in the path, we could use it as well with loadLibrary()
                // at least this is the default location for RenderDoc
                String file = "C:/Program Files/RenderDoc/renderdoc.dll";
                if (new File(file).exists()) {
                    System.load(file);
                } else LOGGER.warn("Did not find RenderDoc, searched '" + file + "'");
            } catch (Exception e) {
                LOGGER.warn("Could not initialize RenderDoc");
                e.printStackTrace();
            }
        }
    }

    public void run() {
        try {

            // todo there should be a way to switch this at runtime, or at least some runtime argument...
            loadRenderDoc();

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
        if (Build.INSTANCE.isDebug()) {
            glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE);
        }
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

    private void setNewTitle(String title) {
        glfwSetWindowTitle(window, title);
    }

    boolean isInFocus = false;

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
        glfwSetWindowFocusCallback(window, new GLFWWindowFocusCallback() {
            @Override
            public void invoke(long window, boolean isInFocus0) {
                isInFocus = isInFocus0;
            }
        });
    }

    GLCapabilities capabilities;

    private void updateVsync() {
        int targetInterval = isInFocus ? enableVsync ? 1 : 0 : 2;
        if (lastVsyncInterval != targetInterval) {
            glfwSwapInterval(targetInterval);
            lastVsyncInterval = targetInterval;
        }
    }

    private void runRenderLoop() {

        Clock tick = new Clock();

        glfwMakeContextCurrent(window);
        updateVsync();

        tick.stop("make context current + vsync");

        capabilities = GL.createCapabilities();

        tick.stop("OpenGL initialization");

        setupDebugging();

        GFX.gameInit.invoke();
        renderStep0();

        long lastTime = System.nanoTime();

        while (!destroyed) {
            synchronized (lock2) {
                renderStep();

                synchronized (lock) {
                    if (!destroyed) {
                        glfwSwapBuffers(window);
                    }
                }

                updateVsync();

                if (!isInFocus) {

                    // enforce 30 fps, because we don't need more
                    // and don't want to waste energy
                    long currentTime = System.nanoTime();
                    long waitingTime = 30 - (currentTime - lastTime) / 1_000_000;
                    lastTime = currentTime;

                    if (waitingTime > 0) try {
                        // wait does not work, causes IllegalMonitorState exception
                        Thread.sleep(waitingTime);
                    } catch (InterruptedException ignored) {
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
                                if (!info.startsWith("[LWJGL]")) {
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
                                } // else idc
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
        if (Build.INSTANCE.isDebug()) {
            // System.loadLibrary("renderdoc");
            GL43.glDebugMessageCallback((source, type, id, severity, length, message, nothing) -> {
                System.out.println("source: " + source + ", type: " + type + ", id: " + id + ", severity: " + severity +
                        ", length: " + length + ", message: " + message);
            }, 0);
            glEnable(GL_DEBUG_OUTPUT);
        }
    }

    public void renderStep() {

        glClear(GL_COLOR_BUFFER_BIT);

        float elapsed = 0.001667f;

        float aspect = (float) width / height;
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(-aspect, aspect, -1f, +1f, -1f, +1f);

        glMatrixMode(GL_MODELVIEW);
        glRotatef(elapsed * 10f, 0, 0, 1);
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

    public Panel trapMousePanel;
    public float trapMouseRadius = 250f;

    void windowLoop() {

        Thread.currentThread().setName("GLFW");

        /*
         * Start new thread to have the OpenGL context current in and which does
         * the rendering.
         */
        new Thread(() -> {
            runRenderLoop();
            cleanUp();
        }).start();

        boolean cursorIsHidden = false;

        while (!shouldClose) {
            while (!glfwWindowShouldClose(window) && !shouldClose) {
                if (newTitle != null) {
                    setNewTitle(newTitle);
                    newTitle = null;
                }
                if (trapMousePanel != null && isInFocus && trapMousePanel == GFX.INSTANCE.getInFocus0()) {
                    if (!cursorIsHidden) {
                        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
                        cursorIsHidden = true;
                    }
                    float x = Input.INSTANCE.getMouseX();
                    float y = Input.INSTANCE.getMouseY();
                    float centerX = GFX.INSTANCE.getWindowWidth() * 0.5f;
                    float centerY = GFX.INSTANCE.getWindowHeight() * 0.5f;
                    float dx = x - centerX;
                    float dy = y - centerY;
                    if (dx * dx + dy * dy > trapMouseRadius * trapMouseRadius) {
                        glfwSetCursorPos(window, centerX, centerY);
                    }
                } else if (cursorIsHidden) {
                    glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                    cursorIsHidden = false;
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
                    GFX.INSTANCE.getWindowStack().peek().setAcceptsClickAway(false);
                    return null;
                });
            }
        }

    }

    public void requestExit() {
        glfwSetWindowShouldClose(window, true);
    }

    public void cleanUp() {
    }

    public static void main(String[] args) {
        new GFXBase0().run();
    }

}