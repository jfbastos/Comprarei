package br.com.zamfir.comprarei.core.data.firestore

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.core.database.AppDatabase
import br.com.zamfir.comprarei.core.database.entity.Cart
import br.com.zamfir.comprarei.core.database.entity.Category
import br.com.zamfir.comprarei.core.database.entity.Product
import br.com.zamfir.comprarei.core.util.Constants
import br.com.zamfir.comprarei.core.domain.exceptions.FirestoreDocumentCreationException
import br.com.zamfir.comprarei.core.domain.exceptions.FirestoreLoadRegistersException
import br.com.zamfir.comprarei.data.workers.BackupWorker
import br.com.zamfir.comprarei.core.util.log.AppLogger
import com.google.firebase.auth.auth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirestoreRepository(private val context : Context, private val appDatabase: AppDatabase, private val dispatcher : CoroutineDispatcher) {

    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    @Throws
    suspend fun saveCarts() = withContext(dispatcher){
        //validate auth
        if(auth.currentUser == null) return@withContext
        val docRef = firestore.collection(auth.uid!!).document(Constants.FIRESTORE_DOCUMENT_PATH)

        createUserDataIfNotExists(docRef){ docRefException ->
            CoroutineScope(dispatcher).launch(CoroutineExceptionHandler{ _, throwable ->
               throw throwable
            }) {
                val collectionRef = docRef.collection(Constants.FIRESTORE_CARTS_DOCUMENT_PATH)
                if(docRefException != null){
                    throw docRefException
                }else{
                    val dataBaseCarts = appDatabase.CartDao().carts

                    getCartsFirestore(collectionRef){ firestoreCarts, exception ->
                        if(exception != null){
                           throw exception
                        }else{
                            //Se tem no firebase, mas não tem no banco local, deleta do firebase
                            //Se tem no firebase, e no banco local, edita
                            firestoreCarts.forEach { firestoreCart ->
                                if(dataBaseCarts.none { it.firestoreUUID == firestoreCart.firestoreUUID }){
                                    collectionRef.document(firestoreCart.firestoreUUID).delete()
                                }else{
                                    dataBaseCarts.firstOrNull { it.firestoreUUID == firestoreCart.firestoreUUID }?.let { databaseCart ->
                                        collectionRef.document(firestoreCart.firestoreUUID).set(databaseCart)
                                    }
                                }
                            }

                            //Se tem no banco local, e não tem no firebase, insere
                            dataBaseCarts.forEach { databaseCart ->
                                if(firestoreCarts.none {it.firestoreUUID == databaseCart.firestoreUUID}){
                                    collectionRef.document(databaseCart.firestoreUUID).set(databaseCart)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Throws
    suspend fun saveProducts() = withContext(dispatcher){
        //validate auth
        if(auth.currentUser == null) return@withContext
        val docRef = firestore.collection(auth.uid!!).document(Constants.FIRESTORE_DOCUMENT_PATH)

        createUserDataIfNotExists(docRef){ docRefException ->
            CoroutineScope(dispatcher).launch(CoroutineExceptionHandler{ _, throwable ->
                throw throwable
            }) {
                val collectionRef = docRef.collection(Constants.FIRESTORE_PRODUCTS_DOCUMENT_PATH)
                if(docRefException != null){
                    throw docRefException
                }else{
                    val dataBaseProducts = appDatabase.ProductDao().allProducts

                    getProductsFirestore(collectionRef){ firestoreProducts, exception ->
                        if(exception != null){
                            throw exception
                        }else{
                            //Se tem no firebase, mas não tem no banco local, deleta do firebase
                            //Se tem no firebase, e no banco local, edita
                            firestoreProducts.forEach { firestoreProduct ->
                                if(dataBaseProducts.none { it.firestoreUUID == firestoreProduct.firestoreUUID }){
                                    collectionRef.document(firestoreProduct.firestoreUUID).delete()
                                }else{
                                    dataBaseProducts.firstOrNull { it.firestoreUUID == firestoreProduct.firestoreUUID }?.let { databaseCart ->
                                        collectionRef.document(firestoreProduct.firestoreUUID).set(databaseCart)
                                    }
                                }
                            }

                            //Se tem no banco local, e não tem no firebase, insere
                            dataBaseProducts.forEach { databaseCart ->
                                if(firestoreProducts.none {it.firestoreUUID == databaseCart.firestoreUUID}){
                                    collectionRef.document(databaseCart.firestoreUUID).set(databaseCart)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Throws
    suspend fun saveCategories() = withContext(dispatcher){
        //validate auth
        if(auth.currentUser == null) return@withContext
        val docRef = firestore.collection(auth.uid!!).document(Constants.FIRESTORE_DOCUMENT_PATH)

        createUserDataIfNotExists(docRef){ docRefException ->
            CoroutineScope(dispatcher).launch(CoroutineExceptionHandler{ _, throwable ->
                throw throwable
            }) {
                val collectionRef = docRef.collection(Constants.FIRESTORE_CATEGORIES_DOCUMENT_PATH)
                if(docRefException != null){
                    throw docRefException
                }else{
                    val dataBaseProducts = appDatabase.CategoryDao().allCategories

                    getCategoryFirestore(collectionRef){ firestoreCategories, exception ->
                        if(exception != null){
                            throw exception
                        }else{
                            //Se tem no firebase, mas não tem no banco local, deleta do firebase
                            //Se tem no firebase, e no banco local, edita
                            firestoreCategories.forEach { firestoreProduct ->
                                if(dataBaseProducts.none { it.firestoreUUID == firestoreProduct.firestoreUUID }){
                                    collectionRef.document(firestoreProduct.firestoreUUID).delete()
                                }else{
                                    dataBaseProducts.firstOrNull { it.firestoreUUID == firestoreProduct.firestoreUUID }?.let { databaseCart ->
                                        collectionRef.document(firestoreProduct.firestoreUUID).set(databaseCart)
                                    }
                                }
                            }

                            //Se tem no banco local, e não tem no firebase, insere
                            dataBaseProducts.forEach { databaseCart ->
                                if(firestoreCategories.none {it.firestoreUUID == databaseCart.firestoreUUID}){
                                    collectionRef.document(databaseCart.firestoreUUID).set(databaseCart)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun getUserDataFromFirebase(onProgress : (String) -> Unit, callback : () -> Unit) = withContext(dispatcher){
        try {
            if (auth.currentUser == null) return@withContext
            val docRef = firestore.collection(auth.uid!!).document(Constants.FIRESTORE_DOCUMENT_PATH)

            val handler = CoroutineExceptionHandler { _, throwable ->
                AppLogger.error("Erro no método obterDadosDoUsuario", throwable)
            }

            runOnMainDispatcher { onProgress("Obtendo informações da núvem...") }
            createUserDataIfNotExists(docRef) { docRefException ->
                CoroutineScope(Dispatchers.IO).launch(handler) {
                    if (docRefException != null) throw docRefException

                    runOnMainDispatcher { onProgress("Obntendo carrinhos...") }
                    docRef.collection(Constants.FIRESTORE_CARTS_DOCUMENT_PATH).get().await().documents.map { FirebaseMapper.cartDocumentToEntity(it.data) }.also {
                        appDatabase.CartDao().insertAll(it)
                    }

                    runOnMainDispatcher { onProgress("Obtendo produtos...") }
                    docRef.collection(Constants.FIRESTORE_PRODUCTS_DOCUMENT_PATH).get().await().documents.map { FirebaseMapper.productDocumentToEntity(it.data) }.also {
                        appDatabase.ProductDao().insertAll(it)
                    }

                    runOnMainDispatcher { onProgress("Obtendo categorias...") }
                    docRef.collection(Constants.FIRESTORE_CATEGORIES_DOCUMENT_PATH).get().await().documents.map { FirebaseMapper.categoryDocumentToEntity(it.data) }.also {
                        appDatabase.CategoryDao().insertAll(it)
                    }

                    runOnMainDispatcher { callback.invoke() }
                }
            }
        } catch (e: Exception) {
            AppLogger.error("Erro no método obterDadosDoUsuario", e)
        }
    }

    private fun runOnMainDispatcher(function: () -> Unit) {    CoroutineScope(Dispatchers.Main).launch { function() }}

    private suspend fun getCartsFirestore(collection : CollectionReference,callBack : (List<Cart>, Exception?) -> Unit) = withContext(dispatcher){
        try {
            if(auth.currentUser == null) callBack.invoke(listOf(), RuntimeException(context.getString(R.string.no_user_logged)))

            collection.get().addOnCompleteListener { documents ->
                    if(documents.isSuccessful){
                        callBack.invoke(documents.result.documents.map { FirebaseMapper.cartDocumentToEntity(it.data)}, null)
                    }

                    if(documents.exception != null){
                        callBack.invoke(listOf(), documents.exception)
                    }

                    if(!documents.isSuccessful){
                        callBack.invoke(listOf(), FirestoreLoadRegistersException(context.getString(R.string.can_t_get_carts_registers)))
                    }
            }
        } catch (e: Exception) {
            AppLogger.error("Erro no método getCartsFirestore", e)
        }
    }

    private suspend fun getProductsFirestore(collection : CollectionReference,callBack : (List<Product>, Exception?) -> Unit) = withContext(dispatcher){
        try {
            if(auth.currentUser == null) callBack.invoke(listOf(), RuntimeException(context.getString(R.string.no_user_logged)))

            collection.get().addOnCompleteListener { documents ->
                if(documents.isSuccessful){
                    callBack.invoke(documents.result.documents.map { FirebaseMapper.productDocumentToEntity(it.data)}, null)
                }

                if(documents.exception != null){
                    callBack.invoke(listOf(), documents.exception)
                }

                if(!documents.isSuccessful){
                    callBack.invoke(listOf(), FirestoreLoadRegistersException(context.getString(R.string.can_t_get_products_registers)))
                }
            }
        } catch (e: Exception) {
            AppLogger.error("Erro no método getProductsFirestore", e)
        }
    }

    private suspend fun getCategoryFirestore(collection : CollectionReference,callBack : (List<Category>, Exception?) -> Unit) = withContext(dispatcher){
        try {
            if(auth.currentUser == null) callBack.invoke(listOf(), RuntimeException(context.getString(R.string.no_user_logged)))

            collection.get().addOnCompleteListener { documents ->
                if(documents.isSuccessful){
                    callBack.invoke(documents.result.documents.map { FirebaseMapper.categoryDocumentToEntity(it.data)}, null)
                }

                if(documents.exception != null){
                    callBack.invoke(listOf(), documents.exception)
                }

                if(!documents.isSuccessful){
                    callBack.invoke(listOf(),  FirestoreLoadRegistersException(context.getString(R.string.can_t_get_categories_registers)))
                }
            }
        } catch (e: Exception) {
            AppLogger.error("Erro no método getCategoryFirestore", e)
        }
    }

    private suspend fun createUserDataIfNotExists(docRef : DocumentReference, callback : (exception : Exception?) -> Unit) = withContext(dispatcher){
        try {
            docRef.get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val document = task.result
                    if(document != null) {
                        if(!document.exists()){
                            docRef.set(hashMapOf("userId" to auth.currentUser!!.uid)).addOnCompleteListener {
                                if(it.isSuccessful) callback.invoke(null)
                                else callback.invoke(if(it.exception != null) it.exception else FirestoreDocumentCreationException(context.getString(R.string.something_went_wrong_on_document_creation)))
                            }
                        }else{
                            callback.invoke(null)
                        }
                    }
                } else {
                    callback.invoke(if(task.exception != null) task.exception else FirestoreDocumentCreationException(context.getString(R.string.something_went_wrong_on_document_creation)))
                }
            }
        } catch (e: Exception) {
            AppLogger.error("Erro no método createUserDataIfNotExists", e)
        }
    }

    suspend fun doBackup() = withContext(dispatcher){
        val uploadWorkRequest: OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<BackupWorker>()
                .setConstraints(Constraints(requiredNetworkType = NetworkType.UNMETERED))
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
}