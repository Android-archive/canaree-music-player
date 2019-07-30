package dev.olog.presentation.createplaylist.di

import dagger.BindsInstance
import dagger.Subcomponent
import dagger.android.AndroidInjector
import dev.olog.presentation.createplaylist.CreatePlaylistFragment
import dev.olog.presentation.dagger.PerFragment


@Subcomponent(modules = [CreatePlaylistFragmentModule::class])
@PerFragment
interface CreatePlaylistFragmentSubComponent : AndroidInjector<CreatePlaylistFragment> {

    @Subcomponent.Factory
    interface Builder : AndroidInjector.Factory<CreatePlaylistFragment> {

        override fun create(@BindsInstance instance: CreatePlaylistFragment): AndroidInjector<CreatePlaylistFragment>
    }

}