import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.MiuixNavigationBar
import top.yukonga.miuix.kmp.MiuixScrollBehavior
import top.yukonga.miuix.kmp.MiuixTopAppBar
import top.yukonga.miuix.kmp.NavigationItem
import top.yukonga.miuix.kmp.basic.MiuixHorizontalPager
import top.yukonga.miuix.kmp.basic.MiuixScaffold
import top.yukonga.miuix.kmp.basic.MiuixSurface
import top.yukonga.miuix.kmp.rememberMiuixTopAppBarState

@Composable
fun UITest() {
    val topAppBarScrollBehavior0 = MiuixScrollBehavior(rememberMiuixTopAppBarState())
    val topAppBarScrollBehavior1 = MiuixScrollBehavior(rememberMiuixTopAppBarState())
    val topAppBarScrollBehavior2 = MiuixScrollBehavior(rememberMiuixTopAppBarState())

    val topAppBarScrollBehaviorList = listOf(topAppBarScrollBehavior0, topAppBarScrollBehavior1, topAppBarScrollBehavior2)

    val selectedItem = remember { mutableStateOf(0) }

    val pagerState = rememberPagerState(initialPage = 0, initialPageOffsetFraction = 0f, pageCount = { 3 })

    val isClickBottomBarChange = remember { mutableStateOf(false) }

    val currentScrollBehavior = when (pagerState.currentPage) {
        0 -> topAppBarScrollBehaviorList[0]
        1 -> topAppBarScrollBehaviorList[1]
        2 -> topAppBarScrollBehaviorList[2]
        else -> throw IllegalStateException("Unsupported page")
    }

    val items = listOf(
        NavigationItem("Main", Icons.Default.Phone),
        NavigationItem("Second", Icons.Default.Email),
        NavigationItem("Third", Icons.Default.Settings)
    )


    LaunchedEffect(selectedItem.value) {
        pagerState.animateScrollToPage(selectedItem.value)
    }
    LaunchedEffect(pagerState.currentPage) {
        if (isClickBottomBarChange.value) {
            isClickBottomBarChange.value = false
        } else {
            selectedItem.value = pagerState.currentPage
        }
    }

    MiuixSurface {
        MiuixScaffold(
            modifier = Modifier
                .fillMaxSize()
                .displayCutoutPadding(),
            topBar = {
                MiuixTopAppBar(
                    title = "Miuix",
                    scrollBehavior = currentScrollBehavior
                )
            },
            bottomBar = {
                MiuixNavigationBar(
                    items = items,
                    selectedItem = selectedItem,
                    onClick = { index ->
                        selectedItem.value = index
                        isClickBottomBarChange.value = true
                    }
                )
            }
        ) { padding ->
            MyHorizontalPager(
                pagerState = pagerState,
                topAppBarScrollBehaviorList = topAppBarScrollBehaviorList,
                padding = padding
            )
        }

    }
}

@Composable
fun MyHorizontalPager(
    pagerState: PagerState,
    topAppBarScrollBehaviorList: List<MiuixScrollBehavior>,
    padding: PaddingValues,
) {
    MiuixHorizontalPager(
        pagerState = pagerState,
        pageContent = { page ->
            when (page) {
                0 -> MainView(topAppBarScrollBehaviorList[0], padding)
                1 -> SecondView(topAppBarScrollBehaviorList[1], padding)
                else -> ThirdView(topAppBarScrollBehaviorList[2], padding)
            }
        }
    )
}