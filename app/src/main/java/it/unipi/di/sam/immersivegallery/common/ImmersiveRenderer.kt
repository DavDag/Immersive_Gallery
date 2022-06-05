package it.unipi.di.sam.immersivegallery.common

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.max
import kotlin.math.min

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
            """#version 100
            attribute vec2 aPos;
            attribute vec2 aTex;
            uniform mat4 uMatrix;
            varying vec2 vTex;
            void main() {
                vTex = aTex;
                gl_Position = uMatrix * vec4(aPos, 0, 1);
            }
            """

        const val F_SHADER_SRC =
            """#version 100
            precision highp float;
            
            #define PI (3.141592)
            #define PI2 (2.0 * PI)
            #define TIME_SPEED 0.75
            #define TILE_COUNT 200.0
            #define VUE_FACTOR 0.5
            #define MOV_RANGE 4.0

            varying vec2 vTex;
            uniform vec2 uResolution;
            uniform sampler2D uTexture1;
            uniform sampler2D uTexture2;
            uniform float uPercentage;
            uniform float uTime;
            
            // https://thebookofshaders.com/10/
            float random(float v) {
                return fract(sin(v * 41.877) * 259.321);
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
            
            float fun(float seed) {
                float x = uTime * TIME_SPEED + seed;
            
                float g = random(floor(x));
                float gn = random(floor(x + 1.0));
            
                float h = fract(x);
                float q = (1.0 - h) * g + h * gn;
                
                q = q / 2.0 + 0.5;
    
                return q;
            }
            
            vec3 proc(vec2 uv, vec2 count, float fx, float fy, sampler2D sampler) {
                uv *= count;
                
                uv.x = mix(floor(uv.x - MOV_RANGE), ceil(uv.x + MOV_RANGE), fx);
                uv.y = mix(floor(uv.y - MOV_RANGE), ceil(uv.y + MOV_RANGE), fy);

                uv /= count;
                
                vec3 col = texture2D(sampler, uv).rgb;
                
                return col;
            }
            
            vec3 all(vec2 uv, vec2 count, vec2 t, float fx, float fy, sampler2D sampler) {
                vec3 col = vec3(0.0);
                
                // weights
                // src: https://datacarpentry.org/image-processing/06-blurring/
    
                // Mid
                col += 0.250 * proc(uv, count, fx, fy, sampler);
                
                // 4 axis
                col += 0.110 * proc(uv + vec2(+t.x, 0.0), count, fx, fy, sampler);
                col += 0.110 * proc(uv + vec2(-t.x, 0.0), count, fx, fy, sampler);
                col += 0.110 * proc(uv + vec2(0.0, +t.y), count, fx, fy, sampler);
                col += 0.110 * proc(uv + vec2(0.0, -t.y), count, fx, fy, sampler);
                
                // 4 corners
                col += 0.050 * proc(uv + vec2(+t.x, +t.y), count, fx, fy, sampler);
                col += 0.050 * proc(uv + vec2(-t.x, +t.y), count, fx, fy, sampler);
                col += 0.050 * proc(uv + vec2(-t.x, -t.y), count, fx, fy, sampler);
                col += 0.050 * proc(uv + vec2(+t.x, -t.y), count, fx, fy, sampler);
                
                // 4 axis (dist 2)
                col += 0.010 * proc(uv + 2.0 * vec2(+t.x, 0.0), count, fx, fy, sampler);
                col += 0.010 * proc(uv + 2.0 * vec2(-t.x, 0.0), count, fx, fy, sampler);
                col += 0.010 * proc(uv + 2.0 * vec2(0.0, +t.y), count, fx, fy, sampler);
                col += 0.010 * proc(uv + 2.0 * vec2(0.0, -t.y), count, fx, fy, sampler);
                
                return col;
            }
            
            void main() {
                vec2 uv = vTex;
                vec2 count = vec2(TILE_COUNT);
                
                float fx = fun(75.43);
                float fy = fun(14.94);
                
                vec2 t = vec2(1.0) / count;
                
                vec3 col = mix(
                    all(uv, count, t, fx, fy, uTexture1),
                    all(uv, count, t, fx, fy, uTexture2),
                    uPercentage
                );

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
    private var swidth = 1
    private var sheight = 1

    private val uTexture1 by lazy { api.glGetUniformLocation(program, "uTexture1").ck() }
    private val uTexture2 by lazy { api.glGetUniformLocation(program, "uTexture2").ck() }
    private var texture1 = 0
    private var texture2 = 0
    private val defBitmap by lazy { Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888) }
    private var placeholderBitmap: Bitmap? = null
    private var bitmap1: Bitmap? = null
    private var bitmap2: Bitmap? = null
    private var isBitmap1Dirty = false
    private var isBitmap2Dirty = false

    private val uPercentageLoc by lazy { api.glGetUniformLocation(program, "uPercentage").ck() }
    private var percentage: Float = 0F

    private val uTimeLoc by lazy { api.glGetUniformLocation(program, "uTime").ck() }
    private var lastTimeTick = 0L
    private var timePassed = 0F

    // =============================================================================================

    public val updateScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

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
        val rawIntBuff = intArrayOf(0, 0)
        api.glGenTextures(2, rawIntBuff, 0).ck()
        texture1 = rawIntBuff[0]
        texture2 = rawIntBuff[1]

        // Update texture parameters
        api.glActiveTexture(api.GL_TEXTURE0 + 0).ck()
        api.glBindTexture(api.GL_TEXTURE_2D, texture1).ck()
        api.glTexParameteri(api.GL_TEXTURE_2D, api.GL_TEXTURE_MIN_FILTER, api.GL_LINEAR).ck()
        api.glTexParameteri(api.GL_TEXTURE_2D, api.GL_TEXTURE_MAG_FILTER, api.GL_LINEAR).ck()
        api.glTexParameteri(api.GL_TEXTURE_2D, api.GL_TEXTURE_WRAP_S, api.GL_CLAMP_TO_EDGE).ck()
        api.glTexParameteri(api.GL_TEXTURE_2D, api.GL_TEXTURE_WRAP_T, api.GL_CLAMP_TO_EDGE).ck()
        GLUtils.texImage2D(api.GL_TEXTURE_2D, 0, defBitmap, 0).ck()
        api.glActiveTexture(api.GL_TEXTURE0 + 1).ck()
        api.glBindTexture(api.GL_TEXTURE_2D, texture2).ck()
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
        swidth = w
        sheight = h

        // Update matrix
        recreateMatrix()
    }

    private fun draw(dt: Float) {
        api.glUseProgram(program).ck()

        // uMatrix
        api.glUniformMatrix4fv(uMatrixLoc, 1, false, matrix, 0)

        // uResolution
        api.glUniform2f(uResolutionLoc, swidth.toFloat(), sheight.toFloat())

        // uTexture1
        api.glUniform1i(uTexture1, 0).ck()
        api.glActiveTexture(api.GL_TEXTURE0 + 0).ck()
        api.glBindTexture(api.GL_TEXTURE_2D, texture1).ck()

        // uTexture2
        api.glUniform1i(uTexture2, 1).ck()
        api.glActiveTexture(api.GL_TEXTURE0 + 1).ck()
        api.glBindTexture(api.GL_TEXTURE_2D, texture2).ck()

        // uPercentage
        api.glUniform1f(uPercentageLoc, percentage).ck()

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
        api.glGetString(api.GL_VERSION).ck().also { Log.w(LOG_TAG, "Version: $it") }
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
        isBitmap1Dirty = true
        isBitmap2Dirty = true
    }

    // =============================================================================================

    private fun recreateMatrix() {
        // Reset
        Matrix.setIdentityM(matrix, 0)

        // Flip Y
        Matrix.scaleM(matrix, 0, 1F, -1F, 1F)

        if (swidth > sheight) {
            // // Scale matrix to correctly fit screen
            // val sratio = swidth.toFloat() / sheight.toFloat()
            // Matrix.scaleM(matrix, 0, 1F, sratio, 1F)

            // // Scale matrix to correctly fit image
            // val tratio = twidth.toFloat() / theight.toFloat()
            // Matrix.scaleM(matrix, 0, 1F, 1F / tratio, 1F)
        } else {
            // // Scale matrix to correctly fit screen
            // val sratio = sheight.toFloat() / swidth.toFloat()
            // Matrix.scaleM(matrix, 0, sratio, 1F, 1F)

            // // Scale matrix to correctly fit image
            // val tratio = theight.toFloat() / twidth.toFloat()
            // Matrix.scaleM(matrix, 0, 1F / tratio, 1F, 1F)
        }
    }

    private fun loadBitmapIfDirty() {
        // Log.d(LOG_TAG, "Bitmap reloaded")

        if (isBitmap1Dirty) {
            isBitmap1Dirty = false

            val bitmap = (bitmap1 ?: placeholderBitmap)?.bestCrop(swidth, sheight) ?: return

            api.glActiveTexture(api.GL_TEXTURE0 + 0).ck()
            api.glBindTexture(api.GL_TEXTURE_2D, texture1).ck()
            GLUtils.texImage2D(api.GL_TEXTURE_2D, 0, bitmap, 0).ck()
            api.glBindTexture(api.GL_TEXTURE_2D, 0).ck()
        }

        if (isBitmap2Dirty) {
            isBitmap2Dirty = false

            val bitmap = bitmap2?.bestCrop(swidth, sheight) ?: return

            api.glActiveTexture(api.GL_TEXTURE0 + 1).ck()
            api.glBindTexture(api.GL_TEXTURE_2D, texture2).ck()
            GLUtils.texImage2D(api.GL_TEXTURE_2D, 0, bitmap, 0).ck()
            api.glBindTexture(api.GL_TEXTURE_2D, 0).ck()
        }

        // Update matrix
        recreateMatrix()
    }

    public fun updateSrcImage(bitmap: Bitmap?, forced: Boolean = false) {
        if (bitmap1 != bitmap || forced) {
            bitmap1 = bitmap
            isBitmap1Dirty = true
        }
    }

    public fun updateDestImage(bitmap: Bitmap?, forced: Boolean = false) {
        if (bitmap2 != bitmap || forced) {
            bitmap2 = bitmap
            isBitmap2Dirty = true
        }
    }

    public fun updatePlaceholderImage(bitmap: Bitmap?, forced: Boolean = false) {
        if (bitmap != placeholderBitmap || forced) {
            placeholderBitmap = bitmap
        }
    }

    public fun updatePercentage(value: Float) {
        percentage = max(min((value / 100F), 1F), 0F)
    }
}