package com.surpassassist.game

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class GameActivity : AppCompatActivity() {

    private var gameView: GameView? = null
    private var gameOverOverlay: FrameLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        val container = findViewById<FrameLayout>(R.id.gameContainer)

        gameView = GameView(this) { score ->
            showGameOver(score)
        }
        container.addView(gameView)
    }

    private fun showGameOver(score: Int) {
        val prefs = getSharedPreferences("surpassassist", Context.MODE_PRIVATE)
        val highScore = prefs.getInt("high_score", 0)
        val isNewHigh = score > highScore
        if (isNewHigh) {
            prefs.edit().putInt("high_score", score).apply()
        }

        runOnUiThread {
            val container = findViewById<FrameLayout>(R.id.gameContainer)

            gameOverOverlay = FrameLayout(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(0xCC000000.toInt())
                isClickable = true
            }

            val content = LinearLayout(this@GameActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
                setPadding(64, 64, 64, 64)
            }

            val titleText = TextView(this@GameActivity).apply {
                text = getString(R.string.game_over_title)
                setTextColor(resources.getColor(R.color.danger_red, theme))
                textSize = 40f
                gravity = Gravity.CENTER
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
            content.addView(titleText)

            val scoreText = TextView(this@GameActivity).apply {
                text = getString(R.string.final_score, score)
                setTextColor(resources.getColor(R.color.text_primary, theme))
                textSize = 28f
                gravity = Gravity.CENTER
                setPadding(0, 24, 0, 0)
            }
            content.addView(scoreText)

            if (isNewHigh) {
                val highScoreText = TextView(this@GameActivity).apply {
                    text = getString(R.string.new_high_score)
                    setTextColor(resources.getColor(R.color.warning_amber, theme))
                    textSize = 22f
                    gravity = Gravity.CENTER
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setPadding(0, 16, 0, 0)
                }
                content.addView(highScoreText)
            }

            val playAgainBtn = MaterialButton(this@GameActivity).apply {
                text = getString(R.string.play_again)
                textSize = 18f
                setBackgroundColor(resources.getColor(R.color.safe_green, theme))
                setTextColor(resources.getColor(R.color.text_primary, theme))
                val params = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                params.topMargin = 48
                layoutParams = params
                setOnClickListener { restartGame() }
            }
            content.addView(playAgainBtn)

            val menuBtn = MaterialButton(this@GameActivity).apply {
                text = getString(R.string.main_menu)
                textSize = 16f
                setBackgroundColor(resources.getColor(R.color.primary, theme))
                setTextColor(resources.getColor(R.color.text_primary, theme))
                val params = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                params.topMargin = 16
                layoutParams = params
                setOnClickListener { finish() }
            }
            content.addView(menuBtn)

            gameOverOverlay?.addView(content)
            container.addView(gameOverOverlay)
        }
    }

    private fun restartGame() {
        val container = findViewById<FrameLayout>(R.id.gameContainer)
        gameOverOverlay?.let { container.removeView(it) }
        gameOverOverlay = null

        gameView?.let { container.removeView(it) }
        gameView = GameView(this) { score ->
            showGameOver(score)
        }
        container.addView(gameView)
    }

    override fun onPause() {
        super.onPause()
        gameView?.pause()
    }

    override fun onResume() {
        super.onResume()
        gameView?.resume()
    }
}
