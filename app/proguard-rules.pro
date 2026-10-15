# kotlinx.serialization: the catalogue's @Serializable classes and their generated serializers
-keepattributes *Annotation*, InnerClasses, Signature
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class br.com.shiurhebraico.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class br.com.shiurhebraico.**$$serializer { *; }
-keepclassmembers class br.com.shiurhebraico.** { *** Companion; }
# the YouTube player talks to its page through a JavaScript interface
-keep class com.pierfrancescosoffritti.androidyoutubeplayer.** { *; }
-keepclassmembers class * { @android.webkit.JavascriptInterface <methods>; }
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
