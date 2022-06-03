package it.unipi.di.sam.immersivegallery.common

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.max

// =================================================================================================

private typealias api = GLES20

private fun <T> T.ck(): T {
    val error = api.glGetError()
    if (error != api.GL_NO_ERROR) {
        val callerLine = Thread.currentThread().stackTrace[4].lineNumber
        val message = GLUtils.getEGLErrorString(error)
        Log.e("GL(err)", "At line $callerLine with error $error => $message")
        throw Exception("GL_ERROR")
        return this.ck()
    }
    return this
}

private fun logIfNotEmpty(tag: String, msg: String) =
    msg.takeIf(String::isNotBlank)?.run { Log.e(tag, this) }

// =================================================================================================

/**
 * https://developer.android.com/training/graphics/opengl/environment#renderer
 */
class ImmersiveRenderer : GLSurfaceView.Renderer {

    companion object {
        const val V_SHADER_SRC =
            """
            attribute vec4 aPos;
            attribute vec2 aTex;
            uniform mat4 uMatrix;
            varying vec2 vTex;
            void main() {
                vTex = aTex;
                gl_Position = uMatrix * aPos;
            }
            """

        const val F_SHADER_SRC =
            """
            precision mediump float;
            
            #define PI (3.141592)
            #define PI2 (2.0 * PI)
            #define TIME_SPEED 0.2
            #define TILE_SIZE 5.0
            #define VUE_FACTOR 0.5

            varying vec2 vTex;
            uniform vec2 uResolution;
            uniform sampler2D uTexture;
            uniform float uTime;
            
            // https://thebookofshaders.com/10/
            float random(float v) {
                return fract(sin(v * 442.8776) * 25619.88321);
            }
            
            float fun(float seed) {
                float x1 = fract(uTime * TIME_SPEED + seed);
                // float g = pow(sin(x1 * PI2), 2.0);
                float g = sin(x1 * PI2) / 2.0 + 0.5;

                float x2 = uTime * TIME_SPEED + seed + 123.441;
                float x3 = uTime * TIME_SPEED + seed + 8753.1249;
                float q = max(
                    random(floor(x2)),
                    random(floor(x3))
                );
                
                float f = (q * g) / 2.0 + 0.5;
                
                return pow(f, 3.0);
            }
            
            vec3 proc(vec2 uv, vec2 size, float fx, float fy) {
                uv *= size;
                
                uv.x = mix(floor(uv.x - 1.0), ceil(uv.x + 1.0), fx);
                uv.y = mix(floor(uv.y - 1.0), ceil(uv.y + 1.0), fy);

                uv /= size;
                
                vec3 col = texture2D(uTexture, uv).rgb;
                
                return col;
            }
            
            // =====================================================================================
            // SRC: http://lolengine.net/blog/2013/07/27/rgb-to-hsv-in-glsl
            
            vec3 rgb2hsv(vec3 c) {
                vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
                vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
                vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
            
                float d = q.x - min(q.w, q.y);
                float e = 1.0e-10;
                return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
            }

            vec3 hsv2rgb(vec3 c) {
                vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
                vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
                return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
            }
            
            // =====================================================================================
            
            void main() {
                vec2 uv = vTex;
                vec2 size = vec2(min(uResolution.x, uResolution.y) / TILE_SIZE);
                
                float fx = fun(75.431234);
                float fy = fun(1264.9441);
                
                vec2 t = 1.0 / uResolution.xy;
                
                vec3 col = vec3(0.0);
                col += 0.500 * proc(uv, size, fx, fy);
                col += 0.125 * proc(uv + vec2(+t.x, 0.0), size, fx, fy);
                col += 0.125 * proc(uv + vec2(-t.x, 0.0), size, fx, fy);
                col += 0.125 * proc(uv + vec2(0.0, +t.y), size, fx, fy);
                col += 0.125 * proc(uv + vec2(0.0, -t.y), size, fx, fy);

                vec3 hsv = rgb2hsv(col);
                hsv.z *= VUE_FACTOR;
                col = hsv2rgb(hsv);
                
                gl_FragColor = vec4(col, 1);
            }
            """

        const val LOG_TAG = "IR(gles)"
    }

