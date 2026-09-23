package com.parcelinbox.app.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parcelinbox.app.MainViewModel
import com.parcelinbox.app.R
import com.parcelinbox.app.SourceChoice
import com.parcelinbox.app.data.ParcelItem
import com.parcelinbox.app.data.ParcelStatus
import com.parcelinbox.app.settings.AppLanguage
import com.parcelinbox.app.settings.RetentionPolicy
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class Destination(
    @DrawableRes val icon: Int
) {
    HOME(R.drawable.ic_home),
    PARCELS(R.drawable.ic_box),
    PICKUP(R.drawable.ic_pin),
    SETTINGS(R.drawable.ic_settings)
}

private enum class ParcelFilter {
    ALL,
    ACTIVE,
    PICKUP,
    EXCEPTION,
    COMPLETED
}

private val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.CHINESE }

@Composable
private fun tr(chinese: String, english: String): String =
    if (LocalAppLanguage.current == AppLanguage.CHINESE) chinese else english

@Composable
fun ParcelInboxApp(
    viewModel: MainViewModel,
    onOpenNotificationSettings: () -> Unit
) {
    val onboardingComplete by viewModel.hasCompletedOnboarding.collectAsState()
    val permissionIntroComplete by viewModel.hasCompletedPermissionIntro.collectAsState()
    val hasNotificationAccess by viewModel.hasNotificationAccess.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val language by viewModel.language.collectAsState()
    var showLaunchScreen by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(1800)
        showLaunchScreen = false
    }

    CompositionLocalProvider(LocalAppLanguage provides language) {
        AnimatedContent(
            targetState = showLaunchScreen,
            transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(220)) },
            label = "launch screen"
        ) { launching ->
            when {
                launching -> LaunchScreen()
                !onboardingComplete -> WelcomeScreen(onStart = viewModel::completeOnboarding)
                !permissionIntroComplete -> PermissionSetupScreen(
                    hasNotificationAccess = hasNotificationAccess,
                    sources = sources,
                    onToggleSource = viewModel::setSourceEnabled,
                    onOpenNotificationSettings = onOpenNotificationSettings,
                    onContinue = viewModel::completePermissionIntro
                )
                else -> MainShell(
                    viewModel = viewModel,
                    onOpenNotificationSettings = onOpenNotificationSettings
                )
            }
        }
    }
}

@Composable
private fun LaunchScreen() {
    var phraseIndex by remember { mutableStateOf(0) }
    var loadingStarted by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (loadingStarted) 1f else 0f,
        animationSpec = tween(1700, easing = FastOutSlowInEasing),
        label = "launch progress"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "launch parcel float")
    val floatingY by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1050, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "launch parcel y"
    )

    LaunchedEffect(Unit) {
        loadingStarted = true
        delay(560)
        phraseIndex = 1
        delay(620)
        phraseIndex = 0
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color(0xFFFFF4E8),
                    0.52f to Color(0xFFF4F8F5),
                    1f to Color.White
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Surface(
                modifier = Modifier.size(124.dp).graphicsLayer { translationY = floatingY },
                shape = RoundedCornerShape(36.dp),
                color = Color(0xFFFFE7D4),
                shadowElevation = 8.dp
            ) {
                ImageAsset(R.drawable.parcel_purple, Modifier.padding(13.dp))
            }
            Text(
                "Parcelume",
                fontSize = 31.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            )
            Text(
                "PRIVATE PARCEL COMPANION",
                color = Color(0xFF796F75),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.7.sp
            )
            AnimatedContent(
                targetState = phraseIndex,
                transitionSpec = {
                    (fadeIn(tween(260)) + slideInHorizontally(tween(300)) { it / 8 })
                        .togetherWith(fadeOut(tween(190)) + slideOutHorizontally(tween(240)) { -it / 8 })
                },
                label = "launch language rotation"
            ) { index ->
                Text(
                    if (index == 0) "每一个包裹，都有迹可循" else "Every parcel, clearly in view",
                    color = Muted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.width(164.dp).height(3.dp).clip(CircleShape).background(Color(0xFFE8E2DE))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(Ink)
                )
            }
            Text(
                "本地整理  ·  PRIVATE BY DESIGN",
                color = Muted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.8.sp
            )
        }
    }
}

