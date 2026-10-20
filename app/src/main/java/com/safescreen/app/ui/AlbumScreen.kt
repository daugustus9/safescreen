package com.safescreen.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safescreen.app.R
import com.safescreen.app.core.Album
import com.safescreen.app.core.Sticker
import com.safescreen.app.core.StickerState
import com.safescreen.app.graph
import com.safescreen.app.system.Share

private val Gold = Color(0xFFE0A800)

/**
 * Álbum de Habilidades: one sticker per ENEM skill. Progress the child can show off without
 * any ranking; a missing sticker can be practiced or asked for from a friend.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlbumScreen(onBack: () -> Unit, onPractice: (String) -> Unit) {
    val graph = LocalContext.current.graph
    val answers by graph.answers.answers.collectAsState()
    val questions = graph.content.questions
    val areaNames = questions.associate { it.area to it.areaName }
    val skills = graph.content.skills.values
        .sortedWith(compareBy({ it.area }, { it.code.drop(1).toInt() }))
    val stickers = remember(answers) { Album.stickers(skills, questions, answers) }
    var open by remember { mutableStateOf<Sticker?>(null) }

    val stuck = stickers.count { it.state == StickerState.EARNED || it.state == StickerState.SHINY }
    ScreenScaffold(title = stringResource(R.string.album_title), onBack = onBack) {
        Text(stringResource(R.string.album_intro), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.album_count, stuck, stickers.size), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.album_shiny, stickers.count { it.state == StickerState.SHINY }),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(stringResource(R.string.album_legend), style = MaterialTheme.typography.bodySmall)

        stickers.groupBy { it.skill.area }.forEach { (area, inArea) ->
            SectionCard(areaNames[area] ?: area) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    inArea.forEach { sticker -> StickerTile(sticker) { open = sticker } }
                }
            }
        }
    }

    open?.let { sticker ->
        StickerDialog(
            sticker = sticker,
            areaName = areaNames[sticker.skill.area] ?: sticker.skill.area,
            onPractice = { open = null; onPractice(sticker.skill.key) },
            onClose = { open = null },
        )
    }
}

@Composable
private fun StickerTile(sticker: Sticker, onClick: () -> Unit) {
    val (container, content, border, mark) = when (sticker.state) {
        StickerState.SHINY -> Quad(Gold, Color.Black, null, "✨")
        StickerState.EARNED -> Quad(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary, null, "✓")
        StickerState.AVAILABLE -> Quad(Color.Transparent, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary, "○")
        StickerState.COMING_SOON -> Quad(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.outline, null, "–")
    }
    Surface(
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(8.dp),
        border = border?.let { BorderStroke(1.dp, it) },
        modifier = Modifier.size(52.dp).clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(sticker.skill.code, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(mark, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

private data class Quad(val container: Color, val content: Color, val border: Color?, val mark: String)

@Composable
private fun StickerDialog(sticker: Sticker, areaName: String, onPractice: () -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val skill = sticker.skill
    val askText = stringResource(R.string.album_ask_text, skill.code, areaName, skill.tip)
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("$areaName · ${skill.code}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(
                        when (sticker.state) {
                            StickerState.SHINY -> R.string.album_state_shiny
                            StickerState.EARNED -> R.string.album_state_earned
                            StickerState.AVAILABLE -> R.string.album_state_available
                            StickerState.COMING_SOON -> R.string.album_coming_soon
                        },
                    ),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(skill.description, style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.summary_study_tip, skill.tip), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            if (sticker.state != StickerState.COMING_SOON) {
                TextButton(onClick = onPractice) { Text(stringResource(R.string.album_practice)) }
            }
        },
        dismissButton = {
            Row {
                if (sticker.state == StickerState.AVAILABLE) {
                    TextButton(onClick = { Share.send(context, askText) }) { Text(stringResource(R.string.album_ask_friend)) }
                }
                TextButton(onClick = onClose) { Text(stringResource(R.string.common_close)) }
            }
        },
    )
}
