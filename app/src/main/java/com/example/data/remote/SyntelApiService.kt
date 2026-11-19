package com.example.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SyntelApiService {

    // ── Auth ──
    @POST("api/mobile/auth/login/")
    suspend fun login(@Body body: LoginRequestDto): LoginResponseDto

    @GET("api/mobile/auth/me/")
    suspend fun getMe(@Query("cafe_code") cafeCode: String? = null): MeResponseDto

    @POST("api/mobile/auth/logout/")
    suspend fun logout(): MessageResponseDto

    // ── Requester ──
    @GET("{cafe}/api/mobile/requester/summary/")
    suspend fun getRequesterSummary(@Path("cafe") cafe: String): RequesterSummaryDto

    @GET("{cafe}/api/mobile/requester/products/")
    suspend fun getRequesterProducts(
        @Path("cafe") cafe: String,
        @Query("q") query: String? = null,
    ): ProductsCatalogResponseDto

    @GET("{cafe}/api/mobile/requester/requests/")
    suspend fun getRequesterRequests(@Path("cafe") cafe: String): RequesterRequestsResponseDto

    @POST("{cafe}/api/mobile/requester/requests/")
    suspend fun createRequesterRequest(
        @Path("cafe") cafe: String,
        @Body payload: CreateRequestPayload,
    ): MessageResponseDto

    @PATCH("{cafe}/api/mobile/requester/requests/{id}/")
    suspend fun editRequesterRequest(
        @Path("cafe") cafe: String,
        @Path("id") requestId: Int,
        @Body payload: CreateRequestPayload,
    ): MessageResponseDto

    @GET("{cafe}/api/mobile/requester/received-items/")
    suspend fun getReceivedItems(@Path("cafe") cafe: String): ReceivedItemsResponseDto

    @POST("{cafe}/api/mobile/requester/received-items/confirm/")
    suspend fun confirmReceivedItems(
        @Path("cafe") cafe: String,
        @Body payload: ConfirmReceivedPayload,
    ): MessageResponseDto

    // ── Warehouse Manager / Officer ──
    @GET("{cafe}/api/mobile/warehouse/dashboard/")
    suspend fun getWarehouseDashboard(
        @Path("cafe") cafe: String,
        @Query("day") day: String? = null,
        @Query("department") department: String? = null,
    ): WarehouseDashboardResponseDto

    @POST("{cafe}/api/mobile/warehouse/items/{itemId}/action/")
    suspend fun performWarehouseItemAction(
        @Path("cafe") cafe: String,
        @Path("itemId") itemId: Int,
        @Body payload: WarehouseItemActionPayload,
    ): MessageResponseDto

    @GET("{cafe}/api/mobile/warehouse/draft/")
    suspend fun getWarehouseDraft(@Path("cafe") cafe: String): WarehouseDraftResponseDto

    @POST("{cafe}/api/mobile/warehouse/draft/items/")
    suspend fun addWarehouseDraftItem(
        @Path("cafe") cafe: String,
        @Body payload: AddDraftItemPayload,
    ): MessageResponseDto

    @PATCH("{cafe}/api/mobile/warehouse/draft/items/{itemId}/")
    suspend fun updateWarehouseDraftItem(
        @Path("cafe") cafe: String,
        @Path("itemId") itemId: Int,
        @Body payload: UpdateDraftItemPayload,
    ): MessageResponseDto

    @DELETE("{cafe}/api/mobile/warehouse/draft/items/{itemId}/")
    suspend fun deleteWarehouseDraftItem(
        @Path("cafe") cafe: String,
        @Path("itemId") itemId: Int,
    ): MessageResponseDto

    @POST("{cafe}/api/mobile/warehouse/draft/send/")
    suspend fun sendWarehouseDraft(
        @Path("cafe") cafe: String,
        @Body payload: SendDraftPayload,
    ): MessageResponseDto

    @GET("{cafe}/api/mobile/warehouse/stock/")
    suspend fun getWarehouseStock(
        @Path("cafe") cafe: String,
        @Query("warehouse_id") warehouseId: Int? = null,
        @Query("low_stock") lowStock: Boolean? = null,
        @Query("q") query: String? = null,
    ): WarehouseStockResponseDto

    @GET("{cafe}/api/mobile/warehouse/stock/{stockId}/transactions/")
    suspend fun getStockTransactions(
        @Path("cafe") cafe: String,
        @Path("stockId") stockId: Int,
    ): StockTransactionsResponseDto

    @GET("{cafe}/api/mobile/warehouse/vouchers/")
    suspend fun getWarehouseVouchers(@Path("cafe") cafe: String): VouchersResponseDto

    @POST("{cafe}/api/mobile/warehouse/vouchers/")
    suspend fun createWarehouseVoucher(
        @Path("cafe") cafe: String,
        @Body payload: CreateVoucherPayload,
    ): MessageResponseDto

    // ── Purchasing Officer & Warehouse Self-Buy ──
    @GET("{cafe}/api/mobile/purchasing/dashboard/")
    suspend fun getPurchasingDashboard(
        @Path("cafe") cafe: String,
        @Query("scope") scope: String = "purchasing",
    ): PurchasingDashboardResponseDto

    @POST("{cafe}/api/mobile/purchasing/action/")
    suspend fun performPurchasingAction(
        @Path("cafe") cafe: String,
        @Body payload: PurchasingActionPayload,
    ): MessageResponseDto

    @GET("{cafe}/api/mobile/purchasing/history/")
    suspend fun getPurchasingHistory(
        @Path("cafe") cafe: String,
        @Query("scope") scope: String = "purchasing",
        @Query("status") status: String = "all",
        @Query("q") query: String? = null,
    ): PurchasingHistoryResponseDto
}