@Composable
private fun WelcomeScreen(onStart: () -> Unit) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val entranceAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "welcome alpha"
    )
    val entranceScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.9f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 260f),
        label = "welcome scale"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "welcome float")
    val floatingY by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "welcome parcel y"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color(0xFFFFE9D7),
                    0.42f to Color(0xFFEAF4EF),
                    0.72f to Color(0xFFFAF7F4),
                    1f to Color.White
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Box(Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(x = (-34).dp, y = 52.dp)
                    .rotate(-8f)
                    .graphicsLayer {
                        alpha = entranceAlpha
                        scaleX = entranceScale
                        scaleY = entranceScale
                        translationY = floatingY
                    }
                    .width(238.dp)
                    .height(260.dp),
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFFFFF8EE),
                shadowElevation = 10.dp
            ) {
                Box {
                    Text(
                        tr("自动发现", "Auto detect"),
                        modifier = Modifier.padding(20.dp),
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black
                    )
                    ImageAsset(
                        drawable = R.drawable.parcel_purple,
                        modifier = Modifier.align(Alignment.BottomCenter).size(210.dp)
                    )
                    BlackDot(modifier = Modifier.align(Alignment.TopEnd).padding(16.dp), text = "✓")
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(x = 48.dp, y = 260.dp)
                    .rotate(8f)
                    .graphicsLayer {
                        alpha = entranceAlpha
                        scaleX = entranceScale
                        scaleY = entranceScale
                        translationY = -floatingY
                    }
                    .width(224.dp)
                    .height(210.dp),
                shape = RoundedCornerShape(26.dp),
                color = Color(0xFFEAF3FF),
                shadowElevation = 12.dp
            ) {
                Box {
                    Text(
                        tr("安静整理", "Quietly sorted"),
                        modifier = Modifier.padding(18.dp),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    ImageAsset(
                        drawable = R.drawable.parcel_blue,
                        modifier = Modifier.align(Alignment.BottomEnd).size(170.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    tr("你的每一个\n包裹，都不会忘", "Every parcel,\nnever forgotten"),
                    fontSize = 36.sp,
                    lineHeight = 41.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                )
                Text(
                    tr(
                        "在本机自动整理物流通知，到了就提醒。",
                        "Organized privately on your phone, with reminders when parcels arrive."
                    ),
                    color = Muted,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(30.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Ink)
                ) {
                    Text(tr("开始使用", "Get started"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun PermissionSetupScreen(
    hasNotificationAccess: Boolean,
    sources: List<SourceChoice>,
    onToggleSource: (String, Boolean) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onContinue: () -> Unit
) {
    val selectedCount = sources.count { it.enabled }
    val ready = hasNotificationAccess && selectedCount > 0

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                tr("先确认权限\n再自动整理", "Permission first,\nthen auto-sort"),
                fontSize = 35.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp
            )
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                color = if (ready) Color(0xFFE7F3EC) else Color(0xFFFFEBD9),
                shadowElevation = 3.dp
            ) {
                Box(Modifier.fillMaxWidth().height(210.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth(0.68f).padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            if (ready) tr("准备完成", "You're all set") else tr("当前为仅浏览模式", "Browse-only for now"),
                            fontSize = 23.sp,
                            lineHeight = 27.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            if (hasNotificationAccess) {
                                tr("通知访问已开启 · 已选择 $selectedCount 个来源", "Notification access on · $selectedCount sources selected")
                            } else {
                                tr("未授权时不会自动识别新包裹", "New parcels cannot be detected without access")
                            },
                            color = Muted,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                    ImageAsset(
                        R.drawable.parcel_locker,
                        Modifier.align(Alignment.BottomEnd).size(155.dp).padding(7.dp)
                    )
                }
            }
        }

        item {
            SettingsSection(title = tr("Parcelume 会读取什么", "What Parcelume reads")) {
                PrivacyPoint(
                    number = "1",
                    title = tr("仅限你选择的平台通知", "Only selected app notifications"),
                    body = tr(
                        "Android 的通知访问授权范围较宽，但 Parcelume 会直接忽略未选择应用的通知。",
                        "Android grants broad notification access, but Parcelume immediately ignores apps you did not select."
                    )
                )
                PrivacyPoint(
                    number = "2",
                    title = tr("只提取必要物流字段", "Only essential delivery fields"),
                    body = tr(
                        "从通知标题和正文提取来源、状态、运单号、取件码和更新时间；能否识别商品名称取决于通知是否包含。",
                        "It extracts source, status, tracking number, pickup code and update time. Item names appear only when the notification includes them."
                    )
                )
                PrivacyPoint(
                    number = "3",
                    title = tr("完全本地，不上传", "Local only, never uploaded"),
                    body = tr(
                        "不申请联网权限，不读取购物历史、照片、联系人或短信；原始通知不会写入数据库。",
                        "No internet permission, shopping history, photos, contacts or messages. Raw notification text is never stored."
                    )
                )
            }
        }

        item {
            SettingsSection(title = tr("选择通知来源", "Choose notification sources")) {
                Text(
                    tr("只开启你希望 Parcelume 处理的平台，可随时在设置中修改。", "Select only the platforms you want processed. You can change this anytime."),
                    color = Muted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sources.forEach { choice ->
                        FilterChip(
                            selected = choice.enabled,
                            onClick = { onToggleSource(choice.source.packageName, !choice.enabled) },
                            label = { Text(localizedSourceLabel(choice.source.packageName, choice.source.label)) },
                            shape = RoundedCornerShape(22.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Ink,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFFF4F2F3),
                                labelColor = Ink
                            ),
                            border = null
                        )
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = {
                        if (ready) onContinue() else onOpenNotificationSettings()
                    },
                    enabled = ready || !hasNotificationAccess,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(30.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Ink)
                ) {
                    Text(
                        when {
                            ready -> tr("完成设置，进入 Parcelume", "Finish setup")
                            !hasNotificationAccess -> tr("开启通知访问", "Enable notification access")
                            else -> tr("请先选择一个来源", "Select at least one source")
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (!hasNotificationAccess) {
                    Text(
                        tr(
                            "点击后将打开手机的系统通知访问页面；权限只能由你在系统界面亲自开启。",
                            "This opens your phone's system notification-access page. Only you can grant access there."
                        ),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        color = Muted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
                TextButton(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        tr("暂不开启，仅浏览界面", "Not now — browse only"),
                        color = Muted,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivacyPoint(number: String, title: String, body: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(modifier = Modifier.size(28.dp), shape = CircleShape, color = Ink) {
            Box(contentAlignment = Alignment.Center) {
                Text(number, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(body, color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun MainShell(
    viewModel: MainViewModel,
    onOpenNotificationSettings: () -> Unit
) {
    val parcels by viewModel.parcels.collectAsState()
    val hasAccess by viewModel.hasNotificationAccess.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val retention by viewModel.retention.collectAsState()
    var destination by remember { mutableStateOf(Destination.HOME) }
    var selectedParcel by remember { mutableStateOf<ParcelItem?>(null) }

    Scaffold(
        containerColor = CanvasWhite,
        bottomBar = {
            AppNavigationBar(
                selected = destination,
                onSelect = { destination = it }
            )
        }
    ) { padding ->
        AnimatedContent(
            targetState = destination,
            modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding()),
            transitionSpec = {
                val direction = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                (fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it * direction / 10 })
                    .togetherWith(
                        fadeOut(tween(150)) + slideOutHorizontally(tween(220)) { -it * direction / 12 }
                    )
            },
            label = "main destination"
        ) { activeDestination ->
            when (activeDestination) {
                Destination.HOME -> HomeScreen(
                    parcels = parcels,
                    hasNotificationAccess = hasAccess,
                    hasEnabledSource = sources.any { it.enabled },
                    onOpenNotificationSettings = onOpenNotificationSettings,
                    onOpenSettings = { destination = Destination.SETTINGS },
                    onOpenAll = { destination = Destination.PARCELS },
                    onSelectParcel = { selectedParcel = it }
                )
                Destination.PARCELS -> ParcelCollectionScreen(
                    parcels = parcels,
                    initialFilter = ParcelFilter.ALL,
                    onSelectParcel = { selectedParcel = it }
                )
                Destination.PICKUP -> ParcelCollectionScreen(
                    parcels = parcels,
                    initialFilter = ParcelFilter.PICKUP,
                    onSelectParcel = { selectedParcel = it }
                )
                Destination.SETTINGS -> SettingsScreen(
                    hasNotificationAccess = hasAccess,
                    sources = sources,
                    retention = retention,
                    language = LocalAppLanguage.current,
                    onOpenNotificationSettings = onOpenNotificationSettings,
                    onToggleSource = viewModel::setSourceEnabled,
                    onSetRetention = viewModel::setRetention,
                    onSetLanguage = viewModel::setLanguage,
                    onAddDemoData = viewModel::addDemoData,
                    onDeleteAll = viewModel::deleteAll,
                    onShowOnboarding = viewModel::showOnboardingAgain
                )
            }
        }
    }

    selectedParcel?.let { parcel ->
        ParcelDetailDialog(
            parcel = parcel,
            onMarkCompleted = {
                viewModel.markCompleted(parcel.id)
                selectedParcel = null
            },
            onDismiss = { selectedParcel = null }
        )
    }
}

@Composable
private fun AppNavigationBar(selected: Destination, onSelect: (Destination) -> Unit) {
    NavigationBar(
        modifier = Modifier.navigationBarsPadding(),
        containerColor = Color.White,
        tonalElevation = 0.dp
    ) {
        Destination.entries.forEach { destination ->
            val active = destination == selected
            val label = destination.localizedLabel()
            val iconScale by animateFloatAsState(
                targetValue = if (active) 1f else 0.92f,
                animationSpec = spring(dampingRatio = 0.68f, stiffness = 420f),
                label = "nav icon scale"
            )
            NavigationBarItem(
                selected = active,
                onClick = { onSelect(destination) },
                icon = {
                    Surface(
                        shape = CircleShape,
                        color = if (active) Ink else Color.Transparent,
                        modifier = Modifier.size(38.dp).graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        }
                    ) {
                        Icon(
                            painter = painterResource(destination.icon),
                            contentDescription = label,
                            tint = if (active) Color.White else Ink,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                },
                label = { Text(label, fontSize = 11.sp, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent,
                    selectedTextColor = Ink,
                    unselectedTextColor = Muted
                )
            )
        }
    }
}

@Composable
private fun HomeScreen(
    parcels: List<ParcelItem>,
    hasNotificationAccess: Boolean,
    hasEnabledSource: Boolean,
    onOpenNotificationSettings: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAll: () -> Unit,
    onSelectParcel: (ParcelItem) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(ParcelFilter.ALL) }
    val filtered = parcels.filter { parcel ->
        parcel.matches(filter) && (query.isBlank() || listOfNotNull(
            parcel.title,
            parcel.sourceLabel,
            parcel.trackingNumber,
            parcel.pickupCode
        ).any { it.contains(query, ignoreCase = true) })
    }
    val featured = parcels.firstOrNull { it.status == ParcelStatus.READY_FOR_PICKUP }
        ?: parcels.firstOrNull { it.status != ParcelStatus.COMPLETED }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(tr("你好，今天", "Hello, today"), fontSize = 19.sp, fontWeight = FontWeight.Black)
                    Text(
                        if (parcels.isEmpty()) {
                            tr("等待第一个包裹", "Waiting for your first parcel")
                        } else {
                            val activeCount = parcels.count { it.status != ParcelStatus.COMPLETED }
                            tr("有 $activeCount 个包裹在路上", "$activeCount parcels on the way")
                        },
                        color = Muted,
                        fontSize = 13.sp
                    )
                }
                Surface(shape = CircleShape, color = Color(0xFFFFEBD8), modifier = Modifier.size(44.dp)) {
                    ImageAsset(R.drawable.parcel_purple, Modifier.padding(5.dp))
                }
            }
        }

        if (!hasNotificationAccess || !hasEnabledSource) {
            item {
                PermissionBanner(
                    hasNotificationAccess = hasNotificationAccess,
                    onClick = if (hasNotificationAccess) onOpenSettings else onOpenNotificationSettings
                )
            }
        }

        item {
            SearchField(value = query, onValueChange = { query = it })
        }

        item {
            FilterRow(selected = filter, onSelect = { filter = it })
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(tr("重点包裹", "Featured parcel"), fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text(
                    tr("查看全部", "See all"),
                    color = Muted,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable(onClick = onOpenAll)
                )
            }
        }

        item {
            FeaturedParcelCard(parcel = featured, onClick = { featured?.let(onSelectParcel) })
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(tr("最近更新", "Recent updates"), fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text(tr("${filtered.size} 件", "${filtered.size} items"), color = Muted, fontSize = 13.sp)
            }
        }

        if (filtered.isEmpty()) {
            item { CompactEmptyState() }
        } else {
            items(filtered.take(4), key = { it.id }) { parcel ->
                CompactParcelRow(parcel = parcel, onClick = { onSelectParcel(parcel) })
            }
        }
    }
}

@Composable
private fun PermissionBanner(hasNotificationAccess: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = Butter
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (hasNotificationAccess) tr("尚未选择通知来源", "No notification sources selected")
                    else tr("当前为仅浏览模式", "Browse-only mode"),
                    fontWeight = FontWeight.Black
                )
                Text(
                    if (hasNotificationAccess) {
                        tr("去设置选择需要识别的平台", "Choose the platforms you want to detect")
                    } else {
                        tr("开启通知访问后才能自动整理包裹", "Enable notification access to detect parcels")
                    },
                    color = Muted,
                    fontSize = 12.sp
                )
            }
            BlackDot(text = "→")
        }
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(52.dp),
        color = Color(0xFFF2F1F3),
        shape = RoundedCornerShape(26.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = null,
                tint = Muted,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Ink),
                decorationBox = { inner ->
                    if (value.isBlank()) {
                        Text(tr("搜索包裹、运单号或取件码", "Search parcels, tracking or pickup code"), color = Muted, fontSize = 13.sp)
                    }
                    inner()
                }
            )
        }
    }
}

@Composable
private fun FilterRow(selected: ParcelFilter, onSelect: (ParcelFilter) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ParcelFilter.entries.forEach { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelect(filter) },
                label = { Text(filter.localizedLabel()) },
                shape = RoundedCornerShape(22.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Ink,
                    selectedLabelColor = Color.White,
                    containerColor = Color.White,
                    labelColor = Ink
                ),
                border = null
            )
        }
    }
}

@Composable
private fun FeaturedParcelCard(parcel: ParcelItem?, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "featured float")
    val floatingY by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "featured parcel y"
    )
    Card(
        modifier = Modifier.fillMaxWidth().height(310.dp).animateContentSize(),
        onClick = onClick,
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Box(Modifier.fillMaxSize().background(parcel.cardBrush())) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    parcel?.let { localizedSourceLabel(it.sourcePackage, it.sourceLabel) } ?: "Parcelume",
                    fontSize = 13.sp,
                    color = Color(0xFF5E4A67)
                )
                Text(
                    parcel?.let { localizedParcelTitle(it) } ?: tr("等待发现\n第一个包裹", "Waiting for\nyour first parcel"),
                    fontSize = 30.sp,
                    lineHeight = 33.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.8).sp,
                    modifier = Modifier.fillMaxWidth(0.65f)
                )
                parcel?.pickupCode?.let {
                    Text(tr("取件码  $it", "Pickup code  $it"), fontSize = 16.sp, fontWeight = FontWeight.Black)
                }
            }
            ImageAsset(
                drawable = if (parcel?.status == ParcelStatus.READY_FOR_PICKUP) R.drawable.parcel_locker else R.drawable.parcel_purple,
                modifier = Modifier.align(Alignment.BottomEnd).size(215.dp).graphicsLayer {
                    translationY = floatingY
                }
            )
            Surface(
                modifier = Modifier.align(Alignment.BottomStart).padding(20.dp),
                shape = RoundedCornerShape(24.dp),
                color = Ink
            ) {
                Text(
                    parcel?.status?.localizedLabel() ?: tr("保持后台运行", "Keep running in background"),
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun CompactParcelRow(parcel: ParcelItem, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().animateContentSize().clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(70.dp),
                shape = RoundedCornerShape(18.dp),
                color = parcel.cardColor()
            ) {
                ImageAsset(parcel.imageResource(), Modifier.padding(5.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(localizedParcelTitle(parcel), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(localizedSourceLabel(parcel.sourcePackage, parcel.sourceLabel), color = Muted, fontSize = 12.sp)
                parcel.pickupCode?.let {
                    Text(tr("取件码 $it", "Pickup $it"), fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
            }
            StatusPill(parcel.status)
        }
    }
}

@Composable
private fun ParcelCollectionScreen(
    parcels: List<ParcelItem>,
    initialFilter: ParcelFilter,
    onSelectParcel: (ParcelItem) -> Unit
) {
    var filter by remember(initialFilter) { mutableStateOf(initialFilter) }
    val visible = parcels.filter { it.matches(filter) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                if (initialFilter == ParcelFilter.PICKUP) {
                    tr("待取\n包裹", "Pickup\nparcels")
                } else {
                    tr("全部\n包裹", "All\nparcels")
                },
                fontSize = 35.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp
            )
        }
        item { FilterRow(selected = filter, onSelect = { filter = it }) }

        if (visible.isEmpty()) {
            item { CollectionEmptyState(isPickup = initialFilter == ParcelFilter.PICKUP) }
        } else {
            items(visible, key = { it.id }) { parcel ->
                CollectionCard(parcel = parcel, onClick = { onSelectParcel(parcel) })
            }
        }
    }
}

@Composable
private fun CollectionCard(parcel: ParcelItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(158.dp).animateContentSize(),
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(Modifier.fillMaxSize().background(parcel.cardBrush())) {
            Column(
                modifier = Modifier.fillMaxWidth(0.61f).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(localizedParcelTitle(parcel), fontSize = 17.sp, fontWeight = FontWeight.Black, maxLines = 2)
                Text(
                    "${localizedSourceLabel(parcel.sourcePackage, parcel.sourceLabel)} · ${parcel.status.localizedLabel()}",
                    color = Muted,
                    fontSize = 12.sp
                )
                parcel.pickupCode?.let {
                    Text(tr("取件码 $it", "Pickup $it"), fontSize = 14.sp, fontWeight = FontWeight.Black)
                }
            }
            ImageAsset(
                drawable = parcel.imageResource(),
                modifier = Modifier.align(Alignment.BottomEnd).size(145.dp)
            )
        }
    }
}

@Composable
private fun CollectionEmptyState(isPickup: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(330.dp),
        shape = RoundedCornerShape(30.dp),
        color = if (isPickup) Color(0xFFEAF2FF) else Color(0xFFFFF0D9)
    ) {
        Box {
            Column(Modifier.padding(22.dp)) {
                Text(
                    if (isPickup) {
                        tr("暂时没有\n待取包裹", "No parcels\nto pick up")
                    } else {
                        tr("这里还很安静", "All quiet here")
                    },
                    fontSize = 27.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    tr("收到相关通知后会自动出现", "Matching notifications will appear automatically"),
                    color = Muted,
                    fontSize = 13.sp
                )
            }
            ImageAsset(
                R.drawable.parcel_locker,
                Modifier.align(Alignment.BottomEnd).size(225.dp)
            )
        }
    }
}

@Composable
private fun CompactEmptyState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.White
    ) {
        Text(
            tr(
                "收到购物或物流通知后，包裹会自动出现在这里。",
                "Parcels will appear here when shopping or delivery notifications arrive."
            ),
            modifier = Modifier.padding(20.dp),
            color = Muted,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun SettingsScreen(
    hasNotificationAccess: Boolean,
    sources: List<SourceChoice>,
    retention: RetentionPolicy,
    language: AppLanguage,
    onOpenNotificationSettings: () -> Unit,
    onToggleSource: (String, Boolean) -> Unit,
    onSetRetention: (RetentionPolicy) -> Unit,
    onSetLanguage: (AppLanguage) -> Unit,
    onAddDemoData: () -> Unit,
    onDeleteAll: () -> Unit,
    onShowOnboarding: () -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(tr("本地\n设置", "Local\nsettings"), fontSize = 35.sp, lineHeight = 38.sp, fontWeight = FontWeight.Black)
        }
        item {
            SettingsSection(title = tr("语言", "Language")) {
                AppLanguage.entries.forEach { option ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onSetLanguage(option) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = language == option, onClick = { onSetLanguage(option) })
                        Text(
                            when (option) {
                                AppLanguage.CHINESE -> tr("中文", "Chinese")
                                AppLanguage.ENGLISH -> "English"
                            },
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
        item {
            SettingsSection(title = tr("自动整理", "Auto sorting")) {
                SettingActionRow(
                    title = tr("通知访问", "Notification access"),
                    subtitle = if (hasNotificationAccess) {
                        tr("已开启，只处理选中的来源", "On — selected sources only")
                    } else {
                        tr("尚未开启", "Not enabled")
                    },
                    action = if (hasNotificationAccess) tr("已开启", "Enabled") else tr("去开启", "Open"),
                    onClick = onOpenNotificationSettings
                )
                HorizontalDivider(color = Color(0xFFF0EDF1))
                sources.forEach { choice ->
                    SettingSwitchRow(
                        title = localizedSourceLabel(choice.source.packageName, choice.source.label),
                        checked = choice.enabled,
                        onCheckedChange = { onToggleSource(choice.source.packageName, it) }
                    )
                }
            }
        }

        item {
            SettingsSection(title = tr("自动清理", "Auto cleanup")) {
                Text(
                    tr("只清理已取件或已归档的记录", "Only removes picked-up or archived records"),
                    color = Muted,
                    fontSize = 12.sp
                )
                RetentionPolicy.entries.forEach { policy ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onSetRetention(policy) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = retention == policy, onClick = { onSetRetention(policy) })
                        Text(policy.localizedLabel(), fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        item {
            SettingsSection(title = tr("测试与数据", "Testing & data")) {
                TextButton(onClick = onAddDemoData) { Text(tr("添加三条演示包裹", "Add three demo parcels")) }
                TextButton(onClick = onShowOnboarding) { Text(tr("重新查看欢迎页", "Show welcome screen again")) }
                TextButton(onClick = { confirmDelete = true }) {
                    Text(tr("删除全部本地数据", "Delete all local data"), color = MaterialTheme.colorScheme.error)
                }
            }
        }

        item {
            Text(
                tr(
                    "Parcelume 不申请网络权限。原始通知只在本机内存中解析，不写入数据库。",
                    "Parcelume has no network permission. Raw notifications are parsed in memory and are never stored."
                ),
                color = Muted,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(tr("删除全部数据？", "Delete all data?"), fontWeight = FontWeight.Black) },
            text = {
                Text(tr("所有本地包裹记录将永久删除，无法恢复。", "All local parcel records will be permanently deleted."))
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteAll()
                    confirmDelete = false
                }) { Text(tr("删除", "Delete"), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(tr("取消", "Cancel")) }
            }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Black)
            content()
        }
    }
}

@Composable
private fun SettingActionRow(
    title: String,
    subtitle: String,
    action: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Muted, fontSize = 12.sp)
        }
        Surface(shape = RoundedCornerShape(18.dp), color = Ink) {
            Text(action, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
        }
    }
}

@Composable
private fun SettingSwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, fontWeight = FontWeight.Medium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ParcelDetailDialog(
    parcel: ParcelItem,
    onMarkCompleted: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(30.dp),
        title = { Text(localizedParcelTitle(parcel), fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = parcel.cardColor()
                ) {
                    ImageAsset(parcel.imageResource(), Modifier.padding(6.dp))
                }
                Text(tr("来源：${localizedSourceLabel(parcel.sourcePackage, parcel.sourceLabel)}", "Source: ${localizedSourceLabel(parcel.sourcePackage, parcel.sourceLabel)}"))
                Text(tr("状态：${parcel.status.localizedLabel()}", "Status: ${parcel.status.localizedLabel()}"))
                parcel.trackingNumber?.let { Text(tr("运单号：$it", "Tracking: $it")) }
                parcel.pickupCode?.let { Text(tr("取件码：$it", "Pickup code: $it"), fontWeight = FontWeight.Black) }
                Text(
                    tr("更新于 ${parcel.localizedDate()}", "Updated ${parcel.localizedDate()}"),
                    color = Muted,
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            if (parcel.status == ParcelStatus.COMPLETED) {
                TextButton(onClick = onDismiss) { Text(tr("关闭", "Close")) }
            } else {
                Button(
                    onClick = onMarkCompleted,
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Ink)
                ) { Text(tr("标记为已取件", "Mark picked up")) }
            }
        },
        dismissButton = {
            if (parcel.status != ParcelStatus.COMPLETED) {
                TextButton(onClick = onDismiss) { Text(tr("关闭", "Close")) }
            }
        }
    )
}

@Composable
private fun StatusPill(status: ParcelStatus) {
    val color = when (status) {
        ParcelStatus.READY_FOR_PICKUP -> Color(0xFF6E4A91)
        ParcelStatus.EXCEPTION -> MaterialTheme.colorScheme.error
        ParcelStatus.COMPLETED -> Muted
        else -> Ink
    }
    Text(status.localizedLabel(), color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun ImageAsset(@DrawableRes drawable: Int, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Image(
        painter = painterResource(drawable),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}

@Composable
private fun BlackDot(modifier: Modifier = Modifier, text: String) {
    Surface(modifier = modifier.size(34.dp), shape = CircleShape, color = Ink) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, color = Color.White, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun Destination.localizedLabel(): String = when (this) {
    Destination.HOME -> tr("首页", "Home")
    Destination.PARCELS -> tr("包裹", "Parcels")
    Destination.PICKUP -> tr("待取", "Pickup")
    Destination.SETTINGS -> tr("设置", "Settings")
}

@Composable
private fun ParcelFilter.localizedLabel(): String = when (this) {
    ParcelFilter.ALL -> tr("全部", "All")
    ParcelFilter.ACTIVE -> tr("运输中", "Transit")
    ParcelFilter.PICKUP -> tr("待取件", "Pickup")
    ParcelFilter.EXCEPTION -> tr("异常", "Issues")
    ParcelFilter.COMPLETED -> tr("已完成", "Done")
}

@Composable
private fun ParcelStatus.localizedLabel(): String = when (this) {
    ParcelStatus.PENDING -> tr("待发货", "Pending")
    ParcelStatus.SHIPPED -> tr("已发货", "Shipped")
    ParcelStatus.IN_TRANSIT -> tr("运输中", "In transit")
    ParcelStatus.OUT_FOR_DELIVERY -> tr("派送中", "Out for delivery")
    ParcelStatus.READY_FOR_PICKUP -> tr("待取件", "Ready for pickup")
    ParcelStatus.DELIVERED -> tr("已送达", "Delivered")
    ParcelStatus.COMPLETED -> tr("已取件", "Picked up")
    ParcelStatus.EXCEPTION -> tr("异常", "Issue")
}

@Composable
private fun RetentionPolicy.localizedLabel(): String = when (this) {
    RetentionPolicy.ONE_WEEK -> tr("一周", "One week")
    RetentionPolicy.ONE_MONTH -> tr("一个月", "One month")
    RetentionPolicy.ONE_YEAR -> tr("一年", "One year")
    RetentionPolicy.FOREVER -> tr("永久保留", "Keep forever")
}

@Composable
private fun localizedSourceLabel(packageName: String, fallback: String): String = when (packageName) {
    "com.taobao.taobao", "demo.taobao" -> tr("淘宝", "Taobao")
    "com.jingdong.app.mall", "demo.jd" -> tr("京东", "JD")
    "com.xunmeng.pinduoduo" -> tr("拼多多", "Pinduoduo")
    "com.cainiao.wireless", "demo.cainiao" -> tr("菜鸟", "Cainiao")
    "com.sf.activity" -> tr("顺丰", "SF Express")
    "com.amazon.mShop.android.shopping" -> "Amazon"
    else -> fallback
}

@Composable
private fun localizedParcelTitle(parcel: ParcelItem): String = when (parcel.trackingNumber) {
    "DEMO10000001" -> tr("日常用品包裹", "Daily essentials")
    "DEMO10000002" -> tr("数码配件", "Tech accessories")
    "DEMO10000003" -> tr("家居用品", "Home goods")
    else -> parcel.title
}

@Composable
private fun ParcelItem.localizedDate(): String = if (LocalAppLanguage.current == AppLanguage.CHINESE) {
    SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.CHINA).format(Date(updatedAt))
} else {
    SimpleDateFormat("MMM d, yyyy HH:mm", Locale.ENGLISH).format(Date(updatedAt))
}

private fun ParcelItem.matches(filter: ParcelFilter): Boolean = when (filter) {
    ParcelFilter.ALL -> true
    ParcelFilter.ACTIVE -> status in setOf(
        ParcelStatus.PENDING,
        ParcelStatus.SHIPPED,
        ParcelStatus.IN_TRANSIT,
        ParcelStatus.OUT_FOR_DELIVERY,
        ParcelStatus.DELIVERED
    )
    ParcelFilter.PICKUP -> status == ParcelStatus.READY_FOR_PICKUP
    ParcelFilter.EXCEPTION -> status == ParcelStatus.EXCEPTION
    ParcelFilter.COMPLETED -> status == ParcelStatus.COMPLETED
}

@DrawableRes
private fun ParcelItem.imageResource(): Int = when (status) {
    ParcelStatus.READY_FOR_PICKUP -> R.drawable.parcel_locker
    ParcelStatus.OUT_FOR_DELIVERY, ParcelStatus.SHIPPED -> R.drawable.parcel_blue
    ParcelStatus.IN_TRANSIT, ParcelStatus.DELIVERED -> R.drawable.parcel_yellow
    else -> R.drawable.parcel_purple
}

private fun ParcelItem.cardColor(): Color = when (status) {
    ParcelStatus.READY_FOR_PICKUP -> Color(0xFFEAF2FF)
    ParcelStatus.OUT_FOR_DELIVERY, ParcelStatus.SHIPPED -> Color(0xFFFFE7DE)
    ParcelStatus.IN_TRANSIT, ParcelStatus.DELIVERED -> Color(0xFFE8F3E4)
    ParcelStatus.EXCEPTION -> Color(0xFFFFE3DE)
    ParcelStatus.COMPLETED -> Color(0xFFF0EEEB)
    else -> Color(0xFFFFF0C9)
}

private fun ParcelItem?.cardBrush(): Brush {
    val colors = when (this?.status) {
        ParcelStatus.READY_FOR_PICKUP -> listOf(Color(0xFFEAF2FF), Color(0xFFDDE8FF))
        ParcelStatus.OUT_FOR_DELIVERY, ParcelStatus.SHIPPED -> listOf(Color(0xFFFFE7DE), Color(0xFFFFF3EC))
        ParcelStatus.IN_TRANSIT, ParcelStatus.DELIVERED -> listOf(Color(0xFFE8F3E4), Color(0xFFFFF0D4))
        ParcelStatus.EXCEPTION -> listOf(Color(0xFFFFE3DE), Color(0xFFFFF3EF))
        ParcelStatus.COMPLETED -> listOf(Color(0xFFF0EEEB), Color(0xFFF7F5F2))
        else -> listOf(Color(0xFFFFF0C9), Color(0xFFE6F3ED))
    }
    return Brush.linearGradient(colors)
}
