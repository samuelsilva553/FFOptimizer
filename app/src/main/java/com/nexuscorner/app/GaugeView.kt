package com.nexuscorner.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class GaugeView(c: Context, a: AttributeSet?) : View(c, a) {
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; color = 0xFF2A2A3A.toInt() }
    private val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val big = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF4F4F8.toInt(); textAlign = Paint.Align.CENTER; typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL) }
    private val small = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF8B8B9E.toInt(); textAlign = Paint.Align.CENTER }
    private val box = RectF()
    private var shown = 0f

    fun setValue(v: Int) {
        ValueAnimator.ofFloat(shown, v.coerceIn(0, 100).toFloat()).apply {
            duration = 900
            addUpdateListener { shown = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        val s = w * 0.11f
        track.strokeWidth = s; arc.strokeWidth = s
        box.set(s, s, w - s, h - s)
        arc.shader = SweepGradient(w / 2f, h / 2f, intArrayOf(0xFF7C5CFF.toInt(), 0xFF3DDCFF.toInt(), 0xFF7C5CFF.toInt()), null)
        big.textSize = w * 0.25f; small.textSize = w * 0.1f
    }

    override fun onDraw(c: Canvas) {
        c.drawArc(box, 0f, 360f, false, track)
        c.save(); c.rotate(-90f, width / 2f, height / 2f)
        c.drawArc(box, 0f, 360f * shown / 100f, false, arc)
        c.restore()
        c.drawText("${shown.toInt()}%", width / 2f, height / 2f + big.textSize * 0.2f, big)
        c.drawText("em uso", width / 2f, height / 2f + big.textSize * 0.2f + small.textSize * 1.5f, small)
    }
}
