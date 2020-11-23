package com.xeinebiu.demo.media_stream

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.xeinebiu.media_stream.MediaStreamService

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun startService(view: View) {
        val streamUrl = MediaStreamService.stream(
            this,
            "Demo Video",
            Uri.parse("https://thepaciellogroup.github.io/AT-browser-tests/video/ElephantsDream.mp4"),
            HashMap()
        )
        AlertDialog.Builder(this)
            .setTitle("Stream")
            .setMessage(streamUrl)
            .show()
    }
}