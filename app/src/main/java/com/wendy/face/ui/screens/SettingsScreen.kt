package com.wendy.face.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val sharedPreferences = remember {
        context.getSharedPreferences("face_app_settings", Context.MODE_PRIVATE)
    }

    var personalization by remember { mutableStateOf(sharedPreferences.getString("personalization", "") ?: "") }
    var llmBaseUrl by remember { mutableStateOf(sharedPreferences.getString("llm_base_url", "https://ark.cn-beijing.volces.com/api/v3/") ?: "") }
    var llmApiKey by remember { mutableStateOf(sharedPreferences.getString("llm_api_key", "") ?: "") }
    var llmModel by remember { mutableStateOf(sharedPreferences.getString("llm_model", "deepseek-v3-250324") ?: "") }
    var settingsPassword by remember { mutableStateOf(sharedPreferences.getString("settings_password", "123456") ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = llmBaseUrl,
                onValueChange = {
                    llmBaseUrl = it
                    with(sharedPreferences.edit()) {
                        putString("llm_base_url", it)
                        apply()
                    }
                },
                label = { Text("LLM Base URL") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = llmApiKey,
                onValueChange = {
                    llmApiKey = it
                    with(sharedPreferences.edit()) {
                        putString("llm_api_key", it)
                        apply()
                    }
                },
                label = { Text("LLM API Key") },
                placeholder = { Text("如果为空,则使用编译时配置")},
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = llmModel,
                onValueChange = {
                    llmModel = it
                    with(sharedPreferences.edit()) {
                        putString("llm_model", it)
                        apply()
                    }
                },
                label = { Text("LLM Model") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = settingsPassword,
                onValueChange = {
                    settingsPassword = it
                    with(sharedPreferences.edit()) {
                        putString("settings_password", it)
                        apply()
                    }
                },
                label = { Text("设置页密码") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = personalization,
                onValueChange = {
                    personalization = it
                    with(sharedPreferences.edit()) {
                        putString("personalization", it)
                        apply()
                    }
                },
                label = { Text("个性化") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                placeholder = { Text("在这里输入您的个性化需求，例如：着重突出如让眼角提升,会对命格产生较好影响。") }
            )
        }
    }
}