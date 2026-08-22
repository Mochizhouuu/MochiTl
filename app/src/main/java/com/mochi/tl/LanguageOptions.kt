package com.mochi.tl

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

object LanguageOptions {
    const val AUTO_DETECT = "Deteksi Otomatis"

    val TARGET_LANGUAGES = listOf(
        "Indonesia",
        "Inggris",
        "Jepang",
        "Korea",
        "Mandarin Simplified",
        "Mandarin Traditional",
        "Spanyol",
        "Prancis",
        "Jerman",
        "Vietnam",
        "Thailand",
        "Melayu"
    )

    val SOURCE_LANGUAGES = listOf(AUTO_DETECT) + TARGET_LANGUAGES
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageDropdownField(
    label: String,
    selectedValue: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}
