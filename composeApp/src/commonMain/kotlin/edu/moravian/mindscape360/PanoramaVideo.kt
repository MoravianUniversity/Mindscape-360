import org.example.cardboard_demo.PanoramaBase


import edu.moravian.cardboard.ATTR_POSITION
import edu.moravian.cardboard.ATTR_UV
import edu.moravian.cardboard.CardboardRendererParams
import edu.moravian.cardboard.GLSLShader
import edu.moravian.cardboard.MSLShader
import edu.moravian.cardboard.Mesh
import edu.moravian.cardboard.NativeRenderView
import edu.moravian.cardboard.NativeRenderer
import edu.moravian.cardboard.ShaderCollection
import edu.moravian.cardboard.UNIFORM_MV
import edu.moravian.cardboard.UNIFORM_ST
import edu.moravian.cardboard.UNIFORM_TEXTURE
import edu.moravian.cardboard.createDefaultNativeRenderer
import edu.moravian.cardboard.util.MediaPlayer
import edu.moravian.mindscape360.sphere

// Things you will have to adjust:
//   1. Panorama mesh: sphere, dome, or truncated sphere
//   2. Floor height: the approximate height of the POV from the floor in meters
//   3. Videos may need to be recolored and/or transcoded to work
//   4. Audio playback volume (we want to use a separate audio file for the voice-over which can
//      have an independent volume, loop independently, and mix-and-match the video with the audio)
//   5. There is a trigger event you can listen to, example:
//          nativeRenderView.addListener(object : RenderViewListener {
//              override fun onTrigger() {
//                  if (player.state == MediaPlayerState.PLAYING) player.pause()
//                  else player.play()
//              }
//          })
//      You can also override onTrigger() in the PanoramaVideo class (it is a RenderViewListener)
//   6. Cardboard device: the user must be able to select which cardboard device they are using
//      (and reselect it if necessary). There is a commented out line in CardboardRenderer that
//      would be good.
//
// Please also test (on real devices) the following:
//   * Brightness changing to max and idle timeout
//      - make sure it resets when backgrounded and hidden
//   * If hiding and reshowing the videos in lots of combinations of backgrounding work


// TODO: current major issues:
//   Both:
//     - the "dome" isn't quite right? (doesn't take into account the floor height?)
//   Android:
//     - when the app is put in the background for >8 secs the renderer dies (happens in non-video ones too; sometimes texture 0 is created?; setupGraphics keeps incrementing texture number?)

class PanoramaVideo(
    view: NativeRenderView,
    val player: MediaPlayer,
    mesh: Mesh = sphere(5.0f), //dome(5.0f),
    floorHeight: Float = -1.7f,  // approximate height of the floor in meters
    params: CardboardRendererParams = CardboardRendererParams(),
    native: NativeRenderer = createDefaultNativeRenderer(),
): PanoramaBase(view, SHADERS, mesh, floorHeight, params, native) {
    companion object {
        private const val VERTEX_SHADER_GLSL = """
uniform mat4 u_MVP;
uniform mat4 u_STMatrix;
attribute vec4 a_Position;
attribute vec2 a_UV;
varying vec2 v_UV;

void main() {
    v_UV = (u_STMatrix * vec4(a_UV, 0.0, 1.0)).st;
    gl_Position = u_MVP * a_Position;
}
"""
        private const val FRAG_SHADER_GLSL = """
#extension GL_OES_EGL_image_external : require
precision mediump float;
uniform samplerExternalOES u_Texture;
varying vec2 v_UV;
void main() { gl_FragColor = texture2D(u_Texture, v_UV); }
"""

        private const val SHADER_MSL = """
#include <metal_stdlib>
#include <simd/simd.h>

using namespace metal;

struct vertex_in {
    float4 position [[attribute(0)]];
    float2 uv [[attribute(1)]];
};
struct vertex_out {
    float4 position [[position]];
    float2 uv [[user(locn0)]];
};

vertex vertex_out vertex_shader(vertex_in in [[stage_in]],
                                constant float4x4& mvp [[buffer(2)]],
                                constant float4x4& stm [[buffer(3)]]) {
    vertex_out out = {
        .position = mvp * in.position,
        .uv = (stm * float4(in.uv, 0.0, 1.0)).xy
    };
    return out;
}

struct frag_out { float4 color [[color(0)]]; };
struct frag_in { float2 uv [[user(locn0)]]; };

fragment frag_out frag_shader(frag_in in [[stage_in]],
                              const texture2d<float> texture [[texture(0)]])
{
    constexpr sampler textureSampler(
        address::clamp_to_edge,
        filter::linear,
        mip_filter::nearest
    );
    frag_out out = { .color = texture.sample(textureSampler, in.uv) };
    return out;
}
"""

        private val SHADERS = ShaderCollection(
            GLSLShader(
                VERTEX_SHADER_GLSL, FRAG_SHADER_GLSL,
                mapOf(ATTR_POSITION to "a_Position", ATTR_UV to "a_UV"),
                mapOf(UNIFORM_MV to "u_MVP", UNIFORM_TEXTURE to "u_Texture", UNIFORM_ST to "u_STMatrix"),
            ),
            MSLShader(
                SHADER_MSL, "vertex_shader", "frag_shader",
                mapOf(ATTR_POSITION to 0, ATTR_UV to 1),
                mapOf(UNIFORM_MV to 2, UNIFORM_TEXTURE to 0, UNIFORM_ST to 3),
            ),
        )
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun onInit() {
        super.onInit()
        texture = native.loadVideoTexture(player.native)
    }

    override fun onPause() {
        super.onPause()
        player.pause()
    }

    override fun onResume() {
        super.onResume()
        player.play()
    }
}