package com.example.sfuerrands.ui.settings

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class EditProfileViewModel : ViewModel() {

    private val _name = MutableLiveData<String>()
    val name: LiveData<String> get() = _name

    private val _email = MutableLiveData<String>()
    val email: LiveData<String> get() = _email

    private val _selectedCampuses = MutableLiveData<List<String>>(emptyList())
    val selectedCampuses: LiveData<List<String>> get() = _selectedCampuses

    // User-picked new image
    private val _imageUri = MutableLiveData<Uri?>(null)
    val imageUri: LiveData<Uri?> get() = _imageUri

    // Existing photo URL loaded from Firestore
    private val _existingPhotoUrl = MutableLiveData<String?>(null)
    val existingPhotoUrl: LiveData<String?> get() = _existingPhotoUrl

    fun setName(value: String) {
        _name.value = value
    }

    fun setEmail(value: String) {
        _email.value = value
    }

    fun setCampuses(list: List<String>) {
        _selectedCampuses.value = list
    }

    fun setImage(uri: Uri) {
        _imageUri.value = uri
    }

    fun setExistingPhotoUrl(url: String?) {
        _existingPhotoUrl.value = url
    }
}
