package me.anno.gpu

import me.anno.Build.isDebug
import me.anno.config.DefaultConfig
import me.anno.gpu.GLNames.getErrorTypeName
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.pipeline.ClickIdBoundsArray
import me.anno.gpu.query.OcclusionQuery
import me.anno.gpu.shader.GPUShader
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.utils.Clock
import me.anno.utils.GFXFeatures
import me.anno.utils.OS
import me.anno.utils.assertions.assertSame
import me.anno.utils.structures.lists.Lists.firstOrNull2
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.EXTTextureFilterAnisotropic
import org.lwjgl.opengl.GL46C
import org.lwjgl.opengl.GL46C.glGetError
import java.util.Stack
import kotlin.math.max
import kotlin.math.min

/**
 * graphics capabilities, and gfx checks
 * */
object GFX {

    @JvmStatic
    private val LOGGER = LogManager.getLogger(GFX::class)

    @JvmField
    val windows = ArrayList<OSWindow>()

    private val firstWindow = OSWindow("Rem's Engine")

    /**
     * current window, which is being rendered to by OpenGL
     * */
    @JvmField
    var activeWindow: OSWindow? = null

    /**
     * window, that is in focus; may be null
     * */
    @JvmStatic
    val focusedWindow
        get(): OSWindow? = windows.firstOrNull2 { it.isInFocus && !it.shouldClose }

    /**
     * window, that is in focus, or arbitrary window, if undefined
     * */
    @JvmStatic
    val someWindow
        get(): OSWindow = focusedWindow
            ?: windows.firstOrNull()
            ?: firstWindow

    @JvmField
    var supportsAnisotropicFiltering = false

    @JvmField
    var supportsDepthTextures = false

    @JvmField
    var supportsComputeShaders = false

    @JvmField
    var anisotropy = 1f

    @JvmField
    var maxSamples = 1

    @JvmStatic
    var supportsClipControl = false

    @JvmField
    var supportsF32Targets = true

    @JvmField
    var supportsF16Targets = true

    @JvmField
    var maxFragmentUniformComponents = 0

    @JvmField
    var maxVertexUniformComponents = 0

    @JvmField
    var maxBoundTextures = 1

    @JvmField
    var maxUniforms = 0

    @JvmField
    var maxAttributes = 8

    @JvmField
    var maxColorAttachments = 1

    @JvmField
    var maxTextureSize = 512 // assumption before loading anything

    /**
     * Tens based version, e.g., 3.2 becomes 32.
     * Be careful when using this, because we might be running on OpenGL ES instead!
     * */
    @JvmField
    var glVersion = 0

    @JvmField
    var canLooseContext = true // on Android, and when somebody uses multiple StudioBase instances

    @JvmField
    val loadTexturesSync = Stack<Boolean>()
        .apply { push(false) }

    /**
     * location of the current default framebuffer
     * */
    @JvmField
    var offsetX = 0

    @JvmField
    var offsetY = 0

    /**
     * location & size of the current panel
     * */
    @JvmField
    var viewportX = 0

    @JvmField
    var viewportY = 0

    @JvmField
    var viewportWidth = 0

    @JvmField
    var viewportHeight = 0

    @JvmField
    var glThread: Thread? = null

