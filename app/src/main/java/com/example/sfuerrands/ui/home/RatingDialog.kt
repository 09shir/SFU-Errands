package com.example.sfuerrands.ui.home

import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.RatingBar
import androidx.appcompat.app.AlertDialog
import com.example.sfuerrands.R

object RatingDialog {

    fun show(context: Context, onSubmit: (Int) -> Unit) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_rating, null)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)
        val submitButton = dialogView.findViewById<Button>(R.id.submitButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)

        ratingBar.setIsIndicator(false)
        ratingBar.stepSize = 1f

        // Disable submit until user selects a rating
        submitButton.isEnabled = false
        submitButton.alpha = 0.5f   // Visual disabled state (optional)

        // Enable when rating > 0
        ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
            submitButton.isEnabled = rating > 0f
            submitButton.alpha = if (rating > 0f) 1f else 0.5f
        }

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        submitButton.setOnClickListener {
            val rating = ratingBar.rating.toInt()
            onSubmit(rating)
            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
