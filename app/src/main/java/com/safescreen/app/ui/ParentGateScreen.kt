package com.safescreen.app.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.safescreen.app.R
import com.safescreen.app.graph

@Composable
fun ParentGateScreen(onUnlocked: () -> Unit, onBack: () -> Unit) {
    val pin = LocalContext.current.graph.parentPin
    val creating = remember { !pin.isSet }
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<Int?>(null) }

    ScreenScaffold(
        title = stringResource(if (creating) R.string.pin_create_title else R.string.pin_enter_title),
        onBack = onBack,
    ) {
        PinField(first, R.string.pin_hint) { first = it; error = null }
        if (creating) PinField(second, R.string.pin_confirm) { second = it; error = null }
        error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
        Button(
            enabled = first.length in 4..8,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                when {
                    creating && first != second -> error = R.string.pin_mismatch
                    creating -> { pin.set(first); onUnlocked() }
                    pin.verify(first) -> onUnlocked()
                    else -> { error = R.string.pin_wrong; first = "" }
                }
            },
        ) { Text(stringResource(R.string.pin_continue)) }
        Text(stringResource(R.string.pin_note), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun PinField(value: String, label: Int, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { new -> onChange(new.filter(Char::isDigit).take(8)) },
        label = { Text(stringResource(label)) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}
