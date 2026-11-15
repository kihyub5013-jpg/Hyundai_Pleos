# 제출 APK 크기 줄이기: 쓰지 않는 Material 아이콘(수만 개)만 덜어 낸다.
# Pleos SDK 와 그 의존성(gson·tink·jna·hilt·AIDL)은 리플렉션·IPC 를 쓰므로 나머지 클래스는 모두 그대로 둔다.
-dontobfuscate
-keep class !androidx.compose.material.icons.**, ** { *; }
-dontwarn **
