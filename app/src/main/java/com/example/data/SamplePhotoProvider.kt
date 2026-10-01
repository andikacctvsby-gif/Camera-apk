package com.example.data

import com.example.R

data class SamplePhotoItem(
    val id: String,
    val title: String,
    val description: String,
    val resId: Int,
    val defaultPreset: String,
    val distractionDesc: String
)

object SamplePhotoProvider {
    val samples = listOf(
        SamplePhotoItem(
            id = "sample_portrait",
            title = "Potret Kafe & Photobomber",
            description = "Foto portrait dengan pejalan kaki di latar belakang yang siap dihapus.",
            resId = R.drawable.sample_portrait,
            defaultPreset = "Studio Portrait",
            distractionDesc = "Pejalan kaki di latar belakang kanan"
        ),
        SamplePhotoItem(
            id = "sample_travel",
            title = "Sunset Tropis & Turis",
            description = "Pemandangan matahari terbenam pantai dengan turis yang mengganggu.",
            resId = R.drawable.sample_travel,
            defaultPreset = "Golden Hour",
            distractionDesc = "Turis di pinggir pantai kanan"
        )
    )
}
