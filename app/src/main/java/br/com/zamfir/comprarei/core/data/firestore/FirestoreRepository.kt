package br.com.zamfir.comprarei.core.data.firestore

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import br.com.zamfir.comprarei.core.data.preferences.ConfigRepository
import br.com.zamfir.comprarei.core.database.AppDatabase
import br.com.zamfir.comprarei.core.database.entity.Cart
import br.com.zamfir.comprarei.core.database.entity.Category
import br.com.zamfir.comprarei.core.database.entity.Product
import br.com.zamfir.comprarei.core.util.Constants
import br.com.zamfir.comprarei.core.util.log.AppLogger
import br.com.zamfir.comprarei.data.workers.BackupWorker
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDateTime
import java.util.concurrent.TimeoutException

/** Resultado de [FirestoreRepository.backup]. Falhas de rede ou do Firestore viram exceção. */
enum class BackupOutcome {
    Done,

    /** Ninguém logado: nada a enviar. */
    NoUser,

    /** A restauração após o login não terminou; o banco local ainda não é confiável. */
    NotRestored,
}

/** O usuário saiu ou trocou de conta durante o backup; o envio parou para não misturar dados. */
class BackupAbortedException(message: String) : IllegalStateException(message)

class FirestoreRepository(
    private val context: Context,
    private val appDatabase: AppDatabase,
    private val configRepository: ConfigRepository,
    private val dispatcher: CoroutineDispatcher,
) {

    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    /**
     * Envia o banco local para a nuvem: grava o que é novo ou mudou e apaga da nuvem o que não
     * existe mais no aparelho. Aguarda cada escrita e lança exceção em qualquer falha.
     *
     * Só roda depois de uma restauração concluída para o usuário logado ([ConfigRepository.isRestoreCompleted]).
     * Sem isso o banco local pode estar vazio ou pela metade, e o sync apagaria dados da nuvem.
     */
    suspend fun backup(): BackupOutcome = withContext(dispatcher) {
        val uid = auth.uid ?: return@withContext BackupOutcome.NoUser
        if (!configRepository.isRestoreCompleted(uid)) {
            AppLogger.warning("Backup ignorado: a restauração dos dados não foi concluída")
            return@withContext BackupOutcome.NotRestored
        }

        // Foto única do banco, lida numa transação para compras e produtos ficarem coerentes.
        val local = appDatabase.runInTransaction<LocalSnapshot> {
            LocalSnapshot(
                carts = appDatabase.CartDao().carts,
                products = appDatabase.ProductDao().allProducts,
                categories = appDatabase.CategoryDao().allCategories,
            )
        }
        // O logoff apaga a marca antes de limpar o banco: se a leitura pegou o banco já limpo, para aqui.
        ensureStillAllowed(uid)

        val docRef = firestore.collection(uid).document(Constants.FIRESTORE_DOCUMENT_PATH)
        if (!docRef.get(Source.SERVER).await().exists()) {
            docRef.set(hashMapOf("userId" to uid)).await()
        }

        sync(
            uid, docRef.collection(Constants.FIRESTORE_CARTS_DOCUMENT_PATH), local.carts,
            uuidOf = { it.firestoreUUID }, fromDocument = FirebaseMapper::cartDocumentToEntity,
        )
        sync(
            uid, docRef.collection(Constants.FIRESTORE_PRODUCTS_DOCUMENT_PATH), local.products,
            uuidOf = { it.firestoreUUID }, fromDocument = FirebaseMapper::productDocumentToEntity,
        )
        sync(
            uid, docRef.collection(Constants.FIRESTORE_CATEGORIES_DOCUMENT_PATH), local.categories,
            uuidOf = { it.firestoreUUID }, fromDocument = FirebaseMapper::categoryDocumentToEntity,
        )

        configRepository.setLastBackupTime(LocalDateTime.now())
        BackupOutcome.Done
    }

    /**
     * Deixa a coleção igual à lista local. Documentos iguais (campos e id) não são regravados,
     * para economizar escritas. As operações vão em lotes de até 500, cada um aguardado.
     */
    private suspend fun <T : Any> sync(
        uid: String,
        collection: CollectionReference,
        local: List<T>,
        uuidOf: (T) -> String,
        fromDocument: (MutableMap<String, Any>?) -> T,
    ) {
        val withoutUuid = local.count { uuidOf(it).isBlank() }
        if (withoutUuid > 0) {
            AppLogger.warning("Backup: $withoutUuid registro(s) sem firestoreUUID em ${collection.id} ignorados")
        }

        val remote = collection.get(Source.SERVER).await().documents
            .associate { it.id to fromDocument(it.data) }

        val plan = planSync(local, remote, uuidOf)
        val operations = buildList<(WriteBatch) -> Unit> {
            plan.toDelete.forEach { uuid -> add { batch -> batch.delete(collection.document(uuid)) } }
            plan.toWrite.forEach { item -> add { batch -> batch.set(collection.document(uuidOf(item)), item) } }
        }

        operations.chunked(MAX_BATCH_SIZE).forEach { chunk ->
            ensureStillAllowed(uid)
            firestore.batch().apply { chunk.forEach { it(this) } }.commit().await()
        }
    }

    /** Interrompe o backup se o usuário saiu da conta (ou trocou de conta) no meio do envio. */
    private fun ensureStillAllowed(uid: String) {
        if (auth.uid != uid || !configRepository.isRestoreCompleted(uid)) {
            throw BackupAbortedException("A sessão mudou durante o backup")
        }
    }

    /**
     * Se o usuário logado precisa restaurar os dados antes de usar o app: uma restauração foi
     * iniciada e não terminou (app fechado no meio, por exemplo).
     *
     * Quem já usava o app antes desta marca existir não tem nenhuma das duas; nesse caso o banco
     * local é a referência, se tiver dados. Banco vazio passa pela restauração (não perde nada).
     */
    suspend fun needsRestore(): Boolean = withContext(dispatcher) {
        val uid = auth.uid ?: return@withContext false
        if (configRepository.isRestoreCompleted(uid)) return@withContext false
        if (configRepository.wasRestoreStarted(uid)) return@withContext true

        val hasLocalData = appDatabase.CartDao().carts.isNotEmpty() || appDatabase.CategoryDao().allCategories.isNotEmpty()
        if (hasLocalData) configRepository.markRestoreCompleted(uid)
        !hasLocalData
    }

    /** Conta recém-criada: não há nada na nuvem para restaurar, o banco local já é a referência. */
    fun markNewAccountReady() {
        auth.uid?.let { configRepository.markRestoreCompleted(it) }
    }

    /**
     * Baixa compras, produtos e categorias da nuvem e substitui o banco local, avisando cada etapa.
     * Lança exceção em qualquer falha: a tela não deve seguir para o app com dados incompletos.
     * Só depois de gravar tudo marca a restauração como concluída, liberando o backup.
     */
    suspend fun restoreUserData(onStep: (RestoreStep) -> Unit) = withContext(dispatcher) {
        val uid = auth.uid ?: throw IllegalStateException("Nenhum usuário logado para restaurar")
        configRepository.markRestoreStarted(uid)
        val docRef = firestore.collection(uid).document(Constants.FIRESTORE_DOCUMENT_PATH)

        onStep(RestoreStep.Preparing)
        if (!docRef.get(Source.SERVER).await().exists()) {
            docRef.set(hashMapOf("userId" to uid)).await()
        }

        // Baixa tudo primeiro e só então grava, numa transação: o banco nunca fica pela metade.
        onStep(RestoreStep.Carts)
        val carts = docRef.collection(Constants.FIRESTORE_CARTS_DOCUMENT_PATH).get(Source.SERVER).await().documents
            .map { FirebaseMapper.cartDocumentToEntity(it.data) }

        onStep(RestoreStep.Products)
        val products = docRef.collection(Constants.FIRESTORE_PRODUCTS_DOCUMENT_PATH).get(Source.SERVER).await().documents
            .map { FirebaseMapper.productDocumentToEntity(it.data) }

        onStep(RestoreStep.Categories)
        val categories = docRef.collection(Constants.FIRESTORE_CATEGORIES_DOCUMENT_PATH).get(Source.SERVER).await().documents
            .map { FirebaseMapper.categoryDocumentToEntity(it.data) }

        // Substitui o que houver no aparelho (pode ser o resto de uma restauração interrompida).
        appDatabase.runInTransaction {
            appDatabase.ProductDao().deleteAll()
            appDatabase.CartDao().deleteAll()
            appDatabase.CategoryDao().deleteAll()
            appDatabase.CartDao().insertAll(carts)
            appDatabase.ProductDao().insertAll(products)
            appDatabase.CategoryDao().insertAll(categories)
        }
        configRepository.markRestoreCompleted(uid)
    }

    /**
     * Apaga da nuvem compras, produtos e categorias do usuário e o documento que os agrupa.
     * Usa o servidor (não o cache) e lança exceção em qualquer falha ou demora, para a tela não
     * seguir com a exclusão da conta deixando dados órfãos no Firestore.
     */
    suspend fun deleteUserData() = withContext(dispatcher) {
        val uid = auth.uid ?: throw IllegalStateException("Nenhum usuário logado para apagar os dados")
        val docRef = firestore.collection(uid).document(Constants.FIRESTORE_DOCUMENT_PATH)

        // Timeout como exceção comum (não CancellationException), para a tela tratar como falha de rede.
        withTimeoutOrNull(DELETE_TIMEOUT_MS) {
            listOf(
                Constants.FIRESTORE_CARTS_DOCUMENT_PATH,
                Constants.FIRESTORE_PRODUCTS_DOCUMENT_PATH,
                Constants.FIRESTORE_CATEGORIES_DOCUMENT_PATH,
                Constants.FIRESTORE_PROFILE_COLLECTION_PATH,
            ).forEach { path ->
                val documents = docRef.collection(path).get(Source.SERVER).await().documents
                // Um lote do Firestore aceita no máximo 500 operações.
                documents.chunked(MAX_BATCH_SIZE).forEach { chunk ->
                    firestore.batch().apply { chunk.forEach { delete(it.reference) } }.commit().await()
                }
            }
            docRef.delete().await()
            // await() de Task<Void> devolve null; sem isto o ?: abaixo trataria o sucesso como timeout.
            Unit
        } ?: throw TimeoutException("Tempo esgotado ao apagar os dados do Firestore")
    }

    /**
     * Grava a foto de perfil no Firestore, substituindo a anterior.
     * Fica num documento próprio (e não no user_data) para o backup não baixá-la a cada leitura.
     * O Cloud Storage não é usado: o projeto está no plano gratuito, que não dá mais acesso a ele.
     */
    suspend fun uploadProfilePicture(bytes: ByteArray) = withContext(dispatcher) {
        require(bytes.size <= MAX_PROFILE_PICTURE_BYTES) { "Foto de perfil grande demais: ${bytes.size} bytes" }
        profilePictureDocument().set(mapOf(Constants.FIRESTORE_PROFILE_PICTURE_FIELD to Blob.fromBytes(bytes))).await()
    }

    /** Foto de perfil salva na nuvem, ou nulo se a conta não tem foto. Falhas de rede viram exceção. */
    suspend fun downloadProfilePicture(): ByteArray? = withContext(dispatcher) {
        profilePictureDocument().get(Source.SERVER).await()
            .getBlob(Constants.FIRESTORE_PROFILE_PICTURE_FIELD)?.toBytes()
    }

    private fun profilePictureDocument(): DocumentReference {
        val uid = auth.uid ?: throw IllegalStateException("Nenhum usuário logado")
        return firestore.collection(uid).document(Constants.FIRESTORE_DOCUMENT_PATH)
            .collection(Constants.FIRESTORE_PROFILE_COLLECTION_PATH)
            .document(Constants.FIRESTORE_PROFILE_PICTURE_DOCUMENT)
    }

    /**
     * Cancela os backups periódico e manual. Usado ao sair e antes de excluir a conta, para um
     * backup não rodar com o banco sendo apagado. O periódico volta a ser agendado pela
     * MainActivity no próximo login.
     */
    fun cancelBackups() {
        WorkManager.getInstance(context).apply {
            cancelUniqueWork(Constants.PERIODIC_BACKUP_WORK_NAME)
            cancelUniqueWork(Constants.MANUAL_BACKUP_WORK_NAME)
        }
    }

    suspend fun doBackup() = withContext(dispatcher) {
        val uploadWorkRequest: OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<BackupWorker>()
                .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
                .setInputData(workDataOf(Constants.MANUAL_BACKUP_KEY to true))
                .build()

        // Trabalho único: tocar de novo enquanto um backup está pendente não enfileira outro.
        WorkManager.getInstance(context).enqueueUniqueWork(Constants.MANUAL_BACKUP_WORK_NAME, ExistingWorkPolicy.KEEP, uploadWorkRequest)
    }

    /** Situação do backup manual mais recente, para a tela acompanhar o envio. */
    fun observeManualBackup(): Flow<BackupStatus> =
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow(Constants.MANUAL_BACKUP_WORK_NAME)
            .map { infos -> infos.lastOrNull()?.state.toBackupStatus() }

    private fun WorkInfo.State?.toBackupStatus() = when (this) {
        null -> BackupStatus.Idle
        WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> BackupStatus.WaitingForNetwork
        WorkInfo.State.RUNNING -> BackupStatus.Running
        WorkInfo.State.SUCCEEDED -> BackupStatus.Succeeded
        WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> BackupStatus.Failed
    }

    private class LocalSnapshot(val carts: List<Cart>, val products: List<Product>, val categories: List<Category>)

    private companion object {
        const val MAX_BATCH_SIZE = 500
        const val DELETE_TIMEOUT_MS = 60_000L

        // Abaixo do limite de 1 MiB por documento, com folga para o nome do campo.
        const val MAX_PROFILE_PICTURE_BYTES = 900 * 1024
    }
}
