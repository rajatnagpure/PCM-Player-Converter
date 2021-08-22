package com.rajatnagpure.pcmplayerconverter

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView

class NeedHelp : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_need_help)
        val cross = findViewById<ImageView>(R.id.cross)
        cross.setOnClickListener{
            finish()
        }
    }
}