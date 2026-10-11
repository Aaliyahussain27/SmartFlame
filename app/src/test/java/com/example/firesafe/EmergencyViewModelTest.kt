package com.example.firesafe

import com.example.firesafe.data.repository.impl.MockOtpRepository
import com.example.firesafe.viewmodel.EmergencyFlowState
import com.example.firesafe.viewmodel.EmergencyViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EmergencyViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private fun createTestViewModel() = EmergencyViewModel(
        locationRepository = MockLocationRepository(),
        fireStationRepository = MockFireStationRepository(),
        alertRepository = MockAlertRepository(),
        otpRepository = MockOtpRepository()
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateIsIdle() {
        val viewModel = createTestViewModel()
        assertTrue(viewModel.uiState.value is EmergencyFlowState.Idle)
    }

    @Test
    fun testStartEmergencyTransitionsToDetectingLocation() {
        val viewModel = createTestViewModel()
        viewModel.startEmergency()
        assertTrue(viewModel.uiState.value is EmergencyFlowState.DetectingLocation)
    }

    @Test
    fun testResolveLocationAndDispatchTransitionsToAlertSent() = runTest {
        val viewModel = createTestViewModel()
        viewModel.startEmergency()
        
        viewModel.resolveLocationAndDispatch {}
        
        testScheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertTrue("Expected AlertSent state but was $state", state is EmergencyFlowState.AlertSent)
        val alertState = state as EmergencyFlowState.AlertSent
        assertEquals("850 Bryant St, San Francisco, CA 94103", alertState.location.address)
        assertEquals("AL-8893-X", alertState.alertConfirmation.alertId)
    }

    @Test
    fun testProceedToPhotoTransitionsToAwaitingPhoto() = runTest {
        val viewModel = createTestViewModel()
        viewModel.startEmergency()
        viewModel.resolveLocationAndDispatch {}
        testScheduler.advanceUntilIdle()
        
        viewModel.proceedToPhoto()
        
        val state = viewModel.uiState.value
        assertTrue("Expected AwaitingPhoto state but was $state", state is EmergencyFlowState.AwaitingPhoto)
    }

    @Test
    fun testSavePhotoAndProceedTransitionsToAwaitingOTP() = runTest {
        val viewModel = createTestViewModel()
        viewModel.startEmergency()
        viewModel.resolveLocationAndDispatch {}
        testScheduler.advanceUntilIdle()
        viewModel.proceedToPhoto()
        
        viewModel.savePhotoAndProceed("content://dummy/photo.jpg", "1234567890")
        
        val state = viewModel.uiState.value
        assertTrue("Expected AwaitingOTP state but was $state", state is EmergencyFlowState.AwaitingOTP)
        val otpState = state as EmergencyFlowState.AwaitingOTP
        assertEquals("content://dummy/photo.jpg", otpState.photoPath)
        assertEquals("1234567890", otpState.phoneNumber)
    }

    @Test
    fun testVerifyOtpTransitionsToConfirmed() = runTest {
        val viewModel = createTestViewModel()
        viewModel.startEmergency()
        viewModel.resolveLocationAndDispatch {}
        testScheduler.advanceUntilIdle()
        viewModel.proceedToPhoto()
        viewModel.savePhotoAndProceed("content://dummy/photo.jpg", "1234567890")
        
        viewModel.verifyOtp("123456") {}
        
        testScheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertTrue("Expected Confirmed state but was $state", state is EmergencyFlowState.Confirmed)
        val confirmedState = state as EmergencyFlowState.Confirmed
        assertEquals("1234567890", confirmedState.verifiedPhoneNumber)
    }

    @Test
    fun testCancelAlertResetsToIdle() = runTest {
        val viewModel = createTestViewModel()
        viewModel.startEmergency()
        viewModel.resolveLocationAndDispatch {}
        testScheduler.advanceUntilIdle()
        
        viewModel.cancelAlert {}
        testScheduler.advanceUntilIdle()
        
        assertTrue(viewModel.uiState.value is EmergencyFlowState.Idle)
    }
}
