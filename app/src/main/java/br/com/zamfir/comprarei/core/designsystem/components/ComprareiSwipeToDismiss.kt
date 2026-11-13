package br.com.zamfir.comprarei.core.designsystem.components

import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Estado de [androidx.compose.material3.SwipeToDismissBox] que NÃO é salvo.
 *
 * O `rememberSwipeToDismissBoxState()` do Material usa rememberSaveable, e a LazyColumn guarda esse
 * estado pela chave do item mesmo depois que ele sai da lista. Num "desfazer exclusão" o item volta
 * com a mesma chave, o estado volta como "deslizado para excluir", e o SwipeToDismissBox chama
 * onDismiss de novo: o item era excluído outra vez logo depois de restaurado.
 *
 * Use este em listas com exclusão por gesto e desfazer.
 */
@Composable
fun rememberUnsavedSwipeToDismissBoxState(): SwipeToDismissBoxState {
    val positionalThreshold = SwipeToDismissBoxDefaults.positionalThreshold
    return remember { SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled, positionalThreshold) }
}
