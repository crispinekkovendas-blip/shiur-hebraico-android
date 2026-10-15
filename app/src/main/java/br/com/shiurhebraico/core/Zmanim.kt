package br.com.shiurhebraico.core

import com.kosherjava.zmanim.ComplexZmanimCalendar
import com.kosherjava.zmanim.util.GeoLocation
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

/**
 * The halachic times the site uses (utils/zmanim.ts, Hebcal's NOAA calculation): alot = 16.1°,
 * chatzot, shkiá = sunset; Shabat candles at Friday's sunset − 18 min, havdalá at Saturday's
 * sunset + 30 min (Sephardic minhag). The site's default place is Barueri – SP.
 */
data class Place(val label: String, val lat: Double, val lng: Double, val tzid: String)

val DEFAULT_PLACE = Place("Barueri – SP", -23.5108, -46.8763, "America/Sao_Paulo")

data class DayTimes(val alot: Date?, val sunrise: Date?, val chatzot: Date?, val shkiah: Date?)

data class ShabatTimes(val candles: Date?, val havdala: Date?)

object Zmanim {
    private fun calendar(place: Place, day: Calendar): ComplexZmanimCalendar {
        val tz = TimeZone.getTimeZone(place.tzid)
        val c = ComplexZmanimCalendar(GeoLocation(place.label, place.lat, place.lng, 0.0, tz))
        c.calendar = (day.clone() as Calendar).also { it.timeZone = tz }
        return c
    }

    fun day(place: Place = DEFAULT_PLACE, at: Date = Date()): DayTimes {
        val cal = Calendar.getInstance(TimeZone.getTimeZone(place.tzid)).apply { time = at }
        val z = calendar(place, cal)
        return DayTimes(z.alosHashachar, z.sunrise, z.chatzos, z.sunset)
    }

    /** the coming Shabat (or this one, if today is Friday or Saturday) */
    fun shabat(place: Place = DEFAULT_PLACE, at: Date = Date()): ShabatTimes {
        val tz = TimeZone.getTimeZone(place.tzid)
        val fri = Calendar.getInstance(tz).apply { time = at }
        val back = if (fri.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) -1 else 0
        fri.add(Calendar.DAY_OF_MONTH, back)
        while (fri.get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY) fri.add(Calendar.DAY_OF_MONTH, 1)
        val sat = (fri.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
        val friSunset = calendar(place, fri).sunset
        val satSunset = calendar(place, sat).sunset
        return ShabatTimes(friSunset?.let { Date(it.time - 18 * 60_000) }, satSunset?.let { Date(it.time + 30 * 60_000) })
    }

    /** which service fits this moment (utils/siddur-now.ts): alot–chatzot Shacharit, chatzot–shkiá Minchá, else Arvit */
    fun serviceNow(now: Date = Date(), z: DayTimes? = day(at = now)): String {
        if (z?.alot != null && z.chatzot != null && z.shkiah != null) {
            if (!now.before(z.alot) && now.before(z.chatzot)) return "shacharit"
            if (!now.before(z.chatzot) && now.before(z.shkiah)) return "mincha"
            return "arvit"
        }
        val h = Calendar.getInstance().apply { time = now }.get(Calendar.HOUR_OF_DAY)
        return if (h in 4..11) "shacharit" else if (h in 12..16) "mincha" else "arvit"
    }

    fun serviceLabel(s: String) = when (s) { "shacharit" -> "Shacharit"; "mincha" -> "Minchá"; else -> "Arvit" }

    /** Friday after sunset or Saturday */
    fun isShabbat(now: Date = Date(), z: DayTimes? = day(at = now)): Boolean {
        val d = Calendar.getInstance(TimeZone.getTimeZone(DEFAULT_PLACE.tzid)).apply { time = now }.get(Calendar.DAY_OF_WEEK)
        val afterShkiah = z?.shkiah != null && now.after(z.shkiah)
        return d == Calendar.SATURDAY || (d == Calendar.FRIDAY && afterShkiah)
    }
}
