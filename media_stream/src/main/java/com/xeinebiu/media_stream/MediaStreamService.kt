package com.xeinebiu.media_stream

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.Parcelable
import android.webkit.URLUtil
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.xeinebiu.media_stream.model.Stream
import com.xeinebiu.media_stream.model.VttSubtitle
import com.xeinebiu.media_stream.util.ProgressiveHttpStream
import com.xeinebiu.media_stream.util.getContentLength
import com.xeinebiu.media_stream.util.getIPAddress
import com.yanzhenjie.andserver.AndServer
import com.yanzhenjie.andserver.Server
import java.io.File
import java.io.InputStream
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

class MediaStreamService : Service() {
    private val servers = HashMap<Int, Server>()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        startForeground(
            SERVICE_CODE,
            createNotification(
                SERVICE_TITLE,
                "",
                DEFAULT_ICON
            ).build()
        )
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        val action = intent?.action ?: return START_STICKY

        val streamAction =
            intent.getParcelableExtra<StreamAction>(EXTRA_ARGS) ?: return START_STICKY

        val port = streamAction.port
        when (action) {
            ACTION_STREAM -> {
                startServer(port)

                streamAction.startStreaming()
            }

            ACTION_STOP -> {
                streamAction.stopStreaming()

                if (STREAMS[port]?.isEmpty() == true) {
                    STREAMS.remove(port)

                    stopServer(port)
                }

                if (STREAMS.isEmpty()) {
                    stopForeground(true)
                    stopSelf()
                }
            }
        }

        return START_STICKY
    }

    private fun startServer(port: Int) {
        if (servers.containsKey(port)) return

        servers[port] = AndServer.webServer(this)
            .port(port)
            .timeout(10, TimeUnit.SECONDS)
            .build()
            .also {
                it.startup()
            }
    }

    private fun stopServer(port: Int) {
        val server = servers[port] ?: return

        server.shutdown()

        servers.remove(port)
    }

    private fun StreamAction.startStreaming() {
        serviceScope.launch {
            showCastNotification()

            val length = getLength()

            val streamOpenCallback = createStreamOpenCallback()

            val stream = Stream(
                id = id,
                title = title,
                length = length,
                inputStream = streamOpenCallback,
                subtitles = subtitles
            )

            addStream(stream, port)
        }
    }

    private suspend fun StreamAction.getLength(): Long {
        val streamUrl = streamUri.toString()

        return when {
            isOnline() -> getContentLength(
                uri = streamUrl,
                headers = headers
            )

            URLUtil.isFileUrl(streamUrl) -> File(streamUrl).length()

            URLUtil.isContentUrl(streamUrl) -> DocumentFile.fromTreeUri(
                this@MediaStreamService,
                streamUri
            )?.length() ?: 0

            else -> 0
        }
    }

    private fun StreamAction.createStreamOpenCallback(): () -> InputStream? {
        return {
            val streamUrl = streamUri.toString()

            when {
                isOnline() -> ProgressiveHttpStream(
                    streamUri = streamUri,
                    headers = headers
                )

                URLUtil.isContentUrl(streamUrl) -> contentResolver.openInputStream(streamUri)

                URLUtil.isFileUrl(streamUrl) -> File(streamUrl).inputStream()

                else -> null
            }
        }
    }

    private fun StreamAction.isOnline(): Boolean = streamUri.toString().let {
        URLUtil.isHttpUrl(it) || URLUtil.isHttpsUrl(it)
    }

    private fun StreamAction.stopStreaming() {
        removeStream(id = id, port = port)

        cancelCastNotification(id)
    }

    private fun createNotification(
        title: String,
        subtitle: String,
        @DrawableRes icon: Int
    ): NotificationCompat.Builder {
        val builder = NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setSmallIcon(icon)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setGroup(SERVICE_CHANNEL_ID)

        if (title.isNotEmpty()) builder.setContentTitle(title)

        if (subtitle.isNotEmpty()) builder.setContentText(subtitle)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                SERVICE_CHANNEL_ID,
                NotificationManager.IMPORTANCE_DEFAULT
            )

            channel.setSound(null, null)

            val notificationManager = getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

            notificationManager.createNotificationChannel(channel)

            builder.setChannelId(SERVICE_CHANNEL_ID)
        }

        return builder
    }

    private fun cancelCastNotification(id: Int) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(id)
    }

    private fun StreamAction.showCastNotification() {
        val cancelIntent = Intent(
            this@MediaStreamService,
            MediaStreamService::class.java
        ).apply {
            action = ACTION_STOP
        }.also {
            it.putExtra(EXTRA_ARGS, this)
        }

        val cancelPendingIntent = PendingIntent.getService(
            this@MediaStreamService,
            System.currentTimeMillis().toInt(),
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = createNotification(
            title = title,
            subtitle = url,
            icon = DEFAULT_ICON
        )

        builder.addAction(
            R.drawable.ic_close_black_24dp,
            getString(R.string.option_cancel),
            cancelPendingIntent
        )

        builder.setOngoing(true)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(id, builder.build())
    }

    @Parcelize
    internal data class StreamAction(
        val id: Int,
        val title: String,
        val headers: HashMap<String, String>,
        val port: Int,
        val url: String,
        val streamUri: Uri,
        val subtitles: List<VttSubtitle>
    ) : Parcelable

    companion object {
        private const val SERVICE_CHANNEL_ID = "Media Streaming"
        private const val SERVICE_TITLE = "Media Streaming"
        private const val SERVICE_CODE = 30006

        private const val DEFAULT_PORT = 9001

        private val DEFAULT_ICON = R.drawable.ic_icon

        private const val EXTRA_ARGS = "extra_args"

        private const val ACTION_STREAM = "action_cast"

        private const val ACTION_STOP = "action_stop"

        private val STREAMS = HashMap<Int, MutableList<Stream>>()

        /**
         * Stream given [stream]
         * @return Url to watch the STREAM
         */
        fun stream(
            context: Context,
            title: String,
            stream: Uri,
            subtitles: List<VttSubtitle>,
            headers: HashMap<String, String>,
            port: Int = DEFAULT_PORT
        ): String {
            val id = Random.nextInt(100, 999)

            val ipV4 = getIPAddress(true)
            val url = "http://$ipV4:$port/stream/$id"

            val intent = Intent(context, MediaStreamService::class.java).apply {
                action = ACTION_STREAM
            }

            intent.putExtra(
                EXTRA_ARGS,
                StreamAction(
                    id = id,
                    title = title,
                    url = url,
                    streamUri = stream,
                    subtitles = subtitles,
                    port = port,
                    headers = headers,
                )
            )

            ContextCompat.startForegroundService(context, intent)

            return url
        }

        private fun addStream(
            stream: Stream,
            port: Int
        ): Unit = synchronized(STREAMS) {
            var list = STREAMS[port]
            if (list != null)
                list.add(stream)
            else {
                list = mutableListOf(stream)
                STREAMS[port] = list
            }
        }

        fun findStream(
            id: String,
            port: Int
        ): Stream? = try {
            val idInt = Integer.parseInt(id)
            findStream(idInt, port)
        } catch (e: NumberFormatException) {
            null
        }

        private fun findStream(
            id: Int,
            port: Int
        ): Stream? = STREAMS[port]?.firstOrNull { it.id == id }

        private fun removeStream(
            id: Int,
            port: Int
        ) {
            val cast = findStream(id, port)

            synchronized(STREAMS) {
                STREAMS[port]?.remove(cast)
            }
        }
    }
}

