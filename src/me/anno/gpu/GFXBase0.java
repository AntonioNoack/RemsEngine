/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package me.anno.gpu;

import kotlin.Unit;
import me.anno.Build;
import me.anno.config.DefaultConfig;
import me.anno.ecs.prefab.Prefab;
import me.anno.ecs.prefab.change.Path;
import me.anno.input.Input;
import me.anno.io.files.FileReference;
import me.anno.io.files.InvalidRef;
import me.anno.io.zip.InnerFolder;
import me.anno.io.zip.InnerPrefabFile;
import me.anno.language.translation.NameDesc;
import me.anno.mesh.obj.OBJReader2;
import me.anno.studio.StudioBase;
import me.anno.ui.base.Panel;
import me.anno.ui.base.menu.Menu;
import me.anno.ui.utils.WindowStack;
import me.anno.utils.Clock;
import me.anno.utils.io.ResourceHelper;
import me.anno.utils.structures.maps.KeyPairMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GLUtil;
import org.lwjgl.system.Callback;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.awt.*;
import java.io.*;
import java.nio.IntBuffer;

import static me.anno.gpu.debug.OpenGLDebug.*;
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
 * <p>
 * todo rewrite this such that we can have multiple windows, which may be nice for the color picker, and maybe other stuff,
 * todo e.g. having multiple editor windows
 * <p>
 * todo rebuild and recompile the glfw driver, which handles the touch input, so the input can be assigned to the window
 * (e.g. add 1 to the pointer)
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

    private static final Logger LOGGER = LogManager.getLogger(GFXBase0.class);

    private GLFWErrorCallback errorCallback;
    private GLFWKeyCallback keyCallback;
    private GLFWFramebufferSizeCallback fsCallback;
    private Callback debugProc;

    public static String projectName = "X";
    public String title = projectName;

    public long window;
    public int width = 800;
    public int height = 700;
    public final Object glfwLock = new Object();
    public final Object openglLock = new Object();
    public boolean destroyed;

    public boolean isInFocus = false;
    public boolean isMinimized = false;
    public boolean needsRefresh = true;

    public float contentScaleX = 1f;
    public float contentScaleY = 1f;

    public GLCapabilities capabilities;

    public Robot robot = null;

    public void loadRenderDoc() {
        // must be executed before OpenGL-init
        String renderDocPath = DefaultConfig.INSTANCE.get("debug.renderdoc.path", "C:/Program Files/RenderDoc/renderdoc.dll");
        boolean renderDocEnabled = DefaultConfig.INSTANCE.get("debug.renderdoc.enabled", Build.INSTANCE.isDebug());
        if (renderDocEnabled) {
            try {
                // if renderdoc is install on linux, or given in the path, we could use it as well with loadLibrary()
                // at least this is the default location for RenderDoc
                if (new File(renderDocPath).exists()) {
                    System.load(renderDocPath);
                } else LOGGER.warn("Did not find RenderDoc, searched '" + renderDocPath + "'");
            } catch (Exception e) {
                LOGGER.warn("Could not initialize RenderDoc");
                e.printStackTrace();
            }
        }
    }

    public void run() {
        try {

            loadRenderDoc();

            init();
            windowLoop();

            // wait for the last frame to be finished,
            // before we actually destroy the window and its framebuffer
            synchronized (glfwLock) {
                destroyed = true;
            }

            synchronized (openglLock) {
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

        double[] x = new double[1], y = new double[1];
        glfwGetCursorPos(window, x, y);
        Input.INSTANCE.setMouseX((float) x[0]);
        Input.INSTANCE.setMouseY((float) y[0]);

        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }

    }

    public void setTitle(String title) {
        newTitle = title;
    }

    private void setNewTitle(String title) {
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

        glfwSetWindowFocusCallback(window, (long window, boolean isInFocus0) -> isInFocus = isInFocus0);
        glfwSetWindowIconifyCallback(window, (long window, boolean isMinimized0) -> {
            isMinimized = isMinimized0;
            // just be sure in case the OS/glfw don't send it
            if (isMinimized0) needsRefresh = true;
        });
        glfwSetWindowRefreshCallback(window, (long window) -> needsRefresh = true);

        // can we use that?
        // glfwSetWindowMaximizeCallback()


        float[] x = {1f};
        float[] y = {1f};
        glfwGetWindowContentScale(window, x, y);
        contentScaleX = x[0];
        contentScaleY = y[0];

        // todo when the content scale changes, we probably should scale our text automatically as well
        // this happens, when the user moved the window from a display with dpi1 to a display with different dpi
        glfwSetWindowContentScaleCallback(window, (long window, float xScale, float yScale) -> {
            LOGGER.info("Window Content Scale changed: " + xScale + " x " + yScale);
            contentScaleX = xScale;
            contentScaleY = yScale;
        });

    }

    public void requestAttention() {
        glfwRequestWindowAttention(window);
    }

    public void requestAttentionMaybe() {
        if (!isInFocus) {
            requestAttention();
        }
    }

    protected void forceUpdateVsync() {
        int targetInterval = isInFocus ? enableVsync ? 1 : 0 : 2;
        glfwSwapInterval(targetInterval);
        lastVsyncInterval = targetInterval;
    }

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

        tick.stop("Make context current + vsync");

        capabilities = GL.createCapabilities();

        tick.stop("OpenGL initialization");

        setupDebugging();

        renderFrame0();

        glfwSwapBuffers(window);

        renderStep0();

        GFX.gameInit.invoke();

        long lastTime = System.nanoTime();

        while (!destroyed) {

            synchronized (openglLock) {
                renderStep();
            }

            synchronized (glfwLock) {
                if (!destroyed) {
                    glfwSwapBuffers(window);
                    updateVsync();
                }
            }

            if (!isInFocus || isMinimized) {

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
                String message2 = message != 0 ? MemoryUtil.memUTF8(message) : null;
                LOGGER.warn(message2 +
                        ", source: " + getDebugSourceName(source) +
                        ", type: " + getDebugTypeName(type) +
                        // mmh, not correct, at least for my simple sample I got a non-mapped code
                        ", id: " + GFX.INSTANCE.getErrorTypeName(id) +
                        ", severity: " + getDebugSeverityName(severity)
                );
            }, 0);
            glEnable(GL_DEBUG_OUTPUT);
        }
        GFX.INSTANCE.checkIsGFXThread();
    }

    // can be set by the application
    public int frame0BackgroundColor = 0;
    public int frame0IconColor = 0x172040;

    public void renderFrame0() {

        // load icon.obj as file, and draw it using OpenGL 1.0

        int c = frame0BackgroundColor;
        glClearColor(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f, 1f);
        glClear(GL_COLOR_BUFFER_BIT);
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();

        // extend space left+right/top+bottom (zooming out a little)
        if (width > height) {
            float dx = (float) width / height;
            glOrtho(-dx, dx, -1f, +1f, -1f, +1f);
        } else {
            float dy = (float) height / width;
            glOrtho(-1f, +1f, -dy, dy, -1f, +1f);
        }

        c = frame0IconColor;
        glColor3f(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f);

        try {
            InputStream stream = ResourceHelper.INSTANCE.loadResource("icon.obj");
            OBJReader2 reader = new OBJReader2(stream, InvalidRef.INSTANCE);
            if (reader.getMeshesFolder().isInitialized()) {
                InnerFolder file = reader.getMeshesFolder().getValue();
                for (FileReference child : file.listChildren()) {
                    // we could use the name as color... probably a nice idea :)
                    Prefab prefab = ((InnerPrefabFile) child).getPrefab();
                    KeyPairMap<Path, String, Object> sets = prefab.getSets();
                    float[] positions = (float[]) sets.get(Path.Companion.getROOT_PATH(), "positions");
                    int[] indices = (int[]) sets.get(Path.Companion.getROOT_PATH(), "indices");
                    if (positions != null) {
                        glBegin(GL_TRIANGLES);
                        if (indices == null) {
                            for (int i = 0; i < positions.length; i += 3) {
                                glVertex2f(positions[i], positions[i + 1]);
                            }
                        } else {
                            for (int index : indices) {
                                int j = index * 3;
                                glVertex2f(positions[j], positions[j + 1]);
                            }
                        }
                        glEnd();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
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

    private String newTitle = null;
    boolean shouldClose = false;

    public Panel trapMousePanel;
    public float trapMouseRadius = 250f;

    public double mouseTargetX = -1.0, mouseTargetY = -1.0;

    public boolean isMouseTrapped() {
        return trapMousePanel != null && isInFocus && trapMousePanel == GFX.INSTANCE.getInFocus0();
    }

    public void moveMouseTo(float x, float y) {
        mouseTargetX = x;
        mouseTargetY = y;
    }

    public void moveMouseTo(double x, double y) {
        mouseTargetX = x;
        mouseTargetY = y;
    }

    public void updateMousePosition() {
        double[] xs = new double[1];
        double[] ys = new double[1];
        glfwGetCursorPos(window, xs, ys);
        Input.INSTANCE.onMouseMove((float) xs[0], (float) ys[0]);
    }

    void windowLoop() {

        Thread.currentThread().setName("GLFW");

        // Start new thread to have the OpenGL context current in and which does the rendering.
        new Thread(() -> {
            runRenderLoop();
            cleanUp();
        }).start();

        boolean cursorIsHidden = false;

        /*new Thread(() -> {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ignored) { }
            requestExit();
        }).start();*/

        while (!shouldClose) {
            while (!glfwWindowShouldClose(window) && !shouldClose) {
                // update title, if necessary
                if (newTitle != null) {
                    setNewTitle(newTitle);
                    newTitle = null;
                }
                // trapping the mouse
                if (isMouseTrapped()) {
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
                if (mouseTargetX != -1.0 && mouseTargetY != -1.0) {
                    if (isInFocus) {
                        glfwSetCursorPos(window, mouseTargetX, mouseTargetY);
                    } else if (robot != null) {
                        int[] x = new int[1];
                        int[] y = new int[1];
                        glfwGetWindowPos(window, x, y);
                        robot.mouseMove((int) mouseTargetX + x[0], (int) mouseTargetY + y[0]);
                    }
                    mouseTargetX = -1.0;
                    mouseTargetY = -1.0;
                }
                // only happens, if keyboard or mouse is used
                glfwWaitEventsTimeout(1.0 / 240.0);// timeout, because otherwise it sleeps forever, until keyboard
                // or mouse input is received
            }
            // close tests
            WindowStack ws = StudioBase.Companion.getDefaultWindowStack();
            if (ws == null || DefaultConfig.INSTANCE.get("window.close.directly", false)) {
                break;
            } else {
                glfwSetWindowShouldClose(window, false);
                GFX.INSTANCE.addGPUTask(1, () -> {
                    Menu.INSTANCE.ask(ws,
                            new NameDesc("Close %1?", "", "ui.closeProgram")
                                    .with("%1", projectName), () -> {
                                shouldClose = true;
                                glfwSetWindowShouldClose(window, true);
                                return null;
                            });
                    Input.INSTANCE.invalidateLayout();
                    ws.peek().setAcceptsClickAway(false);
                    return null;
                });
            }
        }

    }

    public void requestExit() {
        glfwSetWindowShouldClose(window, true);
    }

    public boolean isFramebufferTransparent() {
        return glfwGetWindowAttrib(window, GLFW_TRANSPARENT_FRAMEBUFFER) != GLFW_FALSE;
    }

    /**
     * transparency of the whole window including decoration (buttons, icon and title)
     * window transparency is incompatible with transparent framebuffers!
     * may not succeed, test with getWindowTransparency()
     */
    public void setWindowOpacity(float opacity) {
        glfwWindowHint(GLFW_TRANSPARENT_FRAMEBUFFER, GLFW_FALSE);
        glfwSetWindowOpacity(window, opacity);
    }

    /**
     * rendering special window shapes, e.g. a cloud
     * window transparency is incompatible with transparent framebuffers!
     * may not succeed, test with isFramebufferTransparent()
     */
    public void makeFramebufferTransparent() {
        glfwSetWindowOpacity(window, 1f);
        glfwWindowHint(GLFW_TRANSPARENT_FRAMEBUFFER, GLFW_TRUE);
    }

    /**
     * transparency of the whole window including decoration (buttons, icon and title)
     */
    public float getWindowTransparency() {
        return glfwGetWindowOpacity(window);
    }

    public void cleanUp() {
    }

    public static void main(String[] args) {
        new GFXBase0().run();
    }

}