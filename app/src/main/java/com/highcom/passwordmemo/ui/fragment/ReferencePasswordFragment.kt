package com.highcom.passwordmemo.ui.fragment

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
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.material.snackbar.Snackbar
import com.highcom.passwordmemo.PasswordMemoApplication
import com.highcom.passwordmemo.PasswordMemoLifecycle
import com.highcom.passwordmemo.R
import com.highcom.passwordmemo.ui.PasswordEditData
import com.highcom.passwordmemo.ui.viewmodel.GroupListViewModel
import com.highcom.passwordmemo.util.login.LoginDataManager
import kotlinx.coroutines.launch

/**
 * パスワード参照画面フラグメント
 *
 */
class ReferencePasswordFragment : Fragment() {
    /** パスワード参照画面のビュー */
    private var rootView: View? = null
    /** ログインデータ管理 */
    private var loginDataManager: LoginDataManager? = null
    /** パスワードデータID */
    private var id: Long = 0
    /** グループID */
    private var groupId: Long = 0
    /** 広告コンテナ */
    private var adContainerView: FrameLayout? = null
    /** 広告ビュー */
    private var mAdView: AdView? = null
    /** グループ一覧ビューモデル */
    private val groupListViewModel: GroupListViewModel by viewModels {
        GroupListViewModel.Factory((requireActivity().application as PasswordMemoApplication).repository)
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Fragmentのメニューを有効にする
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.fragment_reference_password, container, false)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adContainerView = rootView?.findViewById(R.id.adView_frame_reference)
        adContainerView?.post { loadBanner() }
        loginDataManager = (requireActivity().application as PasswordMemoApplication).loginDataManager

        // ActionBarに戻るボタンを設定
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // バックグラウンドでは画面の中身が見えないようにする
        if (loginDataManager!!.displayBackgroundSwitchEnable) {
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        // 渡されたデータを取得する
        val args: ReferencePasswordFragmentArgs by navArgs()
        id = args.editData.id
        groupId = args.editData.groupId
        requireActivity().title = args.editData.title
        // TODO:edit_ref_accountとかに直す
        rootView?.findViewById<EditText>(R.id.edit_ref_account)?.setText(args.editData.account)
        rootView?.findViewById<EditText>(R.id.edit_ref_password)?.setText(args.editData.password)
        rootView?.findViewById<EditText>(R.id.edit_ref_url)?.setText(args.editData.url)
        rootView?.findViewById<EditText>(R.id.edit_ref_memo)?.setText(args.editData.memo)
        lifecycleScope.launch {
            groupListViewModel.groupList.collect { list ->
                for (group in list) {
                    if (groupId == group.groupId) {
                        rootView?.findViewById<EditText>(R.id.edit_ref_group)?.setText(group.name)
                        break
                    }
                }
            }
        }

        // アカウントIDをクリックor長押し時の処理
        val accountText = rootView?.findViewById<EditText>(R.id.edit_ref_account)
        accountText?.setOnClickListener { v ->
            if (loginDataManager!!.copyClipboard == OPERATION_TAP) {
                copyClipBoard(
                    v,
                    rootView?.findViewById<EditText>(R.id.edit_ref_account)?.text.toString()
                )
            }
        }
        accountText?.setOnLongClickListener { arg0 ->
            if (loginDataManager!!.copyClipboard == OPERATION_LONGPRESS) {
                copyClipBoard(
                    arg0,
                    rootView?.findViewById<EditText>(R.id.edit_ref_account)?.text.toString()
                )
            }
            true
        }
        val passwordText = rootView?.findViewById<EditText>(R.id.edit_ref_password)
        // パスワードの初期表示設定
        if (loginDataManager!!.passwordVisibleSwitchEnable) passwordText?.inputType =
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        // パスワードをクリックor長押し時の処理
        passwordText?.setOnClickListener { v ->
            if (loginDataManager!!.copyClipboard == OPERATION_TAP) {
                copyClipBoard(
                    v,
                    rootView?.findViewById<EditText>(R.id.edit_ref_password)?.text.toString()
                )
            }
        }
        passwordText?.setOnLongClickListener { arg0 ->
            if (loginDataManager!!.copyClipboard == OPERATION_LONGPRESS) {
                copyClipBoard(
                    arg0,
                    rootView?.findViewById<EditText>(R.id.edit_ref_password)?.text.toString()
                )
            }
            true
        }

        // URLをクリック時の処理
        val urlText = rootView?.findViewById<EditText>(R.id.edit_ref_url)
        urlText?.setOnClickListener(View.OnClickListener { // 何も入力されていなかったら何もしない
            if (urlText.text.toString() == "") return@OnClickListener
            val uri = Uri.parse(urlText.text.toString())
            val intent = Intent(Intent.ACTION_VIEW, uri)
            val chooser = Intent.createChooser(intent, "選択")
            startActivity(chooser)
        })
    }

    /**
     * バナー広告ロード処理
     *
     */
    private fun loadBanner() {
        // Create an ad request.
        mAdView = AdView(requireContext())
        mAdView!!.adUnitId = getString(R.string.admob_unit_id_2)
        adContainerView!!.removeAllViews()
        adContainerView!!.addView(mAdView)
        val adSize = adSize
        mAdView!!.setAdSize(adSize)
        val adRequest = AdRequest.Builder().build()

        // Start loading the ad in the background.
        mAdView!!.loadAd(adRequest)
    }

