package com.highcom.passwordmemo.service

import android.app.assist.AssistStructure
import android.os.Build
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import com.highcom.passwordmemo.R
import com.highcom.passwordmemo.data.PasswordDao
import com.highcom.passwordmemo.domain.billing.PurchaseManager
import com.highcom.passwordmemo.domain.login.LoginDataManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * 登録データを利用したオートフィルサービス。
 * PasswordEntityのurlとリクエストのドメインが一致するものを特定し、accountとpasswordを提供する。
 */
@RequiresApi(Build.VERSION_CODES.O)
@AndroidEntryPoint
class PasswordMemoAutofillService : AutofillService() {

    @Inject
    lateinit var passwordDao: PasswordDao

    @Inject
    lateinit var loginDataManager: LoginDataManager

    @Inject
    lateinit var purchaseManager: PurchaseManager

    override fun onFillRequest(request: FillRequest, cancellationSignal: android.os.CancellationSignal, callback: FillCallback) {

        if (!loginDataManager.autofillSwitchEnable) {
            callback.onSuccess(null)
            return
        }
        if (!purchaseManager.hasActiveSubscription()) {
            callback.onSuccess(null)
            return
        }

        val lastContext = request.fillContexts.lastOrNull() ?: run {
            callback.onSuccess(null)
            return
        }
        val structure = lastContext.structure

        val (requestDomainFromStructure, usernameIds, passwordIds) = parseStructure(structure)
        var requestDomain = requestDomainFromStructure
        if (requestDomain.isNullOrBlank()) {
            for (ctx in request.fillContexts) {
                if (ctx.structure === structure) continue
                val s = ctx.structure ?: continue
                val (domain, _, _) = parseStructure(s)
                if (!domain.isNullOrBlank()) {
                    requestDomain = domain
                    break
                }
            }
        }
        if (usernameIds.isEmpty() && passwordIds.isEmpty()) {
            callback.onSuccess(null)
            return
        }

        val passwords = runBlocking {
            try {
                passwordDao.getAllPasswords()
            } catch (e: Exception) {
                emptyList()
            }
        }

        val matched = if (requestDomain.isNullOrBlank()) {
            // Chromeなどでドメインが取得できない場合は全件を候補表示（ユーザーが選択）
            passwords
        } else {
            passwords.filter { entity ->
                requestDomain == extractDomain(entity.url)
            }
        }

        if (matched.isEmpty()) {
            callback.onSuccess(null)
            return
        }

        val responseBuilder = FillResponse.Builder()
        for (entity in matched) {
            val presentation = RemoteViews(applicationContext.packageName, R.layout.autofill_dataset_item).apply {
                setTextViewText(R.id.autofill_item_title, entity.title.ifBlank { entity.account }.ifBlank { applicationContext.getString(R.string.app_name) })
            }
            val datasetBuilder = Dataset.Builder(presentation)
            usernameIds.forEach { id -> datasetBuilder.setValue(id, AutofillValue.forText(entity.account)) }
            passwordIds.forEach { id -> datasetBuilder.setValue(id, AutofillValue.forText(entity.password)) }
            responseBuilder.addDataset(datasetBuilder.build())
        }
        callback.onSuccess(responseBuilder.build())
    }

    override fun onSaveRequest(request: android.service.autofill.SaveRequest, callback: android.service.autofill.SaveCallback) {
        // 保存リクエストは扱わない（登録はアプリ内のみ）
        callback.onSuccess()
    }

    private fun parseStructure(structure: AssistStructure): Triple<String?, List<AutofillId>, List<AutofillId>> {
        var domain: String? = null
        val usernameIds = mutableListOf<AutofillId>()
        val passwordIds = mutableListOf<AutofillId>()

        fun traverse(node: AssistStructure.ViewNode) {
            node.webDomain?.let { d -> if (domain.isNullOrBlank()) domain = normalizeDomain(d) }
            val hints = node.autofillHints?.map { it.lowercase() } ?: emptyList()
            when {
                hints.any { it in listOf("username", "email", "emailaddress") } -> node.autofillId?.let { if (it !in usernameIds) usernameIds.add(it) }
                hints.any { it in listOf("password", "current-password", "new-password") } -> node.autofillId?.let { if (it !in passwordIds) passwordIds.add(it) }
            }
            for (i in 0 until node.childCount) {
                traverse(node.getChildAt(i))
            }
        }

        for (i in 0 until structure.windowNodeCount) {
            structure.getWindowNodeAt(i).rootViewNode?.let { traverse(it) }
        }
        return Triple(domain, usernameIds, passwordIds)
    }

    private fun normalizeDomain(domain: String?): String? {
        if (domain.isNullOrBlank()) return null
        var d = domain.lowercase().trim()
        if (d.startsWith("www.")) d = d.removePrefix("www.")
        return d.ifBlank { null }
    }

    private fun extractDomain(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val trimmed = url.trim()
        return try {
            val host = if (trimmed.contains("://")) {
                java.net.URI(trimmed).host
            } else {
                trimmed.trimStart('/').split('/').firstOrNull()?.takeIf { it.contains('.') || it.isNotEmpty() }
            }
            host?.let { normalizeDomain(it) }
        } catch (e: Exception) {
            null
        }
    }
}
