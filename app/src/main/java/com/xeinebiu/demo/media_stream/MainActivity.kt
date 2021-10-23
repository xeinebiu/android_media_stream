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
                    uri = Uri.parse("https://gist.githubusercontent.com/samdutton/ca37f3adaf4e23679957b8083e061177/raw/e19399fbccbc069a2af4266e5120ae6bad62699a/sample.vtt")
                ),
                VttSubtitle(
                    language = "al",
                    displayLanguage = "Albanian",
                    uri = Uri.parse("https://raw.githubusercontent.com/brenopolanski/html5-video-webvtt-example/master/MIB2-subtitles-pt-BR.vtt")
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