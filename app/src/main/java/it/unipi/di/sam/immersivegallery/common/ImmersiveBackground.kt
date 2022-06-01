package it.unipi.di.sam.immersivegallery.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class ImmersiveBackground(context: Context, attrs: AttributeSet) : View(context, attrs) {

    companion object {
        const val COLOR_SHADER_SRC =
            """
            half4 main(float2 fragCoord) {
                return half4(1, 0, 0, 1);
            }
            """
    }

    private val paint by lazy {
        Paint().also {
            it.shader = RuntimeShader()
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
    }

}