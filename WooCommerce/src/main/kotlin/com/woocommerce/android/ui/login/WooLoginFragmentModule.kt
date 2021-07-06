package com.woocommerce.android.ui.login

import com.woocommerce.android.di.FragmentScope
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import org.wordpress.android.login.di.LoginFragmentModule

@Module(includes = [LoginFragmentModule::class])
@InstallIn(ActivityComponent::class)
internal abstract class WooLoginFragmentModule {
    @ContributesAndroidInjector
    internal abstract fun loginPrologueFragment(): LoginPrologueFragment

    @FragmentScope
    @ContributesAndroidInjector(modules = [LoginNoJetpackModule::class])
    internal abstract fun loginNoJetpackFragment(): LoginNoJetpackFragment

    @FragmentScope
    @ContributesAndroidInjector(modules = [MagicLinkInterceptModule::class])
    internal abstract fun magicLinkInterceptFragment(): MagicLinkInterceptFragment

    @FragmentScope
    @ContributesAndroidInjector
    internal abstract fun loginEmailHelpDialogFragment(): LoginEmailHelpDialogFragment

    @FragmentScope
    @ContributesAndroidInjector
    internal abstract fun loginDiscoveryErrorFragment(): LoginDiscoveryErrorFragment

    @FragmentScope
    @ContributesAndroidInjector
    internal abstract fun loginNoWPcomAccountFoundFragment(): LoginNoWPcomAccountFoundFragment

    @FragmentScope
    @ContributesAndroidInjector
    internal abstract fun loginSiteCheckErrorFragment(): LoginSiteCheckErrorFragment
}
