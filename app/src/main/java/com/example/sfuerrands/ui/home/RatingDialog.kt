package com.example.sfuerrands.ui.home

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.example.sfuerrands.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object RatingDialog {
    fun show(context: Context, onSubmit: (Int) -> Unit) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_rating, null)
        val ratingBar = view.findViewById<android.widget.RatingBar>(R.id.ratingBar)

        MaterialAlertDialogBuilder(context)
            .setTitle("Rate the other party")
            .setView(view)
            .setPositiveButton("Submit") { _, _ ->
                val r = ratingBar.rating.toInt().coerceIn(1, 5)
                onSubmit(r)
            }
            .setNegativeButton("Skip", null)
            .show()
    }
}
