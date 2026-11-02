package com.alsane.pro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

private val Navy = Color(0xFF101B2D)
private val Gold = Color(0xFFD5AD55)
private val Canvas = Color(0xFFF4F6FA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MohasabiApp(BusinessDatabase.get(this).productDao()) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MohasabiApp(dao: ProductDao) {
    val products by dao.observeAll().collectAsStateWithLifecycle(emptyList())
    val count by dao.observeCount().collectAsStateWithLifecycle(0)
    val stock by dao.observeStockCostValue().collectAsStateWithLifecycle(0L)
    val lowStock by dao.observeLowStockCount().collectAsStateWithLifecycle(0)
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Product?>(null) }
    var adding by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val filtered = products.filter {
        it.name.contains(query.trim(), ignoreCase = true) || it.code.contains(query.trim(), ignoreCase = true)
    }

    MaterialTheme(colorScheme = lightColorScheme(primary = Navy, secondary = Gold, background = Canvas, surface = Color.White)) {
        Scaffold(
            containerColor = Canvas,
            topBar = {
                TopAppBar(title = {
                    Column {
                        Text("محاسبي", fontWeight = FontWeight.Bold)
                        Text("إدارة المتجر والمخزون", style = MaterialTheme.typography.labelSmall, color = Gold)
                    }
                }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy, titleContentColor = Color.White))
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { editing = null; adding = true }, containerColor = Gold, contentColor = Navy) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة منتج")
                }
            }
        ) { inset ->
            LazyColumn(
                Modifier.fillMaxSize().padding(inset).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 18.dp, bottom = 96.dp)
            ) {
                item {
                    Text("لوحة التحكم", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Navy)
                    Text("ملخص المخزون المسجل على هذا الجهاز", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricCard("عدد المنتجات", count.toString(), Modifier.weight(1f), Icons.Default.Inventory2)
                        MetricCard("تكلفة المخزون", "$stock ر.ي", Modifier.weight(1f), null)
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = if (lowStock > 0) Color(0xFFFFF3DD) else Color(0xFFE8F5ED))) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (lowStock > 0) "تنبيه مخزون منخفض: $lowStock منتج" else "المخزون جيد ولا توجد تنبيهات", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                item { Text("المنتجات والمخزون", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Navy) }
                item {
                    OutlinedTextField(
                        value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("ابحث بالاسم أو الباركود") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true, shape = RoundedCornerShape(14.dp)
                    )
                }
                if (filtered.isEmpty()) item {
                    Card {
                        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (query.isBlank()) "لا توجد منتجات مسجلة بعد" else "لا توجد نتائج مطابقة")
                            Text("اضغط + لإضافة منتجك الأول", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                items(filtered, key = { it.id }) { p ->
                    Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(p.name, fontWeight = FontWeight.Bold, color = Navy)
                                    Text("الرمز: " + p.code.ifBlank { "غير محدد" }, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                Text("الكمية: ${p.quantity} ${p.unit}", color = if (p.quantity <= p.lowStockLimit) Color(0xFFB3261E) else Color(0xFF247A4B), fontWeight = FontWeight.SemiBold)
                            }
                            HorizontalDivider(color = Color(0xFFE9ECF1))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("شراء: ${p.purchasePrice} ر.ي", style = MaterialTheme.typography.bodySmall)
                                    Text("بيع: ${p.salePrice} ر.ي", style = MaterialTheme.typography.bodySmall)
                                }
                                TextButton(onClick = { editing = p; adding = true }) { Text("تعديل") }
                                TextButton(onClick = { scope.launch { dao.deleteById(p.id) } }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
            }
        }
        if (adding) ProductDialog(existing = editing, onClose = { adding = false; editing = null }, onSave = { product ->
            scope.launch {
                val duplicate = if (product.code.isBlank()) null else dao.findByCode(product.code)
                if (duplicate != null && duplicate.id != product.id) {
                    // Keep the editor open; duplicate identifiers must not be saved.
                } else {
                    if (product.id == 0L) dao.insert(product) else dao.update(product)
                    adding = false
                    editing = null
                }
            }
        })
    }
}

@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier = Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector?) {
    Card(modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Navy)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (icon != null) Icon(icon, contentDescription = null, tint = Gold)
            Text(title, color = Color.LightGray, style = MaterialTheme.typography.labelMedium)
            Text(value, color = Gold, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun ProductDialog(existing: Product?, onClose: () -> Unit, onSave: (Product) -> Unit) {
    var name by remember(existing?.id) { mutableStateOf(existing?.name ?: "") }
    var code by remember(existing?.id) { mutableStateOf(existing?.code ?: "") }
    var buy by remember(existing?.id) { mutableStateOf(existing?.purchasePrice?.toString() ?: "") }
    var sell by remember(existing?.id) { mutableStateOf(existing?.salePrice?.toString() ?: "") }
    var qty by remember(existing?.id) { mutableStateOf(existing?.quantity?.toString() ?: "") }
    var low by remember(existing?.id) { mutableStateOf(existing?.lowStockLimit?.toString() ?: "5") }
    var unit by remember(existing?.id) { mutableStateOf(existing?.unit ?: "قطعة") }
    var error by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(if (existing == null) "إضافة منتج" else "تعديل المنتج") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("اسم المنتج *") }, singleLine = true)
                OutlinedTextField(code, { code = it }, label = { Text("الكود / الباركود") }, singleLine = true)
                OutlinedTextField(buy, { buy = it.filter(Char::isDigit) }, label = { Text("سعر الشراء (ريال)") }, singleLine = true)
                OutlinedTextField(sell, { sell = it.filter(Char::isDigit) }, label = { Text("سعر البيع (ريال)") }, singleLine = true)
                OutlinedTextField(qty, { qty = it.filter(Char::isDigit) }, label = { Text("الكمية الحالية") }, singleLine = true)
                OutlinedTextField(low, { low = it.filter(Char::isDigit) }, label = { Text("حد التنبيه") }, singleLine = true)
                OutlinedTextField(unit, { unit = it }, label = { Text("الوحدة") }, singleLine = true)
                if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val b = buy.toLongOrNull(); val s = sell.toLongOrNull()
                val q = qty.toIntOrNull(); val l = low.toIntOrNull()
                error = when {
                    name.isBlank() -> "اسم المنتج مطلوب"
                    b == null || s == null || q == null || l == null -> "أكمل حقول الأرقام بقيم صحيحة"
                    b < 0 || s < 0 || q < 0 || l < 0 -> "لا يسمح بالقيم السالبة"
                    unit.isBlank() -> "أدخل وحدة القياس"
                    else -> ""
                }
                if (error.isEmpty()) onSave(Product(
                    id = existing?.id ?: 0, name = name.trim(), code = code.trim(),
                    purchasePrice = b!!, salePrice = s!!, quantity = q!!, lowStockLimit = l!!, unit = unit.trim()
                ))
            }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("إلغاء") } }
    )
}
