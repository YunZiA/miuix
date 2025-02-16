import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.LazyColumn
import top.yukonga.miuix.kmp.basic.PullToRefreshBox
import top.yukonga.miuix.kmp.basic.PullToRefreshState
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.extra.SuperDropdown
import top.yukonga.miuix.kmp.utils.getWindowSize

@Composable
fun SecondPage(
    topAppBarScrollBehavior: ScrollBehavior,
    padding: PaddingValues
) {
    val dropdownOptions = listOf("Option 1", "Option 2", "Option 3", "Option 4")
    val dropdownSelectedOption = remember { mutableStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var itemCount by remember { mutableIntStateOf(5) }
    val state = remember { PullToRefreshState() }
    val onRefresh = {
        isRefreshing = true
        coroutineScope.launch {
            itemCount += 1
            isRefreshing = false
        }
    }
    PullToRefreshBox(
        modifier = Modifier.padding(top = padding.calculateTopPadding()),
        isRefreshing = isRefreshing,
        onRefresh = { onRefresh() },
        state = state
    ) {
        LazyColumn(
            modifier = Modifier.height(getWindowSize().height.dp),
            topAppBarScrollBehavior = topAppBarScrollBehavior,
        ) {
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(top = 12.dp, bottom = 12.dp + padding.calculateBottomPadding())
                ) {
                    for (i in 0 until itemCount) {
                        SuperDropdown(
                            title = "Dropdown $i",
                            items = dropdownOptions,
                            selectedIndex = dropdownSelectedOption.value,
                            onSelectedIndexChange = { newOption -> dropdownSelectedOption.value = newOption }
                        )
                    }
                }
            }
        }
    }
}