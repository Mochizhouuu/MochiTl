package com.mochi.tl.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mochi.tl.designsystem.MochiAppTheme

@Composable
fun ButtonAndCardPreviewContent() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MochiCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "MochiTL Design System Card",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Clean Slate Blue surface with 1dp outline border.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        MochiButton(onClick = {}) {
            Text("Mochi Primary CTA Button")
        }

        MochiOutlinedButton(onClick = {}) {
            Text("Mochi Outlined Button")
        }
    }
}

@Preview(name = "Design System - Light Theme", showBackground = true)
@Composable
fun PreviewDesignSystemLight() {
    MochiAppTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ButtonAndCardPreviewContent()
        }
    }
}

@Preview(name = "Design System - Dark Theme", showBackground = true)
@Composable
fun PreviewDesignSystemDark() {
    MochiAppTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ButtonAndCardPreviewContent()
        }
    }
}
