package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentShippingLabelCreateServicePackageBinding
import com.woocommerce.android.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShippingLabelCreateServicePackageFragment :
    BaseFragment(R.layout.fragment_shipping_label_create_service_package) {

    private val parentViewModel: ShippingLabelCreatePackageViewModel by viewModels({ requireParentFragment() })

    val viewModel: ShippingLabelCreateServicePackageViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentShippingLabelCreateServicePackageBinding.bind(view)
        val adapter = ShippingLabelCreateServicePackageAdapter()
    }
}
