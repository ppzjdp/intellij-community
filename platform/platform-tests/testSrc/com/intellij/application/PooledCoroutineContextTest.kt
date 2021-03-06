// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application

import com.intellij.testFramework.LoggedErrorProcessor
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.assertions.Assertions.assertThat
import kotlinx.coroutines.*
import org.apache.log4j.Logger
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.*

@RunWith(JUnit4::class)
class PooledCoroutineContextTest : UsefulTestCase() {
  @Test
  fun `log error`() = runBlocking<Unit> {
    val errorMessage = "don't swallow me"
    // cannot use assertThatThrownBy here, because AssertJ doesn't support Kotlin coroutines
    val loggedErrors = mutableListOf<Throwable>()
    withLoggedErrorsRecorded(loggedErrors) {
      GlobalScope.launch(Dispatchers.ApplicationThreadPool) {
        throw RuntimeException(errorMessage)
      }.join()
    }
    assertThat(loggedErrors).anyMatch { errorMessage in it.message.orEmpty() }
  }

  private suspend fun <T> withLoggedErrorsRecorded(loggedErrors: List<Throwable>,
                                                   block: suspend () -> T): T {
    val savedInstance = LoggedErrorProcessor.getInstance()
    val synchronizedLoggedErrors = Collections.synchronizedList(loggedErrors)
    LoggedErrorProcessor.setNewInstance(object : LoggedErrorProcessor() {
      override fun processError(message: String, t: Throwable, details: Array<String>, logger: Logger) {
        synchronizedLoggedErrors.add(t)
      }
    })
    return try {
      block()
    }
    finally {
      LoggedErrorProcessor.setNewInstance(savedInstance)
    }
  }

  @Test
  fun `error must be propagated to parent context if available`() = runBlocking {
    class MyCustomException : RuntimeException()

    try {
      withContext(Dispatchers.ApplicationThreadPool) {
        throw MyCustomException()
      }
    }
    catch (ignored: MyCustomException) {
    }
  }
}