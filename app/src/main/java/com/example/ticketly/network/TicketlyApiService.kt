package com.example.ticketly.network

import com.example.ticketly.network.dto.AddCartItemRequestDto
import com.example.ticketly.network.dto.CartResponseDto
import com.example.ticketly.network.dto.CheckoutRequestDto
import com.example.ticketly.network.dto.CheckoutResponseDto
import com.example.ticketly.network.dto.EventResponseDto
import com.example.ticketly.network.dto.EventsResponseDto
import com.example.ticketly.network.dto.LoginRequestDto
import com.example.ticketly.network.dto.LoginResponseDto
import com.example.ticketly.network.dto.MeResponseDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Mirrors `backend/index.js` one-to-one; see `backend/README.md` for the endpoint table. */
interface TicketlyApiService {
    @GET("api/events")
    suspend fun getEvents(@Query("q") query: String? = null): EventsResponseDto

    @GET("api/events/{id}")
    suspend fun getEvent(@Path("id") id: String): EventResponseDto

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequestDto): LoginResponseDto

    @POST("api/auth/logout")
    suspend fun logout()

    @GET("api/auth/me")
    suspend fun getCurrentUser(): MeResponseDto

    @POST("api/cart/items")
    suspend fun addCartItem(@Body request: AddCartItemRequestDto): CartResponseDto

    @DELETE("api/cart/items/{ticketId}")
    suspend fun removeCartItem(@Path("ticketId") ticketId: String): CartResponseDto

    @POST("api/checkout")
    suspend fun checkout(@Body request: CheckoutRequestDto): CheckoutResponseDto
}
