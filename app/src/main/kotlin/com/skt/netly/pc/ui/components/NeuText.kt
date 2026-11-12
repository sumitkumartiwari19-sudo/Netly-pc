package com.skt.netly.pc.ui.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.skt.netly.pc.ui.i18n.LocalLanguage
import com.skt.netly.pc.ui.i18n.Strings
import com.skt.netly.pc.ui.theme.Neu

@Composable
fun NeuText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = Neu.type.body,
    color: Color = Neu.colors.text,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign = TextAlign.Unspecified,
) {
    val shown = Strings.tr(LocalLanguage.current, text)
    BasicText(
        text = shown,
        modifier = modifier,
        style = style.copy(color = color, textAlign = textAlign),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}
