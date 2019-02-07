package com.woocommerce.android.ui.prefs

import android.content.Context
import android.content.Intent
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tooltip.Tooltip
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.SETTINGS_ABOUT_OPEN_SOURCE_LICENSES_LINK_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.SETTINGS_ABOUT_WOOCOMMERCE_LINK_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.SETTINGS_LOGOUT_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.SETTINGS_NOTIFICATIONS_OPEN_CHANNEL_SETTINGS_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.SETTINGS_PRIVACY_SETTINGS_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.SETTING_CHANGE
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_settings_main.*
import javax.inject.Inject

class MainSettingsFragment : Fragment(), MainSettingsContract.View {
    companion object {
        const val TAG = "main-settings"
        private const val SETTING_NOTIFS_ORDERS = "notifications_orders"
        private const val SETTING_NOTIFS_REVIEWS = "notifications_reviews"
        private const val SETTING_NOTIFS_TONE = "notifications_tone"

        private const val TOOLTIP_DELAY = 2000L

        fun newInstance(): MainSettingsFragment {
            return MainSettingsFragment()
        }
    }

    @Inject lateinit var presenter: MainSettingsContract.Presenter

    interface AppSettingsListener {
        fun onRequestLogout()
        fun onRequestShowPrivacySettings()
        fun onRequestShowAbout()
        fun onRequestShowLicenses()
        fun onRequestShowSitePicker()
    }

    private lateinit var listener: AppSettingsListener

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings_main, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (activity is AppSettingsListener) {
            listener = activity as AppSettingsListener
        } else {
            throw ClassCastException(context.toString() + " must implement AppSettingsListener")
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        updateStoreViews()

        buttonLogout.setOnClickListener {
            AnalyticsTracker.track(SETTINGS_LOGOUT_BUTTON_TAPPED)
            listener.onRequestLogout()
        }

        // on API 26+ we show the device notification settings, on older devices we have in-app settings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notifsContainerOlder.visibility = View.GONE
            notifsContainerNewer.visibility = View.VISIBLE
            notifsContainerNewer.setOnClickListener {
                AnalyticsTracker.track(SETTINGS_NOTIFICATIONS_OPEN_CHANNEL_SETTINGS_BUTTON_TAPPED)
                showDeviceAppNotificationSettings()
            }
        } else {
            notifsContainerOlder.visibility = View.VISIBLE
            notifsContainerNewer.visibility = View.GONE

            switchNotifsOrders.isChecked = AppPrefs.isOrderNotificationsEnabled()
            switchNotifsOrders.setOnCheckedChangeListener { _, isChecked ->
                trackSettingToggled(SETTING_NOTIFS_ORDERS, isChecked)
                AppPrefs.setOrderNotificationsEnabled(isChecked)
                switchNotifsTone.isEnabled = isChecked
            }

            switchNotifsReviews.isChecked = AppPrefs.isReviewNotificationsEnabled()
            switchNotifsReviews.setOnCheckedChangeListener { _, isChecked ->
                trackSettingToggled(SETTING_NOTIFS_REVIEWS, isChecked)
                AppPrefs.setReviewNotificationsEnabled(isChecked)
            }

            switchNotifsTone.isChecked = AppPrefs.isOrderNotificationsChaChingEnabled()
            switchNotifsTone.isEnabled = AppPrefs.isOrderNotificationsEnabled()
            switchNotifsTone.setOnCheckedChangeListener { _, isChecked ->
                trackSettingToggled(SETTING_NOTIFS_TONE, isChecked)
                AppPrefs.setOrderNotificationsChaChingEnabled(isChecked)
            }
        }

        textPrivacySettings.setOnClickListener {
            AnalyticsTracker.track(SETTINGS_PRIVACY_SETTINGS_BUTTON_TAPPED)
            listener.onRequestShowPrivacySettings()
        }

        textAbout.setOnClickListener {
            AnalyticsTracker.track(SETTINGS_ABOUT_WOOCOMMERCE_LINK_TAPPED)
            listener.onRequestShowAbout()
        }

        textLicenses.setOnClickListener {
            AnalyticsTracker.track(SETTINGS_ABOUT_OPEN_SOURCE_LICENSES_LINK_TAPPED)
            listener.onRequestShowLicenses()
        }

        // TODO: for now, showing the site picker is only enabled for debug builds
        if (BuildConfig.DEBUG && presenter.hasMultipleStores()) {
            primaryStoreView.setOnClickListener {
                listener.onRequestShowSitePicker()
            }

            showSitePickerTooltipDelayed()
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)

        activity?.setTitle(R.string.settings)
    }

    /**
     * Shows the device's notification settings for this app - only implemented for API 26+ since we only call
     * this on API 26+ devices (will do nothing on older devices)
     */
    override fun showDeviceAppNotificationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent()
            intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
            intent.putExtra("android.provider.extra.APP_PACKAGE", activity?.getPackageName())
            activity?.startActivity(intent)
        }
    }

    fun updateStoreViews() {
        textPrimaryStoreDomain.text = presenter.getStoreDomainName()
        textPrimaryStoreUsername.text = presenter.getUserDisplayName()
    }

    /**
     * Called when a boolean setting is changed so we can track it
     */
    private fun trackSettingToggled(keyName: String, newValue: Boolean) {
        AnalyticsTracker.track(
                SETTING_CHANGE, mapOf(
                AnalyticsTracker.KEY_NAME to keyName,
                AnalyticsTracker.KEY_FROM to !newValue,
                AnalyticsTracker.KEY_TO to newValue)
        )
    }


    private fun showSitePickerTooltipDelayed() {
        Handler().postDelayed({
            if (isAdded) {
                showSitePickerTooltip()
            }
        }, TOOLTIP_DELAY)
    }

    private fun showSitePickerTooltip() {
        val anchorView = primaryStoreView
        val bgColor = ContextCompat.getColor(activity as Context, R.color.wc_purple)
        val textColor = ContextCompat.getColor(activity as Context, R.color.white)
        val padding = resources.getDimensionPixelSize(R.dimen.margin_large)

        val tooltip = Tooltip.Builder(anchorView)
                .setBackgroundColor(bgColor)
                .setTextColor(textColor)
                .setPadding(padding)
                .setGravity(Gravity.BOTTOM)
                .setText("You can tap to switch sites now!")
                .show()

        anchorView.isPressed = true

        Handler().postDelayed({
            if (isAdded) {
                anchorView.isPressed = false
                tooltip.dismiss()
            }
        }, TOOLTIP_DELAY)
    }
}
