package com.rajatnagpure.pcmplayerconverter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.rajatnagpure.pcmplayerconverter.ui.NeedHelpScreen

class NeedHelp : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NeedHelpScreen(onBackClick = { finish() })
        }
    }
}