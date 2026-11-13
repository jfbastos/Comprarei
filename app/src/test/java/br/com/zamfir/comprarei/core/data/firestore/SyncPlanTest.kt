package br.com.zamfir.comprarei.core.data.firestore

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import assertk.assertions.isTrue
import br.com.zamfir.comprarei.core.database.entity.Cart
import org.junit.jupiter.api.Test

class SyncPlanTest {

    private fun cart(uuid: String, name: String = "Mercado", id: Int = 1) =
        Cart(name = name, data = "13/11/2026", total = "R$ 10,00", position = 0, firestoreUUID = uuid, id = id)

    private fun plan(local: List<Cart>, remote: List<Cart>) =
        planSync(local, remote.associateBy { it.firestoreUUID }, uuidOf = { it.firestoreUUID })

    @Test
    fun `equal data produces no operation`() {
        val result = plan(local = listOf(cart("a"), cart("b", id = 2)), remote = listOf(cart("a"), cart("b", id = 2)))

        assertThat(result.isEmpty).isTrue()
    }

    @Test
    fun `new local item is written`() {
        val result = plan(local = listOf(cart("a"), cart("b", id = 2)), remote = listOf(cart("a")))

        assertThat(result.toWrite.map { it.firestoreUUID }).containsExactly("b")
        assertThat(result.toDelete).isEmpty()
    }

    @Test
    fun `changed field is written`() {
        val result = plan(local = listOf(cart("a", name = "Feira")), remote = listOf(cart("a")))

        assertThat(result.toWrite.map { it.name }).containsExactly("Feira")
    }

    @Test
    fun `changed local id is written even though id is outside equals`() {
        val result = plan(local = listOf(cart("a", id = 7)), remote = listOf(cart("a", id = 1)))

        assertThat(result.toWrite.map { it.id }).containsExactly(7)
    }

    @Test
    fun `remote document missing locally is deleted`() {
        val result = plan(local = listOf(cart("a")), remote = listOf(cart("a"), cart("b"), cart("c")))

        assertThat(result.toDelete).containsExactlyInAnyOrder("b", "c")
        assertThat(result.toWrite).isEmpty()
    }

    @Test
    fun `empty local database deletes everything remote`() {
        // Por isso o backup só roda com a restauração concluída: aqui a função não tem como saber.
        val result = plan(local = emptyList(), remote = listOf(cart("a"), cart("b")))

        assertThat(result.toDelete).containsExactlyInAnyOrder("a", "b")
    }

    @Test
    fun `local item without uuid is ignored`() {
        val result = plan(local = listOf(cart("")), remote = emptyList())

        assertThat(result.isEmpty).isTrue()
    }
}
