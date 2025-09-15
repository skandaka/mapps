# Reaction Split (Kotlin / SurfaceView)

A very small beginner-friendly Android game built only with:
- Kotlin
- `SurfaceView` + `Canvas`
- No external game engines or libraries

## Gameplay
- Screen split into LEFT and RIGHT halves by a vertical line.
- Two player circles (cyan = left, magenta = right).
- Tap left half -> left player jumps. Tap right half -> right player jumps.
- Gravity pulls players downward.
- Rectangular obstacles (one per side) fall from the top.
- If either player collides with an obstacle -> Game Over.
- Score = whole seconds survived (shown at top). Best score is kept during the session.
- Difficulty: every 10 seconds obstacle speed increases slightly (+12% each step).
- After 10 minutes of total play time a Toast appears: "Take a short break." (only once).
- Tap anywhere after Game Over to retry.

## Main Files
| File | Purpose |
|------|---------|
| `MainActivity.kt` | Launches the `GameView`. Very small. |
| `GameView.kt` | Core loop: update, draw, input, spawning, scoring, difficulty, game state. |
| `Player.kt` | Player circle: position, gravity physics, jump. |
| `Obstacle.kt` | Falling rectangle: movement + circle-rectangle collision check. |

## How It Works (Quick Walkthrough)
1. `GameView` starts a thread (`Runnable.run`) that repeatedly:
   - Computes `dt` (delta time in seconds)
   - Calls `update(dt)` then `drawFrame()`
2. `update(dt)` handles:
   - Timing & scoring
   - Difficulty scaling (`speedMultiplier`)
   - Player physics (`Player.update` adds gravity and clamps to screen)
   - Obstacle movement & cleanup
   - Spawning new obstacles when each side's spawn timer reaches 0
   - Collision detection (circle vs rectangle)
3. `drawGame(canvas)` draws background, divider, obstacles, players, score, and game-over text.
4. Touch input (`onTouchEvent`) decides: if game over -> restart; else left/right tap -> jump that player.

## Tuning & Tweaks (Beginner Friendly)
Open `GameView.kt` and look for these values:
- `baseSpawnInterval` (seconds) – lower = more obstacles.
- `gravity` and `jumpVelocity` are assigned to each player in `resetGame()`.
- `speedMultiplier` formula: change the `0.12f` to make difficulty ramp faster/slower.
- `baseSpeed` in `spawnUpdate` – the baseline obstacle falling speed.

Open `Player.kt` for:
- `gravity` and `jumpVelocity` defaults (overridden in `resetGame`).

## Adding a Second Obstacle Type (Optional)
Keep it simple: you can randomly pick a different height or color.
Example idea (inside `spawnUpdate` after creating `ob`):
```kotlin
if (Math.random() < 0.3) {
    ob.height *= 0.5f // a thinner bar 30% of the time
}
```

## Collision Logic (Simple Explanation)
For each obstacle rectangle, we find the closest point on the rectangle to the player's circle center. If the distance from that point to the circle center is less than the radius, they overlap -> collision.

## Break Reminder
`totalPlaySeconds` accumulates across retries; once it reaches 600 seconds (10 minutes) a Toast is shown once.

## Resetting Best Score
Currently `bestScore` lives in memory only. Closing the app resets it. To persist it, you could add `SharedPreferences`.

## Known Intentional Simplifications
- No pause menu.
- No sound.
- No frame time smoothing beyond a basic `maxDt` clamp.
- All rendering done on the game thread for simplicity.

## Running
Open in Android Studio and press Run. It should work on API 24+ (adjust as needed). No extra permissions required.

## Next (Optional) Ideas
- Add small particle effect (simple circles) on jump.
- Persist best score using `SharedPreferences`.
- Add a short 3-2-1 countdown before a new run.
- Make obstacles have gaps (instead of full half-width) for extra challenge.

Enjoy exploring and modifying the code!

