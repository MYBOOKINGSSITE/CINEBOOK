
package com.example.wallpaperapp

import android.app.WallpaperManager
import android.content.Context
import android.graphics.*
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as JColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult

import com.android.billingclient.api.*
import com.google.android.gms.ads.*
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File

data class Item(
    val url: String? = null,
    val category: String = "All",
    val text: String? = null,
    val author: String? = null,
    val bg: String? = null,
    val fg: String? = null,
    val lang: String? = null,
    val created: Long = 0L,
    val trend: Int = 0
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this) {}
        setContent {
            MaterialTheme {
                val nav = rememberNavController()
                val vm = remember { AppVM(this) }
                LaunchedEffect(Unit) { vm.start() }
                AppNav(nav, vm)
            }
        }
    }
}

@Composable
fun AppNav(nav: NavHostController, vm: AppVM) {
    NavHost(navController = nav, startDestination = "home") {
        composable("home") { HomeScreen(nav, vm) }
        composable("paywall") { PaywallScreen(vm, onClose = { nav.popBackStack() }) }
    }
}

@Composable
fun CategoryChips(categories: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        categories.forEach { cat -> AssistChip(onClick = { onSelect(cat) }, label = { Text(cat) }, modifier = Modifier.padding(end = 8.dp)) }
    }
}

@Composable
fun HomeScreen(nav: NavHostController, vm: AppVM) {
    val isPremium by vm.isPremium.collectAsState()
    val categories = listOf("All","Nature","Cars","Abstract","Quotes","Space","Anime")
    var selectedCat by remember { mutableStateOf("All") }
    var query by remember { mutableStateOf(TextFieldValue("")) }
    var tabIndex by remember { mutableStateOf(0) }
    var randomMode by remember { mutableStateOf(false) }

    val filtered = vm.items.filter { selectedCat == "All" || it.category == selectedCat }
        .filter { q -> query.text.isBlank() || (q.text ?: "").contains(query.text, true) || (q.url ?: "").contains(query.text, true) }

    val sorted = if (randomMode) filtered.shuffled()
    else if (tabIndex == 0) filtered.sortedByDescending { it.created }
    else filtered.sortedByDescending { it.trend }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Wallpapers") }, actions = {
                if (!isPremium) TextButton(onClick = { nav.navigate("paywall") }) { Text("Go Premium") }
            })
        },
        bottomBar = {
            if (!isPremium && vm.adFrequency > 0) {
                AndroidView(factory = { ctx -> AdView(ctx).apply { setAdSize(AdSize.BANNER); adUnitId = "ca-app-pub-3940256099942544/6300978111"; loadAd(AdRequest.Builder().build()) } }, modifier = Modifier.fillMaxWidth())
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.weight(1f), leadingIcon = { Icon(painterResource(id = android.R.drawable.ic_menu_search), contentDescription = null) }, placeholder = { Text("Search") })
                AssistChip(onClick = { randomMode = !randomMode }, label = { Text(if (randomMode) "Random" else if (tabIndex==0) "Newest" else "Trending") })
            }
            TabRow(tabIndex = tabIndex) {
                Tab(selected = tabIndex==0, onClick = { tabIndex = 0 }, text = { Text("Newest") })
                Tab(selected = tabIndex==1, onClick = { tabIndex = 1 }, text = { Text("Trending") })
            }
            CategoryChips(categories, selectedCat) { selectedCat = it }
            LazyVerticalGrid(
                columns = if (vm.newGridEnabled) GridCells.Adaptive(150.dp) else GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(sorted) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().aspectRatio(9f/16f).clickable {
                            vm.registerInteraction()
                            vm.applyItem(item, LocalContext.current)
                        }
                    ) {
                        if (item.text != null) {
                            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
                                Text(item.text, style = MaterialTheme.typography.titleMedium)
                                item.author?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
                            }
                        } else {
                            val ctx = LocalContext.current
                            if (item.url?.startsWith("asset://") == true) {
                                val name = item.url.removePrefix("asset://").removeSuffix(".png")
                                val id = ctx.resources.getIdentifier(name, "drawable", ctx.packageName)
                                Image(painter = painterResource(id = id), contentDescription = null, modifier = Modifier.fillMaxSize())
                            } else {
                                AsyncImage(model = item.url, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }

    // Smart paywall trigger
    LaunchedEffect(vm.triggerPaywall) {
        if (vm.triggerPaywall) {
            vm.consumeTrigger()
            nav.navigate("paywall")
        }
    }
}

@Composable
fun TableCell(text: String, weight: Float, bold: Boolean = false) {
    Text(
        text,
        modifier = Modifier
            .border(1.dp, JColor.LightGray)
            .weight(weight)
            .padding(8.dp),
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
    )
}

@Composable
fun FeatureTable() {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            TableCell("Feature", 2f, true)
            TableCell("Free", 1f, true)
            TableCell("Premium", 1f, true)
        }
        listOf(
            Triple("Ads", "Yes", "No"),
            Triple("Max quality", "1080p", "4K"),
            Triple("Early access", "—", "Yes"),
            Triple("Favorites backup", "—", "Yes")
        ).forEach { (f, free, pro) ->
            Row(Modifier.fillMaxWidth()) {
                TableCell(f, 2f)
                TableCell(free, 1f)
                TableCell(pro, 1f)
            }
        }
    }
}

