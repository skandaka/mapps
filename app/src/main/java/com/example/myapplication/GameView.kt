package com.example.myapplication

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.MotionEvent
import android.widget.Toast
import kotlin.math.max
import kotlin.math.min

// A simple 2D game made with SurfaceView and Canvas.
// Screen is split into left and right. Tap left/right to make each circle jump.
// Rectangular obstacles fall from the top on each side. Avoid them as long as possible.
class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    private var gameThread: Thread? = null
    @Volatile private var running = false

    // Drawing helpers
    private val bgPaint = Paint().apply { color = Color.BLACK }
    private val dividerPaint = Paint().apply { color = Color.DKGRAY; strokeWidth = 4f }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 60f
        textAlign = Paint.Align.CENTER
    }
    private val gameOverPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 72f
        textAlign = Paint.Align.CENTER
    }

    // Players
    private lateinit var leftPlayer: Player
    private lateinit var rightPlayer: Player

    // Obstacles
    private val obstacles = mutableListOf<Obstacle>()

    // Spawn timers for each side (seconds)
    private var leftSpawnTimer = 0f
    private var rightSpawnTimer = 0f

    // Spawn every X seconds (will vary slightly)
    private var baseSpawnInterval = 1.3f

    // Difficulty: obstacle speed multiplier increases every 10 seconds
    private var speedMultiplier = 1f

    // Scoring
    private var elapsedThisRun = 0f // seconds for current run
    private var bestScore = 0f

    // Break reminder
    private var totalPlaySeconds = 0f // across runs
    private var breakShown = false

    // Game state
    @Volatile private var gameOver = false

    // Cached sizes
    private var screenW = 0
    private var screenH = 0

    init {
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        // Cache size
        screenW = width
        screenH = height
        resetGame()

        running = true
        gameThread = Thread(this)
        gameThread?.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // Update cached sizes if needed
        screenW = width
        screenH = height
        // Update world bounds for players
        if (::leftPlayer.isInitialized) {
            leftPlayer.minY = 0f
            leftPlayer.maxY = screenH.toFloat()
        }
        if (::rightPlayer.isInitialized) {
            rightPlayer.minY = 0f
            rightPlayer.maxY = screenH.toFloat()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running = false
        try {
            gameThread?.join(1000)
        } catch (_: InterruptedException) { }
        gameThread = null
    }

    override fun run() {
        var lastTime = System.nanoTime()
        val maxDt = 0.05f // cap delta to avoid big jumps (50 ms)

        while (running) {
            val now = System.nanoTime()
            var dt = (now - lastTime) / 1_000_000_000f
            if (dt > maxDt) dt = maxDt
            lastTime = now

            update(dt)
            drawFrame()
        }
    }

    private fun resetGame() {
        obstacles.clear()
        elapsedThisRun = 0f
        speedMultiplier = 1f
        leftSpawnTimer = 0f
        rightSpawnTimer = 0f
        gameOver = false

        val halfW = screenW / 2f
        val radius = max(24f, halfW * 0.08f)
        val startY = screenH * 0.8f

        // Left player at center of left half
        leftPlayer = Player(
            x = halfW * 0.5f,
            y = startY,
            radius = radius,
            color = Color.CYAN
        ).apply {
            minY = 0f
            maxY = screenH.toFloat()
            gravity = 1400f
            jumpVelocity = -750f
        }

        // Right player at center of right half
        rightPlayer = Player(
            x = halfW + halfW * 0.5f,
            y = startY,
            radius = radius,
            color = Color.MAGENTA
        ).apply {
            minY = 0f
            maxY = screenH.toFloat()
            gravity = 1400f
            jumpVelocity = -750f
        }
    }

    private fun update(dt: Float) {
        if (!gameOver) {
            elapsedThisRun += dt
            totalPlaySeconds += dt

            // Show break reminder once after 10 minutes of play
            if (!breakShown && totalPlaySeconds >= 10f * 60f) {
                breakShown = true
                post { Toast.makeText(context, "Take a short break.", Toast.LENGTH_LONG).show() }
            }

            // Increase difficulty slightly every 10 seconds
            val level = (elapsedThisRun / 10f).toInt()
            speedMultiplier = 1f + level * 0.12f // +12% per 10s

            // Update players (gravity etc.)
            leftPlayer.update(dt)
            rightPlayer.update(dt)

            // Update obstacles
            val iterator = obstacles.iterator()
            while (iterator.hasNext()) {
                val ob = iterator.next()
                // Apply global speed multiplier
                ob.y += ob.speedY * speedMultiplier * dt
                if (ob.isOffScreen(screenH)) {
                    iterator.remove()
                }
            }

            // Spawn new obstacles per side
            spawnUpdate(dt)

            // Check collisions
            for (ob in obstacles) {
                if (ob.collidesWith(leftPlayer) || ob.collidesWith(rightPlayer)) {
                    gameOver = true
                    bestScore = max(bestScore, elapsedThisRun)
                    break
                }
            }
        }
    }

    private fun spawnUpdate(dt: Float) {
        // Timers count down; when <= 0, spawn and reset to a random interval near base
        leftSpawnTimer -= dt
        rightSpawnTimer -= dt

        // Obstacle sizes
        val halfW = screenW / 2f
        val obHeight = max(24f, screenH * 0.03f) // thin horizontal bar
        val baseSpeed = max(200f, screenH * 0.25f) // pixels/sec

        if (leftSpawnTimer <= 0f) {
            // Spawn covering the left half width
            val ob = Obstacle(
                x = 0f,
                y = -obHeight,
                width = halfW,
                height = obHeight,
                speedY = baseSpeed,
                color = Color.GREEN
            )
            obstacles.add(ob)
            leftSpawnTimer = randomBetween(baseSpawnInterval * 0.8f, baseSpawnInterval * 1.3f)
        }
        if (rightSpawnTimer <= 0f) {
            // Spawn covering the right half width
            val ob = Obstacle(
                x = halfW,
                y = -obHeight,
                width = halfW,
                height = obHeight,
                speedY = baseSpeed,
                color = Color.YELLOW
            )
            obstacles.add(ob)
            rightSpawnTimer = randomBetween(baseSpawnInterval * 0.8f, baseSpawnInterval * 1.3f)
        }
    }

    private fun randomBetween(a: Float, b: Float): Float {
        val low = min(a, b)
        val high = max(a, b)
        return low + (Math.random().toFloat() * (high - low))
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
        // Background
        canvas.drawRect(0f, 0f, screenW.toFloat(), screenH.toFloat(), bgPaint)

        // Middle divider
        canvas.drawLine(
            screenW / 2f, 0f,
            screenW / 2f, screenH.toFloat(),
            dividerPaint
        )

        // Obstacles
        for (ob in obstacles) {
            ob.draw(canvas)
        }

        // Players
        leftPlayer.draw(canvas)
        rightPlayer.draw(canvas)

        // Score at top center (seconds, no decimals)
        val scoreText = "Score: ${'$'}{elapsedThisRun.toInt()}"
        canvas.drawText(scoreText, screenW / 2f, 80f, textPaint)

        if (gameOver) {
            // Dim overlay (simple text only for clarity)
            val centerY = screenH / 2f
            canvas.drawText("Game Over", screenW / 2f, centerY - 40f, gameOverPaint)
            canvas.drawText("Final: ${'$'}{elapsedThisRun.toInt()}  Best: ${'$'}{bestScore.toInt()}", screenW / 2f, centerY + 40f, textPaint)
            canvas.drawText("Tap RETRY", screenW / 2f, centerY + 120f, textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            if (gameOver) {
                resetGame()
                performClick()
                return true
            }
            val half = screenW / 2f
            if (event.x < half) {
                leftPlayer.jump()
            } else {
                rightPlayer.jump()
            }
            performClick()
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        // For accessibility: announce click handling
        super.performClick()
        return true
    }
}
