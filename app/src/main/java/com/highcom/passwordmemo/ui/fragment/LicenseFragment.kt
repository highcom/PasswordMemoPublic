package com.highcom.passwordmemo.ui.fragment

import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.ViewGroup
import com.highcom.passwordmemo.R
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.navigation.fragment.findNavController
import com.highcom.passwordmemo.PasswordMemoDrawerActivity
import com.highcom.passwordmemo.databinding.FragmentLicenseBinding
import com.highcom.passwordmemo.domain.login.LoginDataManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * ライセンス画面フラグメント
 *
 */
@AndroidEntryPoint
class LicenseFragment : Fragment() {
    /** ライセンス画面のbinding */
    private lateinit var binding: FragmentLicenseBinding
    /** ログインデータ管理 */
    @Inject
    lateinit var loginDataManager: LoginDataManager

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
        binding = FragmentLicenseBinding.inflate(inflater)
        return binding.root
    }

    @SuppressLint("ResourceType")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().title = getString(R.string.license)
        // ActionBarに戻るボタンを設定
        val activity = requireActivity()
        if (activity is PasswordMemoDrawerActivity) {
            activity.drawerMenuDisabled()
            activity.toggle.setToolbarNavigationClickListener {
                findNavController().navigate(LicenseFragmentDirections.actionLicenseFragmentToSettingFragment())
            }
        }

        // バックグラウンドでは画面の中身が見えないようにする
        if (loginDataManager.displayBackgroundSwitchEnable) {
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    @SuppressLint("ResourceType")
    override fun onStart() {
        super.onStart()
        binding.licenseView.setBackgroundColor(loginDataManager.backgroundColor)
    }
}