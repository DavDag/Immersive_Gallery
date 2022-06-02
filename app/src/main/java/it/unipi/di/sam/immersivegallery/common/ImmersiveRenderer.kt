package it.unipi.di.sam.immersivegallery.common

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

// =================================================================================================

private typealias api = GLES20

private fun <T> T.ck(): T {
    val error = api.glGetError()
    if (error != api.GL_NO_ERROR) {
        val callerLine = Thread.currentThread().stackTrace[4].lineNumber
        val message = GLUtils.getEGLErrorString(error)
        Log.e("GL(err)", "At line $callerLine with error $error => $message")
        // throw Exception("GL_ERROR")
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
        val U_COLOR = floatArrayOf(1F, 0F, 0F, 1F)

        const val V_SHADER_SRC =
            """
            attribute vec4 aPos;
            attribute vec2 aTex;
            varying vec2 vTex;
            void main() {
                vTex = vec2(aTex.x, 1.0 - aTex.y);
                gl_Position = aPos;
            }
            """

        const val F_SHADER_SRC =
            """
            precision mediump float;
            varying vec2 vTex;
            uniform vec2 uResolution;
            uniform sampler2D uTexture;
            uniform float uTime;
            void main() {
                vec2 res = uResolution;
                vec2 tex = vTex;
                tex.x += uTime / 10.0;
                vec3 color = texture2D(uTexture, tex).rgb;
                gl_FragColor = vec4(color, 1);
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
    private val aPosLoc by lazy { api.glGetAttribLocation(program, "aPos") }
    private val aTexLoc by lazy { api.glGetAttribLocation(program, "aTex") }

    private val uResolutionLoc by lazy { api.glGetUniformLocation(program, "uResolution") }
    private var width = 0
    private var height = 0

    private val uTextureLoc by lazy { api.glGetUniformLocation(program, "uTexture") }
    private var texture = 0
    private val defBitmap by lazy { Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888) }
    private var _bitmapToLoad: Bitmap? = null

    private val uTimeLoc by lazy { api.glGetUniformLocation(program, "uTime") }
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
    }

    private fun draw(dt: Float) {
        api.glUseProgram(program).ck()

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