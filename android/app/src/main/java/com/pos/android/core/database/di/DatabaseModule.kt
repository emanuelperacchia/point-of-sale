package com.pos.android.core.database.di

import android.content.Context
import androidx.room.Room
import com.pos.android.core.database.PosDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PosDatabase {
        return Room.databaseBuilder(
            context,
            PosDatabase::class.java,
            PosDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideProductDao(database: PosDatabase) = database.productDao()

    @Provides
    @Singleton
    fun provideCartItemDao(database: PosDatabase) = database.cartItemDao()

    @Provides
    @Singleton
    fun providePendingSaleDao(database: PosDatabase) = database.pendingSaleDao()
}
