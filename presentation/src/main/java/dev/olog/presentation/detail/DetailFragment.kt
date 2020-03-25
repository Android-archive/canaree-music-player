package dev.olog.presentation.detail

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.*
import androidx.transition.TransitionManager
import dev.olog.media.MediaProvider
import dev.olog.presentation.DottedDividerDecorator
import dev.olog.presentation.PresentationId
import dev.olog.presentation.R
import dev.olog.presentation.animations.FastAutoTransition
import dev.olog.presentation.base.BaseFragment
import dev.olog.presentation.base.adapter.ObservableAdapter
import dev.olog.presentation.base.drag.DragListenerImpl
import dev.olog.presentation.base.drag.IDragListener
import dev.olog.presentation.detail.adapter.*
import dev.olog.presentation.interfaces.CanChangeStatusBarColor
import dev.olog.presentation.interfaces.SetupNestedList
import dev.olog.presentation.model.DisplayableHeader
import dev.olog.presentation.navigator.Navigator
import dev.olog.presentation.utils.removeLightStatusBar
import dev.olog.presentation.utils.setLightStatusBar
import dev.olog.scrollhelper.layoutmanagers.OverScrollLinearLayoutManager
import dev.olog.shared.android.extensions.*
import dev.olog.shared.lazyFast
import kotlinx.android.synthetic.main.fragment_detail.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import kotlin.math.abs
import kotlin.properties.Delegates

