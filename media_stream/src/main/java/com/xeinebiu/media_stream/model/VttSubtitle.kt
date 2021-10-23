package com.xeinebiu.media_stream.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class VttSubtitle(
    val language: String,
    val displayLanguage: String,
    val content: String
) : Parcelable
