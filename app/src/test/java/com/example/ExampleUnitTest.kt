package com.example

import com.example.data.local.InitialDataSeeder
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testPasswordHashingIsConsistent() {
    val hash1 = InitialDataSeeder.hashPassword("secret123")
    val hash2 = InitialDataSeeder.hashPassword("secret123")
    val hash3 = InitialDataSeeder.hashPassword("different")
    assertEquals(hash1, hash2)
    assertNotEquals(hash1, hash3)
    assertEquals(64, hash1.length) // SHA-256 produces 64 hex characters
  }
}
