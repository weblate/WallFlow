package com.ammar.wallflow.ui.common

import android.content.res.Configuration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ammar.wallflow.R
import com.ammar.wallflow.ui.common.taginput.TagInputField
import com.ammar.wallflow.ui.theme.WallFlowTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> Dropdown(
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    options: Set<DropdownOption<T>> = emptySet(),
    initialSelectedOption: T? = null,
    hideCheck: Boolean = false,
    emptyOptionsMessage: String? = null,
    placeholder: @Composable (() -> Unit)? = null,
    onChange: (value: T) -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedOption by remember {
        mutableStateOf(options.find { it.value == initialSelectedOption } ?: options.firstOrNull())
    }

    ExposedDropdownMenuBox(
        modifier = modifier,
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            modifier = Modifier.menuAnchor(),
            readOnly = true,
            value = selectedOption?.text ?: "",
            onValueChange = {},
            label = label,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
            placeholder = placeholder,
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.text) },
                    onClick = {
                        selectedOption = option
                        expanded = false
                        onChange(option.value)
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    trailingIcon = if (!hideCheck && option.value == selectedOption?.value) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                            )
                        }
                    } else {
                        null
                    },
                )
            }
            if (options.isEmpty()) {
                DropdownMenuItem(
                    text = {
                        Text(text = emptyOptionsMessage ?: stringResource(R.string.no_options))
                    },
                    enabled = false,
                    onClick = {},
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

data class DropdownOption<T>(
    val value: T,
    val text: String,
    val icon: @Composable (() -> Unit)? = null,
)

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewDropdownTextInput() {
    WallFlowTheme {
        Surface {
            Dropdown(
                label = { Text(text = "Dropdown") },
                options = setOf(
                    DropdownOption(
                        value = "option1",
                        text = "Option 1",
                    ),
                ),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropdownMultiple(
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    options: Set<DropdownOption<T>> = emptySet(),
    initialSelectedOptions: Set<T>? = null,
    emptyOptionsMessage: String? = null,
    placeholder: @Composable (() -> Unit)? = null,
    onChange: (value: Set<T>) -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedOptions by remember {
        val selected = options.filter {
            initialSelectedOptions?.contains(it.value) == true
        }.ifEmpty {
            val first = options.firstOrNull()
            if (first == null) {
                emptySet()
            } else {
                setOf(first)
            }
        }.toSet()
        mutableStateOf(selected)
    }

    ExposedDropdownMenuBox(
        modifier = modifier,
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        TagInputField(
            modifier = Modifier.menuAnchor(),
            readOnly = true,
            tags = selectedOptions,
            label = label,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
            placeholder = placeholder,
            getTagString = { it.text },
            onRemoveTag = {
                selectedOptions -= it
                onChange(selectedOptions.map { o -> o.value }.toSet())
            },
            getLeadingIcon = { it.icon },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.text) },
                    onClick = {
                        selectedOptions = if (option in selectedOptions) {
                            selectedOptions - option
                        } else {
                            selectedOptions + option
                        }
                        onChange(selectedOptions.map { it.value }.toSet())
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    leadingIcon = option.icon,
                    trailingIcon = if (option in selectedOptions) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                            )
                        }
                    } else {
                        null
                    },
                )
            }
            if (options.isEmpty()) {
                DropdownMenuItem(
                    text = {
                        Text(text = emptyOptionsMessage ?: stringResource(R.string.no_options))
                    },
                    enabled = false,
                    onClick = {},
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewDropdownMultiple() {
    WallFlowTheme {
        Surface {
            DropdownMultiple(
                label = { Text(text = "Dropdown") },
                options = setOf(
                    DropdownOption(
                        value = "option1",
                        text = "Option 1",
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.baseline_folder_24),
                                contentDescription = null,
                            )
                        },
                    ),
                ),
            )
        }
    }
}
