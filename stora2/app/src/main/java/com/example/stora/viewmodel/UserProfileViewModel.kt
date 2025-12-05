package com.example.stora.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserProfile(
    val name: String = "NAMA PENGGUNA",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val profileImageUri: Uri? = null
)

class UserProfileViewModel : ViewModel() {
    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    fun updateProfile(
        name: String? = null,
        email: String? = null,
        phone: String? = null,
        address: String? = null,
        profileImageUri: Uri? = null
    ) {
        _userProfile.value = _userProfile.value.copy(
            name = name ?: _userProfile.value.name,
            email = email ?: _userProfile.value.email,
            phone = phone ?: _userProfile.value.phone,
            address = address ?: _userProfile.value.address,
            profileImageUri = profileImageUri ?: _userProfile.value.profileImageUri
        )
    }

    fun updateProfileImage(uri: Uri) {
        _userProfile.value = _userProfile.value.copy(profileImageUri = uri)
    }
}