    @JvmStatic
    fun setupBasics(tick: Clock?) {
        LOGGER.info("OpenGL Version: ${GL46C.glGetString(GL46C.GL_VERSION)}")
        LOGGER.info("GLSL Version: ${GL46C.glGetString(GL46C.GL_SHADING_LANGUAGE_VERSION)}")
        LOGGER.info("GPU: ${GL46C.glGetString(GL46C.GL_RENDERER)}, Vendor: ${GL46C.glGetString(GL46C.GL_VENDOR)}")
        // these are not defined in WebGL
        glVersion = GL46C.glGetInteger(GL46C.GL_MAJOR_VERSION) * 10 + GL46C.glGetInteger(GL46C.GL_MINOR_VERSION)
        LOGGER.info("OpenGL Version Id $glVersion")
        GL46C.glPixelStorei(GL46C.GL_UNPACK_ALIGNMENT, 1) // OpenGL is evil ;), for optimizations, we might set it back
        val capabilities = WindowManagement.capabilities
        supportsAnisotropicFiltering = capabilities?.GL_EXT_texture_filter_anisotropic ?: false
        LOGGER.info("OpenGL supports Anisotropic Filtering? $supportsAnisotropicFiltering")
        if (supportsAnisotropicFiltering) {
            val max = GL46C.glGetFloat(EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT)
            anisotropy = min(max, DefaultConfig["gpu.filtering.anisotropic.max", 16f])
        }
        // some of these checks should be set by the platform after calling this, because some conditions may be unknown to lwjgl
        // todo check if rendering is still broken in DX11 in default render mode
        val debugLimitedGPUs = false
        supportsDepthTextures = !debugLimitedGPUs && capabilities?.GL_ARB_depth_texture == true
        supportsClipControl = !GFXFeatures.isOpenGLES
        if (debugLimitedGPUs) supportsClipControl = false
        supportsComputeShaders = if (OS.isWeb) false else capabilities?.GL_ARB_compute_shader == true || glVersion >= 43
        ClickIdBoundsArray.needsBoxes = supportsComputeShaders
        maxVertexUniformComponents = GL46C.glGetInteger(GL46C.GL_MAX_VERTEX_UNIFORM_COMPONENTS)
        maxFragmentUniformComponents = GL46C.glGetInteger(GL46C.GL_MAX_FRAGMENT_UNIFORM_COMPONENTS)
        maxBoundTextures = GL46C.glGetInteger(GL46C.GL_MAX_TEXTURE_IMAGE_UNITS)
        maxAttributes = GL46C.glGetInteger(GL46C.GL_MAX_VERTEX_ATTRIBS)
        maxUniforms = GL46C.glGetInteger(GL46C.GL_MAX_UNIFORM_LOCATIONS)
        maxColorAttachments = if (debugLimitedGPUs) 1 else GL46C.glGetInteger(GL46C.GL_MAX_COLOR_ATTACHMENTS)
        maxSamples = if (debugLimitedGPUs) 1 else max(1, GL46C.glGetInteger(GL46C.GL_MAX_SAMPLES))
        maxTextureSize = if (debugLimitedGPUs) 1024 else max(256, GL46C.glGetInteger(GL46C.GL_MAX_TEXTURE_SIZE))
        GPUShader.useShaderFileCache = !WindowManagement.usesRenderDoc && glVersion >= 41
        if (!GFXFeatures.isOpenGLES && glVersion >= 43) {
            OcclusionQuery.target = GL46C.GL_ANY_SAMPLES_PASSED_CONSERVATIVE
        }
        LOGGER.info("Max Uniform Components: [Vertex: $maxVertexUniformComponents, Fragment: $maxFragmentUniformComponents]")
        LOGGER.info("Max Uniforms: $maxUniforms")
        LOGGER.info("Max Attributes: $maxAttributes")
        LOGGER.info("Max Color Attachments: $maxColorAttachments")
        LOGGER.info("Max Samples: $maxSamples")
        LOGGER.info("Max Texture Size: $maxTextureSize")
        LOGGER.info("Max Bound Textures: $maxBoundTextures")
        tick?.stop("Checking OpenGL properties")
    }

    @JvmStatic
    fun resetFBStack() {
        FBStack.reset()
    }

    @JvmField
    var gpuTaskBudgetNanos = 11L * MILLIS_TO_NANOS

    /**
     * Checks whether the current thread is a graphics-capable thread.
     * */
    @JvmStatic
    fun isGFXThread(): Boolean {
        return Thread.currentThread() == glThread
    }

    /**
     * Ensures that the current thread is a graphics-capable thread.
     * Crashes, if the check fails.
     * */
    @JvmStatic
    fun checkIsGFXThread() {
        val currentThread = Thread.currentThread()
        val glThread = glThread
        assertSame(
            currentThread, glThread,
            if (glThread == null) "Missing OpenGL Context"
            else "OpenGL called from wrong thread"
        )
    }

    /**
     * Ensures that the current thread is a graphics-capable thread,
     * and that no OpenGL errors have occurred recently. Crashes on failure.
     *
     * Only runs in debug mode.
     * */
    @JvmStatic
    fun check(name: String = "") {
        // assumes that the first access is indeed from the OpenGL thread
        if (isDebug) {
            checkIsGFXThread()
            val error = glGetError()
            if (error != 0) {
                val title = "GLException[$name]: ${getErrorTypeName(error)}"
                throw RuntimeException(title)
            }
        }
    }

    @JvmStatic
    fun checkWithoutCrashing(name: String) {
        if (isDebug && glThread == Thread.currentThread()) {
            checkIsGFXThread()
            checkWithoutCrashingImpl(name)
        }
    }

    @JvmStatic
    fun checkWithoutCrashingImpl(name: String) {
        for (i in 0 until 10) {
            val error = glGetError()
            if (error != 0) {
                LOGGER.warn("GLException by $name: ${getErrorTypeName(error)}")
            } else break
        }
    }

    @JvmStatic
    fun checkIfGFX(name: String) {
        if (isDebug && isGFXThread()) {
            checkWithoutCrashingImpl(name)
        }
    }

    fun isPointerValid(pointer: Long): Boolean = pointer > 0
    fun isPointerValid(pointer: Int): Boolean = pointer > 0
    const val INVALID_POINTER = 0
    const val INVALID_POINTER64 = 0L
    const val INVALID_SESSION = -1
}