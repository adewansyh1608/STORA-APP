package com.example.stora.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import com.example.stora.utils.TokenManager
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

class UserProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val tokenManager = TokenManager.getInstance(application)
    
    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    init {
        loadProfileFromToken()
    }

    fun loadProfileFromToken() {
        val name = tokenManager.getUserName() ?: "NAMA PENGGUNA"
        val email = tokenManager.getUserEmail() ?: ""
        val fotoProfile = tokenManager.getFotoProfile()
        
        _userProfile.value = _userProfile.value.copy(
            name = name,
            email = email,
            profileImageUri = if (fotoProfile != null) Uri.parse(fotoProfile) else null
        )
    }

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
        
        if (profileImageUri != null) {
            tokenManager.saveFotoProfile(profileImageUri.toString())
        }
    }

    fun updateProfileImage(uri: Uri) {
        _userProfile.value = _userProfile.value.copy(profileImageUri = uri)
        tokenManager.saveFotoProfile(uri.toString())
    }
}
