package com.highcom.passwordmemo.ui.fragment

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
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
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.highcom.passwordmemo.PasswordMemoDrawerActivity
import com.highcom.passwordmemo.R
import com.highcom.passwordmemo.databinding.FragmentReferencePasswordBinding
import com.highcom.passwordmemo.ui.PasswordEditData
import com.highcom.passwordmemo.ui.util.LimitCheckUtil
import com.highcom.passwordmemo.ui.viewmodel.BillingViewModel
import com.highcom.passwordmemo.ui.viewmodel.GroupListViewModel
import com.highcom.passwordmemo.domain.AdBanner
import com.highcom.passwordmemo.domain.DarkModeUtil
import com.highcom.passwordmemo.domain.login.LoginDataManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * パスワード参照画面フラグメント
 *
 */
@AndroidEntryPoint
class ReferencePasswordFragment : Fragment() {
    /** パスワード参照画面のbinding */
    private lateinit var binding: FragmentReferencePasswordBinding
    /** Navigationで渡された引数 */
    private val args: ReferencePasswordFragmentArgs by navArgs()
    /** パスワード編集データ */
    lateinit var passwordEditData: PasswordEditData
    /** ログインデータ管理 */
    @Inject
    lateinit var loginDataManager: LoginDataManager
    /** バナー広告処理 */
    @Inject
    lateinit var adBanner: AdBanner
    /** 広告コンテナ */
    private var adContainerView: FrameLayout? = null
    /** グループ一覧ビューモデル */
    private val groupListViewModel: GroupListViewModel by viewModels()
    /** 課金ビューモデル */
    private val billingViewModel: BillingViewModel by activityViewModels()

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
    ): View {
        // 渡されたデータを取得する
        passwordEditData = args.editData
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_reference_password, container, false)
        binding.fragment = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // BillingViewModelの初期化
        billingViewModel.initializeBillingManager()

        adContainerView = binding.adViewFrameReference
        adContainerView?.post { adBanner.loadBanner(this, adContainerView, getString(R.string.admob_unit_id_2)) }

        // ActionBarに戻るボタンを設定
        val activity = requireActivity()
        if (activity is PasswordMemoDrawerActivity) {
            activity.drawerMenuDisabled()
            activity.toggle.setToolbarNavigationClickListener {
                findNavController().navigate(ReferencePasswordFragmentDirections.actionReferencePasswordFragmentToPasswordListFragment())
            }
        }

        // バックグラウンドでは画面の中身が見えないようにする
        if (loginDataManager.displayBackgroundSwitchEnable) {
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        // パスワードの初期表示設定
        if (loginDataManager.passwordVisibleSwitchEnable) {
            binding.editRefPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }

        requireActivity().title = passwordEditData.title
        lifecycleScope.launch {
            groupListViewModel.groupList.collect { list ->
                for (group in list) {
                    if (passwordEditData.groupId == group.groupId) {
                        binding.editRefGroup.setText(group.name)
                        break
                    }
                }
            }
        }
    }

    /**
     * タップ操作時のクリップボードへのコピーイベント
     *
     * @param view 選択ビュー
     */
    fun onTextClick(view: View) {
        if (view is EditText) {
            if (loginDataManager.copyClipboard == OPERATION_TAP) {
                copyClipBoard(view, view.text.toString())
            }
        }
    }

    /**
     * 長押し操作時のクリップボードへのコピーイベント
     *
     * @param view 選択ビュー
     * @return イベントを消費したか
     */
    fun onTextLongClick(view: View): Boolean {
        if (view is EditText) {
            if (loginDataManager.copyClipboard == OPERATION_LONGPRESS) {
                copyClipBoard(view, view.text.toString())
            }
        }
        return true
    }

    /**
     * URLクリック時のブラウザ選択遷移処理
     *
     * @param view 選択ビュー
     */
    fun onUrlParseTextClick(view: View) {
        if (view is EditText) {
            if (view.text.toString() == "") return
            val uri = Uri.parse(view.text.toString())
            val intent = Intent(Intent.ACTION_VIEW, uri)
            val chooser = Intent.createChooser(intent, "選択")
            startActivity(chooser)
        }
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
            // 複製して編集
            R.id.action_copy -> {
                // 複製して編集は上限チェック
                val editData = passwordEditData.copy()
                editData.edit = false
                editData.id = 0
                editData.title += " " + getString(R.string.copy_title)
                LimitCheckUtil.checkAndNavigate(this, billingViewModel) {
                    findNavController().navigate(ReferencePasswordFragmentDirections.actionReferencePasswordFragmentToInputPasswordFragment(editData = editData))
                }
            }
            // 編集
            R.id.action_edit -> {
                // 入力画面に遷移
                passwordEditData.edit = true
                findNavController().navigate(ReferencePasswordFragmentDirections.actionReferencePasswordFragmentToInputPasswordFragment(editData = passwordEditData))
            }

            else -> findNavController().navigate(ReferencePasswordFragmentDirections.actionReferencePasswordFragmentToPasswordListFragment())
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("ResourceType")
    override fun onStart() {
        super.onStart()

        // 背景色を設定する（ダークモード時はテーマの色を優先）
        if (!DarkModeUtil.isDarkModeEnabled(requireContext(), loginDataManager.darkMode)) {
            binding.referencePasswordView.setBackgroundColor(loginDataManager.backgroundColor)
        }
        // テキストサイズを設定する
        setTextSize(loginDataManager.textSize)
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
        adBanner?.destroy()
    }

    /**
     * テキストサイズ設定処理
     *
     * @param size 指定テキストサイズ
     */
    private fun setTextSize(size: Float) {
        binding.accountRefView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size - 3)
        binding.editRefAccount.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
        binding.passwordRefView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size - 3)
        binding.editRefPassword.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
        binding.urlRefView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size - 3)
        binding.editRefUrl.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
        binding.groupRefView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size - 3)
        binding.editRefGroup.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
        binding.editRefMemo.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
    }

    companion object {
        /** 長押し操作 */
        private const val OPERATION_LONGPRESS = 0
        /** タップ操作 */
        private const val OPERATION_TAP = 1
    }
}