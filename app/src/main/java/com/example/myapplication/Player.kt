package com.example.myapplication

import android.graphics.Canvas
import android.graphics.Paint

// Simple player represented by a colored circle that can jump and is affected by gravity.
class Player(
    var x: Float,
    var y: Float,
    val radius: Float,
    color: Int
) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }

    // Physics
    private var velocityY = 0f
    var gravity = 1200f // pixels per second^2
    var jumpVelocity = -600f // pixels per second (negative is up)

    // World bounds (set by GameView)
    var minY = 0f
    var maxY = 0f

    fun update(dt: Float) {
        // Apply gravity
        velocityY += gravity * dt
        y += velocityY * dt

        // Clamp to world bounds and reset velocity if on the ground or ceiling
        if (y > maxY - radius) {
            y = maxY - radius
            velocityY = 0f
        }
        if (y < minY + radius) {
            y = minY + radius
            velocityY = 0f
        }
    }

    fun jump() {
        // Give an instant upward velocity
        velocityY = jumpVelocity
    }

    fun draw(canvas: Canvas) {
        canvas.drawCircle(x, y, radius, paint)
    }
}

