package me.anno.gpu.rtrt

import org.lwjgl.PointerBuffer
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWVulkan.*
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.EXTDebugReport.VK_EXT_DEBUG_REPORT_EXTENSION_NAME
import org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR
import org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME
import org.lwjgl.vulkan.VK10.*
import java.nio.ByteBuffer
import java.nio.FloatBuffer

fun String.toBuffer(): ByteBuffer {
    val bytes = toByteArray()
    val buffer = ByteBuffer.allocateDirect(bytes.size + 1)
    buffer.put(bytes)
    buffer.put(0)
    buffer.flip()
    return buffer
}

fun List<String>.toBuffers(): PointerBuffer {
    val buffer = PointerBuffer.allocateDirect(size)
    for (index in indices) buffer.put(index, this[index].toBuffer()) // todo correct???
    return buffer
}

inline fun <reified V> PointerBuffer.map(run: (Long) -> V): Array<V> {
    return Array(remaining()) {
        run(get(it))
    }
}

fun main() {

    fun check(code: Int) {
        val error = when (code) {
            0 -> return // fine
            VK_ERROR_OUT_OF_HOST_MEMORY -> "VK_ERROR_OUT_OF_HOST_MEMORY"
            VK_ERROR_OUT_OF_DEVICE_MEMORY -> "VK_ERROR_OUT_OF_HOST_MEMORY "
            VK_ERROR_INITIALIZATION_FAILED -> "VK_ERROR_INITIALIZATION_FAILED "
            VK_ERROR_DEVICE_LOST -> "VK_ERROR_DEVICE_LOST"
            VK_ERROR_MEMORY_MAP_FAILED -> "VK_ERROR_MEMORY_MAP_FAILED"
            VK_ERROR_LAYER_NOT_PRESENT -> "VK_ERROR_LAYER_NOT_PRESENT"
            VK_ERROR_EXTENSION_NOT_PRESENT -> "VK_ERROR_EXTENSION_NOT_PRESENT"
            VK_ERROR_FEATURE_NOT_PRESENT -> "VK_ERROR_FEATURE_NOT_PRESENT"
            VK_ERROR_INCOMPATIBLE_DRIVER -> "VK_ERROR_INCOMPATIBLE_DRIVER"
            VK_ERROR_TOO_MANY_OBJECTS -> "VK_ERROR_TOO_MANY_OBJECTS"
            VK_ERROR_FORMAT_NOT_SUPPORTED -> "VK_ERROR_FORMAT_NOT_SUPPORTED"
            VK_ERROR_FRAGMENTED_POOL -> "VK_ERROR_FRAGMENTED_POOL"
            else -> "$code"
        }
        throw IllegalStateException(error)
    }

    // only available with Vulkan SDK, so remove when creating a build
    val instanceLayers = listOf("VK_LAYER_KHRONOS_validation")
    val instanceExtensions = mutableListOf(VK_EXT_DEBUG_REPORT_EXTENSION_NAME)
    val deviceExtensions = listOf(VK_KHR_SWAPCHAIN_EXTENSION_NAME)

    // don't forget to initialize glfw!
    glfwInit()

    // check for vulkan support
    if (!glfwVulkanSupported()) {
        // not supported
        glfwTerminate()
        return
    }

    val extensions = glfwGetRequiredInstanceExtensions()
    instanceExtensions += extensions!!.map { MemoryUtil.memUTF8(it) }.toList()

    val nullptr = null
    val appName = "GLFW with Vulkan"
    val engineName = "Rem's Engine"

    // create instance
    val applicationInfo = VkApplicationInfo.create()
    applicationInfo.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
    applicationInfo.apiVersion(VK_MAKE_VERSION(1, 0, 2))
    applicationInfo.applicationVersion(VK_MAKE_VERSION(0, 0, 1))
    applicationInfo.engineVersion(VK_MAKE_VERSION(0, 0, 1))
    applicationInfo.pApplicationName(appName.toBuffer())
    applicationInfo.pEngineName(engineName.toBuffer())

    val instanceCreateInfo = VkInstanceCreateInfo.create()
    instanceCreateInfo.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
    instanceCreateInfo.pApplicationInfo(applicationInfo)
    instanceCreateInfo.ppEnabledLayerNames(instanceLayers.toBuffers())
    instanceCreateInfo.ppEnabledExtensionNames(instanceExtensions.toBuffers())

    val vkInstancePtr = PointerBuffer.allocateDirect(1)
    check(vkCreateInstance(instanceCreateInfo, null, vkInstancePtr))
    val instance = VkInstance(vkInstancePtr[0], instanceCreateInfo)

    val tmp1 = IntArray(1)

    // Get GPUs
    vkEnumeratePhysicalDevices(instance, tmp1, nullptr)
    val gpuCount = tmp1[0]
    val gpus = PointerBuffer.allocateDirect(gpuCount)
    check(vkEnumeratePhysicalDevices(instance, tmp1, gpus))
    val gpus2 = gpus.map { VkPhysicalDevice(it, instance) }

    val firstGPU = gpus2.first()

    // select graphics queue family
    vkGetPhysicalDeviceQueueFamilyProperties(firstGPU, tmp1, nullptr)
    val queueFamilyCount = tmp1[0]

    val familyProperties =
        VkQueueFamilyProperties.Buffer(ByteBuffer.allocateDirect(VkQueueFamilyProperties.SIZEOF * queueFamilyCount))
    vkGetPhysicalDeviceQueueFamilyProperties(firstGPU, tmp1, familyProperties)

    var graphicsQueueFamily = -1
    for (i in 0 until queueFamilyCount) {
        if ((familyProperties[i].queueFlags() and VK_QUEUE_GRAPHICS_BIT) != 0) {
            graphicsQueueFamily = i
        }
    }

    if (graphicsQueueFamily == -1) {
        // queue family not found
        glfwTerminate()
        return
    }
    // graphics_queue_family now contains queue family ID which supports graphics

    // Create Vulkan device
    val priorities = FloatBuffer.allocate(1)
    priorities.put(0, 1f)
    val queueCreateInfo = VkDeviceQueueCreateInfo.create()
    queueCreateInfo.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
    // queue_create_info.queueCount(1)
    queueCreateInfo.queueFamilyIndex(graphicsQueueFamily)
    queueCreateInfo.pQueuePriorities(priorities)

    val deviceCreateInfo = VkDeviceCreateInfo.create()
    deviceCreateInfo.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
    val infos = VkDeviceQueueCreateInfo.Buffer(ByteBuffer.allocateDirect(VkDeviceQueueCreateInfo.SIZEOF))
    infos.put(0, queueCreateInfo)
    deviceCreateInfo.pQueueCreateInfos(infos)
//	device_create_info.enabledLayerCount			= device_layers.size()				// deprecated
//	device_create_info.ppEnabledLayerNames			= device_layers.data()				// deprecated
    val deviceFeatures = VkPhysicalDeviceFeatures.create()
    deviceCreateInfo.pEnabledFeatures(deviceFeatures)
    deviceCreateInfo.ppEnabledExtensionNames(deviceExtensions.toBuffers())

    // VkDevice device = VK_NULL_HANDLE
    val devicePtr = PointerBuffer.allocateDirect(1)
    check(vkCreateDevice(firstGPU, deviceCreateInfo, nullptr, devicePtr))
    val device = VkDevice(devicePtr[0], firstGPU, deviceCreateInfo)

    // create window
    var width = 800
    var height = 600
    glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API) // This tells GLFW to not create an OpenGL context with the window
    val window = glfwCreateWindow(width, height, appName, 0L, 0L)

    // make sure we indeed get the surface size we want.
    val tmp2 = IntArray(2)
    glfwGetFramebufferSize(window, tmp1, tmp2)
    width = tmp1[0]
    height = tmp2[0]

    // Create window surface, looks a lot like a Vulkan function ( and not GLFW function )
    // This is a one function solution for all operating systems. No need to hassle with the OS specifics.
    // For windows this would be vkCreateWin32SurfaceKHR() or on linux XCB window library this would be vkCreateXcbSurfaceKHR()
    val tmp1L = LongArray(1)
    if (glfwCreateWindowSurface(instance, window, nullptr, tmp1L) != VK_SUCCESS) {
        // couldn't create surface, exit
        glfwTerminate()
    }
    val surface = tmp1L[0]

    /*
    All regular Vulkan API stuff goes here, no more GLFW commands needed for the window.
    We still need to initialize the swapchain, it's images and all the rest
    just like we would have done with OS native windows.
    */

    // Destroy window surface, Note that this is a native Vulkan API function
    // ( surface was created with GLFW function )
    vkDestroySurfaceKHR(instance, surface, nullptr)

    // destroy window using GLFW function
    glfwDestroyWindow(window)

    // destroy Vulkan device and instance normally
    vkDestroyDevice(device, nullptr)
    vkDestroyInstance(instance, nullptr)

    // don't forget to terminate glfw either
    glfwTerminate()


}