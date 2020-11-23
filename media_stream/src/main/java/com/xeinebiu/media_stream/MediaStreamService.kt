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
import android.os.StrictMode
import android.webkit.URLUtil
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.xeinebiu.media_stream.model.Stream
import com.xeinebiu.media_stream.util.createHttpURLConnection
import com.xeinebiu.media_stream.util.getContentLength
import com.xeinebiu.media_stream.util.getIPAddress
import com.yanzhenjie.andserver.AndServer
import com.yanzhenjie.andserver.Server
import java.io.InputStream
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap
import kotlin.random.Random

class MediaStreamService : Service() {
    private val servers = HashMap<Int, Server>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build())
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
        val port = intent.getIntExtra(EXTRA_PORT, DEFAULT_PORT)
        when (action) {
            ACTION_STREAM -> {
                startServer(port)
                startCasting(intent, port)
            }
            ACTION_STOP -> {
                stopCasting(intent, port)

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

    @Suppress("UNCHECKED_CAST")
    private fun startCasting(
        intent: Intent,
        port: Int
    ) {
        val id = intent.getIntExtra(EXTRA_ID, 0)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val subtitle = intent.getStringExtra(EXTRA_SUBTITLE) ?: ""
        val headers = intent.getSerializableExtra(EXTRA_HEADERS) as HashMap<String, String>

        val streamUri = intent.getParcelableExtra<Uri>(EXTRA_STREAM_URI) ?: return
        val isOnline = streamUri.toString().let {
            URLUtil.isHttpUrl(it) || URLUtil.isHttpsUrl(it)
        }

        showCastNotification(
            id,
            port,
            title,
            subtitle
        )

        val streamOpenCallback: () -> InputStream = {
            if (isOnline)
                ProgressiveHttpStream(streamUri, headers)
            else
                this@MediaStreamService.contentResolver.openInputStream(streamUri)!!
        }

        val length =
            if (isOnline) getContentLength(streamUri.toString(), headers)
            else DocumentFile.fromTreeUri(this, streamUri)?.length() ?: 0

        val stream = Stream(
            id,
            title,
            length,
            streamOpenCallback
        )
        addStream(stream, port)
    }

    private fun stopCasting(intent: Intent, port: Int) {
        val id = intent.getIntExtra(EXTRA_ID, 0)
        removeStream(id, port)
        cancelCastNotification(id)
    }

    private fun createNotification(
        title: String,
        subtitle: String,
        @DrawableRes icon: Int
    ): NotificationCompat.Builder {
        val builder = NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setSmallIcon(icon)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setGroup(SERVICE_CHANNEL_ID)
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

    private fun showCastNotification(
        id: Int,
        port: Int,
        title: String,
        subtitle: String
    ) {
        val cancelIntent = Intent(this, MediaStreamService::class.java)
        cancelIntent.action = ACTION_STOP
        cancelIntent.putExtra(EXTRA_ID, id)
        cancelIntent.putExtra(EXTRA_PORT, port)

        val cancelPendingIntent =
            PendingIntent.getService(
                this,
                System.currentTimeMillis().toInt(),
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

        val builder = createNotification(title, subtitle, DEFAULT_ICON)
        builder.addAction(
            R.drawable.ic_close_black_24dp,
            getString(R.string.option_cancel),
            cancelPendingIntent
        )

        builder.setOngoing(true)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(id, builder.build())
    }

    private class ProgressiveHttpStream(
        private val streamUri: Uri,
        private val headers: HashMap<String, String>?
    ) : InputStream() {
        var inputStream = createHttpURLConnection(
            streamUri.toString(),
            headers
        ).inputStream!!

        override fun read(): Int =
            inputStream.read()

        override fun available(): Int =
            inputStream.available()

        override fun close(): Unit =
            inputStream.close()

        override fun mark(readlimit: Int): Unit =
            inputStream.mark(readlimit)

        override fun markSupported(): Boolean =
            inputStream.markSupported()

        override fun read(b: ByteArray): Int =
            inputStream.read(b)

        override fun read(b: ByteArray, off: Int, len: Int): Int =
            inputStream.read(b, off, len)

        override fun reset(): Unit =
            inputStream.reset()

        override fun skip(n: Long): Long {
            val rangeHeader = headers ?: hashMapOf()
            rangeHeader["Range"] = "bytes=$n-"
            this.inputStream = createHttpURLConnection(
                streamUri.toString(),
                rangeHeader
            ).inputStream
            return n
        }

        override fun equals(other: Any?): Boolean =
            inputStream == other

        override fun hashCode(): Int =
            inputStream.hashCode()

        override fun toString(): String =
            inputStream.toString()
    }

    companion object {
        private const val SERVICE_CHANNEL_ID = "Media Streaming"
        private const val SERVICE_TITLE = "Popup Player"
        private const val SERVICE_CODE = 30005
        private const val DEFAULT_PORT = 9001
        private val DEFAULT_ICON = R.drawable.ic_icon

        private const val EXTRA_ID = "extra_id"
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_SUBTITLE = "extra_subtitle"
        private const val EXTRA_STREAM_URI = "extra_stream_uri"
        private const val EXTRA_HEADERS = "extra_headers"
        private const val EXTRA_PORT = "extra_port"

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
            headers: HashMap<String, String>,
            port: Int = DEFAULT_PORT
        ): String {
            val id = Random.nextInt(1000, 9999)

            val ipV4 = getIPAddress(true)
            val url = "http://$ipV4:$port/stream/video/$id"

            val intent = Intent(context, MediaStreamService::class.java)
            intent.action = ACTION_STREAM
            intent.putExtra(EXTRA_ID, id)
            intent.putExtra(EXTRA_TITLE, title)
            intent.putExtra(EXTRA_SUBTITLE, url)
            intent.putExtra(EXTRA_STREAM_URI, stream)
            intent.putExtra(EXTRA_PORT, port)
            intent.putExtra(EXTRA_HEADERS, headers)
            ContextCompat.startForegroundService(context, intent)

            return url
        }

        private fun addStream(
            stream: Stream,
            port: Int
        ) {
            synchronized(STREAMS) {
                var list = STREAMS[port]
                if (list != null)
                    list.add(stream)
                else {
                    list = mutableListOf(stream)
                    STREAMS[port] = list
                }
            }
        }

        fun findStream(
            id: String,
            port: Int
        ): Stream? =
            try {
                val idInt = Integer.parseInt(id)
                findStream(idInt, port)
            } catch (e: NumberFormatException) {
                null
            }

        private fun findStream(
            id: Int,
            port: Int
        ): Stream? =
            STREAMS[port]?.firstOrNull { it.id == id }

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
