package com.kkaloai.app.ui.weight

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kkaloai.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightEntryScreen(
    onBack: () -> Unit,
    viewModel: WeightEntryViewModel = hiltViewModel()
) {
    val latest by viewModel.latestWeight.collectAsState()
    val status by viewModel.status.collectAsState()
    var draft by remember { mutableStateOf("") }

    LaunchedEffect(status) {
        if (status is WeightEntryViewModel.Status.Saved) {
            draft = ""
            viewModel.clearStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.weight_title_screen), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.MonitorWeight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(stringResource(R.string.weight_latest), style = MaterialTheme.typography.labelMedium)
                        Text(
                            latest?.let { stringResource(R.string.weight_value_kg, it.toFloat()) } ?: stringResource(R.string.weight_none_yet),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it.filter { ch -> ch.isDigit() || ch == '.' } },
                label = { Text(stringResource(R.string.weight_input_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            val parsed = draft.toDoubleOrNull()
            Button(
                onClick = { parsed?.let { viewModel.saveWeight(it) } },
                enabled = parsed != null && parsed in 20.0..400.0 && status !is WeightEntryViewModel.Status.Saving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (status is WeightEntryViewModel.Status.Saving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.weight_save))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (val s = status) {
                is WeightEntryViewModel.Status.Error -> Text(s.message, color = MaterialTheme.colorScheme.error)
                else -> {}
            }

            Text(
                stringResource(R.string.weight_disclaimer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