@Composable
fun PaywallScreen(vm: AppVM, onClose: () -> Unit) {
    val isPremium by vm.isPremium.collectAsState()
    val prices by vm.priceUi.collectAsState()

    if (isPremium) {
        LaunchedEffect(Unit) { onClose() }
        return
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Go Premium") }, actions = { TextButton(onClick = onClose) { Text("Close") } })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Unlock the best experience", style = MaterialTheme.typography.headlineSmall)
                FeatureTable()
                if (prices.monthlyTrial.isNotEmpty() || prices.yearlyIntro.isNotEmpty()) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (prices.monthlyTrial.isNotEmpty()) Text("Free trial: ${prices.monthlyTrial}")
                            if (prices.yearlyIntro.isNotEmpty()) Text("Intro offer: ${prices.yearlyIntro}")
                        }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { vm.buyOneTime() }, modifier = Modifier.fillMaxWidth()) {
                    Text(prices.oneTime.ifEmpty { "One-time unlock" })
                }
                OutlinedButton(onClick = { vm.buyMonthly() }, modifier = Modifier.fillMaxWidth()) {
                    Text(prices.monthly.ifEmpty { "Monthly" })
                }
                OutlinedButton(onClick = { vm.buyYearly() }, modifier = Modifier.fillMaxWidth()) {
                    Text(prices.yearly.ifEmpty { "Yearly" })
                }
                Text("Restore purchases", modifier = Modifier.clickable { vm.restore() }.padding(8.dp))
            }
        }
    }
}

// -------- VM with Remote Config + Billing + Triggers --------
class AppVM(private val activity: ComponentActivity): PurchasesUpdatedListener {
    private val prefs = activity.getSharedPreferences("wallapp", Context.MODE_PRIVATE)

    private val _isPremium = mutableStateOf(false)
    val isPremium: State<Boolean> @Composable get() = _isPremium

    private val _priceUi = mutableStateOf(PriceUi())
    val priceUi: State<PriceUi> @Composable get() = _priceUi

    var adFrequency = 1
    var newGridEnabled = true

    // smart triggers
    private var interactionCount = prefs.getInt("interactions", 0)
    private var triggerThreshold = 6 // default
    var triggerPaywall by mutableStateOf(false); private set

    private lateinit var billing: BillingClient
    private var oneTime: ProductDetails? = null
    private var monthly: ProductDetails? = null
    private var yearly: ProductDetails? = null

    val items: List<Item> by lazy { loadItems() }

