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
import com.fernando.centraldomotorista.data.model.DeliveryPartner
import com.fernando.centraldomotorista.data.model.MasterRouteStop
import com.fernando.centraldomotorista.data.model.PackageType
import com.fernando.centraldomotorista.data.model.Platform
import com.fernando.centraldomotorista.data.model.StopStatus
import com.fernando.centraldomotorista.data.model.TransferStatus
import com.fernando.centraldomotorista.data.remote.supabase
import com.fernando.centraldomotorista.data.repository.DeliveryPartnerRepository
import com.fernando.centraldomotorista.data.repository.MasterRouteRepository
import com.fernando.centraldomotorista.data.repository.PlatformRepository
import com.fernando.centraldomotorista.util.ParsedAddress
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime
import java.util.concurrent.atomic.AtomicBoolean

enum class ScannerStep {
    BARCODE_SEARCH,
    OCR_CONFIRMATION
}

data class RouteScannerUiState(
    val routeId: String = "",
    val totalScannedCount: Int = 0,
    val lastScannedStop: MasterRouteStop? = null,
    val isTorchOn: Boolean = false,
    val isSaving: Boolean = false,
    val duplicateAlertBarcode: String? = null,
    val autoAdvanceCountdown: Int? = null,
    val error: String? = null,
    // Extensões Prompt 2 (Sticky Platform + PackageType)
    val platforms: List<Platform> = emptyList(),
    val currentPlatformId: String? = null,
    val currentPackageType: PackageType = PackageType.PACOTINHO,
    // Extensões Prompt 3 (Gate de Confiança + Foto)
    val isConfidenceWarning: Boolean = false,
    val isPhotoUploading: Boolean = false,
    // Extensões Prompt 6 (Atribuição Cruzada Master ↔ Parceiro)
    val deliveryPartners: List<DeliveryPartner> = emptyList(),
    val selectedPartnerIdForNextScan: String? = null,
    // TASK-DES-08: Scanner Fracionado (2 Etapas)
    val currentStep: ScannerStep = ScannerStep.BARCODE_SEARCH,
    val pendingBarcode: String? = null,
    val pendingParsedAddress: ParsedAddress? = null,
    val isOcrScanning: Boolean = false
) {
    val activePlatform: Platform?
        get() = platforms.firstOrNull { it.id == currentPlatformId }
}

