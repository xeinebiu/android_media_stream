package com.xeinebiu.media_stream.model

import java.io.InputStream

data class Stream(
    val id: Int,
    val title: String,
    val length: Long,
    val inputStream: () -> InputStream
)
