# Room, Hilt e kotlinx.serialization já publicam suas próprias regras de consumo
# via consumer-rules; mantenha este arquivo para regras específicas do app.

-keepattributes *Annotation*
-keepclassmembers class com.beautymanager.app.data.remote.barcode.** { *; }
