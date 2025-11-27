package com.example.sfuerrands.ui.preview

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.sfuerrands.databinding.ActivityImagePreviewBinding

class ImagePreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImagePreviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImagePreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // get image url and start index
        val urls = intent.getStringArrayListExtra(EXTRA_URLS) ?: arrayListOf()
        val startIndex = intent.getIntExtra(EXTRA_START_INDEX, 0)
            .coerceIn(0, (urls.size - 1).coerceAtLeast(0))

        // ViewPager2
        val adapter = ImagePreviewAdapter(urls)
        binding.pager.adapter = adapter
        binding.pager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        binding.pager.setCurrentItem(startIndex, false)

        // close button
        binding.btnClose.setOnClickListener {
            finish()
        }
    }

    companion object {
        const val EXTRA_URLS = "extra_urls"
        const val EXTRA_START_INDEX = "extra_start_index"
    }
}
