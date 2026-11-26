package com.fernando.centraldomotorista.ui.screens.costcenter

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * Representa texto de interface que pode ser resolvido a partir de um recurso de string
 * (com argumentos opcionais) ou de uma mensagem dinâmica já formatada.
 */
sealed interface UiText {
    data class Res(
        @StringRes val id: Int,
        val args: List<Any> = emptyList()
    ) : UiText {
        constructor(@StringRes id: Int, vararg args: Any) : this(id, args.toList())
    }

    data class Raw(val value: String) : UiText
}

@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Res -> if (args.isEmpty()) stringResource(id) else stringResource(id, *args.toTypedArray())
    is UiText.Raw -> value
}

fun UiText.resolve(context: Context): String = when (this) {
    is UiText.Res -> if (args.isEmpty()) context.getString(id) else context.getString(id, *args.toTypedArray())
    is UiText.Raw -> value
}

