package com.example.stora.network

import com.example.stora.data.AuthResponse
import com.example.stora.data.LoginRequest
import com.example.stora.data.SignupRequest
import com.example.stora.data.ResetPasswordRequest
import com.example.stora.data.UpdateProfileRequest
import com.example.stora.data.InventoryApiResponse
import com.example.stora.data.InventoryApiModel
import com.example.stora.data.InventoryRequest
import com.example.stora.data.LoanApiResponse
import com.example.stora.data.LoanApiModel
import com.example.stora.data.LoanCreateRequest
import com.example.stora.data.LoanStatusUpdateRequest
import com.example.stora.data.LoanUpdateRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("login")
    suspend fun login(
        @Body loginRequest: LoginRequest
    ): Response<AuthResponse>

    @POST("signup")
    suspend fun signup(
        @Body signupRequest: SignupRequest
    ): Response<AuthResponse>

    @POST("logout")
    suspend fun logout(
        @Header("Authorization") token: String
    ): Response<AuthResponse>

    @POST("reset-password")
    suspend fun resetPassword(
        @Body resetPasswordRequest: ResetPasswordRequest
    ): Response<AuthResponse>

    @GET("profile")
    suspend fun getProfile(
        @Header("Authorization") token: String
    ): Response<AuthResponse>

    @PUT("profile")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body updateProfileRequest: UpdateProfileRequest
    ): Response<AuthResponse>

    @Multipart
    @POST("profile/photo")
    suspend fun uploadProfilePhoto(
        @Header("Authorization") token: String,
        @Part photo: MultipartBody.Part
    ): Response<AuthResponse>


    @GET("inventaris")
    suspend fun getAllInventory(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100,
        @Query("search") search: String? = null,
        @Query("kategori") kategori: String? = null,
        @Query("kondisi") kondisi: String? = null
    ): Response<InventoryApiResponse<List<InventoryApiModel>>>

    @GET("inventaris/{id}")
    suspend fun getInventoryById(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<InventoryApiResponse<InventoryApiModel>>

    @POST("inventaris")
    suspend fun createInventory(
        @Header("Authorization") token: String,
        @Body inventoryRequest: InventoryRequest
    ): Response<InventoryApiResponse<InventoryApiModel>>

    @Multipart
    @POST("inventaris")
    suspend fun createInventoryWithPhoto(
        @Header("Authorization") token: String,
        @Part("Nama_Barang") namaBarang: RequestBody,
        @Part("Kode_Barang") kodeBarang: RequestBody,
        @Part("Jumlah") jumlah: RequestBody,
        @Part("Kategori") kategori: RequestBody,
        @Part("Lokasi") lokasi: RequestBody,
        @Part("Kondisi") kondisi: RequestBody,
        @Part("Tanggal_Pengadaan") tanggalPengadaan: RequestBody,
        @Part("Deskripsi") deskripsi: RequestBody?,
        @Part foto: MultipartBody.Part?
    ): Response<InventoryApiResponse<InventoryApiModel>>

    @PUT("inventaris/{id}")
    suspend fun updateInventory(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body inventoryRequest: InventoryRequest
    ): Response<InventoryApiResponse<InventoryApiModel>>

    @Multipart
    @PUT("inventaris/{id}")
    suspend fun updateInventoryWithPhoto(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Part("Nama_Barang") namaBarang: RequestBody,
        @Part("Kode_Barang") kodeBarang: RequestBody,
        @Part("Jumlah") jumlah: RequestBody,
        @Part("Kategori") kategori: RequestBody,
        @Part("Lokasi") lokasi: RequestBody,
        @Part("Kondisi") kondisi: RequestBody,
        @Part("Tanggal_Pengadaan") tanggalPengadaan: RequestBody,
        @Part("Deskripsi") deskripsi: RequestBody?,
        @Part foto: MultipartBody.Part?
    ): Response<InventoryApiResponse<InventoryApiModel>>

    @DELETE("inventaris/{id}")
    suspend fun deleteInventory(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<InventoryApiResponse<Any>>

    @GET("inventaris/stats")
    suspend fun getInventoryStats(
        @Header("Authorization") token: String
    ): Response<InventoryApiResponse<Any>>


    @GET("peminjaman")
    suspend fun getAllPeminjaman(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100,
        @Query("status") status: String? = null,
        @Query("search") search: String? = null
    ): Response<LoanApiResponse<List<LoanApiModel>>>

    @GET("peminjaman/{id}")
    suspend fun getPeminjamanById(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<LoanApiResponse<LoanApiModel>>

    @POST("peminjaman")
    suspend fun createPeminjaman(
        @Header("Authorization") token: String,
        @Body request: LoanCreateRequest
    ): Response<LoanApiResponse<LoanApiModel>>

    @Multipart
    @POST("peminjaman/with-photos")
    suspend fun createPeminjamanWithPhotos(
        @Header("Authorization") token: String,
        @Part("Nama_Peminjam") namaPeminjam: RequestBody,
        @Part("NoHP_Peminjam") noHpPeminjam: RequestBody,
        @Part("Tanggal_Pinjam") tanggalPinjam: RequestBody,
        @Part("Tanggal_Kembali") tanggalKembali: RequestBody,
        @Part("ID_User") idUser: RequestBody,
        @Part("barangList") barangList: RequestBody,
        @Part photos: List<MultipartBody.Part>?
    ): Response<LoanApiResponse<LoanApiModel>>

    @PATCH("peminjaman/{id}/status")
    suspend fun updatePeminjamanStatus(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: LoanStatusUpdateRequest
    ): Response<LoanApiResponse<LoanApiModel>>

    @PUT("peminjaman/{id}")
    suspend fun updatePeminjaman(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: LoanUpdateRequest
    ): Response<LoanApiResponse<LoanApiModel>>

    @GET("peminjaman/stats")
    suspend fun getPeminjamanStats(
        @Header("Authorization") token: String
    ): Response<LoanApiResponse<Any>>

    @DELETE("peminjaman/{id}")
    suspend fun deletePeminjaman(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<LoanApiResponse<Any>>

    @Multipart
    @PATCH("peminjaman/{id}/return-photos")
    suspend fun uploadReturnPhotos(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Part("photoMapping") photoMapping: RequestBody,
        @Part photos: List<MultipartBody.Part>?
    ): Response<LoanApiResponse<LoanApiModel>>



    @POST("notifications/register-token")
    suspend fun registerFcmToken(
        @Header("Authorization") token: String,
        @Body request: com.example.stora.data.FcmTokenRequest
    ): Response<com.example.stora.data.NotificationApiResponse<Any>>

    @GET("notifications/reminders")
    suspend fun getReminders(
        @Header("Authorization") token: String
    ): Response<com.example.stora.data.NotificationApiResponse<List<com.example.stora.data.ReminderApiModel>>>

    @POST("notifications/reminders")
    suspend fun createReminder(
        @Header("Authorization") token: String,
        @Body request: com.example.stora.data.ReminderRequest
    ): Response<com.example.stora.data.NotificationApiResponse<com.example.stora.data.ReminderApiModel>>

    @PUT("notifications/reminders/{id}")
    suspend fun updateReminder(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: com.example.stora.data.ReminderRequest
    ): Response<com.example.stora.data.NotificationApiResponse<com.example.stora.data.ReminderApiModel>>

    @DELETE("notifications/reminders/{id}")
    suspend fun deleteReminder(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<com.example.stora.data.NotificationApiResponse<Any>>

    @PATCH("notifications/reminders/{id}/toggle")
    suspend fun toggleReminder(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<com.example.stora.data.NotificationApiResponse<com.example.stora.data.ReminderApiModel>>

    @GET("notifications/history")
    suspend fun getNotificationHistory(
        @Header("Authorization") token: String
    ): Response<com.example.stora.data.NotificationApiResponse<List<com.example.stora.data.NotificationHistoryApiModel>>>

    @POST("notifications/history")
    suspend fun createNotificationHistory(
        @Header("Authorization") token: String,
        @Body request: Map<String, Any>
    ): Response<com.example.stora.data.NotificationApiResponse<com.example.stora.data.NotificationHistoryApiModel>>
}
