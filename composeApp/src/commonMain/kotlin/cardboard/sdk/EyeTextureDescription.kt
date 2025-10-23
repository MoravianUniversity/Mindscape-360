package cardboard.sdk

import kotlin.jvm.JvmField

data class EyeTextureDescription (
    /**
     * Struct to hold information about an eye texture.
     *
     * When using OpenGL ES 2.x and OpenGL ES 3.x, the texture field corresponds to a
     * GLuint variable.
     *
     * When using Vulkan, the texture field corresponds to an uint64_t address pointing
     * to a VkImage variable. The SDK client is expected to manage the object ownership
     * and to guarantee the pointer validity during the CardboardDistortionRenderer_renderEyeToDisplay
     * function execution to ensure it is properly retained.
     *
     * When using Metal, the texture field corresponds to a CFTypeRef variable pointing
     * to a MTLTexture object. The SDK client is expected to manage the object ownership
     * and to guarantee the pointer validity during the CardboardDistortionRenderer_renderEyeToDisplay
     * function execution to ensure it is properly retained.
     *
     * @property texture The texture with eye pixels. Type depends on rendering API:
     *                   - OpenGL ES: GLuint
     *                   - Vulkan: uint64_t address to VkImage
     *                   - Metal: CFTypeRef to MTLTexture
     * @property leftU u coordinate of the left side of the eye
     * @property rightU u coordinate of the right side of the eye
     * @property topV v coordinate of the top side of the eye
     * @property bottomV v coordinate of the bottom side of the eye
     */
    @JvmField val texture: Long = 0L,
    @JvmField val leftU: Float = 0f,
    @JvmField val rightU: Float = 1f,
    @JvmField val topV: Float = 1f,
    @JvmField val bottomV: Float = 0f,
)