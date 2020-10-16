package com.sample

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Serializable
class GitHubMail(
    val email: String?,
    val primary: Boolean,
    val verified: Boolean,
    val visibility: String
)

fun String.jsonToGithubMails(): List<GitHubMail> =
    Json.decodeFromString(ListSerializer(GitHubMail.serializer()), this)
