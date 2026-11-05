package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the data used to authenticate with the master password.
 */
// ARCED: camelCase names, because Vaultwarden matches JSON keys case-sensitively.
@Serializable
data class MasterPasswordAuthenticationDataJson(
    @SerialName("kdf")
    val kdf: KdfJson,

    @SerialName("masterPasswordAuthenticationHash")
    val masterPasswordAuthenticationHash: String,

    @SerialName("salt")
    val salt: String,
)
