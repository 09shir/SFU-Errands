package com.example.sfuerrands.ui.preview

import android.content.Context
import android.content.Intent

object ImagePreviewNavigator {

    /**
     * Open the full-screen image preview.
     *
     * @param context Activity or Fragment.requireContext()
     * @param urls list of HTTPS image URLs
     * @param startIndex which image to show first (0-based)
     */
    fun open(context: Context, urls: List<String>, startIndex: Int = 0) {
        if (urls.isEmpty()) return

        val intent = Intent(context, ImagePreviewActivity::class.java).apply {
            putStringArrayListExtra(
                ImagePreviewActivity.EXTRA_URLS,
                ArrayList(urls)
            )
            putExtra(
                ImagePreviewActivity.EXTRA_START_INDEX,
                startIndex.coerceIn(0, urls.size - 1)
            )
        }
        context.startActivity(intent)
    }
}
