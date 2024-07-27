package dev.cyberdeck.wetbulbapp.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import dev.cyberdeck.wetbulbapp.R
import dev.cyberdeck.wetbulbapp.openmeteo.Location
import dev.cyberdeck.wetbulbapp.openmeteo.TestData
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationEditor(
    location: Location?,
    modifier: Modifier = Modifier,
    provideSuggestions: suspend (text: String) -> List<Location> = { emptyList() },
    onLocationChange: (location: Location) -> Unit = {},
) {
    var text by rememberSaveable { mutableStateOf(location?.name ?: "") }
    var expanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(location) }
    var suggestions by remember { mutableStateOf(emptyList<Location>()) }

    val scope = rememberCoroutineScope()

    val focusManager = LocalFocusManager.current
    val confirmLocation = { newLoc: Location ->
        selected = newLoc
        text = newLoc.name
        expanded = false
        suggestions = emptyList()
        focusManager.clearFocus()
        onLocationChange(newLoc)
    }

    ExposedDropdownMenuBox(
        modifier = modifier,
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        TextField(
            value = text,
            isError = selected == null,
            maxLines = 1,
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable),
            keyboardOptions = KeyboardOptions(
                autoCorrectEnabled = true,
                imeAction = ImeAction.Go
            ),
            keyboardActions = KeyboardActions(
                onGo = {
                    if (suggestions.isNotEmpty()) {
                        confirmLocation(suggestions.first())
                    }
                }
            ),
            onValueChange = {
                text = it
                selected = null
                scope.launch {
                    suggestions = provideSuggestions(it)
                    expanded = suggestions.isNotEmpty()
                }
            },
            label = {
                Text(stringResource(R.string.location))
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },
            colors = ExposedDropdownMenuDefaults.textFieldColors()
        )

        if (suggestions.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                }
            ) {
                suggestions.forEach { suggestion ->
                    DropdownMenuItem(
                        onClick = {
                            confirmLocation(suggestion)
                        },
                        text = {
                            Text(text = suggestion.name)
                        }
                    )
                }
            }
        }
    }
}

@Composable
@Preview
private fun PreviewLocationEditor() {
    Box(modifier = Modifier.fillMaxSize()) {
        LocationEditor(
            location = TestData.shibuya,
            provideSuggestions = { text -> TestData.locations.filter { it.name.startsWith(text) } },
        )
    }
}