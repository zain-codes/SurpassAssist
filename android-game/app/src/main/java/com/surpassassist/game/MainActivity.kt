package com.surpassassist.game

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        updateHighScore()

        findViewById<MaterialButton>(R.id.playButton).setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.howToPlayButton).setOnClickListener {
            AlertDialog.Builder(this, R.style.Theme_MaterialComponents_Dialog_Alert)
                .setTitle(R.string.how_to_play_title)
                .setMessage(R.string.how_to_play_text)
                .setPositiveButton("GOT IT", null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        updateHighScore()
    }

    private fun updateHighScore() {
        val prefs = getSharedPreferences("surpassassist", Context.MODE_PRIVATE)
        val highScore = prefs.getInt("high_score", 0)
        val text = if (highScore > 0) getString(R.string.high_score_label, highScore) else ""
        findViewById<android.widget.TextView>(R.id.highScoreText).text = text
    }
}
