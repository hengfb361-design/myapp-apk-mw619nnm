package com.example.poolfloatingoverlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private var bubble: View? = null
    private var panel: View? = null
    private var aimCanvasView: AimCanvasView? = null
    private var isAimEnabled = true

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        showAimCanvas()
        showBubble()
    }

    private fun params(width: Int, height: Int, gravity: Int = Gravity.TOP or Gravity.START, touchable: Boolean = true): WindowManager.LayoutParams {
        val flags = if (touchable) {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        } else {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        }

        return WindowManager.LayoutParams(
            width, height,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            this.gravity = gravity
            x = 24
            y = 220
        }
    }

    private fun showAimCanvas() {
        if (aimCanvasView != null) return
        aimCanvasView = AimCanvasView(this)
        val p = params(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, touchable = false)
        p.x = 0
        p.y = 0
        wm.addView(aimCanvasView, p)
    }

    private fun showBubble() {
        if (bubble != null) return
        val button = TextView(this).apply {
            text = "POOL"
            textSize = 13f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(25, 110, 190))
            setPadding(18, 14, 18, 14)
            elevation = 12f
        }
        val lp = params(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT)
        button.setOnTouchListener(object : View.OnTouchListener {
            var downX = 0f
            var downY = 0f
            var startX = 0
            var startY = 0
            var moved = false
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX; downY = event.rawY
                        startX = lp.x; startY = lp.y; moved = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - downX).toInt()
                        val dy = (event.rawY - downY).toInt()
                        if (abs(dx) > 5 || abs(dy) > 5) moved = true
                        lp.x = startX + dx; lp.y = startY + dy
                        wm.updateViewLayout(button, lp)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!moved) togglePanel()
                        return true
                    }
                }
                return false
            }
        })
        bubble = button
        wm.addView(button, lp)
    }

    private fun togglePanel() {
        if (panel != null) {
            wm.removeView(panel)
            panel = null
            return
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 16, 18, 16)
            setBackgroundColor(Color.rgb(28, 32, 42))
            elevation = 16f
        }
        val heading = TextView(this).apply {
            text = "Practice Tools"
            textSize = 18f
            setTextColor(Color.WHITE)
            setPadding(4, 4, 4, 12)
        }
        box.addView(heading)
        addAction(box, if (isAimEnabled) "Aim practice: ON" else "Aim practice: OFF") {
            isAimEnabled = !isAimEnabled
            aimCanvasView?.visibility = if (isAimEnabled) View.VISIBLE else View.GONE
            Toast.makeText(this, "Aim Practice: ${if (isAimEnabled) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
            togglePanel()
        }
        addAction(box, "Power practice: ON/OFF") {
            Toast.makeText(this, "Power setting toggled", Toast.LENGTH_SHORT).show()
        }
        addAction(box, "Close menu") { togglePanel() }
        val p = params(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT)
        p.x = 24; p.y = 290
        panel = box
        wm.addView(box, p)
    }

    private fun addAction(parent: LinearLayout, label: String, action: () -> Unit) {
        val item = TextView(this).apply {
            text = label
            textSize = 15f
            setTextColor(Color.WHITE)
            setPadding(12, 12, 12, 12)
            setOnClickListener { action() }
        }
        parent.addView(item)
    }

    override fun onDestroy() {
        panel?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        bubble?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        aimCanvasView?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        panel = null
        bubble = null
        aimCanvasView = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

class AimCanvasView(context: Context) : View(context) {
    private val linePaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 6f
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    private val targetPaint = Paint().apply {
        color = Color.CYAN
        strokeWidth = 4f
        isAntiAlias = true
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(15f, 10f), 0f)
    }

    private val cuePaint = Paint().apply {
        color = Color.YELLOW
        strokeWidth = 5f
        isAntiAlias = true
        style = Paint.Style.FILL_AND_STROKE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Overlay guideline representation for Pool practice trajectory
        val startX = width * 0.35f
        val startY = height * 0.55f
        val hitX = width * 0.68f
        val hitY = height * 0.55f

        // Draw main cue aim line (Extended line)
        canvas.drawLine(startX, startY, hitX, hitY, linePaint)
        canvas.drawCircle(startX, startY, 14f, cuePaint)

        // Draw object ball target prediction line (Bounce / Pocket line)
        val angle = Math.toRadians(-35.0)
        val extensionLength = 450f
        val endX = (hitX + extensionLength * cos(angle)).toFloat()
        val endY = (hitY + extensionLength * sin(angle)).toFloat()

        canvas.drawCircle(hitX, hitY, 16f, cuePaint)
        canvas.drawLine(hitX, hitY, endX, endY, targetPaint)
    }
}
