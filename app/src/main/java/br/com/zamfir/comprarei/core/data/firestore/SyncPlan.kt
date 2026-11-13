package br.com.zamfir.comprarei.core.data.firestore

/**
 * O que o backup precisa fazer numa coleção para ela ficar igual ao banco local.
 * [toDelete] são ids de documentos (firestoreUUID); [toWrite] são os itens novos ou alterados.
 */
data class SyncPlan<T>(val toDelete: List<String>, val toWrite: List<T>) {
    val isEmpty: Boolean get() = toDelete.isEmpty() && toWrite.isEmpty()
}

/**
 * Compara o banco local com a nuvem. Função pura, para testar sem o Firestore.
 *
 * - Documento na nuvem que não existe no aparelho: apagar.
 * - Item local novo ou diferente (qualquer campo, inclusive o id local): gravar.
 * - Item igual: nada, para não gastar escrita.
 * - Item local sem firestoreUUID não vira documento (não teria id); quem chama decide se avisa.
 */
fun <T> planSync(
    local: List<T>,
    remote: Map<String, T>,
    uuidOf: (T) -> String,
): SyncPlan<T> {
    val localByUuid = local.filter { uuidOf(it).isNotBlank() }.associateBy(uuidOf)

    val toDelete = remote.keys.filter { it !in localByUuid }
    val toWrite = localByUuid.values.filter { item ->
        val current = remote[uuidOf(item)]
        current == null || current != item
    }
    return SyncPlan(toDelete, toWrite)
}
