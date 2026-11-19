package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.SyntelAppRoot
import com.example.viewmodel.SyntelAppViewModel

class MainActivity : ComponentActivity() {

    private val appViewModel: SyntelAppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SyntelAppRoot(appViewModel = appViewModel)
        }
    }
}
