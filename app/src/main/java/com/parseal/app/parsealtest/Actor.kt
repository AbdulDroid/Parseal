package com.parseal.app.parsealtest

data class Actor(
    val name: String = "",
    val start_location: Map<String, Double> = mapOf(),
    val start_address: String = "",
    val end_location: Map<String, Double> = mapOf(),
    val current_location: Map<String, Double> = mapOf(),
    val end_address: String = ""
)