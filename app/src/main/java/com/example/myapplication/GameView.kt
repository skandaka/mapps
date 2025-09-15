package com.example.myapplication

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.MotionEvent
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class GameView(context: Context, private val level: Level) : SurfaceView(context), SurfaceHolder.Callback, Runnable {
    private var gameThread: Thread? = null
    @Volatile private var running = false

    // Drawing helpers
    private val bgPaint = Paint().apply { color = Color.BLACK }
    private val barPaint = Paint().apply { color = Color.GREEN }
    private val gapPaint = Paint().apply { color = Color.BLACK }
    private val playerPaint = Paint().apply { color = Color.CYAN }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 60f
        textAlign = Paint.Align.CENTER
    }
    private val gameOverPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        textSize = 72f
        textAlign = Paint.Align.CENTER
    }
    private val winPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.YELLOW
        textSize = 72f
        textAlign = Paint.Align.CENTER
    }

    // Player
    private var playerX = 0f
    private var playerY = 0f
    private var playerRadius = 0f
    private var playerDir = 1 // 1 = right, -1 = left
    private var playerSpeed = 420f // px/sec

    // Bars
    private data class Bar(var y: Float, val gapX: Float, val gapWidth: Float)
    private val bars = mutableListOf<Bar>()
    private var barHeight = 0f

    // State
    private var screenW = 0
    private var screenH = 0
    private var gameOver = false
    private var win = false
    private var started = false

    init {
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        screenW = width
        screenH = height
        resetGame()
        running = true
        gameThread = Thread(this)
        gameThread?.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        screenW = width
        screenH = height
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running = false
        try { gameThread?.join(1000) } catch (_: InterruptedException) {}
        gameThread = null
    }

    private fun resetGame() {
        // Player starts at bottom center
        playerRadius = max(24f, screenW * 0.06f)
        playerX = screenW / 2f
        playerY = screenH - playerRadius - 20f
        playerDir = 1
        playerSpeed = 420f
        // Bars
        bars.clear()
        barHeight = max(32f, screenH * 0.04f)
        val gapPx = level.gapSize * screenW
        // Stronger minimum vertical spacing for more spread out bars
        val minSpacing = playerRadius * 4.0f // at least 4x player diameter
        val maxBars = ((screenH - 2 * playerRadius - 60f) / minSpacing).toInt().coerceAtLeast(1)
        val actualNumBars = min(level.numBars, maxBars)
        // Calculate initial Y offset as a large buffer above the player
        val initialYOffset = max(screenH * 0.22f, 200f)
        // The first bar is placed at playerY - initialYOffset
        val firstBarY = playerY - initialYOffset
        // The remaining space for bars is from the first bar upward
        val availableSpace = firstBarY - 60f
        val spacing = if (actualNumBars > 1) availableSpace / (actualNumBars - 1) else 0f
        // Higher drift factors for more horizontal spread
        val driftFactor = 1.25f // allow up to 125% of max possible movement
        val minDriftFactor = 0.35f // require at least 35% movement
        var prevGapCenter = Random.nextFloat() * (screenW - gapPx) + gapPx / 2f
        for (i in 0 until actualNumBars) {
            val y = firstBarY - i * spacing
            val timeAvailable = if (spacing > 0f) spacing / level.barSpeed else 1f
            val maxHorizDist = playerSpeed * timeAvailable
            val drift = maxHorizDist * driftFactor
            val minDrift = maxHorizDist * minDriftFactor
            var minCenter = max(gapPx / 2f, prevGapCenter - drift)
            var maxCenter = min(screenW - gapPx / 2f, prevGapCenter + drift)
            var gapCenter: Float
            // Every 2nd or 3rd bar, force max drift in a random direction
            if (i > 0 && (i % 2 == 0 || i % 3 == 0) && maxCenter - minCenter > minDrift * 2) {
                if (Random.nextBoolean()) {
                    gapCenter = minCenter
                } else {
                    gapCenter = maxCenter
                }
            } else if (maxCenter - minCenter > minDrift * 2) {
                // More likely to move away from previous gap
                if (Random.nextBoolean()) {
                    maxCenter = min(maxCenter, prevGapCenter - minDrift)
                } else {
                    minCenter = max(minCenter, prevGapCenter + minDrift)
                }
                gapCenter = if (minCenter < maxCenter) {
                    Random.nextFloat() * (maxCenter - minCenter) + minCenter
                } else {
                    minCenter
                }
            } else {
                gapCenter = minCenter
            }
            val gapX = gapCenter - gapPx / 2f
            bars.add(Bar(y, gapX, gapPx))
            prevGapCenter = gapCenter
        }
        gameOver = false
        win = false
        started = false
    }

    override fun run() {
        var lastTime = System.nanoTime()
        val maxDt = 0.05f
        while (running) {
            val now = System.nanoTime()
            var dt = (now - lastTime) / 1_000_000_000f
            if (dt > maxDt) dt = maxDt
            lastTime = now
            if (started && !gameOver && !win) update(dt)
            drawFrame()
        }
    }

    private fun update(dt: Float) {
        // Move player
        playerX += playerDir * playerSpeed * dt
        if (playerX - playerRadius < 0f) {
            playerX = playerRadius
            playerDir = 1
        } else if (playerX + playerRadius > screenW) {
            playerX = screenW - playerRadius
            playerDir = -1
        }
        // Move bars
        val barSpeed = level.barSpeed
        for (bar in bars) {
            bar.y += barSpeed * dt
        }
        // Remove bars off screen
        bars.removeAll { it.y - barHeight/2 > screenH }
        // Collision
        for (bar in bars) {
            if (playerY + playerRadius > bar.y && playerY - playerRadius < bar.y + barHeight) {
                // Check if player is NOT in the gap
                if (playerX - playerRadius > bar.gapX + bar.gapWidth || playerX + playerRadius < bar.gapX) {
                    gameOver = true
                    break
                }
            }
        }
        // Win condition
        if (bars.isEmpty() && !gameOver) {
            win = true
        }
    }

    private fun drawFrame() {
        val canvas = holder.lockCanvas()
        if (canvas != null) {
            try {
                drawGame(canvas)
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        }
    }

    private fun drawGame(canvas: Canvas) {
        canvas.drawRect(0f, 0f, screenW.toFloat(), screenH.toFloat(), bgPaint)
        // Draw bars
        for (bar in bars) {
            // Draw bar
            canvas.drawRect(0f, bar.y, bar.gapX, bar.y + barHeight, barPaint)
            canvas.drawRect(bar.gapX + bar.gapWidth, bar.y, screenW.toFloat(), bar.y + barHeight, barPaint)
            // Draw gap (for clarity, optional)
            // canvas.drawRect(bar.gapX, bar.y, bar.gapX + bar.gapWidth, bar.y + barHeight, gapPaint)
        }
        // Draw player
        canvas.drawCircle(playerX, playerY, playerRadius, playerPaint)
        // UI
        canvas.drawText(level.name, screenW / 2f, 70f, textPaint)
        if (!started) {
            canvas.drawText("Tap to Start & Change Direction", screenW / 2f, screenH / 2f, textPaint)
        }
        if (gameOver) {
            canvas.drawText("Game Over", screenW / 2f, screenH / 2f, gameOverPaint)
            canvas.drawText("Tap to Retry", screenW / 2f, screenH / 2f + 90f, textPaint)
        }
        if (win) {
            canvas.drawText("Level Complete!", screenW / 2f, screenH / 2f, winPaint)
            canvas.drawText("Tap for Level Select", screenW / 2f, screenH / 2f + 90f, textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            if (!started) {
                started = true
                performClick()
                return true
            }
            if (gameOver) {
                resetGame()
                performClick()
                return true
            }
            if (win) {
                // Go back to level select
                (context as? MainActivity)?.recreate()
                performClick()
                return true
            }
            // Change direction
            playerDir *= -1
            performClick()
            return true
        }
        return false
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
