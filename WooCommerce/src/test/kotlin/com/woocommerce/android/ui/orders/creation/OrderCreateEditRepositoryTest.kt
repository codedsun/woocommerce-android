package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.OrderUpdateStore
import java.math.BigDecimal

@ExperimentalCoroutinesApi
class OrderCreateEditRepositoryTest : BaseUnitTest() {
    companion object {
        const val DEFAULT_ERROR_MESSAGE = "error_message"
    }

    private lateinit var sut: OrderCreateEditRepository
    private lateinit var trackerWrapper: AnalyticsTrackerWrapper
    private lateinit var orderUpdateStore: OrderUpdateStore
    private lateinit var selectedSite: SelectedSite

    @Before
    fun setUp() {
        trackerWrapper = mock()

        val siteModel = SiteModel()
        selectedSite = mock {
            on { get() } doReturn siteModel
        }

        orderUpdateStore = mock {
            onBlocking {
                createSimplePayment(eq(siteModel), eq("1"), eq(true), eq(null))
            } doReturn WooResult(
                WooError(WooErrorType.API_ERROR, BaseRequest.GenericErrorType.NETWORK_ERROR, DEFAULT_ERROR_MESSAGE)
            )
        }

        sut = OrderCreateEditRepository(
            selectedSite = selectedSite,
            orderStore = mock(),
            orderUpdateStore = orderUpdateStore,
            orderMapper = mock(),
            dispatchers = coroutinesTestRule.testDispatchers,
            wooCommerceStore = mock(),
            analyticsTrackerWrapper = trackerWrapper
        )
    }

    @Test
    fun `given simple payment order created, when error, then error track event is tracked`() = testBlocking {
        sut.createSimplePaymentOrder(BigDecimal.ONE)

        verify(trackerWrapper).track(
            AnalyticsEvent.PAYMENTS_FLOW_FAILED,
            mapOf(
                AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_SOURCE_AMOUNT,
                AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_FLOW
            )
        )
    }
}
