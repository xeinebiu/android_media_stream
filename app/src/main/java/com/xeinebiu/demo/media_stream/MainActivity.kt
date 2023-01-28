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
            stream = Uri.parse("https://rr1---sn-n0ogpnx-1gie.googlevideo.com/videoplayback?expire=1674960937&ei=yYvVY_n-FceCvdIPpKeC-AQ&ip=194.230.161.221&id=o-ADL_ub-mfYmswF60jCcfKMCg56VIJfIrse22tiLG4UGt&itag=22&source=youtube&requiressl=yes&mh=7b&mm=31%2C29&mn=sn-n0ogpnx-1gie%2Csn-1gi7znek&ms=au%2Crdu&mv=m&mvi=1&pl=24&initcwndbps=866250&spc=H3gIhpbgKYelq0I3hwDAdKR2VZrWbIM&vprv=1&svpuc=1&mime=video%2Fmp4&cnr=14&ratebypass=yes&dur=208.259&lmt=1674828754958102&mt=1674938958&fvip=1&fexp=24007246&c=ANDROID&txp=4432434&sparams=expire%2Cei%2Cip%2Cid%2Citag%2Csource%2Crequiressl%2Cspc%2Cvprv%2Csvpuc%2Cmime%2Ccnr%2Cratebypass%2Cdur%2Clmt&sig=AOq0QJ8wRQIgfptvZw_wnkQve0LLPrkpB9VtQ8xl32JAeV-TWRaGJLACIQDFmzzsLar1maTYRVpRmHuzDR0BHdqZo_NlLkyFSuAeuQ%3D%3D&lsparams=mh%2Cmm%2Cmn%2Cms%2Cmv%2Cmvi%2Cpl%2Cinitcwndbps&lsig=AG3C_xAwRAIgL_x1KxwfpaO5ryzcZRHB8It6qMCPh46BRZkuBeLEmz8CIBG0Tz5xYQEqN7nGKgu4I-lRc-jHBbFCHmiqxRF8LjrH&cpn=5TbbYYYtWfFY2S_e"),
            subtitles = listOf(
                VttSubtitle(
                    language = "en",
                    displayLanguage = "English",
                    uri = Uri.parse("https://gist.githubusercontent.com/samdutton/ca37f3adaf4e23679957b8083e061177/raw/e19399fbccbc069a2af4266e5120ae6bad62699a/sample.vtt"),
                    headers = null
                ),
                VttSubtitle(
                    language = "al",
                    displayLanguage = "Albanian",
                    uri = Uri.parse("https://raw.githubusercontent.com/brenopolanski/html5-video-webvtt-example/master/MIB2-subtitles-pt-BR.vtt"),
                    headers = null
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
