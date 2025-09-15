package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import android.view.Gravity
import android.view.ViewGroup.LayoutParams

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Centered vertical layout for level selector
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
        // Add a button for each level
        for ((idx, level) in Levels.all.withIndex()) {
            val btn = Button(this).apply {
                text = level.name
                textSize = 22f
                setOnClickListener {
                    startGame(idx)
                }
                val params = LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 24, 0, 24)
                layoutParams = params
            }
            layout.addView(btn)
        }
        setContentView(layout)
    }

    private fun startGame(levelIdx: Int) {
        setContentView(GameView(this, Levels.all[levelIdx]))
    }
}
