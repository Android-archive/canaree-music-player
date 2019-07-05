package dev.olog.presentation.prefs.categories

import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.olog.core.MediaIdCategory
import dev.olog.presentation.R
import dev.olog.presentation.base.ListDialog
import dev.olog.presentation.base.drag.DragListenerImpl
import dev.olog.presentation.base.drag.IDragListener
import dev.olog.shared.extensions.lazyFast
import dev.olog.shared.extensions.withArguments
import javax.inject.Inject

class LibraryCategoriesFragment : ListDialog(), IDragListener by DragListenerImpl() {

    companion object {
        const val TAG = "LibraryCategoriesFragment"
        const val TYPE = "$TAG.TYPE"

        @JvmStatic
        fun newInstance(category: MediaIdCategory): LibraryCategoriesFragment {
            return LibraryCategoriesFragment().withArguments(
                TYPE to category.ordinal
            )
        }
    }

    @Inject
    lateinit var presenter: LibraryCategoriesFragmentPresenter
    private val adapter by lazyFast {
        LibraryCategoriesFragmentAdapter(presenter.getDataSet(category), this)
    }

    private val category by lazyFast { MediaIdCategory.values()[arguments!!.getInt(TYPE)] }

    override fun setupBuilder(builder: AlertDialog.Builder): AlertDialog.Builder {
        val title = if (category == MediaIdCategory.SONGS) R.string.prefs_library_categories_title else R.string.prefs_library_categories_title_podcasts

        return builder.setTitle(title)
            .setNeutralButton(R.string.popup_neutral_reset, null)
            .setNegativeButton(R.string.popup_negative_cancel, null)
            .setPositiveButton(R.string.popup_positive_save, null)
    }

    override fun setupRecyclerView(list: RecyclerView) {
        list.adapter = adapter
        list.layoutManager = LinearLayoutManager(context)
        setupDragListener(list, 0)
    }

    override fun positiveAction() {
        presenter.setDataSet(category, adapter.getData())
        dismiss()
    }

    override fun neutralAction() {
        val defaultData = presenter.getDefaultDataSet(category)
        adapter.updateDataSet(defaultData)
    }

}