package com.example.sfuerrands.ui.home

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.example.sfuerrands.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.RatingBar
import android.widget.Button

object RatingDialog {

    fun show(context: Context, onSubmit: (Int) -> Unit) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_rating, null)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)

        // VERY IMPORTANT — allows stars to be clicked
        ratingBar.setIsIndicator(false)
        ratingBar.stepSize = 1f

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialogView.findViewById<Button>(R.id.submitButton).setOnClickListener {
            val rating = ratingBar.rating.toInt()
            onSubmit(rating)
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.cancelButton).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}

