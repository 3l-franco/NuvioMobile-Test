package com.nuvio.app.features.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.DialogButton
import com.nuvio.app.core.ui.DialogButtonStyle
import com.nuvio.app.core.ui.DialogButtons
import com.nuvio.app.core.ui.DialogSurface
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.player.SubtitleFontImportResult
import com.nuvio.app.features.player.SubtitleFontRepository
import com.nuvio.app.features.player.rememberSubtitleFontImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Picks the subtitle font, and imports or deletes the user's own .ttf/.otf fonts. */
@Composable
internal fun SubtitleFontDialog(
    selectedFontFile: String?,
    onFontSelected: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    LaunchedEffect(Unit) { withContext(Dispatchers.Default) { SubtitleFontRepository.ensureLoaded() } }
    val fonts by SubtitleFontRepository.fonts.collectAsState()
    var errorRes by remember { mutableStateOf<StringResource?>(null) }
    val importFont = rememberSubtitleFontImporter(
        onPicked = { fileName, bytes ->
            errorRes = when (val result = SubtitleFontRepository.import(fileName, bytes)) {
                is SubtitleFontImportResult.Imported -> {
                    onFontSelected(result.font.fileName)
                    null
                }
                SubtitleFontImportResult.NotAFont -> Res.string.settings_playback_subtitle_font_invalid
                SubtitleFontImportResult.TooLarge -> Res.string.settings_playback_subtitle_font_too_large
                SubtitleFontImportResult.Failed -> Res.string.settings_playback_subtitle_font_failed
            }
        },
        onError = { errorRes = Res.string.settings_playback_subtitle_font_failed },
    )
    val selected = selectedFontFile?.takeIf { file -> fonts.any { it.fileName == file } }

    DialogSurface(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.settings_playback_subtitle_font),
        message = stringResource(Res.string.settings_playback_subtitle_font_note),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SubtitleFontChoiceRow(
                name = stringResource(Res.string.settings_playback_subtitle_font_default),
                fileName = null,
                selected = selected == null,
                onClick = { onFontSelected(null) },
            )

            fonts.forEach { font ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SubtitleFontChoiceRow(
                        name = font.familyName,
                        fileName = font.fileName,
                        selected = selected == font.fileName,
                        onClick = { onFontSelected(font.fileName) },
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { SubtitleFontRepository.delete(font) }) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = stringResource(
                                Res.string.settings_playback_subtitle_font_delete,
                                font.familyName,
                            ),
                            tint = MaterialTheme.nuvio.colors.textSecondary,
                        )
                    }
                }
            }
        }

        errorRes?.let { res ->
            Text(
                text = stringResource(res),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.nuvio.colors.danger,
            )
        }

        DialogButtons {
            DialogButton(
                text = stringResource(Res.string.settings_playback_subtitle_font_import),
                onClick = {
                    errorRes = null
                    importFont()
                },
            )
            DialogButton(
                text = stringResource(Res.string.action_done),
                style = DialogButtonStyle.Primary,
                onClick = onDismiss,
            )
        }
    }
}

@Composable
private fun SubtitleFontChoiceRow(
    name: String,
    fileName: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val shape = tokens.shapes.compactCard
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (selected) {
                    tokens.colors.accent.copy(alpha = tokens.opacity.selected)
                } else {
                    tokens.colors.surfaceCard
                },
            )
            .border(
                BorderStroke(
                    tokens.borders.thin,
                    if (selected) tokens.colors.accent else tokens.colors.borderSubtle,
                ),
                shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tokens.colors.accent.copy(alpha = tokens.opacity.pressed)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Aa",
                color = tokens.colors.accent,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                color = tokens.colors.textPrimary,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (fileName != null) {
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = tokens.colors.accent,
                modifier = Modifier.size(tokens.icons.md),
            )
        }
    }
}
