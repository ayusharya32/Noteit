package com.appsbyayush.noteit.di

import android.content.Context
import androidx.room.Room
import com.appsbyayush.noteit.db.NoteDatabase
import com.appsbyayush.noteit.utils.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideNoteDatabase(
        @ApplicationContext context: Context
    ) = Room.databaseBuilder(context, NoteDatabase::class.java, "noteDB")
        .build()

    @Singleton
    @Provides
    fun provideNoteDao(db: NoteDatabase) = db.getNoteDao()

    @Singleton
    @Provides
    fun provideSharedPreferences(
        @ApplicationContext context: Context
    ) = context.getSharedPreferences(Constants.APP_SHARED_PREFS, Context.MODE_PRIVATE)
}