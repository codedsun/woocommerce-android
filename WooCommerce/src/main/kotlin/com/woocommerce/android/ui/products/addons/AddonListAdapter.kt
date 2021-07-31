package com.woocommerce.android.ui.products.addons

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.AddonItemBinding
import com.woocommerce.android.model.ProductAddon

class AddonListAdapter(
    val addons: List<ProductAddon>
) : RecyclerView.Adapter<AddonListAdapter.AddonsViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        AddonsViewHolder(
            AddonItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: AddonsViewHolder, position: Int) {
        holder.bind(addons[position])
    }

    override fun getItemCount() = addons.size

    inner class AddonsViewHolder(
        val viewBinding: AddonItemBinding
    ) : RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(addon: ProductAddon) {
            viewBinding.addonName.text = addon.name
        }
    }
}
