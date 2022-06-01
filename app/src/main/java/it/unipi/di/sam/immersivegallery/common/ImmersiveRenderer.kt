package it.unipi.di.sam.immersivegallery.common

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

private typealias api = GLES20

/**
 * https://developer.android.com/training/graphics/opengl/environment#renderer
 */
class ImmersiveRenderer : GLSurfaceView.Renderer {

    // Called once to set up the view's OpenGL ES environment.
    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
        api.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
    }

    //  Called for each redraw of the view.
    override fun onDrawFrame(unused: GL10) {
        api.glClear(api.GL_COLOR_BUFFER_BIT)
    }

    // Called if the geometry of the view changes,
    // for example when the device's screen orientation changes.
    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        api.glViewport(0, 0, width, height)
    }

}