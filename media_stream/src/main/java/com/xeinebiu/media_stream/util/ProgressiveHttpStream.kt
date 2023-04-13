package com.xeinebiu.media_stream.util

import android.net.Uri
import kotlinx.coroutines.runBlocking
import java.io.InputStream

internal class ProgressiveHttpStream(
    private val streamUri: Uri,
    private val headers: HashMap<String, String>?,
) : InputStream() {

    private var inputStream = runBlocking {
        createHttpURLConnection(
            streamUri.toString(),
            headers,
        ).inputStream!!
    }

    override fun read(): Int = inputStream.read()

    override fun available(): Int = inputStream.available()

    override fun close(): Unit = inputStream.close()

    override fun mark(readlimit: Int): Unit = inputStream.mark(readlimit)

    override fun markSupported(): Boolean = inputStream.markSupported()

    override fun read(b: ByteArray): Int = inputStream.read(b)

    override fun read(b: ByteArray, off: Int, len: Int): Int = inputStream.read(b, off, len)

    override fun reset(): Unit = inputStream.reset()

    override fun skip(n: Long): Long {
        val rangeHeader = headers ?: hashMapOf()
        rangeHeader["Range"] = "bytes=$n-"

        this.inputStream = runBlocking {
            createHttpURLConnection(
                streamUri.toString(),
                rangeHeader,
            ).inputStream
        }

        return n
    }

    override fun equals(other: Any?): Boolean = inputStream == other

    override fun hashCode(): Int = inputStream.hashCode()

    override fun toString(): String = inputStream.toString()
}
