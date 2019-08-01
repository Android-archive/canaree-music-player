package dev.olog.presentation.main

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dev.olog.appshortcuts.Shortcuts
import dev.olog.core.MediaId
import dev.olog.intents.AppConstants
import dev.olog.intents.Classes
import dev.olog.intents.FloatingWindowsConstants
import dev.olog.intents.MusicServiceAction
import dev.olog.presentation.FloatingWindowHelper
import dev.olog.presentation.R
import dev.olog.presentation.folder.tree.FolderTreeFragment
import dev.olog.presentation.interfaces.*
import dev.olog.presentation.library.LibraryFragment
import dev.olog.presentation.main.di.inject
import dev.olog.presentation.model.BottomNavigationPage
import dev.olog.presentation.model.PresentationPreferencesGateway
import dev.olog.presentation.navigator.Navigator
import dev.olog.presentation.pro.HasBilling
import dev.olog.presentation.pro.IBilling
import dev.olog.presentation.rateapp.RateAppDialog
import dev.olog.presentation.utils.collapse
import dev.olog.presentation.utils.expand
import dev.olog.presentation.utils.isExpanded
import dev.olog.scrollhelper.MultiListenerBottomSheetBehavior
import dev.olog.scrollhelper.ScrollType
import dev.olog.shared.android.extensions.*
import dev.olog.shared.android.theme.hasPlayerAppearance
import dev.olog.shared.lazyFast
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject


class MainActivity : MusicGlueActivity(),
    HasSlidingPanel,
    HasBilling,
    HasBottomNavigation,
    OnPermissionChanged {

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    private val viewModel by lazyFast {
        viewModelProvider<MainActivityViewModel>(
            factory
        )
    }
    @Inject
    lateinit var navigator: Navigator
    // handles lifecycle itself
    @Inject
    override lateinit var billing: IBilling

    @Inject
    lateinit var presentationPrefs: PresentationPreferencesGateway

    @Suppress("unused")
    @Inject
    lateinit var statusBarColorBehavior: StatusBarColorBehavior
    @Suppress("unused")
    @Inject
    lateinit var rateAppDialog: RateAppDialog

    private lateinit var scrollHelper: SuperCerealScrollHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        inject()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tryLoadAd()

        bottomNavigation.presentationPrefs = presentationPrefs

        if (hasPlayerAppearance().isMini()){
            // TODO made a resource value
            slidingPanelFade.parallax = 0
            slidingPanel.setHeight(dip(300))
        }

        scrollHelper = SuperCerealScrollHelper(
            this, ScrollType.Full(
                slidingPanel = slidingPanel,
                bottomNavigation = bottomWrapper,
                toolbarHeight = dimen(R.dimen.toolbar),
                tabLayoutHeight = dimen(R.dimen.tab)
            )
        )

        when {
            viewModel.isFirstAccess() -> {
                navigator.toFirstAccess()
                return
            }
            savedInstanceState == null -> navigateToLastPage()
        }

        intent?.let { handleIntent(it) }
    }

    override fun onPermissionGranted(permission: Permission) = when (permission){
        Permission.STORAGE -> {
            navigateToLastPage()
            connect()
        }
    }

    private fun navigateToLastPage(){
        bottomNavigation.navigateToLastPage()
    }

    override fun onResume() {
        super.onResume()
        scrollHelper.onAttach()
    }

    override fun onPause() {
        super.onPause()
        scrollHelper.onDetach()
    }

    override fun onDestroy() {
        super.onDestroy()
        scrollHelper.dispose()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            FloatingWindowsConstants.ACTION_START_SERVICE -> {
                FloatingWindowHelper.startServiceIfHasOverlayPermission(this)
            }
            Shortcuts.SEARCH -> bottomNavigation.navigate(BottomNavigationPage.SEARCH)
            AppConstants.ACTION_CONTENT_VIEW -> getSlidingPanel().expand()
            MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH -> {
                val serviceIntent = Intent(this, Class.forName(Classes.SERVICE_MUSIC))
                serviceIntent.action = MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH
                ContextCompat.startForegroundService(this, serviceIntent)
            }
            Shortcuts.DETAIL -> {
                val string = intent.getStringExtra(Shortcuts.DETAIL_EXTRA_ID)!!
                val mediaId = MediaId.fromString(string)
                navigator.toDetailFragment(mediaId)
            }
            Intent.ACTION_VIEW -> {
                val serviceIntent = Intent(this, Class.forName(Classes.SERVICE_MUSIC))
                serviceIntent.action = MusicServiceAction.PLAY_URI.name
                serviceIntent.data = intent.data
                ContextCompat.startForegroundService(this, serviceIntent)
            }
        }
        setIntent(null)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FloatingWindowHelper.REQUEST_CODE_HOVER_PERMISSION) {
            FloatingWindowHelper.startServiceIfHasOverlayPermission(this)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onBackPressed() {
        try {
            val topFragment = supportFragmentManager.getTopFragment()

            when {
                topFragment is CanHandleOnBackPressed && topFragment.handleOnBackPressed()-> {
                    return
                }
                topFragment is DrawsOnTop -> {
                    super.onBackPressed()
                    return
                }
                getSlidingPanel().isExpanded() -> {
                    getSlidingPanel().collapse()
                    return
                }
            }
            if (tryPopFolderBack()) {
                return
            }

            super.onBackPressed()
        } catch (ex: IllegalStateException) {
            /*random fragment manager crashes */
            ex.printStackTrace()
        }

    }

    private fun tryPopFolderBack(): Boolean {
        val categoriesFragment =
            supportFragmentManager.findFragmentByTag(LibraryFragment.TAG_TRACK) as? LibraryFragment ?: return false

        if (categoriesFragment.isCurrentFragmentFolderTree()){
            val folderTree = categoriesFragment.childFragmentManager.fragments
                .find { it is FolderTreeFragment } as? CanHandleOnBackPressed
            return folderTree?.handleOnBackPressed() == true
        }
        return false
    }

    override fun getSlidingPanel(): MultiListenerBottomSheetBehavior<*> {
        return BottomSheetBehavior.from(slidingPanel) as MultiListenerBottomSheetBehavior<*>
    }

    override fun navigate(page: BottomNavigationPage) {
        bottomNavigation.navigate(page)
    }

    fun restoreSlidingPanelHeight(){
        bottomWrapper.animate().translationY(0f).setDuration(100)
        getSlidingPanel().peekHeight = dimen(R.dimen.sliding_panel_peek) + dimen(R.dimen.bottom_navigation_height)
    }

    private fun tryLoadAd(){
        if (viewModel.canShowAds()){
            MobileAds.initialize(this, getString(R.string.ad_mob_key))
            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)
        } else {
            adView.setHeight(0)
        }
    }
}