class RouteScannerViewModel(
    private val masterRouteRepository: MasterRouteRepository = MasterRouteRepository(),
    private val platformRepository: PlatformRepository = PlatformRepository(),
    private val deliveryPartnerRepository: DeliveryPartnerRepository = DeliveryPartnerRepository()
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
        val user = supabase.auth.currentUserOrNull()
        _uiState.value = _uiState.value.copy(routeId = routeId)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val existingStops = masterRouteRepository.getRouteStops(routeId)
                scannedBarcodes.clear()
                existingStops.forEach { scannedBarcodes.add(it.barcode) }

                val currentRoute = masterRouteRepository.getRouteById(routeId)
                val platformsList = if (user != null) platformRepository.getActivePlatforms(user.id) else emptyList()
                val partnersList = if (user != null) deliveryPartnerRepository.getDeliveryPartners(user.id) else emptyList()

                // Se ainda não houver currentPlatformId definido, herda a da rota ou a primeira ativa
                val initialPlatformId = _uiState.value.currentPlatformId
                    ?: currentRoute?.platformId
                    ?: platformsList.firstOrNull()?.id

                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        totalScannedCount = existingStops.size,
                        platforms = platformsList,
                        deliveryPartners = partnersList,
                        currentPlatformId = initialPlatformId
                    )
                }
            } catch (e: Exception) {
                Log.e(tag, "Erro ao carregar dados da rota $routeId: ${e.message}", e)
            }
        }
    }

    /**
     * Alterna a plataforma ativa (Prompt 2).
     * Esta seleção é sticky: persiste entre scans até nova troca manual.
     */
    fun onPlatformSelected(platformId: String) {
        _uiState.value = _uiState.value.copy(currentPlatformId = platformId)
    }

    /**
     * Alterna o tipo de pacote a ser bipado (Pacotinho vs Volumoso).
     * Esta seleção NÃO é sticky: após o salvamento, retorna para PACOTINHO.
     */
    fun onPackageTypeSelected(packageType: PackageType) {
        _uiState.value = _uiState.value.copy(currentPackageType = packageType)
    }

    /**
     * Define com quem fica o pacote lido (Prompt 6).
     * null = Master (padrão); String = ID do Parceiro.
     */
    fun onAssignedPartnerSelected(partnerId: String?) {
        _uiState.value = _uiState.value.copy(selectedPartnerIdForNextScan = partnerId)
    }

    fun isBarcodeAlreadyScanned(barcode: String): Boolean {
        return scannedBarcodes.contains(barcode.trim())
    }

    fun onDuplicateBarcodeDetected(context: Context, barcode: String) {
        val now = System.currentTimeMillis()
        if (now - lastDuplicateBeepTimestamp < 1500L) {
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

    /**
     * Etapa 1 -> Etapa 2: Código de barras detectado com sucesso.
     * Toca som/vibra e transiciona imediatamente para OCR_CONFIRMATION.
     */
    fun onBarcodeDetected(context: Context, barcode: String) {
        val trimmed = barcode.trim()
        if (trimmed.isBlank()) return

        if (scannedBarcodes.contains(trimmed)) {
            onDuplicateBarcodeDetected(context, trimmed)
            return
        }

        emitSensoryFeedback(context, isSuccess = true)
        _uiState.value = _uiState.value.copy(
            currentStep = ScannerStep.OCR_CONFIRMATION,
            pendingBarcode = trimmed,
            pendingParsedAddress = null,
            isOcrScanning = true,
            duplicateAlertBarcode = null
        )
    }

    /**
     * Etapa 2: Refinamento contínuo de OCR da etiqueta enquanto em OCR_CONFIRMATION.
     */
    fun onOcrAddressDetected(parsedAddress: ParsedAddress) {
        if (_uiState.value.currentStep != ScannerStep.OCR_CONFIRMATION) return

        val current = _uiState.value.pendingParsedAddress
        val hasValuableData = !parsedAddress.cep.isNullOrBlank() ||
                !parsedAddress.recipientName.isNullOrBlank() ||
                parsedAddress.street != null

        if (current == null || hasValuableData) {
            _uiState.value = _uiState.value.copy(
                pendingParsedAddress = parsedAddress,
                isOcrScanning = false
            )
        }
    }

    /**
     * Etapa 2: Usuário clica em "CONFIRMAR PACOTE".
     * Persiste o pacote com todos os dados capturados e retorna para BARCODE_SEARCH.
     */
    fun confirmPendingPackage(context: Context) {
        val barcode = _uiState.value.pendingBarcode ?: return
        val parsed = _uiState.value.pendingParsedAddress ?: ParsedAddress(
            recipientName = null,
            street = null,
            number = null,
            neighborhood = null,
            city = null,
            state = null,
            cep = null,
            fullFormattedAddress = ""
        )
        savePackageStop(context, barcode, parsed)
    }

    /**
     * Etapa 2: Usuário clica em "Pular OCR" (salva apenas código e metadados).
     */
    fun skipOcrAndConfirm(context: Context) {
        val barcode = _uiState.value.pendingBarcode ?: return
        val emptyParsed = ParsedAddress(
            recipientName = null,
            street = null,
            number = null,
            neighborhood = null,
            city = null,
            state = null,
            cep = null,
            fullFormattedAddress = ""
        )
        savePackageStop(context, barcode, emptyParsed)
    }

    /**
     * Etapa 2: Usuário clica em "Bipar Novamente" (descarta e volta à Etapa 1).
     */
    fun retryScanningCurrentPackage() {
        _uiState.value = _uiState.value.copy(
            currentStep = ScannerStep.BARCODE_SEARCH,
            pendingBarcode = null,
            pendingParsedAddress = null,
            isOcrScanning = false
        )
    }

    fun onPackageScanned(
        context: Context,
        barcode: String,
        parsedAddress: ParsedAddress
    ) {
        savePackageStop(context, barcode, parsedAddress)
    }

    private fun savePackageStop(
        context: Context,
        barcode: String,
        parsedAddress: ParsedAddress
    ) {
        val trimmedBarcode = barcode.trim()
        if (scannedBarcodes.contains(trimmedBarcode)) {
            onDuplicateBarcodeDetected(context, trimmedBarcode)
            return
        }

        scannedBarcodes.add(trimmedBarcode)

        val nextOrder = _uiState.value.totalScannedCount + 1
        val selectedPlatformId = _uiState.value.currentPlatformId
        val selectedPackageType = _uiState.value.currentPackageType
        val assignedPartner = _uiState.value.selectedPartnerIdForNextScan

        // Gate de confiança (Prompt 3): requer CEP e endereço não vazios
        val isConfident = !parsedAddress.cep.isNullOrBlank() && parsedAddress.fullFormattedAddress.isNotBlank()

        _uiState.value = _uiState.value.copy(
            isSaving = true,
            duplicateAlertBarcode = null,
            isConfidenceWarning = !isConfident
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
                    state = parsedAddress.state,
                    platformId = selectedPlatformId,
                    packageType = selectedPackageType,
                    assignedPartnerId = assignedPartner,
                    transferStatus = if (assignedPartner != null) TransferStatus.ATRIBUIDO_PENDENTE else null,
                    transferredVia = if (assignedPartner != null) "manual_master" else null
                )

                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        totalScannedCount = nextOrder,
                        lastScannedStop = createdStop,
                        // Retorna ao modo de busca de barcode
                        currentStep = ScannerStep.BARCODE_SEARCH,
                        pendingBarcode = null,
                        pendingParsedAddress = null,
                        isOcrScanning = false,
                        // PackageType reseta para PACOTINHO (não-sticky)
                        currentPackageType = PackageType.PACOTINHO,
                        // Atribuição de parceiro reseta para Master (não-sticky)
                        selectedPartnerIdForNextScan = null
                        // currentPlatformId MANTÉM-SE (sticky!)
                    )

                    // Só dispara auto-avanço se a leitura for 100% confiável (Prompt 3)
                    if (isConfident) {
                        startAutoAdvanceTimer()
                    } else {
                        // Sem auto-avanço: card fica aberto aguardando conferência do motorista
                        autoAdvanceJob?.cancel()
                        _uiState.value = _uiState.value.copy(autoAdvanceCountdown = null)
                    }
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
            autoAdvanceCountdown = null,
            isConfidenceWarning = false
        )
    }

    /**
     * Atualiza a foto de backup da etiqueta física (Prompt 3).
     * Define validade de 15 dias para expiração periódica.
     */
    fun attachPhotoToLastStop(photoBase64OrUrl: String) {
        val stop = _uiState.value.lastScannedStop ?: return
        _uiState.value = _uiState.value.copy(isPhotoUploading = true)

        viewModelScope.launch(Dispatchers.IO) {
            val expiresAt = OffsetDateTime.now().plusDays(15)
            val success = masterRouteRepository.updateStopPhoto(
                stopId = stop.id,
                photoUrl = photoBase64OrUrl,
                photoExpiresAt = expiresAt
            )
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(
                    isPhotoUploading = false,
                    lastScannedStop = if (success) stop.copy(photoUrl = photoBase64OrUrl, photoExpiresAt = expiresAt) else stop
                )
            }
        }
    }

    /**
     * Atualização manual do endereço e destinatário em caso de leitura OCR truncada.
     */
    fun updateStopManualData(recipientName: String?, fullAddress: String, cep: String?) {
        val stop = _uiState.value.lastScannedStop ?: return
        _uiState.value = _uiState.value.copy(isSaving = true)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val updatedStop = stop.copy(
                    recipientName = recipientName?.trim(),
                    fullAddress = fullAddress.trim(),
                    cep = cep?.trim()
                )
                masterRouteRepository.addStopsBatch(listOf(updatedStop))
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        lastScannedStop = updatedStop,
                        isConfidenceWarning = false
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(isSaving = false)
                }
            }
        }
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
            try {
                if (isSuccess) {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
                } else {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 180)
                }
            } catch (e: Exception) {
                // Silencioso
            }

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
