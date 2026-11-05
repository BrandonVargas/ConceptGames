# kotlinx.serialization keeps its own generated serializers via consumer rules.
# Ktor uses reflection-free engines; nothing extra is needed for OkHttp.
-dontwarn org.slf4j.**
