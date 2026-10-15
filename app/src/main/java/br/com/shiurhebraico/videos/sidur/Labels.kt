package br.com.shiurhebraico.videos.sidur

/** the Siddur's category and service names with their accents (from the Emunah Shlemah app) */
private val LABELS_PT = mapOf(
    "Bencaos" to "Bênçãos", "Calendario" to "Calendário", "Preparacao" to "Preparação",
    "Shacharit: Preparacao" to "Shacharit · Preparação", "Shacharit: Tefila" to "Shacharit · Tefilá", "Shacharit: Pos-Tefila" to "Shacharit · Pós-Tefilá",
    "Adicoes para Shacharit" to "Adições para Shacharit", "Mincha" to "Minchá", "Velas e Recepcao" to "Velas e Recepção", "Saida" to "Saída",
    "Refeicao" to "Refeição", "Natureza e Saude" to "Natureza e Saúde", "Bircat Halevana" to "Bircat HaLevaná",
    "Festas (Shalosh Regalim)" to "Festas · Shalosh Regalim", "Chanuca" to "Chanucá", "Mishna para Shabat" to "Mishná para Shabat",
)

fun labelPt(raw: String) = LABELS_PT[raw] ?: raw
