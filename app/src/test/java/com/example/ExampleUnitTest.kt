package com.example

import com.example.data.remote.SupabaseConfig
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun supabaseConfig_hasConfiguredCredentials() {
    assertNotNull(SupabaseConfig.supabaseUrl)
    assertTrue("Supabase URL should not be blank", SupabaseConfig.supabaseUrl.isNotBlank())
    assertTrue("Supabase URL should contain supabase.co", SupabaseConfig.supabaseUrl.contains("supabase.co"))
    assertNotNull(SupabaseConfig.supabaseAnonKey)
    assertTrue("Supabase Anon Key should not be blank", SupabaseConfig.supabaseAnonKey.isNotBlank())
  }

  @Test
  fun supabaseConnection_isSuccessful() = runBlocking {
    val result = SupabaseConfig.verifyConnection()
    assertTrue("Supabase connection check should succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)
    val message = result.getOrNull()
    assertNotNull(message)
    assertTrue("Should indicate HTTP 200 connection: $message", message!!.contains("200"))
  }
}
