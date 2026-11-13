package br.com.zamfir.comprarei.core.database.migration

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import br.com.zamfir.comprarei.core.database.AppDatabase
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Garante que toda versão do schema exportada em app/schemas tem uma migração a partir da anterior.
 * Sem isso, subir a versão do banco e esquecer a migração faria o app falhar ao abrir.
 */
class DatabaseMigrationsTest {

    // Testes JVM rodam com o diretório do módulo (app/) como diretório de trabalho.
    private val schemaVersions = File("schemas/${AppDatabase::class.java.name}")
        .listFiles { file -> file.extension == "json" }
        .orEmpty()
        .map { it.nameWithoutExtension.toInt() }
        .sorted()

    @Test
    fun `schema versions start at 1 without gaps`() {
        assertThat(schemaVersions).isNotEmpty()
        assertThat(schemaVersions).isEqualTo((1..schemaVersions.last()).toList())
    }

    @Test
    fun `every schema version has a single step migration`() {
        val steps = DatabaseMigrations.ALL.map { it.startVersion to it.endVersion }
        val expected = (1 until schemaVersions.last()).map { it to it + 1 }

        assertThat(steps).containsExactlyInAnyOrder(*expected.toTypedArray())
    }
}
