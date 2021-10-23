package com.xeinebiu.demo.media_stream

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.xeinebiu.media_stream.MediaStreamService
import com.xeinebiu.media_stream.model.VttSubtitle

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun startService(view: View) {
        val streamUrl = MediaStreamService.stream(
            context = this,
            title = "Demo Video",
            stream = Uri.parse("https://thepaciellogroup.github.io/AT-browser-tests/video/ElephantsDream.mp4"),
            subtitles = listOf(
                VttSubtitle(
                    language = "en",
                    displayLanguage = "English",
                    content = "WEBVTT\n" +
                            "\n" +
                            "00:00:00.500 --> 00:00:02.000\n" +
                            "The Web is always changing\n" +
                            "\n" +
                            "00:00:02.500 --> 00:00:04.300\n" +
                            "and the way we access it is changing"
                ),
                VttSubtitle(
                    language = "al",
                    displayLanguage = "Albanian",
                    content = "WEBVTT\n" +
                            "\n" +
                            "00:00:00.500 --> 00:00:02.000\n" +
                            "Web-i është gjithmonë në ndryshim\n" +
                            "\n" +
                            "00:00:02.500 --> 00:00:04.300\n" +
                            "dhe mënyra se si i qasemi po ndryshon"
                )
            ),
            headers = HashMap()
        )
        AlertDialog.Builder(this)
            .setTitle("Stream")
            .setMessage(streamUrl)
            .show()
    }
}