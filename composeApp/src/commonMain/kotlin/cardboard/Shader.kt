package cardboard

import kotlin.jvm.JvmField

/**
 * A shader program that contains all information to compile the shader.
 * It is written in a specific shading language.
 */
sealed interface Shader

/** A GLSL shader program. */
data class GLSLShader(
    // The vertex shader source code in GLSL
    @JvmField val vertexShader: String,

    // The fragment shader source code in GLSL
    @JvmField val fragmentShader: String,

    // Maps attribute names to their variable names in the shader
    // Keys: "position", "normal", and "uv"
    @JvmField val attributes: Map<String, String>,

    // Maps uniform names to their variable names in the shader
    // Keys: "mv", "texture"
    @JvmField val uniforms: Map<String, String>,
) : Shader

/** A Metal Shading Language (MSL) shader program. */
data class MSLShader(
    // The vertex and fragment shader source code in MSL
    @JvmField val source: String,

    // The name of the entry point function for the vertex shader
    @JvmField val vertexFunc: String = "vertex_main",

    // The name of the entry point function for the fragment shader
    @JvmField val fragmentFunc: String = "fragment_main",

    // Maps attribute names to their indices in the shader
    // Keys: "position", "normal", and "uv"
    @JvmField val attributes: Map<String, Int>,

    // Maps uniform names to their indices in the shader
    // Keys: "mv", "texture"
    @JvmField val uniforms: Map<String, Int>,
) : Shader

const val ATTR_POSITION = "position"
const val ATTR_NORMAL = "normal"
const val ATTR_UV = "uv"

const val UNIFORM_MV = "mv"
const val UNIFORM_ST = "st"
const val UNIFORM_TEXTURE = "texture"

/**
 * A collection of shaders that can be used with different rendering engines.
 */
class ShaderCollection(vararg val shaders: Shader) {
    fun findMatch(engine: Engine): Shader {
        return shaders.first {
            when (it) {
                is GLSLShader -> engine == Engine.GLES2 || engine == Engine.GLES3 || engine == Engine.VULKAN
                is MSLShader -> engine == Engine.METAL
            }
        }
    }
}
