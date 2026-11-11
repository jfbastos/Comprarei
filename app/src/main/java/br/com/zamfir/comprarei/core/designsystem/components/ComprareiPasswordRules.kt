package br.com.zamfir.comprarei.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.designsystem.theme.ComprareiTheme
import br.com.zamfir.comprarei.core.domain.validation.PasswordCheck
import br.com.zamfir.comprarei.core.domain.validation.PasswordRules

/**
 * Lista das regras de senha com ✓/✗ ao vivo.
 * Neutra enquanto a senha está vazia, verde quando a regra é atendida e vermelha quando não.
 */
@Composable
fun ComprareiPasswordRules(
    check: PasswordCheck,
    isEmpty: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        RuleRow(text = stringResource(R.string.six_chars_hint), ok = check.hasMinLength, isEmpty = isEmpty)
        RuleRow(text = stringResource(R.string.uppercase_hint), ok = check.hasUppercase, isEmpty = isEmpty)
        RuleRow(text = stringResource(R.string.number_hint), ok = check.hasDigit, isEmpty = isEmpty)
    }
}

@Composable
private fun RuleRow(text: String, ok: Boolean, isEmpty: Boolean) {
    val color = when {
        isEmpty -> MaterialTheme.colorScheme.onSurfaceVariant
        ok -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.error
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(if (ok) R.drawable.ic_baseline_done_24 else R.drawable.ic_baseline_close_24),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ComprareiPasswordRulesPreview() {
    ComprareiTheme {
        ComprareiPasswordRules(check = PasswordRules.check("abc1"), isEmpty = false)
    }
}