    // =============================================================================================

    private val quadPosRaw = floatArrayOf(-1F, 1F, -1F, -1F, 1F, -1F, 1F, 1F)
    private val posBuff: FloatBuffer by lazy {
        ByteBuffer.allocateDirect(quadPosRaw.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(quadPosRaw)
                position(0)
            }
        }
    }

    private val quadTexRaw = floatArrayOf(0F, 1F, 0F, 0F, 1F, 0F, 1F, 1F)
    private val texBuff: FloatBuffer by lazy {
        ByteBuffer.allocateDirect(quadTexRaw.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(quadTexRaw)
                position(0)
            }
        }
    }

    private val drawOrder = shortArrayOf(0, 1, 2, 0, 2, 3)
    private val drawBuff: ShortBuffer by lazy {
        ByteBuffer.allocateDirect(drawOrder.size * 2).run {
            order(ByteOrder.nativeOrder())
            asShortBuffer().apply {
                put(drawOrder)
                position(0)
            }
        }
    }

    private val program by lazy { api.glCreateProgram().ck() }
    private val aPosLoc by lazy { api.glGetAttribLocation(program, "aPos").ck() }
    private val aTexLoc by lazy { api.glGetAttribLocation(program, "aTex").ck() }

    private val uMatrixLoc by lazy { api.glGetUniformLocation(program, "uMatrix").ck() }
    private val matrix = FloatArray(16)

    private val uResolutionLoc by lazy { api.glGetUniformLocation(program, "uResolution").ck() }
    private var width = 0
    private var height = 0

    private val uTextureLoc by lazy { api.glGetUniformLocation(program, "uTexture").ck() }
    private var texture = 0
    private val defBitmap by lazy { Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888) }
    private var _bitmapToLoad: Bitmap? = null

    private val uTimeLoc by lazy { api.glGetUniformLocation(program, "uTime").ck() }
    private var lastTimeTick = 0L
    private var timePassed = 0F

    // =============================================================================================

    private fun create() {
        // Create vertex shader
        val vshader = api.glCreateShader(api.GL_VERTEX_SHADER).ck()
        api.glShaderSource(vshader, V_SHADER_SRC).ck()
        api.glCompileShader(vshader).ck()
        logIfNotEmpty("VS", api.glGetShaderInfoLog(vshader).ck())

        // Create fragment shader
        val fshader = api.glCreateShader(api.GL_FRAGMENT_SHADER).ck()
        api.glShaderSource(fshader, F_SHADER_SRC).ck()
        api.glCompileShader(fshader).ck()
        logIfNotEmpty("FS", api.glGetShaderInfoLog(fshader).ck())

        // Create program
        api.glAttachShader(program, vshader).ck()
        api.glAttachShader(program, fshader).ck()
        api.glLinkProgram(program).ck()
        logIfNotEmpty("PR", api.glGetProgramInfoLog(program).ck())

        // Create texture
        val rawIntBuff = intArrayOf(0)
        api.glGenTextures(1, rawIntBuff, 0).ck()
        texture = rawIntBuff[0]

        // Update texture parameters
        api.glActiveTexture(api.GL_TEXTURE0 + 0).ck()
        api.glBindTexture(api.GL_TEXTURE_2D, texture).ck()
        api.glTexParameteri(api.GL_TEXTURE_2D, api.GL_TEXTURE_MIN_FILTER, api.GL_LINEAR).ck()
        api.glTexParameteri(api.GL_TEXTURE_2D, api.GL_TEXTURE_MAG_FILTER, api.GL_LINEAR).ck()
        api.glTexParameteri(api.GL_TEXTURE_2D, api.GL_TEXTURE_WRAP_S, api.GL_CLAMP_TO_EDGE).ck()
        api.glTexParameteri(api.GL_TEXTURE_2D, api.GL_TEXTURE_WRAP_T, api.GL_CLAMP_TO_EDGE).ck()
        GLUtils.texImage2D(api.GL_TEXTURE_2D, 0, defBitmap, 0).ck()
        api.glBindTexture(api.GL_TEXTURE_2D, 0).ck()
    }

    private fun init(w: Int, h: Int) {
        // Update gl viewport
        api.glViewport(0, 0, w, h).ck()

        // Update resolution
        width = w
        height = h

        // Update matrix
        Matrix.setIdentityM(matrix, 0)
        Matrix.scaleM(matrix, 0, 1F, -1F, 1F)

        // Scale matrix to correctly fit bg image
        val mx = max(w, h).toFloat()
        Matrix.scaleM(matrix, 0, mx / w, mx / h, 1F)
    }

    private fun draw(dt: Float) {
        api.glUseProgram(program).ck()

        // uMatrix
        api.glUniformMatrix4fv(uMatrixLoc, 1, false, matrix, 0)

        // uResolution
        api.glUniform2f(uResolutionLoc, width.toFloat(), height.toFloat())

        // uTexture
        api.glUniform1i(uTextureLoc, 0).ck()
        api.glActiveTexture(api.GL_TEXTURE0 + 0).ck()
        api.glBindTexture(api.GL_TEXTURE_2D, texture).ck()

        // uTime
        api.glUniform1f(uTimeLoc, timePassed).ck()

        // Buffer data
        api.glEnableVertexAttribArray(aPosLoc).ck()
        api.glEnableVertexAttribArray(aTexLoc).ck()
        api.glVertexAttribPointer(aPosLoc, 2, api.GL_FLOAT, false, 4 * 2, posBuff).ck()
        api.glVertexAttribPointer(aTexLoc, 2, api.GL_FLOAT, false, 4 * 2, texBuff).ck()

        // Draw
        api.glDrawElements(api.GL_TRIANGLES, drawOrder.size, api.GL_UNSIGNED_SHORT, drawBuff).ck()

        // Clean up
        api.glDisableVertexAttribArray(aTexLoc).ck()
        api.glDisableVertexAttribArray(aPosLoc).ck()
        api.glBindTexture(api.GL_TEXTURE_2D, 0).ck()
        api.glUseProgram(0).ck()
    }

    // =============================================================================================

    private fun logInfos() {
        api.glGetString(GL10.GL_VERSION).ck().also { Log.w(LOG_TAG, "Version: $it") }
    }

    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
        // Called once to set up the view's OpenGL ES environment.
        // Log.d(LOG_TAG, "Create()")

        logInfos()
        create()
    }

    override fun onDrawFrame(unused: GL10) {
        //  Called for each redraw of the view.
        // Log.d(LOG_TAG, "Draw()")

        val now = System.currentTimeMillis()
        val delta = (now - lastTimeTick)
        lastTimeTick = now

        val dt = (if (delta > 2000) 0F else (delta / 1000F))

        api.glClearColor(0F, 0F, 0F, 1F).ck()
        api.glClear(api.GL_COLOR_BUFFER_BIT).ck()

        loadBitmapIfDirty()
        draw(dt)

        timePassed += dt
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        // Called if the geometry of the view changes,
        // for example when the device's screen orientation changes.
        // Log.d(LOG_TAG, "Init($width, $height)")

        init(width, height)
    }

    // =============================================================================================

    private fun loadBitmapIfDirty() {
        if (_bitmapToLoad == null) return
        // Log.d(LOG_TAG, "Bitmap reloaded")

        api.glActiveTexture(api.GL_TEXTURE0 + 0).ck()
        api.glBindTexture(api.GL_TEXTURE_2D, texture).ck()
        GLUtils.texImage2D(api.GL_TEXTURE_2D, 0, _bitmapToLoad, 0).ck()
        api.glBindTexture(api.GL_TEXTURE_2D, 0).ck()

        _bitmapToLoad!!.recycle()
        _bitmapToLoad = null
    }

    public fun updateImage(bitmap: Bitmap?) {
        _bitmapToLoad = bitmap
    }

}