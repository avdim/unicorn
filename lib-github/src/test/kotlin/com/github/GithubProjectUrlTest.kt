package com.github

import org.junit.Test
import kotlin.test.assertEquals

class GithubProjectUrlTest {

  @Test
  fun parseGithubUrlSsh() {
    assertEquals(
      GithubProject("avdim", "codeforces"),
      parseGithubUrl("git@github.com:avdim/codeforces.git")
    )
  }

  @Test
  fun parseGithubUrlHttps() {
    assertEquals(
      GithubProject("avdim", "codeforces"),
      parseGithubUrl("https://github.com/avdim/codeforces.git")
    )
  }

  @Test
  fun toSshUrl() {
    assertEquals(
      "git@github.com:avdim/codeforces.git",
      GithubProject("avdim", "codeforces").toSshUrl()
    )
  }

  @Test
  fun toHttpsUrl() {
    assertEquals(
      "https://github.com/avdim/codeforces.git",
      GithubProject("avdim", "codeforces").toHttpsUrl()
    )

  }
}
