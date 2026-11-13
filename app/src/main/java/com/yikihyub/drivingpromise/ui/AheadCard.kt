package com.yikihyub.drivingpromise.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.CarCrash
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import androidx.compose.material.icons.outlined.Flood
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalParking
import androidx.compose.material.icons.outlined.Merge
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.RoundaboutRight
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SyncAlt
import androidx.compose.material.icons.outlined.Toll
import androidx.compose.material.icons.outlined.Train
import androidx.compose.material.icons.outlined.TurnLeft
import androidx.compose.material.icons.outlined.UTurnLeft
import androidx.compose.material.icons.outlined.Umbrella
import androidx.compose.material.icons.outlined.UnfoldLess
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.yikihyub.drivingpromise.domain.Situation
import com.yikihyub.drivingpromise.domain.SituationKind
import com.yikihyub.drivingpromise.ui.theme.Pleos

val SituationKind.icon: ImageVector
    get() = when (this) {
        SituationKind.LEFT_TURN -> Icons.Outlined.TurnLeft
        SituationKind.UTURN -> Icons.Outlined.UTurnLeft
        SituationKind.ROUNDABOUT -> Icons.Outlined.RoundaboutRight
        SituationKind.MERGE -> Icons.Outlined.Merge
        SituationKind.TUNNEL -> Icons.Outlined.Lightbulb
        SituationKind.UNDERPASS -> Icons.Outlined.Flood
        SituationKind.TOLL -> Icons.Outlined.Toll
        SituationKind.REST_AREA, SituationKind.REST -> Icons.Outlined.LocalCafe
        SituationKind.ACCIDENT -> Icons.Outlined.CarCrash
        SituationKind.SCHOOL -> Icons.Outlined.School
        SituationKind.RAILROAD -> Icons.Outlined.Train
        SituationKind.BUMP -> Icons.Outlined.Warning
        SituationKind.FOG -> Icons.Outlined.Cloud
        SituationKind.DOWNHILL -> Icons.AutoMirrored.Outlined.TrendingDown
        SituationKind.NARROW -> Icons.Outlined.UnfoldLess
        SituationKind.SLIPPERY -> Icons.Outlined.AcUnit
        SituationKind.WRONG_WAY -> Icons.Outlined.DoNotDisturbOn
        SituationKind.SIGNAL -> Icons.Outlined.SyncAlt
        SituationKind.RAIN -> Icons.Outlined.Umbrella
        SituationKind.DUSK -> Icons.Outlined.DarkMode
        SituationKind.PARKING -> Icons.Outlined.LocalParking
        SituationKind.SPOT -> Icons.Outlined.Place
    }

/** 사고·어린이·철길·역주행·지하차도·깜짝 지점은 주의(주황), 나머지는 안내(파랑) */
private val SituationKind.serious: Boolean
    get() = this in setOf(
        SituationKind.ACCIDENT, SituationKind.SCHOOL, SituationKind.RAILROAD, SituationKind.WRONG_WAY,
        SituationKind.UNDERPASS, SituationKind.SPOT, SituationKind.SLIPPERY, SituationKind.FOG,
    )

/**
 * 곧 만나는 곳: 차가 방금 말로 안내한 상황을 그림과 남은 거리로 확인시켜 준다.
 * 음성이 먼저 나가고 카드는 1~2초 눈길로 확인하는 용도라 아이콘·거리·한 문장만 둔다.
 */
@Composable
fun AheadCard(s: Situation?, modifier: Modifier = Modifier) {
    AnimatedVisibility(s != null, modifier, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
        val sit = s ?: return@AnimatedVisibility
        val tone = if (sit.kind.serious) Pleos.caution else Pleos.info
        Panel(Modifier.fillMaxWidth(), color = tone.copy(alpha = if (Pleos.c.dark) 0.18f else 0.08f).compositeOver(Pleos.surface)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                Box(Modifier.size(104.dp).clip(CircleShape).background(tone), contentAlignment = Alignment.Center) {
                    Icon(sit.kind.icon, null, Modifier.size(60.dp), tint = Color.White)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("곧 만나는 곳", style = MaterialTheme.typography.bodySmall, color = Pleos.ink3)
                    Text(sit.kind.title, style = MaterialTheme.typography.headlineMedium, color = Pleos.ink)
                }
                sit.distanceM?.takeIf { it > 0 }?.let { m ->
                    Text(
                        if (m >= 1000) "%.1fkm".format(m / 1000f) else "${(m / 10) * 10}m",
                        style = MaterialTheme.typography.displayMedium,
                        color = tone,
                    )
                }
            }
            Text(sit.speech, style = MaterialTheme.typography.bodyLarge, color = Pleos.ink, maxLines = 3)
        }
    }
}
