package br.com.zamfir.comprarei.core.database.migration

import androidx.room.migration.Migration

/**
 * Migrações do banco local. O app não usa migração destrutiva: perder o banco local é grave,
 * porque o backup deixa a nuvem igual ao banco local e apagaria os dados do usuário no Firestore.
 *
 * Para mudar o schema:
 * 1. Altere as entidades e suba `version` em `AppDatabase`.
 * 2. Compile: o Room gera `app/schemas/.../<nova versão>.json`. Faça commit desse arquivo,
 *    ele é a referência dos testes de migração.
 * 3. Adicione aqui uma `Migration(N, N + 1)` com o SQL da mudança (uma migração por versão,
 *    sem pular números).
 * 4. Rode `DatabaseMigrationsTest` (JVM) e `MigrationTest` (instrumentado, só no emulador ou com
 *    `adb shell am instrument` no aparelho físico).
 */
object DatabaseMigrations {

    val ALL: Array<Migration> = arrayOf()
}
