package com.highcom.passwordmemo

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.material.snackbar.Snackbar
import com.highcom.passwordmemo.ui.viewmodel.GroupListViewModel
import com.highcom.passwordmemo.util.login.LoginDataManager
import kotlinx.coroutines.launch

class ReferencePasswordActivity : AppCompatActivity() {
    private var loginDataManager: LoginDataManager? = null
    private var id: Long = 0
    private var groupId: Long = 0
    private var adContainerView: FrameLayout? = null
    private var mAdView: AdView? = null

    private val groupListViewModel: GroupListViewModel by viewModels {
        GroupListViewModel.Factory((application as PasswordMemoApplication).repository)
    }
    @Suppress("NAME_SHADOWING")
    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reference_password)
        adContainerView = findViewById(R.id.adView_frame_reference)
        adContainerView?.post { loadBanner() }
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        loginDataManager = LoginDataManager.Companion.getInstance(this)

        // バックグラウンドでは画面の中身が見えないようにする
        if (loginDataManager!!.displayBackgroundSwitchEnable) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        // 渡されたデータを取得する
        val intent = intent
        id = intent.getLongExtra("ID", -1)
        groupId = intent.getLongExtra("GROUP", 1)
        title = intent.getStringExtra("TITLE")
        (findViewById<View>(R.id.editRefAccount) as EditText).setText(intent.getStringExtra("ACCOUNT"))
        (findViewById<View>(R.id.editRefPassword) as EditText).setText(intent.getStringExtra("PASSWORD"))
        (findViewById<View>(R.id.editRefUrl) as EditText).setText(intent.getStringExtra("URL"))
        (findViewById<View>(R.id.editRefMemo) as EditText).setText(intent.getStringExtra("MEMO"))
        lifecycleScope.launch {
            groupListViewModel.groupList.collect { list ->
                for (group in list) {
                    if (groupId == group.groupId) {
                        (findViewById<View>(R.id.editRefGroup) as EditText).setText(group.name)
                        break
                    }
                }
            }
        }

        // アカウントIDをクリックor長押し時の処理
        val accountText = findViewById<View>(R.id.editRefAccount) as EditText
        accountText.setOnClickListener { v ->
            if (loginDataManager!!.copyClipboard == OPERATION_TAP) {
                copyClipBoard(
                    v,
                    (findViewById<View>(R.id.editRefAccount) as EditText).text.toString()
                )
            }
        }
        accountText.setOnLongClickListener { arg0 ->
            if (loginDataManager!!.copyClipboard == OPERATION_LONGPRESS) {
                copyClipBoard(
                    arg0,
                    (findViewById<View>(R.id.editRefAccount) as EditText).text.toString()
                )
            }
            true
        }
        val passwordText = findViewById<View>(R.id.editRefPassword) as EditText
        // パスワードの初期表示設定
        if (loginDataManager!!.passwordVisibleSwitchEnable) passwordText.inputType =
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        // パスワードをクリックor長押し時の処理
        passwordText.setOnClickListener { v ->
            if (loginDataManager!!.copyClipboard == OPERATION_TAP) {
                copyClipBoard(
                    v,
                    (findViewById<View>(R.id.editRefPassword) as EditText).text.toString()
                )
            }
        }
        passwordText.setOnLongClickListener { arg0 ->
            if (loginDataManager!!.copyClipboard == OPERATION_LONGPRESS) {
                copyClipBoard(
                    arg0,
                    (findViewById<View>(R.id.editRefPassword) as EditText).text.toString()
                )
            }
            true
        }

        // URLをクリック時の処理
        val urlText = findViewById<View>(R.id.editRefUrl) as EditText
        urlText.setOnClickListener(View.OnClickListener { // 何も入力されていなかったら何もしない
            if (urlText.text.toString() == "") return@OnClickListener
            val uri = Uri.parse(urlText.text.toString())
            val intent = Intent(Intent.ACTION_VIEW, uri)
            val chooser = Intent.createChooser(intent, "選択")
            startActivity(chooser)
        })
    }

    private fun loadBanner() {
        // Create an ad request.
        mAdView = AdView(this)
        mAdView!!.adUnitId = getString(R.string.admob_unit_id_2)
        adContainerView!!.removeAllViews()
        adContainerView!!.addView(mAdView)
        val adSize = adSize
        mAdView!!.setAdSize(adSize)
        val adRequest = AdRequest.Builder().build()

        // Start loading the ad in the background.
        mAdView!!.loadAd(adRequest)
    }

    @Suppress("DEPRECATION")
    private val adSize: AdSize
        get() {
            // Determine the screen width (less decorations) to use for the ad width.
            val display = windowManager.defaultDisplay
            val outMetrics = DisplayMetrics()
            display.getMetrics(outMetrics)
            val density = outMetrics.density
            var adWidthPixels = adContainerView!!.width.toFloat()

            // If the ad hasn't been laid out, default to the full screen width.
            if (adWidthPixels == 0f) {
                adWidthPixels = outMetrics.widthPixels.toFloat()
            }
            val adWidth = (adWidthPixels / density).toInt()
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
        }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_reference, menu)
        return true
    }

    @SuppressLint("NonConstantResourceId")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val intent: Intent
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.action_copy -> {
                // 入力画面を生成
                intent = Intent(this@ReferencePasswordActivity, InputPasswordActivity::class.java)
                // 選択アイテムを複製モードで設定
                intent.putExtra("EDIT", false)
                intent.putExtra("TITLE", title.toString() + " " + getString(R.string.copy_title))
                intent.putExtra(
                    "ACCOUNT",
                    (findViewById<View>(R.id.editRefAccount) as EditText).text.toString()
                )
                intent.putExtra(
                    "PASSWORD",
                    (findViewById<View>(R.id.editRefPassword) as EditText).text.toString()
                )
                intent.putExtra(
                    "URL",
                    (findViewById<View>(R.id.editRefUrl) as EditText).text.toString()
                )
                intent.putExtra("GROUP", groupId)
                intent.putExtra(
                    "MEMO",
                    (findViewById<View>(R.id.editRefMemo) as EditText).text.toString()
                )
                startActivity(intent)
                finish()
            }

            R.id.action_edit -> {
                // 入力画面を生成
                intent = Intent(this@ReferencePasswordActivity, InputPasswordActivity::class.java)
                // 選択アイテムを編集モードで設定
                intent.putExtra("ID", id)
                intent.putExtra("EDIT", true)
                intent.putExtra("TITLE", title.toString())
                intent.putExtra(
                    "ACCOUNT",
                    (findViewById<View>(R.id.editRefAccount) as EditText).text.toString()
                )
                intent.putExtra(
                    "PASSWORD",
                    (findViewById<View>(R.id.editRefPassword) as EditText).text.toString()
                )
                intent.putExtra(
                    "URL",
                    (findViewById<View>(R.id.editRefUrl) as EditText).text.toString()
                )
                intent.putExtra("GROUP", groupId)
                intent.putExtra(
                    "MEMO",
                    (findViewById<View>(R.id.editRefMemo) as EditText).text.toString()
                )
                startActivity(intent)
                finish()
            }

            else -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("ResourceType")
    override fun onStart() {
        super.onStart()

        // 背景色を設定する
        findViewById<View>(R.id.referencePasswordView).setBackgroundColor(
            loginDataManager!!.backgroundColor
        )
        // テキストサイズを設定する
        setTextSize(loginDataManager!!.textSize)
    }

    // クリップボードにコピーする処理
    private fun copyClipBoard(view: View?, allText: String) {
        // クリップボードへの格納成功時は成功メッセージをトーストで表示
        val result = setClipData(allText)
        if (result) {
            Snackbar.make(view!!, getString(R.string.copy_clipboard_success), Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        } else {
            Snackbar.make(view!!, getString(R.string.copy_clipboard_failure), Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
    }

    // テキストデータをクリップボードに格納する
    private fun setClipData(allText: String): Boolean {
        return try {
            //クリップボードに格納するItemを作成
            val item = ClipData.Item(allText)

            //MIMETYPEの作成
            val mimeType = arrayOfNulls<String>(1)
            mimeType[0] = ClipDescription.MIMETYPE_TEXT_PLAIN

            //クリップボードに格納するClipDataオブジェクトの作成
            val cd = ClipData(ClipDescription("text_data", mimeType), item)

            //クリップボードにデータを格納
            val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(cd)
            true
        } catch (e: Exception) {
            false
        }
    }

    public override fun onDestroy() {
        if (mAdView != null) mAdView!!.destroy()
        //バックグラウンドの場合、全てのActivityを破棄してログイン画面に戻る
        if (loginDataManager!!.displayBackgroundSwitchEnable && PasswordMemoLifecycle.Companion.isBackground) {
            finishAffinity()
        }
        super.onDestroy()
    }

    private fun setTextSize(size: Float) {
        (findViewById<View>(R.id.accountRefView) as TextView).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size - 3
        )
        (findViewById<View>(R.id.editRefAccount) as EditText).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size
        )
        (findViewById<View>(R.id.passwordRefView) as TextView).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size - 3
        )
        (findViewById<View>(R.id.editRefPassword) as EditText).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size
        )
        (findViewById<View>(R.id.urlRefView) as TextView).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size - 3
        )
        (findViewById<View>(R.id.editRefUrl) as EditText).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size
        )
        (findViewById<View>(R.id.groupRefView) as TextView).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size - 3
        )
        (findViewById<View>(R.id.editRefGroup) as EditText).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size
        )
        (findViewById<View>(R.id.editRefMemo) as EditText).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size
        )
    }

    companion object {
        private const val OPERATION_LONGPRESS = 0
        private const val OPERATION_TAP = 1
    }
}