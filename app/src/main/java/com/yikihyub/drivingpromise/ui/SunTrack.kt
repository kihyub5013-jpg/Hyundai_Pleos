package com.yikihyub.drivingpromise.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yikihyub.drivingpromise.domain.spoken
import com.yikihyub.drivingpromise.ui.theme.Pleos
import java.time.Duration
import java.time.ZonedDateTime

/**
 * "해 지기 전 도착"을 한 줄로 보여주는 해의 길.
 * 지금부터 일몰 뒤까지를 완만한 호로 그리고, 낮 구간은 해 색, 일몰 뒤는 밤 색.
 * 지금(검은 점) · 일몰(해) · 도착 예정(핀)이 같은 시간축에 놓여 한눈에 앞뒤가 보인다.
 * 도착이 일몰 앞이면 초록, 뒤면 주황.
 */
@Composable
fun SunTrack(now: ZonedDateTime, sunset: ZonedDateTime, arrival: ZonedDateTime?, modifier: Modifier = Modifier) {
    val c = Pleos.c
    val start = now
    val endCandidate = listOfNotNull(sunset, arrival).maxOrNull()!!.plusMinutes(40)
    val end = if (Duration.between(start, endCandidate).toMinutes() < 120) start.plusMinutes(120) else endCandidate
    val span = Duration.between(start, end).seconds.toFloat().coerceAtLeast(1f)
    fun frac(t: ZonedDateTime) = (Duration.between(start, t).seconds / span).coerceIn(0f, 1f)

    val sunsetF = frac(sunset)
    val arrivalF = arrival?.let { frac(it) }
    val arrivalColor = if (arrival != null && arrival.isBefore(sunset.minusMinutes(10))) c.kept else c.caution
    val density = LocalDensity.current

    BoxWithConstraints(modifier.fillMaxWidth().height(200.dp)) {
        val wPx = with(density) { maxWidth.toPx() }
        val trackTop = with(density) { 40.dp.toPx() }
        val trackBottom = with(density) { 116.dp.toPx() }
        val pad = with(density) { 24.dp.toPx() }

        // 완만한 호: 왼쪽 아래 → 가운데 위 → 오른쪽 아래
        val path = Path().apply {
            moveTo(pad, trackBottom)
            quadraticBezierTo(wPx / 2f, trackTop - (trackBottom - trackTop), wPx - pad, trackBottom)
        }
        val measure = PathMeasure().apply { setPath(path, false) }
        val len = measure.length
        fun at(f: Float): Offset = measure.getPosition(len * f)

        Canvas(Modifier.fillMaxWidth().height(200.dp)) {
            val day = Path(); val night = Path()
            measure.getSegment(0f, len * sunsetF, day, true)
            measure.getSegment(len * sunsetF, len, night, true)
            drawPath(night, c.night.copy(alpha = if (c.dark) 0.9f else 0.55f), style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round))
            drawPath(day, c.sun, style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round))
            // 일몰: 해 원반
            val s = at(sunsetF)
            drawCircle(c.sun, radius = 26.dp.toPx(), center = s)
            drawCircle(if (c.dark) Color(0xFF131417) else Color.White, radius = 26.dp.toPx(), center = s, style = Stroke(6.dp.toPx()))
            // 지금
            val n = at(0f)
            drawCircle(c.ink, radius = 15.dp.toPx(), center = n)
            drawCircle(if (c.dark) Color(0xFF131417) else Color.White, radius = 15.dp.toPx(), center = n, style = Stroke(5.dp.toPx()))
            // 도착 핀
            if (arrivalF != null) {
                val a = at(arrivalF)
                drawLine(arrivalColor, a, Offset(a.x, a.y - 52.dp.toPx()), strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)
                drawCircle(arrivalColor, radius = 17.dp.toPx(), center = Offset(a.x, a.y - 58.dp.toPx()))
                drawCircle(arrivalColor, radius = 11.dp.toPx(), center = a)
            }
        }

        // 라벨: 각 점 아래(도착은 위). 가장자리에서 잘리지 않게 폭 안으로 민다.
        val labelW = 220.dp
        @Composable
        fun label(f: Float, title: String, time: String, color: Color, above: Boolean = false) {
            val p = at(f)
            val xDp = with(density) { p.x.toDp() } - labelW / 2
            val x = xDp.coerceIn(0.dp, maxWidth - labelW)
            val y = with(density) { p.y.toDp() } + if (above) (-150).dp else 30.dp
            Box(Modifier.offset(x = x, y = y).width(labelW)) {
                Text(
                    "$title\n$time",
                    style = MaterialTheme.typography.bodySmall,
                    color = color,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        label(0f, "지금", now.spoken(), Pleos.ink2)
        label(sunsetF, "해 지는 시각", sunset.spoken(), Pleos.ink2)
        if (arrival != null && arrivalF != null) label(arrivalF, "도착 예정", arrival.spoken(), arrivalColor, above = true)
    }
}
