package com.example.myapplication

data class Level(
    val name: String,
    val numBars: Int,
    val gapSize: Float, // fraction of screen width (0.1 = 10% of width)
    val barSpeed: Float // pixels per second
)

object Levels {
    val all = listOf(
        Level("Level 1", 6, 0.35f, 250f),
        Level("Level 2", 7, 0.32f, 280f),
        Level("Level 3", 8, 0.30f, 320f),
        Level("Level 4", 9, 0.28f, 360f),
        Level("Level 5", 10, 0.26f, 400f),
        Level("Level 6", 12, 0.25f, 440f),
        Level("Level 7", 14, 0.24f, 500f),
        Level("Level 8", 16, 0.23f, 560f),
        Level("Level 9", 18, 0.22f, 620f),
        Level("Level 10", 20, 0.21f, 700f)
    )
}
