package com.skt.netly.pc.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skt.netly.pc.ui.theme.Neu
import com.skt.netly.pc.ui.theme.neuInset

/** Pressed-in track with equal-width buttons; the selected one sinks in. */
@Composable
fun NeuSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    /** True: all options share the width (use with a fixed-width parent). False: each option fits its text. */
    equalWidth: Boolean = false,
) {
    val c = Neu.colors
    Row(
        modifier = modifier.then(Modifier.neuInset(c, 18.dp))
            .then(if (equalWidth) Modifier else Modifier.horizontalScroll(rememberScrollState()))
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { index, label ->
            NeuTextButton(
                text = label,
                onClick = { onSelect(index) },
                modifier = if (equalWidth) Modifier.weight(1f) else Modifier,
                selected = index == selectedIndex,
                radius = 14.dp,
                minHeight = 38.dp,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp),
            )
        }
    }
}
