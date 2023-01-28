package com.xeinebiu.media_stream

import com.xeinebiu.media_stream.model.Stream
import com.xeinebiu.media_stream.model.VttSubtitle
import com.xeinebiu.media_stream.util.createHttpURLConnection
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.request.ranges
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.utils.io.ByteWriteChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

internal class StreamServer(
    private val port: Int,
    private val streams: () -> List<Stream>
) {
    private var server: NettyApplicationEngine? = null

    private fun ApplicationCall.findStream(pathId: String): Stream? {
        val streamId = parameters[pathId]?.toInt() ?: return null
        return streams().find { it.id == streamId }
    }

    fun start(wait: Boolean = false) {
        server = embeddedServer(Netty, port) {
            routing {
                indexPage()

                subtitlesPage()

                streamPage()
            }
        }.start(wait)
    }

    fun stop() {
        server?.stop()
        server = null
    }

    private fun Routing.indexPage() {
        get("/stream/{streamId}") {
            val stream = call.findStream("streamId") ?: return@get

            val subtitlesHtml = createSubtitlesHtml(stream)
            val html = "<style>\n" +
                "  body {\n" +
                "    margin: 0px\n" +
                "  }\n" +
                "</style>\n" +
                "<link href=\"https://unpkg.com/video.js/dist/video-js.min.css\" rel=\"stylesheet\">\n" +
                "<script src=\"https://unpkg.com/video.js/dist/video.min.js\"></script>\n" +
                "<video id=\"my-player\" class=\"video-js\" controls preload=\"auto\" data-setup='{\"fluid\": true}'>\n" +
                "  <source src=\"/stream/${stream.id}/video\" type=\"video/mp4\">\n" +
                "  </source> $subtitlesHtml <p class=\"vjs-no-js\"> To view this video please enable JavaScript, and consider upgrading to a web browser that <a href=\"https://videojs.com/html5-video-support/\" target=\"_blank\"> supports HTML5 video </a>\n" +
                "  </p>\n" +
                "</video>"

            call.respondText(
                contentType = ContentType.parse("text/html"),
                text = html
            )
        }
    }

    private fun Routing.subtitlesPage() {
        get("/stream/{streamId}/subtitle/{language}") {
            val stream = call.findStream("streamId") ?: return@get
            val language = call.parameters["language"] ?: return@get

            for (subtitle in stream.subtitles) {
                if (subtitle.language.equals(language, ignoreCase = true)) {
                    runCatching {
                        val subtitleContent = downloadSubtitleContent(subtitle)

                        call.respondText(
                            contentType = ContentType.parse("text/vtt"),
                            text = subtitleContent
                        )
                    }
                }
            }
        }
    }

    private fun Routing.streamPage() {
        get("/stream/{streamId}/video") {
            val stream = call.findStream("streamId") ?: return@get

            val contentLength: Long = stream.length

            val ranges = call
                .request
                .ranges()
                ?.merge(contentLength)
                ?.firstOrNull()

            val rangeStart = ranges?.start ?: 0
            val rangeEnd = ranges?.endInclusive ?: contentLength

            if (rangeStart > 0) {
                call.response.header(
                    "Content-Range",
                    "bytes $rangeStart-$rangeEnd/$contentLength"
                )
            } else {
                call.response.header(
                    "Content-Range",
                    "bytes " + rangeStart + "-" + (contentLength - 1) + "/" + contentLength
                )
            }

            call.response.status(HttpStatusCode.fromValue(206))

            call.respond(
                VideoWriteChannelContent(
                    stream = stream,
                    rangeStart = rangeStart,
                    rangeEnd = rangeEnd
                )
            )
        }
    }

    private fun createSubtitlesHtml(streamItem: Stream): String {
        val sb = StringBuilder()
        for ((language, displayLanguage) in streamItem.subtitles) {
            sb.append("<track kind='captions' src='/stream/")
                .append(streamItem.id)
                .append("/subtitle/")
                .append(language)
                .append("' srclang='")
                .append(language)
                .append("' label='")
                .append(displayLanguage)
                .append("' default />")
                .append("\n")
        }
        return sb.toString()
    }

    private suspend fun downloadSubtitleContent(
        subtitle: VttSubtitle
    ): String = withContext(Dispatchers.IO) {
        val connection = createHttpURLConnection(
            uri = subtitle.uri.toString(),
            headers = subtitle.headers
        )

        val responseCode: Int = connection.responseCode
        val inputStream: InputStream = if (responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }

        inputStream
            .bufferedReader()
            .readText()
    }

    private class VideoWriteChannelContent(
        private val stream: Stream,
        private val rangeStart: Long,
        private val rangeEnd: Long
    ) : OutgoingContent.WriteChannelContent() {

        override suspend fun writeTo(channel: ByteWriteChannel) {
            withContext(Dispatchers.IO) {
                val inputStream = stream.inputStream() ?: return@withContext

                val buffer = ByteArray(4096)
                inputStream.skip(rangeStart)

                var len = 0
                var total = 0
                while (rangeEnd > total && inputStream.read(buffer)
                    .also { len = it } != -1
                ) {
                    channel.writeFully(buffer, 0, len)
                    total += len
                }
            }
        }
    }
}
