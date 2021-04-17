package com.github

import kotlin.reflect.full.allSuperclasses

interface Permission {
    interface Mail : Permission
    interface Repo : Permission
}

inline fun <reified T, reified I> checkInterface() =
    T::class == I::class || T::class.allSuperclasses.contains(I::class)

inline fun <reified T : Permission> scopes(): String {
    var result: List<String> = listOf()
    if (checkInterface<T, Permission.Mail>()) {
        result = result + "user:email"
    }
    if (checkInterface<T, Permission.Repo>()) {
        result = result + "repo"
    }
    return result.joinToString(",")
}
