package com.tkbiswas.pilesclinic.native

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tkbiswas.pilesclinic.databinding.ActivityUserPhotoBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Native rebuild -- staff/doctor "My Photo" (avatar). Device-local, matching the
 * WebView (localStorage rk_userPhotos). Pick a gallery image, downscale, and
 * store to SharedPreferences keyed by the logged-in user's mobile.
 */
class UserPhotoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserPhotoBinding
    private lateinit var user: NativeUser
    private var pendingPhotoData: String = ""
    private var targetMobile: String = ""

    private var cameraPhotoUri: Uri? = null

    /** 🔄🔒 V524 (২২.০৮.২০২৬, TK-নির্দেশ) — ঘোরানোর জন্য **মূল ছবি** ও মোট কোণ।
     *  বারবার ঘোরালেও ছবি ঝাপসা হয় না, কারণ প্রতিবার মূল থেকেই একবার বানানো হয়।
     *  (Patient Photo পর্দার হুবহু একই নিয়ম।) */
    private var baseBitmap: android.graphics.Bitmap? = null
    private var rotateDegrees: Int = 0

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) processPickedImage(uri)
    }

    // 🔒 TK-APPROVED (28.07.2026, ফটো-প্রুফে পাশ · খাতার সারি B47): এই পর্দায়
    // এতদিন **ক্যামেরাই ছিল না** — শুধু গ্যালারি। PatientPhotoActivity-র হুবহু
    // একই প্রমাণিত পথ এখানে বসানো হলো, একই processPickedImage()-এ গিয়ে মেশে।
    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) openCamera()
            else Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
        }

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val uri = cameraPhotoUri
            if (success && uri != null) processPickedImage(uri)
        }

    private fun launchCameraWithPermission() {
        val granted = androidx.core.content.ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) openCamera() else requestCameraPermission.launch(android.Manifest.permission.CAMERA)
    }

    private fun openCamera() {
        try {
            val dir = java.io.File(cacheDir, "images").apply { mkdirs() }
            val file = java.io.File(dir, "staff_${System.currentTimeMillis()}.jpg")
            val uri = androidx.core.content.FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            cameraPhotoUri = uri
            takePicture.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(this, "Camera could not open", Toast.LENGTH_SHORT).show()
        }
    }

    /** ⛔ নতুন কিছু নয় — Registration ও Patient Photo-র হুবহু একই পপ-আপ। */
    private fun showPhotoSourceDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Photo"))
            .setItems(arrayOf("📷 Camera", "🖼️ Gallery")) { _, which ->
                if (which == 0) launchCameraWithPermission() else pickImage.launch("image/*")
            }
            .setNegativeButton("Cancel", null)
            .show().also { PremiumAlert.paint(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TK-REPORTED (2026-07-27): this screen has no bottom bar, so until
        // now opening it never retried a save that was still stuck on this
        // phone. A staff member could sit here while a registration or a
        // payment stayed unsent. Same retry every other screen already does.
        try { BottomNav.retryStuckSaves(this) } catch (_: Throwable) { }
        binding = ActivityUserPhotoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        UppercaseInputUtil.applyToAll(binding.root)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically

        val session = NativeSession.current(this)
        if (session == null) { finish(); return }
        user = session

        // Master photo manager: manage another staff member's photo when a
        // targetMobile is passed (and the current user is Master); else self.
        val extraMobile = intent.getStringExtra("targetMobile")?.filter { it.isDigit() }?.takeLast(10) ?: ""
        targetMobile = if (extraMobile.isNotBlank() && user.role == "master") extraMobile else user.mobile.filter { it.isDigit() }.takeLast(10).ifBlank { user.mobile }

        binding.btnBack.setOnClickListener { finish() }
        binding.btnPick.setOnClickListener { showPhotoSourceDialog() }
        binding.btnSave.setOnClickListener { savePhoto() }
        binding.btnRemove.setOnClickListener { removePhoto() }
        // 🔄 V524 (TK-নির্দেশ): এক চাপে ৯০°।
        binding.btnRotate.setOnClickListener { rotatePhoto() }
        // TK-REPORTED PATTERN (2026-07-18): same "still navy" oversight found
        // and fixed on the Draft screen — this screen's buttons were also
        // never explicitly colored. Text/click/enabled-logic unchanged.
        // ⛔ btnPick-এ আর রং বসানো হয় না — ওটা এখন সবুজ পিল (`bg_btn_photo_pill`)।
        binding.btnSave.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#0C9E33"))
        binding.btnRemove.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#B8324A"))

        showPhoto(UserPhotoStore.get(this, targetMobile))
        hasSavedPhoto = !UserPhotoStore.get(this, targetMobile).isNullOrBlank()
        applyPhotoButtons()
    }

    private fun processPickedImage(uri: Uri) {
        lifecycleScope.launch {
            val dataUrl = withContext(Dispatchers.IO) { PhotoUtils.encodeResized(this@UserPhotoActivity, uri) }
            if (dataUrl == null) {
                Toast.makeText(this@UserPhotoActivity, "Could not read image", Toast.LENGTH_SHORT).show()
                return@launch
            }
            pendingPhotoData = dataUrl
            showPhoto(dataUrl)
            // খাতার সারি B47: নতুন ছবি এলে তবেই Save দেখা যায়।
            binding.btnSave.isEnabled = true
            binding.btnSave.visibility = View.VISIBLE
        }
    }

    private fun showPhoto(dataUrl: String?) {
        val bmp = PhotoUtils.decodeDataUrl(dataUrl)
        /* 🔄 V524: এই ছবিটাই ঘোরানোর "মূল"; নতুন ছবি এলে গোনা শূন্য থেকে শুরু। */
        baseBitmap = bmp
        rotateDegrees = 0
        binding.btnRotate.visibility = if (bmp != null) View.VISIBLE else View.GONE
        if (bmp != null) {
            binding.imgPhoto.setImageBitmap(bmp)
            binding.imgPhoto.visibility = View.VISIBLE
        }
    }

    /**
     * 🔄🔒 V524 (TK-নির্দেশ): পর্দার ছবিটা ৯০° ঘুরিয়ে দেয়।
     * আগে সেভ করা ছবিও ঘোরানো যায় — তখন `Save Photo` দেখা যায়।
     * ⛔ নিজে থেকে কিছু সেভ হয় না; TK না চাপলে জমাও হয় না।
     * ⛔ ছবি না থাকলে বোতামটাই দেখা যায় না।
     */
    private fun rotatePhoto() {
        val base = baseBitmap ?: return
        rotateDegrees = (rotateDegrees + 90) % 360
        val shown = PhotoUtils.rotated(base, rotateDegrees)
        val dataUrl = PhotoUtils.encodeBitmap(shown)
        if (dataUrl == null) {
            Toast.makeText(this, "Could not rotate", Toast.LENGTH_SHORT).show()
            return
        }
        pendingPhotoData = dataUrl
        binding.imgPhoto.setImageBitmap(shown)
        binding.imgPhoto.visibility = View.VISIBLE
        binding.btnSave.isEnabled = true
        binding.btnSave.visibility = View.VISIBLE
    }

    /**
     * খাতার সারি B47 — ছবি থাকলে "📷 Change Photo" ও Remove; না থাকলে
     * "📷 Add Photo" আর Remove দেখানোই হয় না। Save শুধু নতুন ছবি বাছার পরে।
     */
    private var hasSavedPhoto = false

    private fun applyPhotoButtons() {
        binding.btnPick.text = if (hasSavedPhoto) "📷 Change Photo" else "📷 Add Photo"
        binding.btnRemove.visibility = if (hasSavedPhoto) View.VISIBLE else View.GONE
    }

    private fun savePhoto() {
        if (pendingPhotoData.isBlank()) return
        UserPhotoStore.set(this, targetMobile, pendingPhotoData)
        Toast.makeText(this, "Photo saved", Toast.LENGTH_SHORT).show()
        binding.btnSave.isEnabled = false
        binding.btnSave.visibility = View.GONE
        hasSavedPhoto = true
        applyPhotoButtons()
    }

    private fun removePhoto() {
        UserPhotoStore.clear(this, targetMobile)
        pendingPhotoData = ""
        binding.imgPhoto.setImageDrawable(null)
        // 🔄 V524: ছবি নেই ⇒ ঘোরানোরও কিছু নেই।
        baseBitmap = null
        rotateDegrees = 0
        binding.btnRotate.visibility = View.GONE
        binding.btnSave.isEnabled = false
        binding.btnSave.visibility = View.GONE
        hasSavedPhoto = false
        applyPhotoButtons()
        Toast.makeText(this, "Photo removed", Toast.LENGTH_SHORT).show()
    }
}