    fun start() {
        // Remote Config
        val rc = FirebaseRemoteConfig.getInstance()
        rc.setConfigSettingsAsync(remoteConfigSettings { minimumFetchIntervalInSeconds = 60 })
        rc.setDefaultsAsync(mapOf("newGridEnabled" to true, "adFrequency" to 1, "paywallTriggerThreshold" to 6))
        rc.fetchAndActivate().addOnCompleteListener {
            newGridEnabled = rc.getBoolean("newGridEnabled")
            adFrequency = rc.getLong("adFrequency").toInt()
            triggerThreshold = rc.getLong("paywallTriggerThreshold").toInt()
            checkTrigger()
        }

        // Billing
        billing = BillingClient.newBuilder(activity).enablePendingPurchases().setListener(this).build()
        billing.startConnection(object: BillingClientStateListener {
            override fun onBillingSetupFinished(p0: BillingResult) {
                queryProducts(); restore()
            }
            override fun onBillingServiceDisconnected() {}
        })
    }

    fun registerInteraction() {
        interactionCount += 1
        prefs.edit().putInt("interactions", interactionCount).apply()
        checkTrigger()
    }

    private fun checkTrigger() {
        if (!_isPremium.value && interactionCount >= triggerThreshold) {
            triggerPaywall = true
        }
    }
    fun consumeTrigger() { triggerPaywall = false; interactionCount = 0; prefs.edit().putInt("interactions", 0).apply() }

