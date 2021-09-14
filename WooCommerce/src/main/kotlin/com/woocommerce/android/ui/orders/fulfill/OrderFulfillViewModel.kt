package com.woocommerce.android.ui.orders.fulfill

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.Callback
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_TRACKING_ADD
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Order.Item
import com.woocommerce.android.model.OrderShipmentTracking
import com.woocommerce.android.model.getNonRefundedProducts
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.orders.OrderNavigationTarget.AddOrderShipmentTracking
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.details.OrderDetailViewModel
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.*
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.order.OrderIdSet
import org.wordpress.android.fluxc.model.order.toIdSet
import javax.inject.Inject

@OpenClassOnDebug
@HiltViewModel
class OrderFulfillViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val appPrefs: AppPrefs,
    private val networkStatus: NetworkStatus,
    private val resourceProvider: ResourceProvider,
    private val repository: OrderDetailRepository
) : ScopedViewModel(savedState) {
    companion object {
        const val KEY_ORDER_FULFILL_RESULT = "key_order_fulfill_result"
        const val KEY_REFRESH_SHIPMENT_TRACKING_RESULT = "key_refresh_shipment_tracking_result"
    }

    private val navArgs: OrderFulfillFragmentArgs by savedState.navArgs()

    final val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    private val orderIdSet: OrderIdSet
        get() = navArgs.orderIdentifier.toIdSet()

    private val _productList = MutableLiveData<List<Item>>()
    val productList: LiveData<List<Item>> = _productList

    private val _shipmentTrackings = MutableLiveData<List<OrderShipmentTracking>>()
    val shipmentTrackings: LiveData<List<OrderShipmentTracking>> = _shipmentTrackings

    // Keep track of the deleted shipment tracking number in case
    // the request to server fails, we need to display an error message
    // and add the deleted tracking number back to the list
    private var deletedOrderShipmentTrackingSet = mutableSetOf<String>()

    final var order: Order
        get() = requireNotNull(viewState.order)
        set(value) {
            viewState = viewState.copy(
                order = value
            )
        }

    init {
        start()
    }

    final fun start() {
        val order = repository.getOrder(navArgs.orderIdentifier)
        order?.let {
            displayOrderDetails(it)
            displayOrderProducts(it)
            displayShipmentTrackings()
        }
    }

    private fun displayOrderDetails(order: Order) {
        viewState = viewState.copy(
            order = order,
            toolbarTitle = resourceProvider.getString(R.string.order_fulfill_title)
        )
    }

    private fun displayOrderProducts(order: Order) {
        val products = repository.getOrderRefunds(orderIdSet.remoteOrderId).getNonRefundedProducts(order.items)
        _productList.value = products
    }

    private fun displayShipmentTrackings() {
        val isShippingLabelAvailable = repository.getOrderShippingLabels(orderIdSet.remoteOrderId).isNotEmpty()
        val trackingAvailable = appPrefs.isTrackingExtensionAvailable() &&
            !hasVirtualProductsOnly() && !isShippingLabelAvailable
        viewState = viewState.copy(isShipmentTrackingAvailable = trackingAvailable)
        if (trackingAvailable) {
            _shipmentTrackings.value = repository.getOrderShipmentTrackings(orderIdSet.id)
        }
    }

    fun hasVirtualProductsOnly(): Boolean {
        return if (order.items.isNotEmpty()) {
            val remoteProductIds = order.getProductIds()
            repository.hasVirtualProductsOnly(remoteProductIds)
        } else false
    }

    fun onMarkOrderCompleteButtonClicked() {
        if (networkStatus.isConnected()) {
            triggerEvent(
                ExitWithResult(
                    data = OrderDetailViewModel.OrderStatusUpdateSource.FullFillScreen(oldStatus = order.status.value),
                    key = KEY_ORDER_FULFILL_RESULT
                )
            )
        } else {
            triggerEvent(ShowSnackbar(string.offline_error))
        }
    }

    fun onAddShipmentTrackingClicked() {
        triggerEvent(
            AddOrderShipmentTracking(
                orderIdentifier = order.identifier,
                orderTrackingProvider = appPrefs.getSelectedShipmentTrackingProviderName(),
                isCustomProvider = appPrefs.getIsSelectedShipmentTrackingProviderCustom()
            )
        )
    }

    fun onNewShipmentTrackingAdded(shipmentTracking: OrderShipmentTracking) {
        AnalyticsTracker.track(
            ORDER_TRACKING_ADD,
            mapOf(
                AnalyticsTracker.KEY_ID to order.remoteId,
                AnalyticsTracker.KEY_STATUS to order.status,
                AnalyticsTracker.KEY_CARRIER to shipmentTracking.trackingProvider
            )
        )
        viewState = viewState.copy(shouldRefreshShipmentTracking = true)
        _shipmentTrackings.value = repository.getOrderShipmentTrackings(orderIdSet.id)
    }

    fun onDeleteShipmentTrackingClicked(trackingNumber: String) {
        if (networkStatus.isConnected()) {
            repository.getOrderShipmentTrackingByTrackingNumber(
                orderIdSet.id, trackingNumber
            )?.let { deletedShipmentTracking ->
                deletedOrderShipmentTrackingSet.add(trackingNumber)

                val shipmentTrackings = _shipmentTrackings.value?.toMutableList() ?: mutableListOf()
                shipmentTrackings.remove(deletedShipmentTracking)
                _shipmentTrackings.value = shipmentTrackings

                triggerEvent(
                    ShowUndoSnackbar(
                        message = resourceProvider.getString(string.order_shipment_tracking_delete_snackbar_msg),
                        undoAction = { onDeleteShipmentTrackingReverted(deletedShipmentTracking) },
                        dismissAction = object : Callback() {
                            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                                super.onDismissed(transientBottomBar, event)
                                if (event != DISMISS_EVENT_ACTION) {
                                    // delete the shipment only if user has not clicked on the undo snackbar
                                    deleteOrderShipmentTracking(deletedShipmentTracking)
                                }
                            }
                        }
                    )
                )
            }
        } else {
            triggerEvent(ShowSnackbar(string.offline_error))
        }
    }

    private fun onDeleteShipmentTrackingReverted(shipmentTracking: OrderShipmentTracking) {
        deletedOrderShipmentTrackingSet.remove(shipmentTracking.trackingNumber)
        val shipmentTrackings = _shipmentTrackings.value?.toMutableList() ?: mutableListOf()
        shipmentTrackings.add(shipmentTracking)
        _shipmentTrackings.value = shipmentTrackings
    }

    private fun deleteOrderShipmentTracking(shipmentTracking: OrderShipmentTracking) {
        launch {
            val deletedShipment = repository.deleteOrderShipmentTracking(
                orderIdSet.id, orderIdSet.remoteOrderId, shipmentTracking.toDataModel()
            )
            if (deletedShipment) {
                viewState = viewState.copy(shouldRefreshShipmentTracking = true)
                triggerEvent(ShowSnackbar(string.order_shipment_tracking_delete_success))
            } else {
                onDeleteShipmentTrackingReverted(shipmentTracking)
                triggerEvent(ShowSnackbar(string.order_shipment_tracking_delete_error))
            }
        }
    }

    fun onBackButtonClicked() {
        if (viewState.shouldRefreshShipmentTracking) {
            triggerEvent(ExitWithResult(true, key = KEY_REFRESH_SHIPMENT_TRACKING_RESULT))
        } else triggerEvent(Exit)
    }

    @Parcelize
    data class ViewState(
        val order: Order? = null,
        val toolbarTitle: String? = null,
        val isShipmentTrackingAvailable: Boolean? = null,
        val shouldRefreshShipmentTracking: Boolean = false
    ) : Parcelable
}
