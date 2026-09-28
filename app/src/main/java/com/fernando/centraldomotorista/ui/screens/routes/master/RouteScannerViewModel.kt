package com.fernando.centraldomotorista.ui.screens.routes.master

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.camera.core.CameraControl
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fernando.centraldomotorista.data.model.MasterRouteStop
import com.fernando.centraldomotorista.data.model.StopStatus
import com.fernando.centraldomotorista.data.repository.MasterRouteRepository
import com.fernando.centraldomotorista.util.BrazilianLabelParser
import com.fernando.centraldomotorista.util.ParsedAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

data class RouteScannerUiState(
    val routeId: String = "",
    val totalScannedCount: Int = 0,
    val lastScannedStop: MasterRouteStop? = null,
    val isTorchOn: Boolean = false,
    val isSaving: Boolean = false,
    val duplicateAlertBarcode: String? = null,
    val autoAdvanceCountdown: Int? = null,
    val error: String? = null
)

class RouteScannerViewModel(
    private val masterRouteRepository: MasterRouteRepository = MasterRouteRepository()
) : ViewModel() {

    private val tag = "RouteScannerVM"

    private val _uiState = MutableStateFlow(RouteScannerUiState())
    val uiState: StateFlow<RouteScannerUiState> = _uiState.asStateFlow()

    private val scannedBarcodes = mutableSetOf<String>()
    private var lastDuplicateBeepTimestamp = 0L
    private var autoAdvanceJob: Job? = null
    val isAnalyzingFrame = AtomicBoolean(false)

    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        } catch (e: Exception) {
            Log.w(tag, "Não foi possível inicializar ToneGenerator: ${e.message}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            toneGenerator?.release()
        } catch (e: Exception) {
            // Ignora
        }
    }

    fun initRoute(routeId: String) {
        _uiState.value = _uiState.value.copy(routeId = routeId)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val existingStops = masterRouteRepository.getRouteStops(routeId)
                scannedBarcodes.clear()
                existingStops.forEach { scannedBarcodes.add(it.barcode) }

                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        totalScannedCount = existingStops.size
                    )
                }
            } catch (e: Exception) {
                Log.e(tag, "Erro ao carregar paradas existentes da rota $routeId: ${e.message}", e)
            }
        }
    }

    fun isBarcodeAlreadyScanned(barcode: String): Boolean {
        return scannedBarcodes.contains(barcode.trim())
    }

    fun onDuplicateBarcodeDetected(context: Context, barcode: String) {
        val now = System.currentTimeMillis()
        if (now - lastDuplicateBeepTimestamp < 1500L) {
            // Debounce para evitar sobrecarga sensorial com o mesmo código
            return
        }
        lastDuplicateBeepTimestamp = now

        emitSensoryFeedback(context, isSuccess = false)
        _uiState.value = _uiState.value.copy(duplicateAlertBarcode = barcode)

        viewModelScope.launch {
            delay(1200)
            if (_uiState.value.duplicateAlertBarcode == barcode) {
                _uiState.value = _uiState.value.copy(duplicateAlertBarcode = null)
            }
        }
    }

    fun onPackageScanned(
        context: Context,
        barcode: String,
        parsedAddress: ParsedAddress
    ) {
        val trimmedBarcode = barcode.trim()
        if (scannedBarcodes.contains(trimmedBarcode)) {
            onDuplicateBarcodeDetected(context, trimmedBarcode)
            return
        }

        // Adiciona imediatamente ao conjunto para evitar reentrância em frames subsequentes
        scannedBarcodes.add(trimmedBarcode)

        emitSensoryFeedback(context, isSuccess = true)

        val nextOrder = _uiState.value.totalScannedCount + 1
        _uiState.value = _uiState.value.copy(
            isSaving = true,
            duplicateAlertBarcode = null
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val createdStop = masterRouteRepository.addStop(
                    routeId = _uiState.value.routeId,
                    barcode = trimmedBarcode,
                    recipientName = parsedAddress.recipientName,
                    fullAddress = parsedAddress.fullFormattedAddress,
                    cep = parsedAddress.cep,
                    stopOrder = nextOrder,
                    street = parsedAddress.street,
                    number = parsedAddress.number,
                    neighborhood = parsedAddress.neighborhood,
                    city = parsedAddress.city,
                    state = parsedAddress.state
                )

                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        totalScannedCount = nextOrder,
                        lastScannedStop = createdStop
                    )
                    startAutoAdvanceTimer()
                }
            } catch (e: Exception) {
                Log.e(tag, "Erro ao salvar parada bipada $trimmedBarcode: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = "Erro ao persistir pacote $trimmedBarcode."
                    )
                }
            }
        }
    }

    private fun startAutoAdvanceTimer() {
        autoAdvanceJob?.cancel()
        autoAdvanceJob = viewModelScope.launch {
            for (sec in 2 downTo 1) {
                _uiState.value = _uiState.value.copy(autoAdvanceCountdown = sec)
                delay(1000)
            }
            dismissLastScannedCard()
        }
    }

    fun dismissLastScannedCard() {
        autoAdvanceJob?.cancel()
        _uiState.value = _uiState.value.copy(
            lastScannedStop = null,
            autoAdvanceCountdown = null
        )
    }

    fun toggleTorch(cameraControl: CameraControl?) {
        val nextTorch = !_uiState.value.isTorchOn
        _uiState.value = _uiState.value.copy(isTorchOn = nextTorch)
        try {
            cameraControl?.enableTorch(nextTorch)
        } catch (e: Exception) {
            Log.w(tag, "Não foi possível alternar lanterna: ${e.message}")
        }
    }

    private fun emitSensoryFeedback(context: Context, isSuccess: Boolean) {
        viewModelScope.launch(Dispatchers.Main) {
            // 1. Áudio
            try {
                if (isSuccess) {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
                } else {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 180)
                }
            } catch (e: Exception) {
                // Silencioso em caso de erro de áudio
            }

            // 2. Vibração Háptica
            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vibratorManager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }

                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val duration = if (isSuccess) 50L else 150L
                        vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(if (isSuccess) 50L else 150L)
                    }
                }
            } catch (e: Exception) {
                // Silencioso
            }
        }
    }
}
