package it.unipi.di.sam.immersivegallery.common

import android.opengl.GLES20
import android.opengl.GLSurfaceView
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
        Log.e("GL(err)", "At $callerLine: $error")
        return this
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
                vTex = aTex;
                gl_Position = aPos;
            }
            """

        const val F_SHADER_SRC =
            """
            precision mediump float;
            varying vec2 vTex;
            void main() {
                gl_FragColor = vec4(vTex.xy, 0, 1);
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

    private var program = 0
    private val aPosLoc by lazy { api.glGetAttribLocation(program, "aPos") }
    private val aTexLoc by lazy { api.glGetAttribLocation(program, "aTex") }

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
        program = api.glCreateProgram().ck()
        api.glAttachShader(program, vshader).ck()
        api.glAttachShader(program, fshader).ck()
        api.glLinkProgram(program).ck()
        logIfNotEmpty("PR", api.glGetProgramInfoLog(program).ck())
    }

    private fun init(w: Int, h: Int) {
        // Update gl viewport
        api.glViewport(0, 0, w, h).ck()
    }

    private fun draw() {
        api.glUseProgram(program).ck()
        api.glEnableVertexAttribArray(aPosLoc).ck()
        api.glEnableVertexAttribArray(aTexLoc).ck()
        api.glVertexAttribPointer(aPosLoc, 2, api.GL_FLOAT, false, 4 * 2, posBuff).ck()
        api.glVertexAttribPointer(aTexLoc, 2, api.GL_FLOAT, false, 4 * 2, texBuff).ck()
        // api.glUniform4fv(uColorLoc, 1, U_COLOR, 0)
        api.glDrawElements(api.GL_TRIANGLES, drawOrder.size, api.GL_UNSIGNED_SHORT, drawBuff).ck()
        api.glDisableVertexAttribArray(aTexLoc).ck()
        api.glDisableVertexAttribArray(aPosLoc).ck()
        api.glUseProgram(0).ck()
    }

    // =============================================================================================

    private fun logInfos() {
        api.glGetString(GL10.GL_VERSION).ck().also { Log.w(LOG_TAG, "Version: $it") }
    }

    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
        // Called once to set up the view's OpenGL ES environment.
        Log.d(LOG_TAG, "Create()")

        logInfos()
        create()
    }

    override fun onDrawFrame(unused: GL10) {
        //  Called for each redraw of the view.
        Log.d(LOG_TAG, "Draw()")

        api.glClearColor(0F, 0F, 0F, 1F).ck()
        api.glClear(api.GL_COLOR_BUFFER_BIT).ck()

        draw()
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        // Called if the geometry of the view changes,
        // for example when the device's screen orientation changes.
        Log.d(LOG_TAG, "Init($width, $height)")

        init(width, height)
    }

}