class DetailFragment : BaseFragment(),
    CanChangeStatusBarColor,
    SetupNestedList,
    IDragListener by DragListenerImpl() {

    companion object {
        @JvmStatic
        val TAG = DetailFragment::class.java.name
        const val ARGUMENTS_MEDIA_ID = "media_id"
        const val ARGUMENTS_TRANSITION = "transition"

        @JvmStatic
        fun newInstance(mediaId: PresentationId.Category, transition: String): DetailFragment {
            return DetailFragment().withArguments(
                ARGUMENTS_MEDIA_ID to mediaId,
                ARGUMENTS_TRANSITION to transition
            )
        }

    }

    @Inject
    internal lateinit var navigator: Navigator

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel by viewModels<DetailFragmentViewModel> {
        viewModelFactory
    }

    private val mediaId by lazyFast {
        getArgument<PresentationId.Category>(ARGUMENTS_MEDIA_ID)
    }

    private val mostPlayedAdapter by lazyFast {
        DetailMostPlayedAdapter(navigator, requireActivity() as MediaProvider)
    }
    private val recentlyAddedAdapter by lazyFast {
        DetailRecentlyAddedAdapter(navigator, requireActivity() as MediaProvider)
    }
    private val relatedArtistAdapter by lazyFast {
        DetailRelatedArtistsAdapter(navigator)
    }
    private val albumsAdapter by lazyFast {
        DetailSiblingsAdapter(navigator)
    }

    private val adapter by lazyFast {
        DetailFragmentAdapter(
            mediaId = mediaId,
            setupNestedList = this,
            navigator = navigator,
            mediaProvider = requireActivity() as MediaProvider,
            viewModel = viewModel,
            dragListener = this,
            afterImageLoad = { startPostponedEnterTransition() }
        )
    }

    private val recyclerOnScrollListener by lazyFast {
        HeaderVisibilityScrollListener(
            this
        )
    }
    private val recycledViewPool by lazyFast { RecyclerView.RecycledViewPool() }

    internal var hasLightStatusBarColor by Delegates.observable(false) { _, old, new ->
        if (old != new) {
            adjustStatusBarColor(new)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postponeEnterTransition()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.transitionName = requireArguments().getString(ARGUMENTS_TRANSITION)

        list.layoutManager = OverScrollLinearLayoutManager(list)
        list.adapter = adapter
        list.setRecycledViewPool(recycledViewPool)
        list.setHasFixedSize(true)
        list.addItemDecoration(DottedDividerDecorator(
            requireContext(), listOf(
                R.layout.item_detail_header,
                R.layout.item_detail_header_albums,
                R.layout.item_detail_header_all_song,
                R.layout.item_detail_header_recently_added
            )
        ))

        var swipeDirections = ItemTouchHelper.LEFT
        if (adapter.canSwipeRight) {
            swipeDirections = swipeDirections or ItemTouchHelper.RIGHT
        }
        setupDragListener(list, swipeDirections)

        fastScroller.attachRecyclerView(list)
        fastScroller.showBubble(false)

        combine(
            viewModel.songs,
            viewModel.mostPlayed,
            viewModel.recentlyAdded,
            viewModel.relatedArtists,
            viewModel.siblings
        ) { songs, most, recent, related, siblings ->
            DetailValues(songs, most, recent, related, siblings)
        }.onEach {
            if (it.songs.isEmpty()) {
                requireActivity().onBackPressed()
            } else {
                mostPlayedAdapter.submitList(it.mostPlayed)
                recentlyAddedAdapter.submitList(it.recentlyAdded)
                relatedArtistAdapter.submitList(it.relatedArtists)
                albumsAdapter.submitList(it.siblings)
                mostPlayedAdapter.submitList(it.mostPlayed)

                adapter.submitList(it.songs)
            }
            restoreTranslations()
        }.launchIn(viewLifecycleOwner.lifecycleScope)

        viewModel.item
            .onEach { item ->
                require(item is DisplayableHeader)
                headerText.text = item.title
            }.launchIn(viewLifecycleOwner.lifecycleScope)

        if (mediaId.isAnyPodcast) {
            viewModel.observeAllCurrentPositions()
                .onEach { adapter.updatePodcastPositions(it) }
                .launchIn(viewLifecycleOwner.lifecycleScope)
        }

        editText.afterTextChange()
            .debounce(200)
            .filter { it.isEmpty() || it.length >= 2 }
            .onEach { viewModel.updateFilter(it) }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    override fun setupNestedList(layoutId: Int, recyclerView: RecyclerView) {
        when (layoutId) {
            R.layout.item_detail_list_most_played -> {
                setupHorizontalListAsGrid(recyclerView, mostPlayedAdapter)
            }
            R.layout.item_detail_list_recently_added -> {
                setupHorizontalListAsGrid(recyclerView, recentlyAddedAdapter)
            }
            R.layout.item_detail_list_related_artists -> {
                setupHorizontalListAsList(recyclerView, relatedArtistAdapter)
            }
            R.layout.item_detail_list_albums -> {
                setupHorizontalListAsList(recyclerView, albumsAdapter)
            }
        }
    }

    private fun setupHorizontalListAsGrid(list: RecyclerView, adapter: ObservableAdapter<*>) {
        val layoutManager = GridLayoutManager(
            list.context, DetailFragmentViewModel.NESTED_SPAN_COUNT,
            GridLayoutManager.HORIZONTAL, false
        )
        layoutManager.isItemPrefetchEnabled = true
        layoutManager.initialPrefetchItemCount = DetailFragmentViewModel.NESTED_SPAN_COUNT
        list.layoutManager = layoutManager
        list.adapter = adapter
        list.setRecycledViewPool(recycledViewPool)

        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(list)
    }

    private fun setupHorizontalListAsList(list: RecyclerView, adapter: ObservableAdapter<*>) {
        val layoutManager = LinearLayoutManager(list.context, LinearLayoutManager.HORIZONTAL, false)
        layoutManager.isItemPrefetchEnabled = true
        layoutManager.initialPrefetchItemCount = DetailFragmentViewModel.NESTED_SPAN_COUNT
        list.layoutManager = layoutManager
        list.adapter = adapter
        list.setRecycledViewPool(recycledViewPool)
    }

    override fun onResume() {
        super.onResume()
        list.addOnScrollListener(recyclerOnScrollListener)
        list.addOnScrollListener(scrollListener)
        back.setOnClickListener { requireActivity().onBackPressed() }
        more.setOnClickListener { navigator.toDialog(viewModel.mediaId, more, null) }
        filter.setOnClickListener {
            TransitionManager.beginDelayedTransition(toolbar, FastAutoTransition)
            searchWrapper.toggleVisibility(!searchWrapper.isVisible, true)
        }
    }

    override fun onPause() {
        super.onPause()
        list.removeOnScrollListener(recyclerOnScrollListener)
        list.removeOnScrollListener(scrollListener)
        back.setOnClickListener(null)
        more.setOnClickListener(null)
        filter.setOnClickListener(null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        disposeDragListener()
    }

    override fun onCurrentPlayingChanged(mediaId: PresentationId.Track) {
        adapter.onCurrentPlayingChanged(adapter, mediaId)
        mostPlayedAdapter.onCurrentPlayingChanged(mostPlayedAdapter, mediaId)
        recentlyAddedAdapter.onCurrentPlayingChanged(recentlyAddedAdapter, mediaId)
    }

    override fun adjustStatusBarColor() {
        adjustStatusBarColor(hasLightStatusBarColor)
    }

    override fun adjustStatusBarColor(lightStatusBar: Boolean) {
        if (lightStatusBar) {
            setLightStatusBar()
        } else {
            removeLightStatusBar()
        }
    }

    private fun removeLightStatusBar() {
        val color = Color.WHITE
        back.setColorFilter(color)
        more.setColorFilter(color)
        filter.setColorFilter(color)

        requireActivity().window.removeLightStatusBar()
    }

    private fun setLightStatusBar() {
        if (requireContext().isDarkMode()) {
            return
        }
        val color = requireContext().colorControlNormal()
        back.setColorFilter(color)
        more.setColorFilter(color)
        filter.setColorFilter(color)

        requireActivity().window.setLightStatusBar()
    }

    private fun restoreTranslations() {
        restoreUpperWidgetsTranslation()
        back.animate().alpha(1f)
        filter.animate().alpha(1f)
        more.animate().alpha(1f)
        searchWrapper.animate().alpha(1f)
        headerText.animate().alpha(1f)
    }

    override fun provideLayoutId(): Int = R.layout.fragment_detail

    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            val alpha = 1 - abs(toolbar.translationY) / toolbar.height
            back.alpha = alpha
            filter.alpha = alpha
            more.alpha = alpha
            searchWrapper.alpha = alpha
            headerText.alpha = alpha
        }
    }
}
