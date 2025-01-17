package com.woocommerce.android.ui.sitepicker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.AppUrls
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentSitePickerBinding
import com.woocommerce.android.extensions.handleNotice
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.support.HelpActivity
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewFragment
import com.woocommerce.android.ui.login.LoginActivity
import com.woocommerce.android.ui.login.LoginEmailHelpDialogFragment
import com.woocommerce.android.ui.login.LoginWhatIsJetpackDialogFragment
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerEvent.NavigateToEmailHelpDialogEvent
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerEvent.NavigateToMainActivityEvent
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerEvent.NavigateToWPComWebView
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerEvent.NavigationToHelpFragmentEvent
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerEvent.NavigationToLearnMoreAboutJetpackEvent
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerEvent.NavigationToWhatIsJetpackFragmentEvent
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerEvent.ShowWooUpgradeDialogEvent
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerState.AccountMismatchState
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerState.NoStoreState
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerState.StoreListState
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerState.WooNotFoundState
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Logout
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.widgets.CustomProgressDialog
import com.woocommerce.android.widgets.SkeletonView
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.login.LoginMode
import javax.inject.Inject

@AndroidEntryPoint
class SitePickerFragment : BaseFragment(R.layout.fragment_site_picker), LoginEmailHelpDialogFragment.Listener {
    private var _binding: FragmentSitePickerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SitePickerViewModel by viewModels()
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private var skeletonView = SkeletonView()
    private var progressDialog: CustomProgressDialog? = null

    override val activityAppBarStatus: AppBarStatus
        get() = if (viewModel.shouldShowToolbar) AppBarStatus.Visible() else AppBarStatus.Hidden

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentSitePickerBinding.bind(view)

