package br.com.zamfir.comprarei.core.data.firestore

enum class BackupStatus {
    /** Nenhum backup manual registrado. */
    Idle,

    /** Na fila, esperando a rede exigida (Wi-Fi). */
    WaitingForNetwork,
    Running,
    Succeeded,
    Failed,
}
