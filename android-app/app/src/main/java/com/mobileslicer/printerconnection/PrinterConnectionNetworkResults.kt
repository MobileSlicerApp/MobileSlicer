package com.mobileslicer.printerconnection

internal sealed interface NetworkResult {
    val isSuccess: Boolean
    val errorMessage: String?

    data object Success : NetworkResult {
        override val isSuccess: Boolean = true
        override val errorMessage: String? = null
    }

    data class Failure(override val errorMessage: String) : NetworkResult {
        override val isSuccess: Boolean = false
    }
}

internal sealed interface TextNetworkResult {
    val isSuccess: Boolean
    val body: String?
    val errorMessage: String?

    data class Success(override val body: String) : TextNetworkResult {
        override val isSuccess: Boolean = true
        override val errorMessage: String? = null
    }

    data class Failure(override val errorMessage: String) : TextNetworkResult {
        override val isSuccess: Boolean = false
        override val body: String? = null
    }
}
