package com.example.amap.util

import android.content.Context
import com.amap.api.maps.MapsInitializer
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class AmapPrivacyTest {

    private lateinit var mockContext: Context

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        // Mock the static MapsInitializer methods
        mockkStatic(MapsInitializer::class)
        every { MapsInitializer.updatePrivacyShow(any(), any(), any()) } returns Unit
        every { MapsInitializer.updatePrivacyAgree(any(), any()) } returns Unit
    }

    @Test
    fun `ensureCompliance should call MapsInitializer with correct parameters`() {
        // Given - mock context
        
        // When - ensuring compliance
        AmapPrivacy.ensureCompliance(mockContext)
        
        // Then - should call both privacy methods correctly
        verify(exactly = 1) { 
            MapsInitializer.updatePrivacyShow(mockContext, true, true) 
        }
        verify(exactly = 1) { 
            MapsInitializer.updatePrivacyAgree(mockContext, true) 
        }
    }

    @Test
    fun `ensureCompliance called multiple times should work`() {
        // Given - mock context
        
        // When - calling multiple times
        AmapPrivacy.ensureCompliance(mockContext)
        AmapPrivacy.ensureCompliance(mockContext)
        AmapPrivacy.ensureCompliance(mockContext)
        
        // Then - should call privacy methods multiple times
        verify(exactly = 3) { 
            MapsInitializer.updatePrivacyShow(mockContext, true, true) 
        }
        verify(exactly = 3) { 
            MapsInitializer.updatePrivacyAgree(mockContext, true) 
        }
    }

    @Test
    fun `ensureCompliance with different contexts should work`() {
        // Given - multiple mock contexts
        val context1 = mockk<Context>(relaxed = true)
        val context2 = mockk<Context>(relaxed = true)
        
        // When - calling with different contexts
        AmapPrivacy.ensureCompliance(context1)
        AmapPrivacy.ensureCompliance(context2)
        
        // Then - should call privacy methods for each context
        verify(exactly = 1) { 
            MapsInitializer.updatePrivacyShow(context1, true, true) 
        }
        verify(exactly = 1) { 
            MapsInitializer.updatePrivacyShow(context2, true, true) 
        }
        verify(exactly = 1) { 
            MapsInitializer.updatePrivacyAgree(context1, true) 
        }
        verify(exactly = 1) { 
            MapsInitializer.updatePrivacyAgree(context2, true) 
        }
    }
} 