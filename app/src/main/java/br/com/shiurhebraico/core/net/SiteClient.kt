package br.com.shiurhebraico.core.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/** The one door to the website: the static shelves, the live lists and the streamed AI work. */
class SiteClient(
    cacheDir: File?,
    val base: String = BASE,
    client: OkHttpClient? = null,
) {
    val json = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }

    val http: OkHttpClient = client ?: OkHttpClient.Builder()
        .apply { if (cacheDir != null) cache(Cache(File(cacheDir, "site"), 10L * 1024 * 1024)) }
        .connectTimeout(20, TimeUnit.SECONDS)
        // a summary stream can take minutes; the heartbeats keep the read alive
        .readTimeout(90, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("x-emunah-app", "1")
                    .header("user-agent", "EmunahShlemah-Android")
                    .build(),
            )
        }
        .build()

    private fun request(path: String): Request = Request.Builder().url(if (path.startsWith("http")) path else base + path).build()

    /** GET → text, or an IOException with the status */
    suspend fun text(path: String): String = withContext(Dispatchers.IO) {
        http.newCall(request(path)).execute().use { res ->
            if (!res.isSuccessful) throw HttpError(res.code)
            res.body?.string() ?: ""
        }
    }

    suspend fun element(path: String): JsonElement = json.parseToJsonElement(text(path))

    suspend fun obj(path: String): JsonObject = element(path).jsonObject

    /**
     * A streamed answer read line by line (the site's readNdjson): heartbeats, chunks, then the
     * document. A plain JSON body counts as one line. Returns the HTTP status.
     */
    suspend fun ndjson(path: String, onLine: suspend (JsonObject) -> Unit): Int = withContext(Dispatchers.IO) {
        http.newCall(request(path)).execute().use { res ->
            if (!res.isSuccessful) return@withContext res.code
            val body = res.body ?: return@withContext res.code
            val source = body.source()
            while (true) {
                val line = source.readUtf8Line() ?: break
                val obj = parseLine(line) ?: continue
                onLine(obj)
            }
            res.code
        }
    }

    fun parseLine(line: String): JsonObject? {
        val s = line.trim()
        if (s.isEmpty()) return null
        return try {
            json.parseToJsonElement(s) as? JsonObject
        } catch (_: Exception) {
            null // a partial line
        }
    }

    companion object {
        const val BASE = "https://emunah-shlemah-qwik.vercel.app"
    }
}

class HttpError(val code: Int) : IOException("HTTP $code")
