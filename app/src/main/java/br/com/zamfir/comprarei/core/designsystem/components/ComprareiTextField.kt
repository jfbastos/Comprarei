package br.com.zamfir.comprarei.core.designsystem.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.designsystem.theme.ComprareiTheme

/**
 * Campo de texto padrão do app (outlined).
 *
 * @param error mensagem de erro; quando não nula, o campo fica vermelho e mostra a mensagem abaixo.
 * @param supportingText texto de apoio exibido quando não há erro.
 * @param isPassword oculta o texto e mostra o botão de mostrar/ocultar senha.
 * @param leadingContent conteúdo inicial customizado (ex.: bolinha de cor); tem prioridade sobre [leadingIcon].
 * @param trailingContent conteúdo final customizado (ex.: seta de dropdown); ignorado quando [isPassword].
 * @param readOnly campo que não abre o teclado, para seletores (data, dropdown); use [interactionSource] para o toque.
 */
@Composable
fun ComprareiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    supportingText: String? = null,
    placeholder: String? = null,
    prefix: String? = null,
    leadingIcon: Painter? = null,
    isPassword: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    readOnly: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
) {
    // Estado puramente visual do componente, não pertence ao ViewModel.
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val bottomText = error ?: supportingText

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        singleLine = singleLine,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        prefix = prefix?.let { { Text(it) } },
        leadingIcon = leadingContent ?: leadingIcon?.let { { Icon(painter = it, contentDescription = null) } },
        trailingIcon = if (isPassword) {
            { PasswordToggle(visible = passwordVisible, onToggle = { passwordVisible = !passwordVisible }) }
        } else {
            trailingContent
        },
        isError = error != null,
        supportingText = bottomText?.let { { Text(it) } },
        visualTransformation = when {
            isPassword && !passwordVisible -> PasswordVisualTransformation()
            else -> visualTransformation
        },
        keyboardOptions = if (isPassword && keyboardOptions == KeyboardOptions.Default) {
            KeyboardOptions(keyboardType = KeyboardType.Password)
        } else {
            keyboardOptions
        },
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
        shape = MaterialTheme.shapes.small,
    )
}

@Composable
private fun PasswordToggle(visible: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        Icon(
            painter = painterResource(
                if (visible) R.drawable.visibility_off_black_24dp else R.drawable.visibility_black_24dp
            ),
            contentDescription = stringResource(
                if (visible) R.string.cd_hide_password else R.string.cd_show_password
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ComprareiTextFieldPreview() {
    ComprareiTheme {
        Surface {
            Column(Modifier.padding(16.dp)) {
                ComprareiTextField(
                    value = "Arroz",
                    onValueChange = {},
                    label = "Nome",
                    modifier = Modifier.fillMaxWidth(),
                )
                ComprareiTextField(
                    value = "12,90",
                    onValueChange = {},
                    label = "Preço",
                    prefix = "R$ ",
                    supportingText = "Opcional",
                    modifier = Modifier.fillMaxWidth(),
                )
                ComprareiTextField(
                    value = "",
                    onValueChange = {},
                    label = "E-mail",
                    leadingIcon = painterResource(R.drawable.email_black_24dp),
                    error = "Não pode ser vazio!",
                    modifier = Modifier.fillMaxWidth(),
                )
                ComprareiTextField(
                    value = "segredo123",
                    onValueChange = {},
                    label = "Senha",
                    leadingIcon = painterResource(R.drawable.lock_black_24dp),
                    isPassword = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ComprareiTextFieldDarkPreview() {
    ComprareiTheme(darkTheme = true) {
        Surface {
            Column(Modifier.padding(16.dp)) {
                ComprareiTextField(value = "Arroz", onValueChange = {}, label = "Nome")
                ComprareiTextField(value = "", onValueChange = {}, label = "E-mail", error = "Não pode ser vazio!")
            }
        }
    }
}
