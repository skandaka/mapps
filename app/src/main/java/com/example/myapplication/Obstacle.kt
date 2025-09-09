package com.example.myapplication

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min

// A simple rectangular obstacle that moves downward from the top of the screen.
class Obstacle(
    var x: Float,           // left position
    var y: Float,           // top position
    var width: Float,
    var height: Float,
    var speedY: Float,      // pixels per second, downward
    color: Int
) {
    private val rect = RectF()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }

    fun update(dt: Float) {
        y += speedY * dt
    }

    fun draw(canvas: Canvas) {
        rect.set(x, y, x + width, y + height)
        canvas.drawRect(rect, paint)
    }

    fun isOffScreen(screenHeight: Int): Boolean {
        return y > screenHeight
    }

    // Circle-rectangle collision: check if the player's circle intersects this rectangle
    fun collidesWith(player: Player): Boolean {
        rect.set(x, y, x + width, y + height)

        val closestX = clamp(player.x, rect.left, rect.right)
        val closestY = clamp(player.y, rect.top, rect.bottom)

        val dx = player.x - closestX
        val dy = player.y - closestY
        val distanceSq = dx * dx + dy * dy

        return distanceSq <= player.radius * player.radius
    }

    private fun clamp(value: Float, minValue: Float, maxValue: Float): Float {
        return max(minValue, min(value, maxValue))
    }
}
