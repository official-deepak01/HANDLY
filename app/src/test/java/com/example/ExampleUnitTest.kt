package com.example

import com.example.data.DefaultProfiles
import com.example.model.ActionType
import com.example.model.GestureType
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testDefaultProfilesLoaded() {
    val profiles = DefaultProfiles.getAllBuiltInProfiles()
    assertTrue("Default profiles should not be empty", profiles.isNotEmpty())
    val generalProfile = profiles.find { it.id == DefaultProfiles.GENERAL_PHONE.id }
    assertNotNull("General Phone profile must exist", generalProfile)
    assertTrue("General Phone should have gesture mappings", generalProfile!!.mappings.isNotEmpty())
    assertEquals(ActionType.BACK, generalProfile.mappings[GestureType.FIST]?.action)
  }

  @Test
  fun testGestureTypesCount() {
    val allGestures = GestureType.values()
    assertEquals(12, allGestures.size)
    assertTrue(allGestures.contains(GestureType.OPEN_PALM))
    assertTrue(allGestures.contains(GestureType.FIST))
    assertTrue(allGestures.contains(GestureType.PINCH))
    assertTrue(allGestures.contains(GestureType.OK_SIGN))
  }
}
