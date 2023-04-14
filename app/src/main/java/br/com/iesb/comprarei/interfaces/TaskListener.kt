package br.com.iesb.comprarei.interfaces

interface TaskListener {
    fun onSuccess(`object` : Any?)
    fun onError(`object` : Any?)
}