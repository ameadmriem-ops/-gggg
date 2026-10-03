package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.*

@Database(
    entities = [
        UserEntity::class,
        ChannelEntity::class,
        VideoEntity::class,
        CommentEntity::class,
        LikeEntity::class,
        FollowEntity::class,
        SavedVideoEntity::class,
        WatchHistoryEntity::class,
        NotificationEntity::class,
        ReportEntity::class,
        AppealEntity::class,
        AuditLogEntity::class,
        SearchHistoryEntity::class,
        MonetizationProfileEntity::class,
        AdImpressionEntity::class,
        PayoutEntity::class,
        PlatformSettingsEntity::class,
        BlockedUserEntity::class,
        NotInterestedEntity::class,
        DislikedCategoryEntity::class,
        LiveStreamEntity::class,
        LiveCommentEntity::class,
        LiveGiftEntity::class,
        LiveGiftTransactionEntity::class,
        CoinPackageEntity::class,
        CoinPurchaseOrderEntity::class,
        LiveMutedUserEntity::class,
        LiveBannedViewerEntity::class,
        LiveViewerSessionEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class VidoMixDatabase : RoomDatabase() {
    abstract fun vidoMixDao(): VidoMixDao

    companion object {
        @Volatile
        private var INSTANCE: VidoMixDatabase? = null

        fun getInstance(context: Context): VidoMixDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VidoMixDatabase::class.java,
                    "vidomix_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
