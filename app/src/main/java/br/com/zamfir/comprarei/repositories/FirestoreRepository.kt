package br.com.zamfir.comprarei.repositories

import android.util.Log
import br.com.zamfir.comprarei.model.AppDatabase
import br.com.zamfir.comprarei.model.entity.Cart
import br.com.zamfir.comprarei.model.entity.Category
import br.com.zamfir.comprarei.model.entity.Product
import br.com.zamfir.comprarei.model.mappers.FirebaseMapper
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
import java.time.LocalTime

class FirestoreRepository(private val appDatabase: AppDatabase, private val dispatcher : CoroutineDispatcher) {

    /*
    * Obter os registros do firestore
    *
    * Se, o registro estiver no firestore mas não estiver no banco, deleta o registro
    * Se, o registro não estiver no firestore mas tiver no banco, insere o registro
    * Se, o registro estiver no banco e no firestore, salva o registro (firestore verifica se teve edição ou não
    *
    * */

    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    @Throws
    suspend fun saveCarts() = withContext(dispatcher){
        //validate auth
        if(auth.currentUser == null) return@withContext
        val docRef = firestore.collection(auth.uid!!).document("user_data")

        createUserDataIfNotExists(docRef){ docRefException ->
            CoroutineScope(dispatcher).launch(CoroutineExceptionHandler{ _, throwable ->
               throw throwable
            }) {
                val collectionRef = docRef.collection("Carts")
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
        val docRef = firestore.collection(auth.uid!!).document("user_data")

        createUserDataIfNotExists(docRef){ docRefException ->
            CoroutineScope(dispatcher).launch(CoroutineExceptionHandler{ _, throwable ->
                throw throwable
            }) {
                val collectionRef = docRef.collection("Products")
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
        val docRef = firestore.collection(auth.uid!!).document("user_data")

        createUserDataIfNotExists(docRef){ docRefException ->
            CoroutineScope(dispatcher).launch(CoroutineExceptionHandler{ _, throwable ->
                throw throwable
            }) {
                val collectionRef = docRef.collection("Categories")
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

    suspend fun obterDadosDoUsuario(callback : () -> Unit) = withContext(dispatcher){
        if(auth.currentUser == null) return@withContext
        val docRef = firestore.collection(auth.uid!!).document("user_data")

        val handler = CoroutineExceptionHandler{ _, throwable ->
            throw throwable
        }

        createUserDataIfNotExists(docRef){ docRefException ->
            CoroutineScope(Dispatchers.IO).launch(handler) {
                if(docRefException != null) throw docRefException
                docRef.collection("Carts").get().await().documents.map { FirebaseMapper.cartDocumentToEntity(it.data) }.also {
                    appDatabase.CartDao().insertAll(it)
                }

                docRef.collection("Products").get().await().documents.map { FirebaseMapper.productDocumentToEntity(it.data) }.also {
                    appDatabase.ProductDao().insertAll(it)
                }

                docRef.collection("Categories").get().await().documents.map { FirebaseMapper.categoryDocumentToEntity(it.data) }.also {
                    appDatabase.CategoryDao().insertAll(it)
                }
                callback.invoke()
            }
        }
    }

    private suspend fun getCartsFirestore(collection : CollectionReference,callBack : (List<Cart>, Exception?) -> Unit) = withContext(dispatcher){
        if(auth.currentUser == null) callBack.invoke(listOf(), RuntimeException("Sem usuário logado"))

        collection.get().addOnCompleteListener { documents ->
                if(documents.isSuccessful){
                    callBack.invoke(documents.result.documents.map { FirebaseMapper.cartDocumentToEntity(it.data)}, null)
                }

                if(documents.exception != null){
                    callBack.invoke(listOf(), documents.exception)
                }

                if(!documents.isSuccessful){
                    callBack.invoke(listOf(), RuntimeException("Não foi possível obter os registros"))
                }
        }
    }

    private suspend fun getProductsFirestore(collection : CollectionReference,callBack : (List<Product>, Exception?) -> Unit) = withContext(dispatcher){
        if(auth.currentUser == null) callBack.invoke(listOf(), RuntimeException("Sem usuário logado"))

        collection.get().addOnCompleteListener { documents ->
            if(documents.isSuccessful){
                callBack.invoke(documents.result.documents.map { FirebaseMapper.productDocumentToEntity(it.data)}, null)
            }

            if(documents.exception != null){
                callBack.invoke(listOf(), documents.exception)
            }

            if(!documents.isSuccessful){
                callBack.invoke(listOf(), RuntimeException("Não foi possível obter os registros"))
            }
        }
    }

    private suspend fun getCategoryFirestore(collection : CollectionReference,callBack : (List<Category>, Exception?) -> Unit) = withContext(dispatcher){
        if(auth.currentUser == null) callBack.invoke(listOf(), RuntimeException("Sem usuário logado"))

        collection.get().addOnCompleteListener { documents ->
            if(documents.isSuccessful){
                callBack.invoke(documents.result.documents.map { FirebaseMapper.categoryDocumentToEntity(it.data)}, null)
            }

            if(documents.exception != null){
                callBack.invoke(listOf(), documents.exception)
            }

            if(!documents.isSuccessful){
                callBack.invoke(listOf(), RuntimeException("Não foi possível obter os registros"))
            }
        }
    }

    private suspend fun createUserDataIfNotExists(docRef : DocumentReference, callback : (exception : Exception?) -> Unit) = withContext(dispatcher){
        docRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val document = task.result
                if(document != null) {
                    if(!document.exists()){
                        docRef.set(hashMapOf("userId" to auth.currentUser!!.uid)).addOnCompleteListener {
                            if(it.isSuccessful) callback.invoke(null)
                            else callback.invoke(if(it.exception != null) it.exception else RuntimeException("Houve uma falha ao criar o arquivo."))
                        }
                    }else{
                        callback.invoke(null)
                    }
                }
            } else {
                callback.invoke(if(task.exception != null) task.exception else RuntimeException("Houve uma falha ao criar o arquivo."))
            }
        }
    }
}