package com.example.core

import android.util.Log

object ContentModerationEngine {

    enum class ModerationStatus {
        APPROVED,
        UNDER_REVIEW,
        REJECTED,
        REMOVED
    }

    data class ModerationResult(
        val status: String,
        val violationCategory: String?,
        val confidence: Double,
        val explanation: String,
        val isFlagged: Boolean
    )

    // Violation taxonomy matching YouTube / TikTok standards
    val CATEGORIES = listOf(
        "عري أو محتوى جنسي صريح",
        "عنف شديد أو مشاهد دموية",
        "خطاب كراهية وتمييز",
        "تحرش وتنمر وتهديدات",
        "محتوى خطير أو إيذاء ذاتي",
        "Spam واحتيال إلكتروني",
        "انتهاك حقوق الملكية الفكرية",
        "ترويج سلع أو مواد غير قانونية",
        "سبب آخر"
    )

    // Severe critical triggers (Immediate rejection)
    private val SEVERE_KEYWORDS = listOf(
        "child_exploit", "csam", "terror", "isis", "suicide_broadcast",
        "murder_live", "blood_gore", "weapons_blackmarket", "credit_card_hack",
        "قتل_مباشر", "إرهاب", "داعش", "انتحار_مباشر", "اختراق_بطاقات", "مخدرات_للبيع",
        "سلاح_ممنوع", "إباحية_أطفال", "تعذيب_حيوانات"
    )

    // Borderline / Suspicious triggers (Queued for Admin Review)
    private val SUSPICIOUS_KEYWORDS = listOf(
        "free_vbucks", "whatsapp_girls", "hack_gems", "get_rich_quick",
        "شحن_مجاني", "ربح_1000_دولار_بدقيقة", "تسريب_فيديو", "بنات_واتساب",
        "مهكر_مجانا", "هكر_ببجي", "سيرفر_خاص_مجاني", "فيديو_مسرب_فضيحة"
    )

    /**
     * Inspects content before publication.
     * Evaluates text metadata, tags, URLs, and contextual flags.
     */
    fun inspectContent(
        title: String,
        description: String = "",
        tags: String = "",
        mediaUrl: String = "",
        uploaderId: String = ""
    ): ModerationResult {
        val fullCorpus = "$title $description $tags $mediaUrl".lowercase()

        // 1. Check for severe violations
        for (keyword in SEVERE_KEYWORDS) {
            if (fullCorpus.contains(keyword)) {
                Log.w("ContentModeration", "Severe violation detected: $keyword")
                return ModerationResult(
                    status = ModerationStatus.REJECTED.name,
                    violationCategory = detectCategoryFromKeyword(keyword),
                    confidence = 0.98,
                    explanation = "تم رفض المحتوى تلقائياً لاحتوائه على مخالفة صريحة لمعايير الأمان والسلامة.",
                    isFlagged = true
                )
            }
        }

        // 2. Check for suspicious / borderline content
        for (keyword in SUSPICIOUS_KEYWORDS) {
            if (fullCorpus.contains(keyword)) {
                Log.i("ContentModeration", "Suspicious content flagged for review: $keyword")
                return ModerationResult(
                    status = ModerationStatus.UNDER_REVIEW.name,
                    violationCategory = "Spam واحتيال إلكتروني",
                    confidence = 0.75,
                    explanation = "تم تعليق المحتوى للمراجعة الإدارية للتأكد من عدم احتوائه على تضليل أو احتيال.",
                    isFlagged = true
                )
            }
        }

        // 3. Safe content
        return ModerationResult(
            status = ModerationStatus.APPROVED.name,
            violationCategory = null,
            confidence = 0.95,
            explanation = "المحتوى متوافق مع إرشادات منتدى VidoMix.",
            isFlagged = false
        )
    }

    private fun detectCategoryFromKeyword(keyword: String): String {
        return when {
            keyword.contains("murder") || keyword.contains("blood") || keyword.contains("قتل") || keyword.contains("تعذيب") -> "عنف شديد أو مشاهد دموية"
            keyword.contains("terror") || keyword.contains("إرهاب") || keyword.contains("داعش") -> "محتوى خطير أو إيذاء ذاتي"
            keyword.contains("weapons") || keyword.contains("مخدرات") || keyword.contains("سلاح") -> "ترويج سلع أو مواد غير قانونية"
            keyword.contains("hack") || keyword.contains("credit") || keyword.contains("بطاقات") -> "Spam واحتيال إلكتروني"
            else -> "عري أو محتوى جنسي صريح"
        }
    }
}
