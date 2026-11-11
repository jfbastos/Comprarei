package br.com.zamfir.comprarei.core.domain.validation

/** Regras de senha do app (cadastro e alteração no perfil). */
data class PasswordCheck(
    val hasMinLength: Boolean,
    val hasUppercase: Boolean,
    val hasDigit: Boolean,
) {
    val isValid: Boolean get() = hasMinLength && hasUppercase && hasDigit
}

object PasswordRules {
    const val MIN_LENGTH = 6

    fun check(password: String) = PasswordCheck(
        hasMinLength = password.length >= MIN_LENGTH,
        hasUppercase = password.any { it.isUpperCase() },
        hasDigit = password.any { it.isDigit() },
    )
}
