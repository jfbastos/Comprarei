package br.com.zamfir.comprarei.core.database.migration

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.zamfir.comprarei.core.database.AppDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Valida as migrações contra os schemas exportados (app/schemas, empacotados como assets do teste).
 * Usa um banco próprio ("migration-test"), sem tocar no banco do app.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val targetContext = instrumentation.targetContext

    @get:Rule
    val helper = MigrationTestHelper(instrumentation, AppDatabase::class.java)

    private val latestVersion: Int = instrumentation.context.assets
        .list(AppDatabase::class.java.name)!!
        .map { it.removeSuffix(".json").toInt() }
        .max()

    @After
    fun tearDown() {
        targetContext.deleteDatabase(TEST_DB)
    }

    @Test
    fun migratesEveryVersionToLatest() {
        for (version in 1 until latestVersion) {
            helper.createDatabase(TEST_DB, version).close()
            helper.runMigrationsAndValidate(TEST_DB, latestVersion, true, *DatabaseMigrations.ALL).close()
            targetContext.deleteDatabase(TEST_DB)
        }
    }

    /**
     * Banco da primeira versão publicada (a dos usuários atuais) abre no app atual sem perder dados.
     * Também confere que o schema 1.json é o mesmo das entidades quando ainda não há migrações.
     */
    @Test
    fun firstReleaseDatabaseOpensWithDataKept() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                "INSERT INTO CARTS (id, name, data, total, position, categoryId, store, firestoreUUID) " +
                    "VALUES (1, 'Mercado', '10/11/2026', 'R$ 10,00', 0, 0, '', 'cart-uuid')"
            )
            execSQL(
                "INSERT INTO PRODUCTS (id, name, brand, price, quantity, cartId, done, position, firestoreUUID) " +
                    "VALUES (1, 'Arroz', '', 5.0, 2, 1, 0, 0, 'product-uuid')"
            )
            close()
        }

        val db = Room.databaseBuilder(targetContext, AppDatabase::class.java, TEST_DB)
            .addMigrations(*DatabaseMigrations.ALL)
            .allowMainThreadQueries()
            .build()
        try {
            val progress = db.ProductDao().progressByCart().single()
            assertEquals(1, progress.cartId)
            assertEquals(10.0, progress.totalValue, 0.001)
            assertEquals("cart-uuid", db.CartDao().carts.single().firestoreUUID)
        } finally {
            db.close()
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
