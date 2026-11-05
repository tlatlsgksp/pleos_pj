package com.example.pleos.domain.gateway

/** Makes entity ids. Injected so tests get predictable ids. */
fun interface IdGenerator {
    fun newId(): String
}
