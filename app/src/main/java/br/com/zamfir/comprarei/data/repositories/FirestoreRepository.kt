package br.com.zamfir.comprarei.data.repositories

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import br.com.zamfir.comprarei.R
import br.com.zamfir.comprarei.data.model.AppDatabase
import br.com.zamfir.comprarei.data.model.entity.Cart
import br.com.zamfir.comprarei.data.model.entity.Category
import br.com.zamfir.comprarei.data.model.entity.Product
import br.com.zamfir.comprarei.data.model.mappers.FirebaseMapper
import br.com.zamfir.comprarei.util.Constants
import br.com.zamfir.comprarei.util.exceptions.FirestoreDocumentCreationException
import br.com.zamfir.comprarei.util.exceptions.FirestoreLoadRegistersException
import br.com.zamfir.comprarei.data.workers.BackupWorker
import br.com.zamfir.comprarei.util.log.LogUtil
import br.com.zamfir.comprarei.util.log.TelegramLogLevel
import br.com.zamfir.comprarei.view.listeners.LoginProgressListener
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

    suspend fun getUserDataFromFirebase(callback : () -> Unit) = withContext(dispatcher){
        try {
            if (auth.currentUser == null) return@withContext
            val docRef = firestore.collection(auth.uid!!).document(Constants.FIRESTORE_DOCUMENT_PATH)

            val handler = CoroutineExceptionHandler { _, throwable ->
                LogUtil.sendLog(TelegramLogLevel.ERROR, "Erro no método obterDadosDoUsuario ${throwable.stackTraceToString()}")
            }

            runOnMainDispatcher { LoginProgressListener.loginProgressListener.onProgress("Download info from cloud...") }
            createUserDataIfNotExists(docRef) { docRefException ->
                CoroutineScope(Dispatchers.IO).launch(handler) {
                    if (docRefException != null) throw docRefException

                    runOnMainDispatcher { LoginProgressListener.loginProgressListener.onProgress("Fetching carts...") }
                    docRef.collection(Constants.FIRESTORE_CARTS_DOCUMENT_PATH).get().await().documents.map { FirebaseMapper.cartDocumentToEntity(it.data) }.also {
                        appDatabase.CartDao().insertAll(it)
                    }

                    runOnMainDispatcher { LoginProgressListener.loginProgressListener.onProgress("Fetching products...") }
                    docRef.collection(Constants.FIRESTORE_PRODUCTS_DOCUMENT_PATH).get().await().documents.map { FirebaseMapper.productDocumentToEntity(it.data) }.also {
                        appDatabase.ProductDao().insertAll(it)
                    }

                    runOnMainDispatcher { LoginProgressListener.loginProgressListener.onProgress("Fetching categories...") }
                    docRef.collection(Constants.FIRESTORE_CATEGORIES_DOCUMENT_PATH).get().await().documents.map { FirebaseMapper.categoryDocumentToEntity(it.data) }.also {
                        appDatabase.CategoryDao().insertAll(it)
                    }

                    runOnMainDispatcher { callback.invoke() }
                }
            }
        } catch (e: Exception) {
            LogUtil.sendLog(TelegramLogLevel.ERROR, "Erro no método obterDadosDoUsuario ${e.stackTraceToString()}")
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
            LogUtil.sendLog(TelegramLogLevel.ERROR,  "Erro no método getCartsFirestore ${e.stackTraceToString()}")
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
            LogUtil.sendLog(TelegramLogLevel.ERROR,  "Erro no método getProductsFirestore ${e.stackTraceToString()}")
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
            LogUtil.sendLog(TelegramLogLevel.ERROR,  "Erro no método getCategoryFirestore ${e.stackTraceToString()}")
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
            LogUtil.sendLog(TelegramLogLevel.ERROR,  "Erro no método createUserDataIfNotExists ${e.stackTraceToString()}")
        }
    }

    suspend fun doBackup() = withContext(dispatcher){
        val uploadWorkRequest: WorkRequest =
            OneTimeWorkRequestBuilder<BackupWorker>()
                .setConstraints(Constraints(requiredNetworkType = NetworkType.UNMETERED))
                .setInputData(workDataOf(Constants.MANUAL_BACKUP_KEY to true))
                .build()

        WorkManager.getInstance(context).enqueue(uploadWorkRequest)
    }
}