    private fun loadItems(): List<Item> {
        return try {
            val cache = File(activity.filesDir, "remote_cache.json")
            val text = if (cache.exists()) cache.readText()
            else activity.assets.open("remote_wallpapers.json").bufferedReader().readText()
            val arr = JSONObject(text).getJSONArray("wallpapers")
            val out = mutableListOf<Item>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(Item(
                    url = o.optString("url", null),
                    category = o.optString("category","All"),
                    text = o.optString("text", null),
                    author = o.optString("author", null),
                    bg = o.optString("bg", null),
                    fg = o.optString("fg", null),
                    lang = o.optString("lang", null),
                    created = o.optLong("created", 0L),
                    trend = o.optInt("trend", 0)
                ))
            }
            out
        } catch (_: Exception) { emptyList() }
    }

    private fun queryProducts() {
        val oneTimeId = "premium_unlock"
        val monthlyId = "premium_sub_month"
        val yearlyId = "premium_sub_year"

        billing.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder()
                .setProductList(listOf(QueryProductDetailsParams.Product.newBuilder().setProductId(oneTimeId).setProductType(BillingClient.ProductType.INAPP).build()))
                .build()
        ) { _, list ->
            oneTime = list.firstOrNull()
            updatePriceUi()
        }

        billing.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder()
                .setProductList(listOf(
                    QueryProductDetailsParams.Product.newBuilder().setProductId(monthlyId).setProductType(BillingClient.ProductType.SUBS).build(),
                    QueryProductDetailsParams.Product.newBuilder().setProductId(yearlyId).setProductType(BillingClient.ProductType.SUBS).build()
                )).build()
        ) { _, list ->
            monthly = list.find { it.productId == monthlyId }
            yearly  = list.find { it.productId == yearlyId }
            updatePriceUi()
        }
    }

    private fun updatePriceUi() {
        val monthlyPhase = monthly?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()
        val yearlyPhase  = yearly?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()

        val monthlyTrial = monthly?.subscriptionOfferDetails?.flatMap { it.pricingPhases.pricingPhaseList }?.find { it.priceAmountMicros == 0L }?.let {
            if (it.billingPeriod != null) "Try ${it.billingPeriod}" else "Free trial available"
        } ?: ""

        val yearlyIntro = yearly?.subscriptionOfferDetails?.flatMap { it.pricingPhases.pricingPhaseList }?.find { it.priceAmountMicros > 0 && it.billingPeriod != null }?.let {
            "Intro: ${it.formattedPrice} for ${it.billingPeriod}"
        } ?: ""

        _priceUi.value = PriceUi(
            oneTime = oneTime?.oneTimePurchaseOfferDetails?.formattedPrice ?: "",
            monthly = monthlyPhase?.formattedPrice ?: "",
            yearly = yearlyPhase?.formattedPrice ?: "",
            monthlyTrial = monthlyTrial,
            yearlyIntro = yearlyIntro
        )
    }

    fun buyOneTime() { launchPurchase(oneTime, null) }
    fun buyMonthly() { launchPurchase(monthly, monthly?.subscriptionOfferDetails?.firstOrNull()?.offerToken) }
    fun buyYearly() { launchPurchase(yearly, yearly?.subscriptionOfferDetails?.firstOrNull()?.offerToken) }

    private fun launchPurchase(pd: ProductDetails?, offerToken: String?) {
        if (pd == null) return
        val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(
            if (pd.productType == BillingClient.ProductType.SUBS)
                BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(pd).setOfferToken(offerToken ?: "").build()
            else
                BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(pd).build()
        )).build()
        billing.launchBillingFlow(activity, params)
    }

    fun restore() {
        billing.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()) { _, purchases ->
            _isPremium.value = purchases.any { it.products.contains("premium_unlock") && it.purchaseState == Purchase.PurchaseState.PURCHASED }
        }
        billing.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()) { _, purchases ->
            if (purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }) _isPremium.value = true
        }
    }

    override fun onPurchasesUpdated(res: BillingResult, purchases: MutableList<Purchase>?) {
        if (res.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            if (purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }) _isPremium.value = true
        }
    }

    fun applyItem(item: Item, ctx: Context) {
        val wm = WallpaperManager.getInstance(ctx)
        val bmp = if (item.text != null) buildQuoteBitmap(item) else getBitmapForItem(item, ctx)
        bmp?.let { wm.setBitmap(it) }
    }

    private fun getBitmapForItem(item: Item, ctx: Context): Bitmap? {
        return if (item.url?.startsWith("asset://") == true) {
            val name = item.url.removePrefix("asset://").removeSuffix(".png")
            val id = ctx.resources.getIdentifier(name, "drawable", ctx.packageName)
            val d = ctx.getDrawable(id)
            if (d is android.graphics.drawable.BitmapDrawable) d.bitmap else null
        } else {
            try {
                val loader = ImageLoader(ctx)
                val req = ImageRequest.Builder(ctx).data(item.url).allowHardware(false).build()
                val res = loader.execute(req)
                (res as? SuccessResult)?.drawable?.let { (it as android.graphics.drawable.BitmapDrawable).bitmap }
            } catch (e: Exception) { null }
        }
    }

    private fun buildQuoteBitmap(item: Item): Bitmap {
        val w = 1080; val h = 1920
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val bgColor = try { Color.parseColor(item.bg ?: "#111827") } catch (e: Exception) { Color.BLACK }
        val fgColor = try { Color.parseColor(item.fg ?: "#FFFFFF") } catch (e: Exception) { Color.WHITE }
        canvas.drawColor(bgColor)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fgColor; textSize = 56f }
        val authorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fgColor; textSize = 36f }
        val maxW = w - 160
        val lines = wrapText(item.text ?: "Quote", textPaint, maxW)
        var y = h/3f
        for (line in lines) { canvas.drawText(line, 80f, y, textPaint); y += 72f }
        item.author?.let { canvas.drawText(it, 80f, y + 40f, authorPaint) }
        return bmp
    }
    private fun wrapText(text: String, paint: Paint, maxWidth: Int): List<String> {
        val out = mutableListOf<String>(); var line = ""
        for (word in text.split(" ")) {
            val attempt = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(attempt) <= maxWidth) line = attempt else { if (line.isNotEmpty()) out.add(line); line = word }
        }
        if (line.isNotEmpty()) out.add(line); return out
    }
}

data class PriceUi(
    val oneTime: String = "",
    val monthly: String = "",
    val yearly: String = "",
    val monthlyTrial: String = "",
    val yearlyIntro: String = ""
)
