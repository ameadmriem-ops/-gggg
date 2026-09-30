package com.example.data.local

import com.example.data.model.*
import java.security.MessageDigest

object InitialDataSeeder {

    fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun seedDatabaseIfEmpty(dao: VidoMixDao) {
        // Reliable public test videos (HTTP 200)
        val vBigBuck = "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/720/Big_Buck_Bunny_720_10s_1MB.mp4"
        val vElephants = "https://filesamples.com/samples/video/mp4/sample_960x400_ocean_with_audio.mp4"
        val vForBigger = "https://test-videos.co.uk/vids/jellyfish/mp4/h264/720/Jellyfish_720_10s_1MB.mp4"
        val vSubaru = "https://filesamples.com/samples/video/mp4/sample_640x360.mp4"
        val vTears = "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/1080/Big_Buck_Bunny_1080_10s_1MB.mp4"
        val vSintel = "https://test-videos.co.uk/vids/sintel/mp4/h264/720/Sintel_720_10s_1MB.mp4"
        val vWeAreGoingOnBullrun = "https://filesamples.com/samples/video/mp4/sample_1280x720.mp4"

        val existingUsers = dao.getUserByIdDirect("user_admin")
        if (existingUsers != null) {
            // Self-repair: update any outdated URLs that return 403
            val checkVid = dao.getVideoByIdDirect("vid_long_1")
            if (checkVid != null && checkVid.videoUrl.contains("commondatastorage.googleapis.com")) {
                dao.insertVideo(checkVid.copy(videoUrl = vTears))
                dao.getVideoByIdDirect("vid_long_2")?.let { dao.insertVideo(it.copy(videoUrl = vForBigger)) }
                dao.getVideoByIdDirect("vid_long_3")?.let { dao.insertVideo(it.copy(videoUrl = vWeAreGoingOnBullrun)) }
                dao.getVideoByIdDirect("vid_long_4")?.let { dao.insertVideo(it.copy(videoUrl = vElephants)) }
                dao.getVideoByIdDirect("vid_long_5")?.let { dao.insertVideo(it.copy(videoUrl = vSubaru)) }

                dao.getVideoByIdDirect("short_1")?.let { dao.insertVideo(it.copy(videoUrl = vForBigger)) }
                dao.getVideoByIdDirect("short_2")?.let { dao.insertVideo(it.copy(videoUrl = vBigBuck)) }
                dao.getVideoByIdDirect("short_3")?.let { dao.insertVideo(it.copy(videoUrl = vSubaru)) }
                dao.getVideoByIdDirect("short_4")?.let { dao.insertVideo(it.copy(videoUrl = vSintel)) }
                dao.getVideoByIdDirect("short_5")?.let { dao.insertVideo(it.copy(videoUrl = vTears)) }
            }
            return
        }

        // 1. Initial Users
        val users = listOf(
            UserEntity(
                id = "user_admin",
                username = "admin",
                fullName = "إدارة VidoMix",
                email = "admin@vidomix.app",
                passwordHash = hashPassword("admin123"),
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&auto=format&fit=crop&q=80",
                bio = "الحساب الرسمي لإدارة منصة VidoMix. نرحب بالجميع!",
                followersCount = 12400,
                followingCount = 15,
                isVerified = true,
                isAdmin = true
            ),
            UserEntity(
                id = "user_tech",
                username = "nebras_tech",
                fullName = "نبراس التقنية",
                email = "tech@vidomix.app",
                passwordHash = hashPassword("123456"),
                avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200&auto=format&fit=crop&q=80",
                bio = "مراجعات تقنية، هواتف ذكية، وذكاء اصطناعي يومياً ⚡",
                followersCount = 48200,
                followingCount = 120,
                isVerified = true
            ),
            UserEntity(
                id = "user_gaming",
                username = "kimo_gaming",
                fullName = "كيمو ألعاب",
                email = "gaming@vidomix.app",
                passwordHash = hashPassword("123456"),
                avatarUrl = "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=200&auto=format&fit=crop&q=80",
                bio = "تحديات، بثوث مباشرة، وأحدث ألعاب البلايستيشن والـ PC 🎮",
                followersCount = 89000,
                followingCount = 85,
                isVerified = true
            ),
            UserEntity(
                id = "user_sports",
                username = "koora_zone",
                fullName = "كورة وبس",
                email = "sports@vidomix.app",
                passwordHash = hashPassword("123456"),
                avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&auto=format&fit=crop&q=80",
                bio = "كل ما يخص دوري أبطال أوروبا والكرة العالمية والعربية ⚽🏆",
                followersCount = 31500,
                followingCount = 42,
                isVerified = true
            ),
            UserEntity(
                id = "user_chef",
                username = "chef_future",
                fullName = "طاهي المستقبل",
                email = "chef@vidomix.app",
                passwordHash = hashPassword("123456"),
                avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200&auto=format&fit=crop&q=80",
                bio = "وصفات سريعة ومبتكرة من المطبخ العربي والعالمي 🍳🍰",
                followersCount = 22100,
                followingCount = 60,
                isVerified = false
            ),
            UserEntity(
                id = "user_cinema",
                username = "cinemix",
                fullName = "سينيما ميكس",
                email = "cinema@vidomix.app",
                passwordHash = hashPassword("123456"),
                avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200&auto=format&fit=crop&q=80",
                bio = "مراجعات أفلام، كواليس السينما والأنيميشن العالمية 🎬🍿",
                followersCount = 15300,
                followingCount = 34,
                isVerified = true
            )
        )
        users.forEach { dao.insertUser(it) }

        // 2. Initial Channels
        val channels = listOf(
            ChannelEntity(
                id = "chan_tech",
                userId = "user_tech",
                name = "قناة نبراس التقنية",
                handle = "@nebras_tech",
                avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200&auto=format&fit=crop&q=80",
                bannerUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?w=800&auto=format&fit=crop&q=80",
                bio = "القناة العربية الأولى للشروحات التقنية والبرمجية والأجهزة الذكية.",
                subscriberCount = 48200,
                videoCount = 42,
                isVerified = true
            ),
            ChannelEntity(
                id = "chan_gaming",
                userId = "user_gaming",
                name = "كيمو جيمينج",
                handle = "@kimo_gaming",
                avatarUrl = "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=200&auto=format&fit=crop&q=80",
                bannerUrl = "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=800&auto=format&fit=crop&q=80",
                bio = "مقاطع جيمينج حماسية ولقطات أسطورية في ألعاب الشوتر والمغامرة.",
                subscriberCount = 89000,
                videoCount = 78,
                isVerified = true
            ),
            ChannelEntity(
                id = "chan_sports",
                userId = "user_sports",
                name = "كورة وبس",
                handle = "@koora_zone",
                avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&auto=format&fit=crop&q=80",
                bannerUrl = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=800&auto=format&fit=crop&q=80",
                bio = "تحليلات تكتيكية، أهداف تاريخية، وملخصات مباريات الدوريات الكبرى.",
                subscriberCount = 31500,
                videoCount = 33,
                isVerified = true
            ),
            ChannelEntity(
                id = "chan_chef",
                userId = "user_chef",
                name = "مطبخ المستقبل",
                handle = "@chef_future",
                avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200&auto=format&fit=crop&q=80",
                bannerUrl = "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=800&auto=format&fit=crop&q=80",
                bio = "تعلم الطبخ في دقائق مع أسهل وأشهى الوصفات المنزلية.",
                subscriberCount = 22100,
                videoCount = 29,
                isVerified = false
            ),
            ChannelEntity(
                id = "chan_cinema",
                userId = "user_cinema",
                name = "سينيما ميكس",
                handle = "@cinemix",
                avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200&auto=format&fit=crop&q=80",
                bannerUrl = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=800&auto=format&fit=crop&q=80",
                bio = "أفضل لقطات الأفلام السينمائية العالمية والتحليلات الدرامية.",
                subscriberCount = 15300,
                videoCount = 19,
                isVerified = true
            )
        )
        channels.forEach { dao.insertChannel(it) }

        // 4. Initial Long Videos
        val longVideos = listOf(
            VideoEntity(
                id = "vid_long_1",
                channelId = "chan_tech",
                title = "مستقبل الذكاء الاصطناعي في 2026: كيف ستتغير حياتنا اليومية؟",
                description = "في هذه الحلقة نستعرض أحدث تطورات نماذج الذكاء الاصطناعي وكيف أصبحت قادرة على محاكاة الروبوتات والبرمجة الذاتية.",
                videoUrl = vTears,
                thumbnailUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop&q=80",
                durationSeconds = 734,
                viewsCount = 154200,
                likesCount = 9840,
                commentsCount = 412,
                category = "تقنية",
                tags = "ذكاء_اصطناعي, تقنية, روبوتات, 2026, حواسيب",
                isShort = false
            ),
            VideoEntity(
                id = "vid_long_2",
                channelId = "chan_gaming",
                title = "تحدي الهروب المستحيل! مغامرة حماسية حتى اللحظة الأخيرة 🎮🔥",
                description = "لعبنا اليوم واحدة من أصعب مراحل ألعاب البقاء والمغامرات التفاعلية، هل استطعنا الفوز وتجاوز العقبات المستحيلة؟",
                videoUrl = vForBigger,
                thumbnailUrl = "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=600&auto=format&fit=crop&q=80",
                durationSeconds = 620,
                viewsCount = 289300,
                likesCount = 18700,
                commentsCount = 890,
                category = "ألعاب",
                tags = "ألعاب, تحديات, جيمينج, مغامرة, بلايستيشن",
                isShort = false
            ),
            VideoEntity(
                id = "vid_long_3",
                channelId = "chan_sports",
                title = "أعظم 10 ريمونتادات في تاريخ دوري الأبطال التي هزت العالم ⚽",
                description = "لحظات كروية تاريخية لا تُنسى عندما قلبت الفرق الطاولة في الدقائق القاتلة لترسم أروع ملاحم كرة القدم.",
                videoUrl = vWeAreGoingOnBullrun,
                thumbnailUrl = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=600&auto=format&fit=crop&q=80",
                durationSeconds = 890,
                viewsCount = 412000,
                likesCount = 31200,
                commentsCount = 1450,
                category = "رياضة",
                tags = "كرة_قدم, دوري_أبطال_أوروبا, ريمونتادا, أهداف, رياضة",
                isShort = false
            ),
            VideoEntity(
                id = "vid_long_4",
                channelId = "chan_cinema",
                title = "أسرار المؤثرات البصرية وتصميم عوالم الخيال العلمي في هوليوود 🎬",
                description = "شاهد كيف تم تصوير وتحريك أضخم المشاهد السينمائية باستخدام تقنيات CGI الحديثة والإضاءة الحجمية.",
                videoUrl = vElephants,
                thumbnailUrl = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=600&auto=format&fit=crop&q=80",
                durationSeconds = 940,
                viewsCount = 98500,
                likesCount = 7300,
                commentsCount = 280,
                category = "أفلام",
                tags = "سينما, مؤثرات_بصرية, كواليس, هوليوود, أنيميشن",
                isShort = false
            ),
            VideoEntity(
                id = "vid_long_5",
                channelId = "chan_chef",
                title = "دليل الشيف المتكامل: أسرار تحضير أشهى المعجنات والخبز المنزلي 🥐",
                description = "خطوة بخطوة نتعلم درجات حرارة التخمير والفرن للحصول على قرمشة مثالية وطعم لا يقاوم.",
                videoUrl = vSubaru,
                thumbnailUrl = "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=600&auto=format&fit=crop&q=80",
                durationSeconds = 540,
                viewsCount = 67200,
                likesCount = 4900,
                commentsCount = 195,
                category = "تعليم",
                tags = "طبخ, معجنات, تعليم, وصفات, مطبخ",
                isShort = false
            )
        )
        longVideos.forEach { dao.insertVideo(it) }

        // 5. Initial Shorts (Vertical Videos)
        val shortVideos = listOf(
            VideoEntity(
                id = "short_1",
                channelId = "chan_tech",
                title = "ميزة سرية في هاتفك ستوفر 40% من البطارية! 🔋⚡",
                description = "جرب هذه الإعدادات فوراً واشترك للمزيد من النصائح التقنية اليومية #تقنية #نصائح #شورتس",
                videoUrl = vForBigger,
                thumbnailUrl = "https://images.unsplash.com/photo-1512499617640-c74ae3a79d37?w=400&auto=format&fit=crop&q=80",
                durationSeconds = 35,
                viewsCount = 890000,
                likesCount = 74300,
                commentsCount = 2100,
                category = "تقنية",
                tags = "تقنية, شورتس, بطارية, نصائح",
                isShort = true,
                soundTrackTitle = "نغمات تقنية حماسية - Nebras"
            ),
            VideoEntity(
                id = "short_2",
                channelId = "chan_gaming",
                title = "أغرب قتلة شفتها في حياتي برمي قنبلة عشوائية! 🤯🎯",
                description = "الحظ مليون من مية ههههه ما صدقت اللي صار! اكتبولنا في التعليقات #ألعاب #لقطة #ضحك",
                videoUrl = vBigBuck,
                thumbnailUrl = "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=400&auto=format&fit=crop&q=80",
                durationSeconds = 28,
                viewsCount = 1250000,
                likesCount = 112000,
                commentsCount = 4320,
                category = "ألعاب",
                tags = "ألعاب, لقطات, حظ, جيمينج",
                isShort = true,
                soundTrackTitle = "Epic Gaming Beat 808"
            ),
            VideoEntity(
                id = "short_3",
                channelId = "chan_sports",
                title = "مراوغة مستحيلة خلت المدافع يدوخ في مكانه! ⚽🔥",
                description = "سحر السامبا ومهارة خيالية تتكرر في الملاعب #مهارات #كورة #مراوغة #shorts",
                videoUrl = vSubaru,
                thumbnailUrl = "https://images.unsplash.com/photo-1579952363873-27f3bade9f55?w=400&auto=format&fit=crop&q=80",
                durationSeconds = 22,
                viewsCount = 670000,
                likesCount = 59800,
                commentsCount = 1840,
                category = "رياضة",
                tags = "رياضة, كورة, مهارات, سرعة",
                isShort = true,
                soundTrackTitle = "Samba Stadium Vibes"
            ),
            VideoEntity(
                id = "short_4",
                channelId = "chan_chef",
                title = "أسرع كوكيز نوتيلا في دقيقة واحدة بالميكروويف 🍫✨",
                description = "ثلاث مكونات فقط والطعم إدمان! احفظ الفيديو لتجربه الليلة #حلويات #سريع #طبخ",
                videoUrl = vSintel,
                thumbnailUrl = "https://images.unsplash.com/photo-1499636136210-6f4ee915583e?w=400&auto=format&fit=crop&q=80",
                durationSeconds = 40,
                viewsCount = 430000,
                likesCount = 41200,
                commentsCount = 980,
                category = "ترفيه",
                tags = "طبخ, حلويات, وصفات_سريعة, ترفيه",
                isShort = true,
                soundTrackTitle = "Chill Acoustic Melody"
            ),
            VideoEntity(
                id = "short_5",
                channelId = "chan_cinema",
                title = "كيف يبدو المشهد قبل وبعد إضافة المؤثرات البصرية؟ 🎬👀",
                description = "صدمة لما تشوف الممثل يكلم شاشة خضراء فقط! #سينما #كواليس #انبهار",
                videoUrl = vTears,
                thumbnailUrl = "https://images.unsplash.com/photo-1478760329108-5c3ed9d495a0?w=400&auto=format&fit=crop&q=80",
                durationSeconds = 30,
                viewsCount = 580000,
                likesCount = 48500,
                commentsCount = 1340,
                category = "أفلام",
                tags = "سينما, كواليس, قبل_وبعد, هوليوود",
                isShort = true,
                soundTrackTitle = "Cinematic Ambient FX"
            )
        )
        shortVideos.forEach { dao.insertVideo(it) }

        // 6. Initial Comments
        val initialComments = listOf(
            CommentEntity(
                id = "comm_1",
                videoId = "vid_long_1",
                userId = "user_gaming",
                content = "محتوى جبار كالعادة يا بشمهندس! أكثر شيء مبهر تطور الروبوتات الطبية.",
                likesCount = 45
            ),
            CommentEntity(
                id = "comm_1_rep",
                videoId = "vid_long_1",
                userId = "user_tech",
                parentCommentId = "comm_1",
                content = "شكراً كيمو يا غالي، والقادم بإذن الله مذهل أكثر!",
                likesCount = 18
            ),
            CommentEntity(
                id = "comm_2",
                videoId = "vid_long_1",
                userId = "user_sports",
                content = "هل تتوقع الذكاء الاصطناعي يدير خطط المباريات كمدرب في المستقبل؟",
                likesCount = 22
            ),
            CommentEntity(
                id = "comm_3",
                videoId = "short_1",
                userId = "user_chef",
                content = "جربتها على جوالي ونفعت فعلاً، شكراً جزيلاً!",
                likesCount = 63
            ),
            CommentEntity(
                id = "comm_4",
                videoId = "short_2",
                userId = "user_tech",
                content = "هههههه لقطة الموسم الصراحة! مستحيل تتكرر.",
                likesCount = 89
            )
        )
        initialComments.forEach { dao.insertComment(it) }

        // 7. Initial Notifications for admin
        val notifications = listOf(
            NotificationEntity(
                id = "notif_1",
                userId = "user_admin",
                type = "NEW_FOLLOWER",
                title = "متابع جديد",
                message = "قام نبراس التقنية بمتابعة حسابك.",
                targetId = "user_tech"
            ),
            NotificationEntity(
                id = "notif_2",
                userId = "user_admin",
                type = "NEW_VIDEO",
                title = "فيديو جديد من كيمو ألعاب",
                message = "نشر كيمو ألعاب فيديو جديد: تحدي الهروب المستحيل!",
                targetId = "vid_long_2"
            ),
            NotificationEntity(
                id = "notif_3",
                userId = "user_admin",
                type = "LIKE",
                title = "إعجاب جديد",
                message = "أعجب 50 مستخدماً بتعليقك الأخير.",
                targetId = "comm_1"
            )
        )
        notifications.forEach { dao.insertNotification(it) }

        // 8. Platform Ad Settings
        dao.insertPlatformSettings(
            PlatformSettingsEntity(
                key = "global",
                platformSharePercent = 60.0,
                creatorSharePercent = 40.0,
                shortsAdInterval = 3,
                midrollEnabled = true,
                minPayoutThreshold = 50.0
            )
        )

        // 9. Initial Monetization Profiles
        val initialProfiles = listOf(
            MonetizationProfileEntity(
                userId = "user_admin",
                channelId = "chan_tech",
                status = "APPROVED",
                tier = 2,
                followersCount = 12400,
                publicVideosCount = 15,
                watchHours = 5200.0,
                shortsViews = 2400000L,
                appliedAt = System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 30),
                reviewedAt = System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 28),
                currentBalance = 142.50,
                pendingBalance = 28.30,
                lifetimeEarnings = 480.00,
                adImpressionsCount = 14200,
                monetizedPlaybacksCount = 8900
            ),
            MonetizationProfileEntity(
                userId = "user_tech",
                channelId = "chan_tech",
                status = "APPROVED",
                tier = 2,
                followersCount = 48200,
                publicVideosCount = 42,
                watchHours = 12800.0,
                shortsViews = 8900000L,
                appliedAt = System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 60),
                reviewedAt = System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 58),
                currentBalance = 320.00,
                pendingBalance = 65.40,
                lifetimeEarnings = 1250.00,
                adImpressionsCount = 38000,
                monetizedPlaybacksCount = 29000
            ),
            MonetizationProfileEntity(
                userId = "user_gaming",
                channelId = "chan_gaming",
                status = "PENDING_REVIEW",
                tier = 2,
                followersCount = 89000,
                publicVideosCount = 78,
                watchHours = 18400.0,
                shortsViews = 15000000L,
                appliedAt = System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 2),
                currentBalance = 0.0,
                pendingBalance = 0.0,
                lifetimeEarnings = 0.0
            ),
            MonetizationProfileEntity(
                userId = "user_sports",
                channelId = "chan_sports",
                status = "ELIGIBLE",
                tier = 1,
                followersCount = 31500,
                publicVideosCount = 33,
                watchHours = 3800.0,
                shortsViews = 1800000L
            ),
            MonetizationProfileEntity(
                userId = "user_chef",
                channelId = "chan_chef",
                status = "NOT_ELIGIBLE",
                tier = 0,
                followersCount = 420,
                publicVideosCount = 2,
                watchHours = 450.0,
                shortsViews = 120000L
            )
        )
        initialProfiles.forEach { dao.insertMonetizationProfile(it) }
    }
}
