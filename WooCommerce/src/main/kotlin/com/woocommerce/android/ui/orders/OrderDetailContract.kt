package com.woocommerce.android.ui.orders

import android.content.Context
import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderNoteModel
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier

interface OrderDetailContract {
    interface Presenter : BasePresenter<View> {
        var orderModel: WCOrderModel?
        var orderIdentifier: OrderIdentifier?
        var isUsingCachedNotes: Boolean
        var isUsingCachedShipmentTrackings: Boolean
        var isShipmentTrackingsFetched: Boolean
        var deletedOrderShipmentTrackingModel: WCOrderShipmentTrackingModel?
        fun fetchOrder(remoteOrderId: Long)
        fun loadOrderDetailFromDb(orderIdentifier: OrderIdentifier): WCOrderModel?
        fun loadOrderDetail(orderIdentifier: OrderIdentifier, markComplete: Boolean)
        fun loadOrderNotes()
        fun fetchOrderNotesFromDb(order: WCOrderModel): List<WCOrderNoteModel>
        fun fetchAndLoadOrderNotesFromDb()
        fun loadOrderShipmentTrackings()
        fun getOrderShipmentTrackingsFromDb(order: WCOrderModel): List<WCOrderShipmentTrackingModel>
        fun loadShipmentTrackingsFromDb()
        fun doChangeOrderStatus(newStatus: String)
        fun markOrderNotificationRead(context: Context, remoteNoteId: Long)
        fun getOrderStatusForStatusKey(key: String): WCOrderStatusModel
        fun getOrderStatusOptions(): Map<String, WCOrderStatusModel>
        fun refreshOrderStatusOptions()
        fun deleteOrderShipmentTracking(wcOrderShipmentTrackingModel: WCOrderShipmentTrackingModel)
        fun pushShipmentTrackingRecord(
            wcOrderShipmentTrackingModel: WCOrderShipmentTrackingModel,
            isCustomProvider: Boolean
        )
    }

    interface View : BaseView<Presenter>, OrderActionListener, OrderProductActionListener,
            OrderShipmentTrackingActionListener {
        fun showOrderDetail(order: WCOrderModel?)
        fun showOrderNotes(notes: List<WCOrderNoteModel>)
        fun showOrderNotesSkeleton(show: Boolean)
        fun showAddOrderNoteScreen(order: WCOrderModel)
        fun updateOrderNotes(notes: List<WCOrderNoteModel>)
        fun showOrderShipmentTrackings(trackings: List<WCOrderShipmentTrackingModel>)
        fun setOrderStatus(newStatus: String)
        fun showChangeOrderStatusSnackbar(newStatus: String)
        fun showNotesErrorSnack()
        fun showAddOrderNoteErrorSnack()
        fun showOrderStatusChangedError()
        fun markOrderStatusChangedSuccess()
        fun markOrderStatusChangedFailed()
        fun showLoadOrderProgress(show: Boolean)
        fun showLoadOrderError()
        fun refreshOrderStatus()
        fun refreshProductImages()
        fun undoDeletedTrackingOnError(wcOrderShipmentTrackingModel: WCOrderShipmentTrackingModel?)
        fun markTrackingDeletedOnSuccess()
        fun showDeleteTrackingErrorSnack()
        fun showAddShipmentTrackingSnack()
        fun showAddAddShipmentTrackingErrorSnack()
    }
}
