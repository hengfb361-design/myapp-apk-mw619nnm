package com.example.poolfloatingoverlay

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.view.Gravity
import android.graphics.Color

class MainActivity : Activity() {
    private val requestOverlay = 701

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 28, 28, 28)
            setBackgroundColor(Color.rgb(20, 25, 35))
        }
        val title = TextView(this).apply {
            text = "Pool Floating Tools"
            textSize = 24f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        val info = TextView(this).apply {
            text = "Floating practice panel for Android. It does not modify 8 Ball Pool or interact with its game data."
            textSize = 15f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 18, 0, 24)
        }
        val start = Button(this).apply {
            text = "Enable Floating Button"
            setOnClickListener {
                if (Settings.canDrawOverlays(this@MainActivity)) {
                    startService(Intent(this@MainActivity, OverlayService::class.java))
                    finish()
                } else {
                    val uri = Uri.parse("package:$packageName")
                    startActivityForResult(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri),
                        requestOverlay
                    )
                }
            }
        }
        val stop = Button(this).apply {
            text = "Stop Floating Button"
            setOnClickListener {
                stopService(Intent(this@MainActivity, OverlayService::class.java))
            }
        }
        root.addView(title)
        root.addView(info)
        root.addView(start)
        root.addView(stop)
        setContentView(root)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestOverlay && Settings.canDrawOverlays(this)) {
            startService(Intent(this, OverlayService::class.java))
            finish()
        }
    }
}
