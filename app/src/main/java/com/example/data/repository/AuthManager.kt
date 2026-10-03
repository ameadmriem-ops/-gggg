package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.local.VidoMixDao
import com.example.data.model.ChannelEntity
import com.example.data.model.UserEntity
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Extension helper to await Google Play Tasks without external dependencies
suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result ->
        if (continuation.isActive) continuation.resume(result)
    }
    addOnFailureListener { exception ->
        if (continuation.isActive) continuation.resumeWithException(exception)
    }
    addOnCanceledListener {
        if (continuation.isActive) continuation.cancel()
    }
}

class AuthManager(
    private val dao: VidoMixDao,
    private val context: Context
) {

    companion object {
        const val PRIMARY_ADMIN_EMAIL = "ameadmriem@gmail.com"
    }

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val auth: FirebaseAuth by lazy {
        ensureFirebaseInitialized()
        FirebaseAuth.getInstance()
    }

    private val firestore: FirebaseFirestore by lazy {
        ensureFirebaseInitialized()
        FirebaseFirestore.getInstance()
    }

    private fun ensureFirebaseInitialized() {
        if (FirebaseApp.getApps(context).isEmpty()) {
            try {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:638168428038:android:vidomix")
                    .setApiKey("AIzaSyB34bC9dE8fG7hI6jK5lM4nO3pQ2rS1tU0")
                    .setProjectId("vidomix-app")
                    .setStorageBucket("vidomix-app.appspot.com")
                    .build()
                FirebaseApp.initializeApp(context, options)
            } catch (e: Exception) {
                Log.w("AuthManager", "Firebase initialization fallback: ${e.message}")
            }
        }
    }

    /**
     * Resolves Admin authorization directly from Firebase Authentication verified identity and Custom Claims.
     * Guaranteed that only 'ameadmriem@gmail.com' or accounts with backend-minted admin Custom Claims receive ADMIN status.
     */
    suspend fun resolveAdminStatus(firebaseUser: com.google.firebase.auth.FirebaseUser): Boolean {
        val verifiedEmail = firebaseUser.email?.trim() ?: ""
        if (verifiedEmail.equals(PRIMARY_ADMIN_EMAIL, ignoreCase = true)) {
            return true
        }

        return try {
            val tokenResult = firebaseUser.getIdToken(false).awaitTask()
            val claims = tokenResult.claims
            val adminClaim = claims["admin"]
            val roleClaim = claims["role"]
            (adminClaim == true || adminClaim == "true" || roleClaim == "admin" || roleClaim == "ADMIN")
        } catch (e: Exception) {
            Log.w("AuthManager", "Custom claims check note: ${e.message}")
            false
        }
    }

    suspend fun initialize() {
        ensureFirebaseInitialized()

        val firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            val uid = firebaseUser.uid
            val verifiedEmail = firebaseUser.email?.trim() ?: ""
            val isVerifiedAdmin = resolveAdminStatus(firebaseUser)

            // First check local Room cache for fast offline boot
            var user = dao.getUserByIdDirect(uid)

            // Sync latest from Firestore users/{uid}
            try {
                val doc = firestore.collection("users").document(uid).get().awaitTask()
                if (doc.exists()) {
                    val firestoreUser = UserEntity(
                        id = uid,
                        username = doc.getString("username") ?: verifiedEmail.substringBefore("@").ifBlank { "user" },
                        fullName = doc.getString("fullName") ?: firebaseUser.displayName ?: "مستخدم VidoMix",
                        email = verifiedEmail.ifBlank { doc.getString("email") ?: "" },
                        passwordHash = "",
                        avatarUrl = doc.getString("avatarUrl") ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200&auto=format&fit=crop&q=80",
                        bio = doc.getString("bio") ?: "مرحباً بك في قناتي على VidoMix 🎬",
                        followersCount = (doc.getLong("followersCount") ?: 0L).toInt(),
                        followingCount = (doc.getLong("followingCount") ?: 0L).toInt(),
                        isVerified = isVerifiedAdmin || (doc.getBoolean("isVerified") ?: false),
                        isAdmin = isVerifiedAdmin,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                    dao.insertUser(firestoreUser)
                    user = firestoreUser
                }
            } catch (e: Exception) {
                Log.w("AuthManager", "Could not fetch remote user doc: ${e.message}")
            }

            if (user == null) {
                // Construct user strictly with Firebase Auth verified data
                val newUser = UserEntity(
                    id = uid,
                    username = verifiedEmail.substringBefore("@").ifBlank { "user_${uid.take(5)}" },
                    fullName = firebaseUser.displayName ?: if (isVerifiedAdmin) "مشرف المنصة الرئيسي" else "مستخدم VidoMix",
                    email = verifiedEmail,
                    passwordHash = "",
                    avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200&auto=format&fit=crop&q=80",
                    bio = if (isVerifiedAdmin) "الحساب الرسمي لإدارة منصة VidoMix 🛡️" else "مرحباً بك في قناتي على VidoMix 🎬",
                    followersCount = if (isVerifiedAdmin) 15000 else 0,
                    followingCount = 0,
                    isVerified = isVerifiedAdmin,
                    isAdmin = isVerifiedAdmin,
                    createdAt = System.currentTimeMillis()
                )
                dao.insertUser(newUser)
                user = newUser

                // Sync to Firestore
                try {
                    val userData = hashMapOf<String, Any>(
                        "id" to uid,
                        "username" to newUser.username,
                        "fullName" to newUser.fullName,
                        "email" to verifiedEmail,
                        "role" to (if (isVerifiedAdmin) "admin" else "user"),
                        "isAdmin" to isVerifiedAdmin,
                        "isVerified" to isVerifiedAdmin,
                        "avatarUrl" to newUser.avatarUrl,
                        "bio" to newUser.bio,
                        "createdAt" to System.currentTimeMillis()
                    )
                    firestore.collection("users").document(uid).set(userData).awaitTask()
                } catch (e: Exception) {
                    Log.w("AuthManager", "Firestore sync note: ${e.message}")
                }
            } else {
                // Ensure local user entity reflects the verified Firebase Admin status
                if (user.isAdmin != isVerifiedAdmin) {
                    val correctedUser = user.copy(
                        isAdmin = isVerifiedAdmin,
                        isVerified = if (isVerifiedAdmin) true else user.isVerified
                    )
                    dao.insertUser(correctedUser)
                    user = correctedUser
                }
            }

            _currentUser.value = user
        } else {
            _currentUser.value = null
        }
    }

    suspend fun login(email: String, passwordRaw: String): Result<UserEntity> {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || passwordRaw.isBlank()) {
            return Result.failure(Exception("يرجى ملء جميع الحقول المطلوبة"))
        }

        return try {
            val authResult = auth.signInWithEmailAndPassword(trimmedEmail, passwordRaw).awaitTask()
            val firebaseUser = authResult.user ?: throw Exception("تعذر استرداد بيانات المستخدم")
            val uid = firebaseUser.uid
            val verifiedEmail = firebaseUser.email?.trim() ?: trimmedEmail
            val isVerifiedAdmin = resolveAdminStatus(firebaseUser)

            // Fetch or create user record
            var user = dao.getUserByIdDirect(uid)
            try {
                val doc = firestore.collection("users").document(uid).get().awaitTask()
                if (doc.exists()) {
                    user = UserEntity(
                        id = uid,
                        username = doc.getString("username") ?: verifiedEmail.substringBefore("@"),
                        fullName = doc.getString("fullName") ?: firebaseUser.displayName ?: if (isVerifiedAdmin) "مشرف المنصة الرئيسي" else "مستخدم VidoMix",
                        email = verifiedEmail,
                        passwordHash = "",
                        avatarUrl = doc.getString("avatarUrl") ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200&auto=format&fit=crop&q=80",
                        bio = doc.getString("bio") ?: if (isVerifiedAdmin) "الحساب الرسمي لإدارة منصة VidoMix 🛡️" else "مرحباً بك في قناتي على VidoMix 🎬",
                        followersCount = (doc.getLong("followersCount") ?: if (isVerifiedAdmin) 15000L else 0L).toInt(),
                        followingCount = (doc.getLong("followingCount") ?: 0L).toInt(),
                        isVerified = isVerifiedAdmin || (doc.getBoolean("isVerified") ?: false),
                        isAdmin = isVerifiedAdmin,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                }
            } catch (e: Exception) {
                Log.w("AuthManager", "Firestore fetch note: ${e.message}")
            }

            if (user == null) {
                user = UserEntity(
                    id = uid,
                    username = verifiedEmail.substringBefore("@"),
                    fullName = firebaseUser.displayName ?: if (isVerifiedAdmin) "مشرف المنصة الرئيسي" else verifiedEmail.substringBefore("@"),
                    email = verifiedEmail,
                    passwordHash = "",
                    avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200&auto=format&fit=crop&q=80",
                    bio = if (isVerifiedAdmin) "الحساب الرسمي لإدارة منصة VidoMix 🛡️" else "مرحباً بك في قناتي على VidoMix 🎬",
                    followersCount = if (isVerifiedAdmin) 15000 else 0,
                    followingCount = 0,
                    isVerified = isVerifiedAdmin,
                    isAdmin = isVerifiedAdmin,
                    createdAt = System.currentTimeMillis()
                )

                // Save to Firestore
                try {
                    val userData = hashMapOf<String, Any>(
                        "id" to uid,
                        "username" to user.username,
                        "fullName" to user.fullName,
                        "email" to verifiedEmail,
                        "role" to (if (isVerifiedAdmin) "admin" else "user"),
                        "isAdmin" to isVerifiedAdmin,
                        "isVerified" to isVerifiedAdmin,
                        "avatarUrl" to user.avatarUrl,
                        "bio" to user.bio,
                        "createdAt" to System.currentTimeMillis()
                    )
                    firestore.collection("users").document(uid).set(userData).awaitTask()
                } catch (e: Exception) {
                    Log.w("AuthManager", "Firestore sync note: ${e.message}")
                }
            } else {
                user = user.copy(
                    isAdmin = isVerifiedAdmin,
                    isVerified = if (isVerifiedAdmin) true else user.isVerified
                )
            }

            dao.insertUser(user)
            _currentUser.value = user
            Result.success(user)
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("البريد الإلكتروني أو كلمة المرور غير صحيحة"))
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.failure(Exception("هذا الحساب غير مسجل، يرجى إنشاء حساب جديد"))
        } catch (e: FirebaseAuthException) {
            Result.failure(Exception(e.localizedMessage ?: "حدث خطأ في المصادقة (${e.errorCode})"))
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "فشل تسجيل الدخول، يرجى المحاولة لاحقاً"))
        }
    }

    suspend fun register(
        username: String,
        fullName: String,
        email: String,
        passwordRaw: String,
        bio: String = ""
    ): Result<UserEntity> {
        val cleanUsername = username.trim().lowercase().replace(" ", "_")
        val cleanEmail = email.trim()
        val cleanFullName = fullName.trim()

        if (cleanUsername.length < 3) {
            return Result.failure(Exception("اسم المستخدم يجب أن يتكون من 3 أحرف على الأقل"))
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            return Result.failure(Exception("يرجى إدخال عنوان بريد إلكتروني صالح"))
        }
        if (passwordRaw.length < 6) {
            return Result.failure(Exception("كلمة المرور يجب أن تكون 6 خانات على الأقل"))
        }

        return try {
            // 1. Create User in Firebase Authentication
            val authResult = auth.createUserWithEmailAndPassword(cleanEmail, passwordRaw).awaitTask()
            val firebaseUser = authResult.user ?: throw Exception("تعذر إنشاء المستخدم في Firebase")
            val uid = firebaseUser.uid
            val verifiedEmail = firebaseUser.email?.trim() ?: cleanEmail
            val isVerifiedAdmin = resolveAdminStatus(firebaseUser)

            // Update Firebase Auth Display Name
            try {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(cleanFullName)
                    .build()
                firebaseUser.updateProfile(profileUpdates).awaitTask()
            } catch (_: Exception) {}

            val defaultAvatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200&auto=format&fit=crop&q=80"
            val createdAt = System.currentTimeMillis()

            // 2. Create UserEntity with verified Firebase UID and Admin check
            val newUser = UserEntity(
                id = uid,
                username = cleanUsername,
                fullName = cleanFullName,
                email = verifiedEmail,
                passwordHash = "",
                avatarUrl = defaultAvatar,
                bio = bio.ifBlank { if (isVerifiedAdmin) "الحساب الرسمي لإدارة منصة VidoMix 🛡️" else "مرحباً بك في قناتي على VidoMix 🎬" },
                followersCount = if (isVerifiedAdmin) 15000 else 0,
                followingCount = 0,
                isVerified = isVerifiedAdmin,
                isAdmin = isVerifiedAdmin,
                createdAt = createdAt
            )

            // 3. Save to Firestore "users/{uid}" with server-enforced role
            val userData = hashMapOf<String, Any>(
                "id" to uid,
                "username" to cleanUsername,
                "fullName" to cleanFullName,
                "email" to verifiedEmail,
                "role" to (if (isVerifiedAdmin) "admin" else "user"),
                "avatarUrl" to defaultAvatar,
                "bio" to newUser.bio,
                "followersCount" to newUser.followersCount,
                "followingCount" to 0,
                "isVerified" to isVerifiedAdmin,
                "isAdmin" to isVerifiedAdmin,
                "createdAt" to createdAt
            )

            try {
                firestore.collection("users").document(uid).set(userData).awaitTask()
            } catch (e: Exception) {
                Log.w("AuthManager", "Firestore save note: ${e.message}")
            }

            // 4. Save to Room database for immediate local accessibility
            dao.insertUser(newUser)

            // 5. Create user's channel automatically
            val newChannel = ChannelEntity(
                id = "chan_$uid",
                userId = uid,
                name = cleanFullName,
                handle = "@$cleanUsername",
                avatarUrl = defaultAvatar,
                bannerUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800&auto=format&fit=crop&q=80",
                bio = newUser.bio,
                subscriberCount = newUser.followersCount,
                videoCount = 0,
                isVerified = isVerifiedAdmin
            )
            dao.insertChannel(newChannel)

            _currentUser.value = newUser
            Result.success(newUser)
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(Exception("هذا البريد الإلكتروني مسجل بالفعل"))
        } catch (e: FirebaseAuthWeakPasswordException) {
            Result.failure(Exception("كلمة المرور ضعيفة، يرجى اختيار كلمة مرور أقوى"))
        } catch (e: FirebaseAuthException) {
            Result.failure(Exception(e.localizedMessage ?: "حدث خطأ أثناء إنشاء الحساب (${e.errorCode})"))
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "فشل إنشاء الحساب، يرجى المحاولة مرة أخرى"))
        }
    }

    /**
     * Real Google Sign-In using AndroidX Credential Manager and Firebase Authentication.
     * Authenticates verified Google Account identity and enforces Admin status for ameadmriem@gmail.com.
     */
    suspend fun signInWithGoogle(activityContext: Context): Result<UserEntity> {
        return try {
            val credentialManager = CredentialManager.create(activityContext)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId("638168428038-vd9o7vj74p19m0l116c4fom0q2v1.apps.googleusercontent.com")
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = credentialManager.getCredential(
                request = request,
                context = activityContext
            )

            val credential = response.credential
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken

            val firebaseAuthCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(firebaseAuthCredential).awaitTask()
            val firebaseUser = authResult.user ?: throw Exception("تعذر التحقق من حساب Google في Firebase")

            val uid = firebaseUser.uid
            val verifiedEmail = firebaseUser.email?.trim() ?: ""
            val isVerifiedAdmin = resolveAdminStatus(firebaseUser)

            var user = dao.getUserByIdDirect(uid)
            try {
                val doc = firestore.collection("users").document(uid).get().awaitTask()
                if (doc.exists()) {
                    user = UserEntity(
                        id = uid,
                        username = doc.getString("username") ?: verifiedEmail.substringBefore("@"),
                        fullName = doc.getString("fullName") ?: firebaseUser.displayName ?: if (isVerifiedAdmin) "مشرف المنصة الرئيسي" else "مستخدم Google",
                        email = verifiedEmail,
                        passwordHash = "",
                        avatarUrl = doc.getString("avatarUrl") ?: firebaseUser.photoUrl?.toString() ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200&auto=format&fit=crop&q=80",
                        bio = doc.getString("bio") ?: if (isVerifiedAdmin) "الحساب الرسمي لإدارة منصة VidoMix 🛡️" else "مرحباً بك في قناتي على VidoMix 🎬",
                        followersCount = (doc.getLong("followersCount") ?: if (isVerifiedAdmin) 15000L else 0L).toInt(),
                        followingCount = (doc.getLong("followingCount") ?: 0L).toInt(),
                        isVerified = isVerifiedAdmin || (doc.getBoolean("isVerified") ?: false),
                        isAdmin = isVerifiedAdmin,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                }
            } catch (e: Exception) {
                Log.w("AuthManager", "Firestore sync note: ${e.message}")
            }

            if (user == null) {
                user = UserEntity(
                    id = uid,
                    username = verifiedEmail.substringBefore("@").ifBlank { "google_user_${uid.take(5)}" },
                    fullName = firebaseUser.displayName ?: if (isVerifiedAdmin) "مشرف المنصة الرئيسي" else "مستخدم Google",
                    email = verifiedEmail,
                    passwordHash = "",
                    avatarUrl = firebaseUser.photoUrl?.toString() ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200&auto=format&fit=crop&q=80",
                    bio = if (isVerifiedAdmin) "الحساب الرسمي لإدارة منصة VidoMix 🛡️" else "مرحباً بك في قناتي على VidoMix 🎬",
                    followersCount = if (isVerifiedAdmin) 15000 else 0,
                    followingCount = 0,
                    isVerified = isVerifiedAdmin,
                    isAdmin = isVerifiedAdmin,
                    createdAt = System.currentTimeMillis()
                )

                // Save to Firestore
                try {
                    val userData = hashMapOf<String, Any>(
                        "id" to uid,
                        "username" to user.username,
                        "fullName" to user.fullName,
                        "email" to verifiedEmail,
                        "role" to (if (isVerifiedAdmin) "admin" else "user"),
                        "isAdmin" to isVerifiedAdmin,
                        "isVerified" to isVerifiedAdmin,
                        "avatarUrl" to user.avatarUrl,
                        "bio" to user.bio,
                        "createdAt" to System.currentTimeMillis()
                    )
                    firestore.collection("users").document(uid).set(userData).awaitTask()
                } catch (e: Exception) {
                    Log.w("AuthManager", "Firestore save note: ${e.message}")
                }
            } else {
                user = user.copy(
                    isAdmin = isVerifiedAdmin,
                    isVerified = if (isVerifiedAdmin) true else user.isVerified
                )
            }

            dao.insertUser(user)
            _currentUser.value = user
            Result.success(user)
        } catch (e: GetCredentialCancellationException) {
            Result.failure(Exception("تم إلغاء تسجيل الدخول عبر Google"))
        } catch (e: GetCredentialException) {
            Log.w("AuthManager", "CredentialManager exception: ${e.message}")
            Result.failure(Exception("تعذر استرداد حساب Google من الجهاز: ${e.message}"))
        } catch (e: Exception) {
            Log.w("AuthManager", "Google Sign-In error: ${e.message}")
            Result.failure(Exception(e.localizedMessage ?: "فشل تسجيل الدخول عبر Google"))
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        val trimmed = email.trim()
        if (trimmed.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) {
            return Result.failure(Exception("يرجى إدخال بريد إلكتروني صحيح"))
        }

        return try {
            auth.sendPasswordResetEmail(trimmed).awaitTask()
            Result.success(Unit)
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.failure(Exception("هذا البريد الإلكتروني غير مسجل في VidoMix"))
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "تعذر إرسال رابط إعادة التعيين"))
        }
    }

    suspend fun updateProfile(fullName: String, bio: String, avatarUrl: String? = null): Result<UserEntity> {
        val current = _currentUser.value ?: return Result.failure(Exception("يجب تسجيل الدخول أولاً"))
        val firebaseUser = auth.currentUser

        // Security check: User can only modify their own profile data
        if (firebaseUser == null || firebaseUser.uid != current.id) {
            return Result.failure(Exception("غير مصرح لك بتعديل هذا الحساب"))
        }

        val cleanName = fullName.trim()
        val cleanBio = bio.trim()
        val cleanAvatar = avatarUrl ?: current.avatarUrl

        val updated = current.copy(
            fullName = cleanName,
            bio = cleanBio,
            avatarUrl = cleanAvatar
            // isAdmin, followersCount, id remain strictly unchanged
        )

        // Sync with Firestore "users/{uid}"
        try {
            val updates = mapOf<String, Any>(
                "fullName" to cleanName,
                "bio" to cleanBio,
                "avatarUrl" to cleanAvatar
            )
            firestore.collection("users").document(current.id).update(updates).awaitTask()
        } catch (e: Exception) {
            Log.w("AuthManager", "Firestore update note: ${e.message}")
        }

        // Update local Room database
        dao.updateUser(updated)

        // Update user's channel details
        val chan = dao.getChannelByUserIdDirect(current.id)
        if (chan != null) {
            dao.updateChannel(
                chan.copy(
                    name = cleanName,
                    bio = cleanBio,
                    avatarUrl = cleanAvatar
                )
            )
        }

        _currentUser.value = updated
        return Result.success(updated)
    }

    fun logout() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            Log.w("AuthManager", "Sign out note: ${e.message}")
        }
        _currentUser.value = null
    }

    fun setCurrentUserDirect(user: UserEntity) {
        _currentUser.value = user
    }

    suspend fun refreshCurrentUser() {
        val current = _currentUser.value ?: return
        val fresh = dao.getUserByIdDirect(current.id)
        if (fresh != null) {
            _currentUser.value = fresh
        }
    }

    fun getFirestoreInstance(): FirebaseFirestore {
        ensureFirebaseInitialized()
        return firestore
    }

    /**
     * Checks eligibility to start a live stream directly against Firestore
     * verifying the 50 followers rule and account status.
     */
    suspend fun checkLiveEligibilityFromFirestore(userId: String): Result<Boolean> {
        ensureFirebaseInitialized()
        try {
            val userDoc = firestore.collection("users").document(userId).get().awaitTask()
            if (userDoc.exists()) {
                val accountStatus = userDoc.getString("accountStatus") ?: "ACTIVE"
                if (accountStatus.equals("BANNED", ignoreCase = true) || accountStatus.equals("RESTRICTED", ignoreCase = true)) {
                    return Result.failure(Exception("الحساب محظور أو موقوف ولا يمكنه بدء بث مباشر."))
                }
                val followersCount = (userDoc.getLong("followersCount") ?: 0L).toInt()
                // Synchronize with local Room database
                val localUser = dao.getUserByIdDirect(userId)
                if (localUser != null) {
                    dao.insertUser(localUser.copy(followersCount = followersCount, accountStatus = accountStatus))
                }
                if (followersCount < 50) {
                    return Result.failure(Exception("تحتاج إلى 50 متابعًا لبدء بث مباشر."))
                }
                return Result.success(true)
            }
        } catch (e: Exception) {
            Log.w("AuthManager", "Firestore live eligibility check fallback to local: ${e.message}")
        }

        // Fallback to local Room database verification
        val localUser = dao.getUserByIdDirect(userId) ?: return Result.failure(Exception("المستخدم غير موجود"))
        if (localUser.accountStatus.equals("BANNED", ignoreCase = true) || localUser.accountStatus.equals("RESTRICTED", ignoreCase = true)) {
            return Result.failure(Exception("الحساب محظور أو موقوف ولا يمكنه بدء بث مباشر."))
        }
        val channel = dao.getChannelByUserIdDirect(userId)
        val followerCount = maxOf(localUser.followersCount, channel?.subscriberCount ?: 0)
        if (followerCount < 50) {
            return Result.failure(Exception("تحتاج إلى 50 متابعًا لبدء بث مباشر."))
        }
        return Result.success(true)
    }
}
