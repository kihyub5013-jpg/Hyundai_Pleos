package com.yikihyub.drivingpromise.domain

/** 운전자가 말한 한 마디를 앱이 할 일로 바꾼다. 화면을 보지 않고 쓸 수 있게. */
sealed interface VoiceCommand {
    data object GoHome : VoiceCommand
    data object GoOffice : VoiceCommand
    /** 최근 목적지·즐겨찾기 이름과 맞춰볼 검색어 */
    data class GoTo(val query: String) : VoiceCommand
    data class SetPromises(val promises: Set<Promise>) : VoiceCommand
    data object HowFar : VoiceCommand
    data object CarStatus : VoiceCommand
    data object MonthReport : VoiceCommand
    data class Yes(val yes: Boolean) : VoiceCommand
    data object Unknown : VoiceCommand

    companion object {
        private val goSuffix = Regex("(으로|로|에|까지)?\\s*(가자|가줘|가 줘|갈래|가요|안내해 ?줘|안내|길 ?찾아 ?줘)\\s*[.!?]*$")

        fun parse(raw: String): VoiceCommand {
            val said = raw.trim()
            val s = said.replace(" ", "")
            if (s.isEmpty()) return Unknown
            if (s in setOf("응", "네", "예", "어", "있어", "있어요", "맞아", "그래")) return Yes(true)
            if (s in setOf("아니", "아니요", "없어", "없어요", "혼자야", "혼자")) return Yes(false)
            if (s.contains("약속")) Promise.parse(said).takeIf { it.isNotEmpty() }?.let { return SetPromises(it) }
            if (s.contains("얼마나남") || s.contains("언제도착") || s.contains("몇분남")) return HowFar
            if (s.contains("차상태") || s.contains("차괜찮")) return CarStatus
            if (s.contains("이번달") || s.contains("이달")) return MonthReport
            if (goSuffix.containsMatchIn(said)) {
                val place = said.replace(goSuffix, "").trim()
                val p = place.replace(" ", "")
                return when {
                    p == "집" || p == "우리집" -> GoHome
                    p == "회사" || p == "직장" -> GoOffice
                    p.isNotEmpty() -> GoTo(place)
                    else -> Unknown
                }
            }
            // 약속이라는 말 없이 "밤에는 운전 안 할래"
            Promise.parse(said).takeIf { it.isNotEmpty() && (s.contains("안") || s.contains("피") || s.contains("만")) }?.let { return SetPromises(it) }
            return Unknown
        }

        /** 장소 이름 중 검색어와 가장 잘 맞는 것. 공백 무시 포함 관계로 본다. */
        fun <T> bestMatch(query: String, items: List<T>, name: (T) -> String): T? {
            val q = query.replace(" ", "")
            return items.firstOrNull { name(it).replace(" ", "") == q }
                ?: items.firstOrNull { name(it).replace(" ", "").contains(q) }
                ?: items.firstOrNull { q.contains(name(it).replace(" ", "")) && name(it).isNotBlank() }
        }
    }
}
