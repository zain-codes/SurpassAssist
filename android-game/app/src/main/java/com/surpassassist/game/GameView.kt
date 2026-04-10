package com.surpassassist.game

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign
import kotlin.random.Random

class GameView(
    context: Context,
    private val onGameOver: (Int) -> Unit
) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    // ── Thread ──────────────────────────────────────────────────────────
    private var gameThread: Thread? = null
    @Volatile private var isRunning = false

    // ── Screen ──────────────────────────────────────────────────────────
    private var screenW = 0f
    private var screenH = 0f
    private var initialized = false

    // ── Road geometry ───────────────────────────────────────────────────
    private var roadLeft = 0f
    private var roadRight = 0f
    private var roadWidth = 0f
    private var laneWidth = 0f
    private val laneCenters = FloatArray(3)

    // Lane layout:
    //   Lane 0 (left)   = Oncoming traffic lane
    //   Lane 1 (middle) = Overtaking lane
    //   Lane 2 (right)  = Player's default lane, trucks spawn here

    // ── Player ──────────────────────────────────────────────────────────
    private var playerX = 0f
    private var playerTargetX = 0f
    private var playerY = 0f
    private var playerW = 0f
    private var playerH = 0f
    private var currentLane = 2

    // ── Game state ──────────────────────────────────────────────────────
    private var score = 0
    private var lives = 3
    private var gameSpeed = 420f
    private var markingOffset = 0f
    private var gameOver = false
    private var invulnerable = false
    private var invulnerableTimer = 0f
    private var elapsedTime = 0f
    private var overtakeComboCount = 0

    // ── Vehicles ────────────────────────────────────────────────────────
    data class Vehicle(
        var x: Float,
        var y: Float,
        var w: Float,
        var h: Float,
        var lane: Int,
        var overtaken: Boolean = false,
        val isTruck: Boolean,
        val colorIndex: Int = 0
    )

    private val trucks = mutableListOf<Vehicle>()
    private val oncomingCars = mutableListOf<Vehicle>()

    // ── Spawn timers ────────────────────────────────────────────────────
    private var truckSpawnTimer = 0f
    private var truckSpawnInterval = 3.2f
    private var oncomingSpawnTimer = 0f
    private var oncomingSpawnInterval = 2.8f

    // ── Safety analysis (mirrors the real SurpassAssist logic) ──────────
    private var nearestTruckDist = -1f   // in game "meters"
    private var nearestOncomingDist = -1f
    private var safeToOvertake = true
    private var overtakeWarning = ""

    // ── Paints ──────────────────────────────────────────────────────────
    private val grassPaint       = Paint().apply { color = Color.parseColor("#3E7B27") }
    private val roadPaint        = Paint().apply { color = Color.parseColor("#3A3A3A"); style = Paint.Style.FILL }
    private val roadShoulderPaint = Paint().apply { color = Color.parseColor("#5A5A5A"); style = Paint.Style.FILL }
    private val centerLinePaint  = Paint().apply { color = Color.parseColor("#FDD835"); strokeWidth = 6f; style = Paint.Style.FILL }
    private val laneDashPaint    = Paint().apply { color = Color.WHITE; strokeWidth = 3f; style = Paint.Style.FILL }
    private val edgePaint        = Paint().apply { color = Color.WHITE; strokeWidth = 5f }
    private val playerBodyPaint  = Paint().apply { color = Color.parseColor("#1E88E5") }
    private val playerAccentPaint= Paint().apply { color = Color.parseColor("#1565C0") }
    private val windshieldPaint  = Paint().apply { color = Color.parseColor("#81D4FA") }

    private val truckColors = arrayOf(
        Color.parseColor("#5D4037"),
        Color.parseColor("#4E342E"),
        Color.parseColor("#6D4C41"),
        Color.parseColor("#795548")
    )
    private val oncomingColors = arrayOf(
        Color.parseColor("#E53935"),
        Color.parseColor("#D32F2F"),
        Color.parseColor("#C62828"),
        Color.parseColor("#B71C1C"),
        Color.parseColor("#FF7043"),
        Color.parseColor("#8E24AA")
    )

    private val hudBgPaint = Paint().apply { color = Color.argb(160, 0, 0, 0); style = Paint.Style.FILL }
    private val hudTextPaint = Paint().apply {
        color = Color.WHITE; textSize = 40f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true
    }
    private val safeTextPaint = Paint().apply {
        color = Color.parseColor("#4CAF50"); textSize = 44f; typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true; textAlign = Paint.Align.CENTER
    }
    private val unsafeTextPaint = Paint().apply {
        color = Color.parseColor("#F44336"); textSize = 44f; typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true; textAlign = Paint.Align.CENTER
    }
    private val warningTextPaint = Paint().apply {
        color = Color.parseColor("#FFC107"); textSize = 36f; typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true; textAlign = Paint.Align.CENTER
    }
    private val scorePaint = Paint().apply {
        color = Color.WHITE; textSize = 48f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true
    }
    private val comboPaint = Paint().apply {
        color = Color.parseColor("#FFC107"); textSize = 36f; typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true; textAlign = Paint.Align.CENTER
    }
    private val arrowPaint = Paint().apply {
        color = Color.argb(80, 255, 255, 255); textSize = 72f; isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    private val distLabelPaint = Paint().apply {
        color = Color.parseColor("#81D4FA"); textSize = 28f; isAntiAlias = true
        textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
    }

    // ── Init ────────────────────────────────────────────────────────────
    init {
        holder.addCallback(this)
        isFocusable = true
    }

    // ── Surface callbacks ───────────────────────────────────────────────
    override fun surfaceCreated(holder: SurfaceHolder) {
        screenW = width.toFloat()
        screenH = height.toFloat()
        initGeometry()
        initialized = true
        startThread()
    }

    override fun surfaceChanged(holder: SurfaceHolder, fmt: Int, w: Int, h: Int) {
        screenW = w.toFloat()
        screenH = h.toFloat()
        initGeometry()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopThread()
    }

    private fun initGeometry() {
        roadWidth = screenW * 0.78f
        roadLeft = (screenW - roadWidth) / 2f
        roadRight = roadLeft + roadWidth
        laneWidth = roadWidth / 3f

        for (i in 0..2) {
            laneCenters[i] = roadLeft + laneWidth * i + laneWidth / 2f
        }

        playerW = laneWidth * 0.52f
        playerH = playerW * 2.1f
        playerY = screenH * 0.72f
        playerX = laneCenters[currentLane]
        playerTargetX = playerX
    }

    // ── Thread control ──────────────────────────────────────────────────
    private fun startThread() {
        isRunning = true
        gameThread = Thread(this).also { it.start() }
    }

    private fun stopThread() {
        isRunning = false
        try { gameThread?.join(500) } catch (_: Exception) {}
        gameThread = null
    }

    override fun run() {
        var lastTime = System.nanoTime()
        while (isRunning) {
            val now = System.nanoTime()
            val dt = ((now - lastTime) / 1_000_000_000f).coerceAtMost(0.05f)
            lastTime = now

            if (!gameOver) update(dt)
            render()

            val frameMs = (System.nanoTime() - now) / 1_000_000
            if (frameMs < 16) {
                try { Thread.sleep(16 - frameMs) } catch (_: Exception) {}
            }
        }
    }

    // ── Update ──────────────────────────────────────────────────────────
    private fun update(dt: Float) {
        elapsedTime += dt

        // Gradually increase difficulty
        gameSpeed = 420f + elapsedTime * 6f
        truckSpawnInterval = (3.2f - elapsedTime * 0.018f).coerceAtLeast(1.3f)
        oncomingSpawnInterval = (2.8f - elapsedTime * 0.014f).coerceAtLeast(0.9f)

        // Smooth player lane transition
        val moveSpeed = laneWidth * 8f * dt
        val diff = playerTargetX - playerX
        if (abs(diff) > 2f) {
            playerX += diff.sign * min(moveSpeed, abs(diff))
        } else {
            playerX = playerTargetX
        }

        // Road scroll
        markingOffset = (markingOffset + gameSpeed * dt) % 80f

        // Spawn vehicles
        truckSpawnTimer += dt
        if (truckSpawnTimer >= truckSpawnInterval) {
            truckSpawnTimer = 0f
            spawnTruck()
        }
        oncomingSpawnTimer += dt
        if (oncomingSpawnTimer >= oncomingSpawnInterval) {
            oncomingSpawnTimer = 0f
            spawnOncomingCar()
        }

        // Move trucks (player is faster, so trucks drift down slowly)
        val truckDrift = gameSpeed * 0.25f
        val iter = trucks.iterator()
        while (iter.hasNext()) {
            val t = iter.next()
            t.y += truckDrift * dt

            // Check overtake
            if (!t.overtaken && currentLane != t.lane && playerY + playerH / 2 < t.y - t.h / 2) {
                t.overtaken = true
                overtakeComboCount++
                val bonus = 100 * overtakeComboCount
                score += bonus
            }

            if (t.y > screenH + t.h) iter.remove()
        }

        // Move oncoming cars (fast, toward player)
        val oncomingSpeed = gameSpeed * 1.6f
        val oIter = oncomingCars.iterator()
        while (oIter.hasNext()) {
            val c = oIter.next()
            c.y += oncomingSpeed * dt
            if (c.y > screenH + c.h) oIter.remove()
        }

        // Reset combo if no truck nearby to overtake
        if (trucks.none { !it.overtaken && it.y > playerY - screenH * 0.5f && it.y < playerY + screenH * 0.3f }) {
            overtakeComboCount = 0
        }

        // Safety analysis
        updateSafety()

        // Invulnerability
        if (invulnerable) {
            invulnerableTimer -= dt
            if (invulnerableTimer <= 0f) invulnerable = false
        }

        // Collisions
        if (!invulnerable) checkCollisions()
    }

    private fun spawnTruck() {
        // Trucks mostly in the right lane, sometimes middle
        val lane = if (Random.nextFloat() < 0.75f) 2 else 1
        // Avoid stacking: don't spawn if a truck is already near the top in the same lane
        if (trucks.any { it.lane == lane && it.y < screenH * 0.15f }) return

        val tw = laneWidth * 0.62f
        val th = tw * 2.8f
        trucks.add(Vehicle(
            x = laneCenters[lane], y = -th,
            w = tw, h = th, lane = lane, isTruck = true,
            colorIndex = Random.nextInt(truckColors.size)
        ))
    }

    private fun spawnOncomingCar() {
        // Only lane 0
        if (oncomingCars.any { it.y < -screenH * 0.1f }) return
        val cw = laneWidth * 0.48f
        val ch = cw * 2.1f
        oncomingCars.add(Vehicle(
            x = laneCenters[0], y = -ch * 1.5f,
            w = cw, h = ch, lane = 0, isTruck = false,
            colorIndex = Random.nextInt(oncomingColors.size)
        ))
    }

    private fun updateSafety() {
        // Nearest truck ahead
        nearestTruckDist = -1f
        trucks.filter { !it.overtaken }.forEach { t ->
            val dist = playerY - t.y
            if (dist > 0 && (nearestTruckDist < 0 || dist < nearestTruckDist)) {
                nearestTruckDist = dist / 8f  // scale to "meters"
            }
        }

        // Nearest oncoming vehicle
        nearestOncomingDist = -1f
        oncomingCars.forEach { c ->
            val dist = playerY - c.y
            if (dist > 0 && (nearestOncomingDist < 0 || dist < nearestOncomingDist)) {
                nearestOncomingDist = dist / 8f
            }
        }

        // Safety decision (mirrors real SurpassAssist logic)
        val dangerZoneMeters = 70f
        val warningZoneMeters = 120f
        val hasOncomingNear = nearestOncomingDist in 0f..dangerZoneMeters
        val hasOncomingApproaching = nearestOncomingDist in 0f..warningZoneMeters

        safeToOvertake = !hasOncomingNear
        overtakeWarning = when {
            hasOncomingNear -> "ONCOMING VEHICLE — DO NOT OVERTAKE"
            hasOncomingApproaching -> "CAUTION — Vehicle approaching"
            nearestTruckDist in 0f..20f -> "TOO CLOSE — Maintain distance"
            else -> ""
        }
    }

    private fun checkCollisions() {
        val pRect = RectF(
            playerX - playerW / 2, playerY - playerH / 2,
            playerX + playerW / 2, playerY + playerH / 2
        )
        for (v in trucks + oncomingCars) {
            val vRect = RectF(
                v.x - v.w / 2, v.y - v.h / 2,
                v.x + v.w / 2, v.y + v.h / 2
            )
            if (RectF.intersects(pRect, vRect)) {
                lives--
                invulnerable = true
                invulnerableTimer = 2.0f
                overtakeComboCount = 0
                if (lives <= 0) {
                    gameOver = true
                    (context as? Activity)?.runOnUiThread { onGameOver(score) }
                }
                return
            }
        }
    }

    // ── Touch input ─────────────────────────────────────────────────────
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN && !gameOver) {
            if (event.x < screenW / 2f) {
                currentLane = (currentLane - 1).coerceAtLeast(0)
            } else {
                currentLane = (currentLane + 1).coerceAtMost(2)
            }
            playerTargetX = laneCenters[currentLane]
            performClick()
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    // ── Render ──────────────────────────────────────────────────────────
    private fun render() {
        val canvas: Canvas
        try {
            canvas = holder.lockCanvas() ?: return
        } catch (_: Exception) {
            return
        }
        try {
            drawScene(canvas)
        } finally {
            try { holder.unlockCanvasAndPost(canvas) } catch (_: Exception) {}
        }
    }

    private fun drawScene(c: Canvas) {
        // Sky/grass
        c.drawColor(Color.parseColor("#2E7D32"))
        // Lighter grass strips
        val stripPaint = Paint().apply { color = Color.parseColor("#388E3C") }
        var gy = (-80f + markingOffset * 0.4f) % 60f - 60f
        while (gy < screenH) {
            c.drawRect(0f, gy, roadLeft, gy + 30f, stripPaint)
            c.drawRect(roadRight, gy, screenW, gy + 30f, stripPaint)
            gy += 60f
        }

        // Road shoulder
        c.drawRect(roadLeft - 12f, 0f, roadLeft, screenH, roadShoulderPaint)
        c.drawRect(roadRight, 0f, roadRight + 12f, screenH, roadShoulderPaint)

        // Road surface
        c.drawRect(roadLeft, 0f, roadRight, screenH, roadPaint)

        // Center line (double yellow between lane 0 and lane 1)
        val clX = roadLeft + laneWidth
        var my = -80f + markingOffset
        while (my < screenH) {
            c.drawRect(clX - 5f, my, clX - 1f, my + 40f, centerLinePaint)
            c.drawRect(clX + 1f, my, clX + 5f, my + 40f, centerLinePaint)
            my += 80f
        }

        // Lane dash (white between lane 1 and lane 2)
        val ldX = roadLeft + laneWidth * 2f
        var dy = -80f + markingOffset
        while (dy < screenH) {
            c.drawRect(ldX - 2f, dy, ldX + 2f, dy + 35f, laneDashPaint)
            dy += 80f
        }

        // Road edge lines
        c.drawLine(roadLeft, 0f, roadLeft, screenH, edgePaint)
        c.drawLine(roadRight, 0f, roadRight, screenH, edgePaint)

        // Direction arrows on lanes
        drawLaneArrows(c)

        // Vehicles
        trucks.forEach { drawTruck(c, it) }
        oncomingCars.forEach { drawOncomingCar(c, it) }

        // Distance labels to vehicles
        drawDistanceLabels(c)

        // Player car (blink when invulnerable)
        if (!invulnerable || (System.currentTimeMillis() % 200 < 120)) {
            drawPlayerCar(c)
        }

        // Touch zone hints
        c.drawText("\u25C0", screenW * 0.06f, screenH * 0.52f, arrowPaint)
        c.drawText("\u25B6", screenW * 0.94f, screenH * 0.52f, arrowPaint)

        // HUD
        drawHUD(c)
    }

    private fun drawLaneArrows(c: Canvas) {
        val arrowP = Paint().apply {
            color = Color.argb(30, 255, 255, 255); isAntiAlias = true
        }
        // Arrows in lane 1 & 2 pointing up (player direction), lane 0 pointing down
        for (lane in 0..2) {
            val cx = laneCenters[lane]
            var ay = -80f + markingOffset * 0.5f
            while (ay < screenH) {
                val path = Path()
                if (lane == 0) {
                    // Down arrow (oncoming)
                    path.moveTo(cx, ay + 30f)
                    path.lineTo(cx - 12f, ay + 10f)
                    path.lineTo(cx + 12f, ay + 10f)
                    path.close()
                } else {
                    // Up arrow
                    path.moveTo(cx, ay)
                    path.lineTo(cx - 12f, ay + 20f)
                    path.lineTo(cx + 12f, ay + 20f)
                    path.close()
                }
                c.drawPath(path, arrowP)
                ay += 160f
            }
        }
    }

    private fun drawPlayerCar(c: Canvas) {
        val l = playerX - playerW / 2f
        val t = playerY - playerH / 2f
        val r = playerX + playerW / 2f
        val b = playerY + playerH / 2f

        // Shadow
        val shadowP = Paint().apply { color = Color.argb(60, 0, 0, 0) }
        c.drawRoundRect(RectF(l + 4f, t + 6f, r + 4f, b + 6f), 10f, 10f, shadowP)

        // Body
        c.drawRoundRect(RectF(l, t, r, b), 10f, 10f, playerBodyPaint)

        // Roof / darker center
        val roofInset = playerW * 0.12f
        c.drawRoundRect(
            RectF(l + roofInset, t + playerH * 0.15f, r - roofInset, b - playerH * 0.15f),
            6f, 6f, playerAccentPaint
        )

        // Windshield (front = top since car faces up)
        c.drawRoundRect(
            RectF(l + roofInset + 2f, t + 8f, r - roofInset - 2f, t + playerH * 0.28f),
            4f, 4f, windshieldPaint
        )

        // Rear window
        c.drawRoundRect(
            RectF(l + roofInset + 4f, b - playerH * 0.22f, r - roofInset - 4f, b - 8f),
            4f, 4f, windshieldPaint
        )

        // Headlights
        val hlP = Paint().apply { color = Color.parseColor("#FFF9C4") }
        c.drawOval(RectF(l + 4f, t + 2f, l + playerW * 0.25f, t + 10f), hlP)
        c.drawOval(RectF(r - playerW * 0.25f, t + 2f, r - 4f, t + 10f), hlP)

        // Taillights
        val tlP = Paint().apply { color = Color.parseColor("#F44336") }
        c.drawOval(RectF(l + 4f, b - 10f, l + playerW * 0.2f, b - 2f), tlP)
        c.drawOval(RectF(r - playerW * 0.2f, b - 10f, r - 4f, b - 2f), tlP)
    }

    private fun drawTruck(c: Canvas, v: Vehicle) {
        val l = v.x - v.w / 2f
        val t = v.y - v.h / 2f
        val r = v.x + v.w / 2f
        val b = v.y + v.h / 2f
        val bodyP = Paint().apply { color = truckColors[v.colorIndex]; isAntiAlias = true }

        // Shadow
        val shadowP = Paint().apply { color = Color.argb(50, 0, 0, 0) }
        c.drawRoundRect(RectF(l + 4f, t + 6f, r + 4f, b + 6f), 8f, 8f, shadowP)

        // Trailer (top portion)
        c.drawRoundRect(RectF(l, t, r, b - v.h * 0.2f), 6f, 6f, bodyP)

        // Cab (bottom)
        val cabP = Paint().apply { color = Color.parseColor("#37474F") }
        c.drawRoundRect(RectF(l + 2f, b - v.h * 0.28f, r - 2f, b), 6f, 6f, cabP)

        // Cab windshield
        c.drawRoundRect(
            RectF(l + v.w * 0.15f, b - v.h * 0.22f, r - v.w * 0.15f, b - v.h * 0.08f),
            4f, 4f, windshieldPaint
        )

        // Truck rear lights (top since it faces away from us)
        val rlP = Paint().apply { color = Color.parseColor("#FF7043") }
        c.drawRect(l + 3f, t + 2f, l + v.w * 0.2f, t + 8f, rlP)
        c.drawRect(r - v.w * 0.2f, t + 2f, r - 3f, t + 8f, rlP)
    }

    private fun drawOncomingCar(c: Canvas, v: Vehicle) {
        val l = v.x - v.w / 2f
        val t = v.y - v.h / 2f
        val r = v.x + v.w / 2f
        val b = v.y + v.h / 2f
        val bodyP = Paint().apply { color = oncomingColors[v.colorIndex]; isAntiAlias = true }

        // Shadow
        val shadowP = Paint().apply { color = Color.argb(50, 0, 0, 0) }
        c.drawRoundRect(RectF(l + 3f, t + 5f, r + 3f, b + 5f), 8f, 8f, shadowP)

        // Body
        c.drawRoundRect(RectF(l, t, r, b), 8f, 8f, bodyP)

        // Roof
        val inset = v.w * 0.12f
        val roofP = Paint().apply { color = Color.argb(40, 0, 0, 0) }
        c.drawRoundRect(
            RectF(l + inset, t + v.h * 0.2f, r - inset, b - v.h * 0.2f),
            4f, 4f, roofP
        )

        // Windshield (facing us = at bottom since car comes toward us)
        c.drawRoundRect(
            RectF(l + inset + 2f, b - v.h * 0.3f, r - inset - 2f, b - 8f),
            4f, 4f, windshieldPaint
        )

        // Headlights (at bottom, facing us)
        val hlP = Paint().apply { color = Color.parseColor("#FFF9C4") }
        c.drawOval(RectF(l + 4f, b - 8f, l + v.w * 0.25f, b - 1f), hlP)
        c.drawOval(RectF(r - v.w * 0.25f, b - 8f, r - 4f, b - 1f), hlP)
    }

    private fun drawDistanceLabels(c: Canvas) {
        // Show distance above nearby trucks
        trucks.filter { !it.overtaken && it.y > 0 && it.y < playerY }.forEach { t ->
            val distMeters = ((playerY - t.y) / 8f).toInt()
            c.drawText("${distMeters}m", t.x, t.y + t.h / 2 + 36f, distLabelPaint)
        }
    }

    private fun drawHUD(c: Canvas) {
        // Top HUD bar
        c.drawRect(0f, 0f, screenW, 155f, hudBgPaint)

        // Safety indicator (centered, top)
        val indicatorY = 42f
        if (safeToOvertake) {
            safeTextPaint.textSize = screenW * 0.038f
            c.drawText("\u2713 SAFE TO OVERTAKE", screenW / 2f, indicatorY, safeTextPaint)
        } else {
            unsafeTextPaint.textSize = screenW * 0.038f
            c.drawText("\u2717 DO NOT OVERTAKE", screenW / 2f, indicatorY, unsafeTextPaint)
        }

        // Warning text
        if (overtakeWarning.isNotEmpty()) {
            warningTextPaint.textSize = screenW * 0.028f
            c.drawText(overtakeWarning, screenW / 2f, indicatorY + 34f, warningTextPaint)
        }

        // Score (left)
        scorePaint.textSize = screenW * 0.04f
        c.drawText("$score", 20f, 130f, scorePaint)

        // Combo
        if (overtakeComboCount > 1) {
            comboPaint.textSize = screenW * 0.032f
            c.drawText("${overtakeComboCount}x COMBO!", screenW / 2f, 130f, comboPaint)
        }

        // Lives (right) as hearts
        val heartStr = "\u2764".repeat(lives)
        val heartPaint = Paint().apply {
            color = Color.parseColor("#F44336"); textSize = screenW * 0.04f
            isAntiAlias = true; textAlign = Paint.Align.RIGHT
        }
        c.drawText(heartStr, screenW - 20f, 130f, heartPaint)

        // Speed (bottom-left)
        val speedKmh = (gameSpeed / 8f).toInt()
        val speedPaint = Paint().apply {
            color = Color.WHITE; textSize = screenW * 0.032f; isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }
        val speedBgRect = RectF(8f, screenH - 60f, 200f, screenH - 8f)
        c.drawRoundRect(speedBgRect, 8f, 8f, hudBgPaint)
        c.drawText("$speedKmh km/h", 16f, screenH - 24f, speedPaint)

        // Distance info (bottom-right)
        if (nearestTruckDist > 0) {
            val dText = "Truck: ${nearestTruckDist.toInt()}m"
            val tw = speedPaint.measureText(dText) + 24f
            val dBgRect = RectF(screenW - tw - 8f, screenH - 60f, screenW - 8f, screenH - 8f)
            c.drawRoundRect(dBgRect, 8f, 8f, hudBgPaint)
            speedPaint.textAlign = Paint.Align.RIGHT
            c.drawText(dText, screenW - 16f, screenH - 24f, speedPaint)
            speedPaint.textAlign = Paint.Align.LEFT
        }

        // Lane indicator (bottom center)
        val laneNames = arrayOf("ONCOMING", "OVERTAKE", "DRIVING")
        val laneLabel = laneNames[currentLane]
        val laneP = Paint().apply {
            color = when (currentLane) {
                0 -> Color.parseColor("#F44336")
                1 -> Color.parseColor("#FFC107")
                else -> Color.parseColor("#4CAF50")
            }
            textSize = screenW * 0.03f; isAntiAlias = true; textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        val labelBg = RectF(screenW / 2f - 80f, screenH - 60f, screenW / 2f + 80f, screenH - 8f)
        c.drawRoundRect(labelBg, 8f, 8f, hudBgPaint)
        c.drawText(laneLabel, screenW / 2f, screenH - 24f, laneP)
    }

    // ── Lifecycle ───────────────────────────────────────────────────────
    fun pause() {
        stopThread()
    }

    fun resume() {
        if (initialized && !gameOver && !isRunning) {
            startThread()
        }
    }
}
