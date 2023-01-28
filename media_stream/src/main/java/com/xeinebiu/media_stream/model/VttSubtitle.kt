package com.xeinebiu.media_stream.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class VttSubtitle(
    val language: String,
    val displayLanguage: String,
    val uri: Uri,
    val headers: Map<String, String>?
) : Parcelable
