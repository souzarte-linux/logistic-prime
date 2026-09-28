# Relatório de DevOps - Integração ML Kit Text Recognition e Permissões de Navegação

| Metadado | Detalhe |
| :--- | :--- |
| **Documento** | Relatório de Dependências, Permissões e Validação de Build |
| **Projeto** | Central do Motorista (`logistic-prime`) |
| **Package** | `com.fernando.centraldomotorista` |
| **Módulo** | Bipagem de Pacotes & Roteirização Master |
| **Data** | 28 de Setembro de 2026 |
| **Responsável** | Agente DevOps (Central do Motorista - Pocket) |
| **Status do Build** | :white_check_mark: **Aprovado (BUILD SUCCESSFUL)** |

---

## 1. Sumário Executivo

Como parte da implementação da Bipagem de Encomendas (Barcode + OCR) e da Roteirização Avançada para Usuários Master, foram efetuadas as configurações de infraestrutura de compilação, dependências e permissões do Android:
1. **Version Catalog (`gradle/libs.versions.toml`):** Inclusão da biblioteca Google ML Kit Text Recognition (`com.google.mlkit:text-recognition:16.0.1`).
2. **Build Script (`app/build.gradle.kts`):** Inclusão da dependência `implementation(libs.mlkit.text.recognition)`.
3. **Manifesto (`app/src/main/AndroidManifest.xml`):**
   - Garantia das permissões de hardware e localização: `CAMERA`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` e `VIBRATE`.
   - Adição das declarações de `<queries>` para visibilidade dos pacotes do **Google Maps** (`com.google.android.apps.maps`) e **Waze** (`com.waze`), em conformidade com o Android 11+ (API 30+).
   - Inclusão dos esquemas de intent de navegação (`geo:` e `google.navigation:`).
   - Configuração de meta-data para pré-download automático de modelos de ML (`ocr,barcode`) via Google Play Services.
4. **Validação de Build:** Compilação Kotlin e processamento de manifesto executados com sucesso via `./gradlew compileDebugKotlin`.

---

## 2. Detalhamento das Alterações

### 2.1. Version Catalog (`gradle/libs.versions.toml`)

No bloco `[versions]`:
```toml
camerax = "1.4.1"
mlkitBarcode = "17.3.0"
mlkitTextRecognition = "16.0.1"
```

No bloco `[libraries]`:
```toml
# CameraX & ML Kit Barcode Scanning / Text Recognition
androidx-camera-core = { group = "androidx.camera", name = "camera-core", version.ref = "camerax" }
androidx-camera-camera2 = { group = "androidx.camera", name = "camera-camera2", version.ref = "camerax" }
androidx-camera-lifecycle = { group = "androidx.camera", name = "camera-lifecycle", version.ref = "camerax" }
androidx-camera-view = { group = "androidx.camera", name = "camera-view", version.ref = "camerax" }
mlkit-barcode-scanning = { group = "com.google.mlkit", name = "barcode-scanning", version.ref = "mlkitBarcode" }
mlkit-text-recognition = { group = "com.google.mlkit", name = "text-recognition", version.ref = "mlkitTextRecognition" }
```

### 2.2. Script de Build da Aplicação (`app/build.gradle.kts`)

Adição da dependência mapeada pelo accessor `libs.mlkit.text.recognition`:
```kotlin
    // CameraX & ML Kit Barcode & Text Recognition
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.mlkit.text.recognition)
```

### 2.3. Manifesto Android (`app/src/main/AndroidManifest.xml`)

#### Permissões e Recursos de Hardware
- `android.permission.CAMERA`: Leitura de código de barras e OCR de etiquetas.
- `android.permission.ACCESS_FINE_LOCATION` e `android.permission.ACCESS_COARSE_LOCATION`: Obtenção da localização do motorista para cálculo de rota e distância.
- `android.permission.VIBRATE`: Feedback tátil na confirmação de bipagem com sucesso.
- `<uses-feature android:name="android.hardware.camera" android:required="false" />`: Permite instalação em dispositivos sem câmera traseira obrigatória (ex: coletores industriais com scanner integrado).

#### Package Visibility (`<queries>`)
Para atender às restrições de visibilidade de pacotes introduzidas no Android 11 (API level 30), foram declarados explicitamente os pacotes e schemes dos aplicativos externos de navegação e comunicação:
```xml
    <queries>
        <!-- Mensageria / WhatsApp -->
        <package android:name="com.whatsapp" />
        <package android:name="com.whatsapp.w4b" />
        <!-- Aplicativos de Navegação e Rotas -->
        <package android:name="com.google.android.apps.maps" />
        <package android:name="com.waze" />
        <intent>
            <action android:name="android.intent.action.VIEW" />
            <data android:scheme="geo" />
        </intent>
        <intent>
            <action android:name="android.intent.action.VIEW" />
            <data android:scheme="google.navigation" />
        </intent>
        <!-- Navegador / Web -->
        <intent>
            <action android:name="android.intent.action.VIEW" />
            <data android:scheme="https" />
        </intent>
    </queries>
```

#### Pré-carregamento de Modelos ML Kit (`<meta-data>`)
Inclusão da instrução para que a Google Play Store baixe antecipadamente os modelos de OCR e Barcode no ato do download do app, evitando atraso na primeira bipagem:
```xml
    <!-- Pré-download automático dos modelos ML Kit (OCR e Barcode) via Google Play Services -->
    <meta-data
        android:name="com.google.mlkit.vision.DEPENDENCIES"
        android:value="barcode,ocr" />
```

---

## 3. Compatibilidade R8 / ProGuard

A auditoria de [`app/proguard-rules.pro`](file:///d:/Dev/logistic-prime/logistic-prime/app/proguard-rules.pro) confirmou que a seção 7 já contém as regras para preservação dos símbolos e classes do Google Play Services e ML Kit:
```proguard
# 7. Google Play Services, Credentials & ML Kit
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-keep class androidx.camera.** { *; }
```
Dessa forma, builds de release (`assembleRelease` / `bundleRelease`) não sofrerão quebras de ofuscação com a introdução do reconhecimento de texto.

---

## 4. Resultado da Validação de Build

Execução do comando:
```powershell
.\gradlew compileDebugKotlin
```

Resultado obtido:
```text
> Task :app:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :app:generateDebugBuildConfig
> Task :app:generateDebugResValues
> Task :app:checkDebugAarMetadata
> Task :app:processDebugMainManifest
> Task :app:processDebugManifest
> Task :app:processDebugManifestForPackage
> Task :app:processDebugResources
> Task :app:compileDebugKotlin

BUILD SUCCESSFUL in 7m 17s
15 actionable tasks: 15 executed
```

- **Resolução de dependências:** Sem conflitos de dependências transitivas.
- **Manifest Merger:** Sem erros de colisão no merge de manifestos com o AAR do ML Kit.
- **Status:** Pronto para consumo pelos agentes Mobile Developer e QA.