    /** 広告サイズ設定 */
    @Suppress("DEPRECATION")
    private val adSize: AdSize
        get() {
            // Determine the screen width (less decorations) to use for the ad width.
            val display = requireActivity().windowManager.defaultDisplay
            val outMetrics = DisplayMetrics()
            display.getMetrics(outMetrics)
            val density = outMetrics.density
            var adWidthPixels = adContainerView!!.width.toFloat()

            // If the ad hasn't been laid out, default to the full screen width.
            if (adWidthPixels == 0f) {
                adWidthPixels = outMetrics.widthPixels.toFloat()
            }
            val adWidth = (adWidthPixels / density).toInt()
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(requireContext(), adWidth)
        }

    @Deprecated("Deprecated in Java", ReplaceWith(
        "inflater.inflate(R.menu.menu_reference, menu)",
        "com.highcom.passwordmemo.R"
    )
    )
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_reference, menu)
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            // 戻るボタン
            android.R.id.home -> findNavController().navigate(R.id.action_referencePasswordFragment_to_passwordListFragment)
            // 複製して編集
            R.id.action_copy -> {
                // 入力画面に遷移
                val passwordEditData = PasswordEditData(
                    edit = false,
                    title = requireActivity().title?.toString() + " " + getString(R.string.copy_title),
                    account = rootView?.findViewById<EditText>(R.id.edit_ref_account)?.text.toString(),
                    password = rootView?.findViewById<EditText>(R.id.edit_ref_password)?.text.toString(),
                    url = rootView?.findViewById<EditText>(R.id.edit_ref_url)?.text.toString(),
                    groupId = groupId,
                    memo = rootView?.findViewById<EditText>(R.id.edit_ref_memo)?.text.toString()
                )
                findNavController().navigate(ReferencePasswordFragmentDirections.actionReferencePasswordFragmentToInputPasswordFragment(editData = passwordEditData))
                // TODO:以前はfinishしているがNavigationでは問題ないか確認
//                finish()
            }
            // 編集
            R.id.action_edit -> {
                // 入力画面に遷移
                val passwordEditData = PasswordEditData(
                    edit = true,
                    id = id,
                    title = requireActivity().title?.toString() ?: "",
                    account = rootView?.findViewById<EditText>(R.id.edit_ref_account)?.text.toString(),
                    password = rootView?.findViewById<EditText>(R.id.edit_ref_password)?.text.toString(),
                    url = rootView?.findViewById<EditText>(R.id.edit_ref_url)?.text.toString(),
                    groupId = groupId,
                    memo = rootView?.findViewById<EditText>(R.id.edit_ref_memo)?.text.toString()
                )
                findNavController().navigate(ReferencePasswordFragmentDirections.actionReferencePasswordFragmentToInputPasswordFragment(editData = passwordEditData))
                // TODO:以前はfinishしているがNavigationでは問題ないか確認
//                finish()
            }

            else -> findNavController().navigate(R.id.action_referencePasswordFragment_to_passwordListFragment)
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("ResourceType")
    override fun onStart() {
        super.onStart()

        // 背景色を設定する
        rootView?.findViewById<View>(R.id.reference_password_view)?.setBackgroundColor(loginDataManager!!.backgroundColor)
        // テキストサイズを設定する
        setTextSize(loginDataManager!!.textSize)
    }

    /**
     * クリップボードコピー処理
     *
     * @param view 通知用ビュー
     * @param allText コピー対象文字列
     */
    private fun copyClipBoard(view: View, allText: String) {
        // クリップボードへの格納成功時は成功メッセージをトーストで表示
        val result = setClipData(allText)
        if (result) {
            Snackbar.make(view, getString(R.string.copy_clipboard_success), Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        } else {
            Snackbar.make(view, getString(R.string.copy_clipboard_failure), Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
    }

    /**
     * テキストデータをクリップボードに格納する処理
     *
     * @param allText 格納対象文字列
     * @return 格納できたかどうか
     */
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
            val cm = requireActivity().getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(cd)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (mAdView != null) mAdView!!.destroy()
        //バックグラウンドの場合、全てのActivityを破棄してログイン画面に戻る
        if (loginDataManager!!.displayBackgroundSwitchEnable && PasswordMemoLifecycle.isBackground) {
            // TODO:これでログイン画面に戻るのか？
            requireActivity().finishAffinity()
        }
    }

    /**
     * テキストサイズ設定処理
     *
     * @param size 指定テキストサイズ
     */
    private fun setTextSize(size: Float) {
        rootView?.findViewById<TextView>(R.id.account_ref_view)?.setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size - 3
        )
        rootView?.findViewById<EditText>(R.id.edit_ref_account)?.setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size
        )
        rootView?.findViewById<TextView>(R.id.password_ref_view)?.setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size - 3
        )
        rootView?.findViewById<EditText>(R.id.edit_ref_password)?.setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size
        )
        rootView?.findViewById<TextView>(R.id.url_ref_view)?.setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size - 3
        )
        rootView?.findViewById<EditText>(R.id.edit_ref_url)?.setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size
        )
        rootView?.findViewById<TextView>(R.id.group_ref_view)?.setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size - 3
        )
        rootView?.findViewById<EditText>(R.id.edit_ref_group)?.setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size
        )
        rootView?.findViewById<EditText>(R.id.edit_ref_memo)?.setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size
        )
    }

    companion object {
        /** 長押し操作 */
        private const val OPERATION_LONGPRESS = 0
        /** タップ操作 */
        private const val OPERATION_TAP = 1
    }
}