        setupViews()
        setupObservers(viewModel)
        handleResults()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupViews() {
        binding.buttonHelp.setOnClickListener { viewModel.onHelpButtonClick() }
        binding.sitesRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            adapter = SitePickerAdapter(viewModel::onSiteSelected, viewModel::onNonWooSiteSelected)
        }
        binding.loginEpilogueButtonBar.buttonSecondary.setOnClickListener {
            viewModel.onTryAnotherAccountButtonClick()
        }
    }

    @Suppress("LongMethod", "ComplexMethod")
    private fun setupObservers(viewModel: SitePickerViewModel) {
        viewModel.sitePickerViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.userInfo?.takeIfNotEqualTo(old?.userInfo) {
                updateUserInfoView(it)
            }
            new.isHelpBtnVisible.takeIfNotEqualTo(old?.isHelpBtnVisible) {
                binding.buttonHelp.isVisible = it
            }
            new.isPrimaryBtnVisible.takeIfNotEqualTo(old?.isPrimaryBtnVisible) {
                binding.loginEpilogueButtonBar.buttonPrimary.isVisible = it
            }
            new.isSecondaryBtnVisible.takeIfNotEqualTo(old?.isSecondaryBtnVisible) {
                binding.loginEpilogueButtonBar.buttonSecondary.isVisible = it
            }
            new.isNoStoresViewVisible.takeIfNotEqualTo(old?.isNoStoresViewVisible) {
                binding.sitesRecycler.isVisible = !it
                binding.noStoresView.isVisible = it
            }
            new.toolbarTitle?.takeIfNotEqualTo(old?.toolbarTitle) {
                activity?.title = it
            }
            new.primaryBtnText?.takeIfNotEqualTo(old?.primaryBtnText) {
                binding.loginEpilogueButtonBar.buttonPrimary.text = it
            }
            new.secondaryBtnText?.takeIfNotEqualTo(old?.secondaryBtnText) {
                binding.loginEpilogueButtonBar.buttonSecondary.text = it
            }
            new.noStoresBtnText?.takeIfNotEqualTo(old?.noStoresBtnText) {
                binding.noStoresView.noStoresBtnText = it
            }
            new.noStoresLabelText?.takeIfNotEqualTo(old?.noStoresLabelText) {
                binding.noStoresView.noStoresText = it
            }
            new.isSkeletonViewVisible.takeIfNotEqualTo(old?.isSkeletonViewVisible) {
                updateSkeletonView(it)
            }
            new.isProgressDiaLogVisible.takeIfNotEqualTo(old?.isProgressDiaLogVisible) {
                showProgressDialog(it)
            }
            new.currentSitePickerState.takeIfNotEqualTo(old?.currentSitePickerState) {
                when (it) {
                    StoreListState -> updateStoreListView()
                    NoStoreState -> updateNoStoresView()
                    AccountMismatchState -> updateAccountMismatchView()
                    WooNotFoundState -> updateWooNotFoundView()
                }
            }
        }

        viewModel.sites.observe(viewLifecycleOwner) {
            (binding.sitesRecycler.adapter as? SitePickerAdapter)?.submitList(it)
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is NavigateToMainActivityEvent -> (activity as? MainActivity)?.handleSitePickerResult()
                is ShowWooUpgradeDialogEvent -> showWooUpgradeDialog()
                is NavigationToHelpFragmentEvent -> navigateToHelpScreen()
                is NavigationToWhatIsJetpackFragmentEvent -> navigateToWhatIsJetpackScreen()
                is NavigationToLearnMoreAboutJetpackEvent -> navigateToLearnMoreAboutJetpackScreen()
                is NavigateToEmailHelpDialogEvent -> navigateToNeedHelpFindingEmailScreen()
                is NavigateToWPComWebView -> navigateToWPComWebView(event)
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is Logout -> onLogout()
                is Exit -> findNavController().navigateUp()
                else -> event.isHandled = false
            }
        }
    }

    private fun handleResults() {
        handleNotice(WPComWebViewFragment.WEBVIEW_RESULT) {
            AnalyticsTracker.track(AnalyticsEvent.LOGIN_WOOCOMMERCE_SETUP_COMPLETED)
            viewModel.onWooInstalled()
        }
        handleNotice(WPComWebViewFragment.WEBVIEW_DISMISSED) {
            AnalyticsTracker.track(AnalyticsEvent.LOGIN_WOOCOMMERCE_SETUP_DISMISSED)
        }
    }

    private fun updateStoreListView() {
        binding.loginEpilogueButtonBar.buttonPrimary.setOnClickListener {
            viewModel.onContinueButtonClick()
        }
    }

    private fun updateNoStoresView() {
        binding.loginEpilogueButtonBar.buttonPrimary.setOnClickListener {
            viewModel.onLearnMoreAboutJetpackButtonClick()
        }
        binding.noStoresView.clickSecondaryAction {
            viewModel.onWhatIsJetpackButtonClick()
        }
    }

    private fun updateAccountMismatchView() {
        binding.loginEpilogueButtonBar.buttonPrimary.setOnClickListener {
            viewModel.onViewConnectedStoresButtonClick()
        }
        binding.noStoresView.clickSecondaryAction {
            viewModel.onNeedHelpFindingEmailButtonClick()
        }
    }

    private fun updateWooNotFoundView() {
        binding.loginEpilogueButtonBar.buttonPrimary.setOnClickListener {
            AnalyticsTracker.track(AnalyticsEvent.LOGIN_WOOCOMMERCE_SETUP_BUTTON_TAPPED)
            viewModel.onInstallWooClicked()
        }
        binding.noStoresView.clickSecondaryAction {
            viewModel.onViewConnectedStoresButtonClick()
        }
    }

    private fun updateUserInfoView(userInfo: SitePickerViewModel.UserInfo) {
        binding.loginUserInfo.displayName = userInfo.displayName
        binding.loginUserInfo.userName = userInfo.username
        binding.loginUserInfo.avatarUrl(userInfo.userAvatarUrl)
    }

    private fun updateSkeletonView(visible: Boolean) {
        when (visible) {
            true -> {
                skeletonView.show(binding.sitePickerRoot, R.layout.skeleton_site_picker, delayed = true)
            }
            false -> skeletonView.hide()
        }
    }

    private fun showProgressDialog(show: Boolean) {
        progressDialog?.dismiss()
        if (show) {
            progressDialog = CustomProgressDialog.show(
                getString(R.string.login_verifying_site),
                getString(R.string.product_update_dialog_message)
            ).also {
                it.show(parentFragmentManager, CustomProgressDialog.TAG)
            }
            progressDialog?.isCancelable = false
        }
    }

    private fun showWooUpgradeDialog() {
        WooUpgradeRequiredDialog.show().also {
            it.show(parentFragmentManager, WooUpgradeRequiredDialog.TAG)
        }
    }

    private fun navigateToWhatIsJetpackScreen() {
        LoginWhatIsJetpackDialogFragment().show(parentFragmentManager, LoginWhatIsJetpackDialogFragment.TAG)
    }

    private fun navigateToLearnMoreAboutJetpackScreen() {
        ChromeCustomTabUtils.launchUrl(requireContext(), AppUrls.JETPACK_INSTRUCTIONS)
    }

    private fun navigateToHelpScreen() {
        startActivity(HelpActivity.createIntent(requireContext(), HelpActivity.Origin.LOGIN_EPILOGUE, null))
    }

    private fun navigateToNeedHelpFindingEmailScreen() {
        LoginEmailHelpDialogFragment.newInstance(this).also {
            it.show(parentFragmentManager, LoginEmailHelpDialogFragment.TAG)
        }
    }

    private fun navigateToWPComWebView(event: NavigateToWPComWebView) {
        findNavController().navigate(
            NavGraphMainDirections.actionGlobalWPComWebViewFragment(
                urlToLoad = event.url,
                urlToTriggerExit = event.validationUrl
            )
        )
    }

    private fun onLogout() {
        activity?.setResult(Activity.RESULT_CANCELED)
        val intent = Intent(context, LoginActivity::class.java)
        LoginMode.WOO_LOGIN_MODE.putInto(intent)
        startActivity(intent)
        activity?.finish()
    }

    override fun onEmailNeedMoreHelpClicked() {
        startActivity(HelpActivity.createIntent(requireContext(), HelpActivity.Origin.LOGIN_CONNECTED_EMAIL_HELP, null))
    }
}
