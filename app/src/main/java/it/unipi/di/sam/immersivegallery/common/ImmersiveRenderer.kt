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

private fun <T> enclose(function: ((T) -> Unit), value: T, default: T, body: () -> Unit) {
    function(value)
    body()
    function(default)
}

private fun <T> T.ck(): T {
    val error = api.glGetError()
    if (error != api.GL_NO_ERROR) {
        val callerLine = Thread.currentThread().stackTrace[4].lineNumber
        Log.e("GL(err)", "At $callerLine: $error")
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
            """#version 100
            attribute vec2 aPos;
            // attribute vec2 vTex;
            // varying vec2 fTex;
            void main() {
                // fTex = vTex;
                gl_Position = vec4(aPos, 0, 1);
            }
            """

        const val F_SHADER_SRC =
            """#version 100
            precision mediump float;
            // uniform vec4 uColor;
            // varying vec2 fTex;
            void main() {
                // gl_FragColor = uColor;
                gl_FragColor = vec4(0, 1, 0, 1);
            }
            """
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

    private val quadDrawOrder = shortArrayOf(0, 1, 2, 0, 2, 3)
    private val drawBuff: ShortBuffer by lazy {
        ByteBuffer.allocateDirect(quadDrawOrder.size * 2).run {
            order(ByteOrder.nativeOrder())
            asShortBuffer().apply {
                put(quadDrawOrder)
                position(0)
            }
        }
    }

    private var program = 0
    private var aPosLoc = 0

    // =============================================================================================

    private fun create() {
        //<editor-fold desc="VERTEX SHADER">
        val vshader = api.glCreateShader(api.GL_VERTEX_SHADER).ck()
        api.glShaderSource(vshader, V_SHADER_SRC).ck()
        api.glCompileShader(vshader).ck()
        logIfNotEmpty("VS", api.glGetShaderInfoLog(vshader).ck())
        //</editor-fold>

        //<editor-fold desc="FRAGMENT SHADER">
        val fshader = api.glCreateShader(api.GL_FRAGMENT_SHADER).ck()
        api.glShaderSource(fshader, F_SHADER_SRC).ck()
        api.glCompileShader(vshader).ck()
        logIfNotEmpty("FS", api.glGetShaderInfoLog(fshader).ck())
        //</editor-fold>

        //<editor-fold desc="PROGRAM">
        program = api.glCreateProgram().ck()
        api.glAttachShader(program, vshader).ck()
        api.glAttachShader(program, fshader).ck()
        api.glLinkProgram(program).ck()
        logIfNotEmpty("PR", api.glGetProgramInfoLog(program).ck())
        //</editor-fold>

        aPosLoc = api.glGetAttribLocation(program, "aPos").ck()
        Log.d("IR(gles)", "aPosLoc: $aPosLoc")
        // uColorLoc.ck()
    }

    private fun init(w: Int, h: Int) {
        api.glViewport(0, 0, w, h)
    }

    private fun draw() {
        api.glUseProgram(program).ck()

        api.glEnableVertexAttribArray(aPosLoc).ck()
        // api.glEnableVertexAttribArray(1).ck()

        api.glVertexAttribPointer(aPosLoc, 2, api.GL_FLOAT, false, 0, posBuff).ck()
        // api.glVertexAttribPointer(1, 2, api.GL_FLOAT, false, 2 * 4, texBuff).ck()

        // api.glUniform4fv(uColorLoc, 1, U_COLOR, 0).ck()

        api.glDrawElements(api.GL_TRIANGLES, quadDrawOrder.size, api.GL_UNSIGNED_SHORT, drawBuff).ck()

        // api.glDisableVertexAttribArray(1).ck()
        api.glDisableVertexAttribArray(aPosLoc).ck()

        api.glUseProgram(0).ck()
    }

    // =============================================================================================

    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
        // Called once to set up the view's OpenGL ES environment.
        Log.d("IR(gles)", "Create()")

        create()
    }

    override fun onDrawFrame(unused: GL10) {
        //  Called for each redraw of the view.
        Log.d("IR(gles)", "Draw()")

        api.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        api.glClear(api.GL_COLOR_BUFFER_BIT)
        draw()
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        // Called if the geometry of the view changes,
        // for example when the device's screen orientation changes.
        Log.d("IR(gles)", "Init($width, $height)")

        init(width, height)
    }

}