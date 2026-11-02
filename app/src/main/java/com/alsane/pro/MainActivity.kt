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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { StoreHome(BusinessDatabase.get(this).productDao()) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StoreHome(dao: ProductDao) {
    val products by dao.observeAll().collectAsStateWithLifecycle(emptyList())
    val count by dao.observeCount().collectAsStateWithLifecycle(0)
    val stock by dao.observeStockCostValue().collectAsStateWithLifecycle(0L)
    var adding by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    MaterialTheme(colorScheme = lightColorScheme(primary = Navy, secondary = Gold)) {
        Scaffold(
            topBar = { TopAppBar(title = { Column {
                Text("AL-SANE PRO", fontWeight = FontWeight.Bold)
                Text("السّناي برو • إدارة المتجر", style = MaterialTheme.typography.labelSmall, color = Gold)
            } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy, titleContentColor = Color.White)) },
            floatingActionButton = { FloatingActionButton(onClick = { adding = true }, containerColor = Gold) {
                Icon(Icons.Default.Add, contentDescription = "إضافة منتج")
            } }
        ) { inset ->
            LazyColumn(Modifier.fillMaxSize().padding(inset).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { Text("لوحة التحكم", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
                item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard("عدد المنتجات", count.toString(), Modifier.weight(1f))
                    MetricCard("تكلفة المخزون", "$stock ر.ي", Modifier.weight(1f))
                } }
                item { Text("المنتجات والمخزون", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                if (products.isEmpty()) item { Text("لا توجد منتجات بعد. استخدم زر + لإضافة أول منتج.") }
                items(products, key = { it.id }) { p ->
                    Card(shape = RoundedCornerShape(14.dp)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(p.name, fontWeight = FontWeight.Bold)
                                Text("كود: " + p.code.ifBlank { "—" })
                                Text("شراء: " + p.purchasePrice + " • بيع: " + p.salePrice + " ر.ي")
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("الكمية: " + p.quantity, color = if (p.quantity <= p.lowStockLimit) Color.Red else Navy)
                                TextButton(onClick = { scope.launch { dao.deleteById(p.id) } }) { Text("حذف") }
                            }
                        }
                    }
                }
            }
        }
        if (adding) ProductDialog(
            onClose = { adding = false },
            onSave = { product -> scope.launch { dao.insert(product); adding = false } }
        )
    }
}

@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Navy)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = Color.LightGray)
            Text(value, color = Gold, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun ProductDialog(onClose: () -> Unit, onSave: (Product) -> Unit) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var buy by remember { mutableStateOf("") }
    var sell by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("إضافة منتج") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("اسم المنتج") }, singleLine = true)
            OutlinedTextField(code, { code = it }, label = { Text("الكود / الباركود") }, singleLine = true)
            OutlinedTextField(buy, { buy = it.filter(Char::isDigit) }, label = { Text("سعر الشراء بالريال") }, singleLine = true)
            OutlinedTextField(sell, { sell = it.filter(Char::isDigit) }, label = { Text("سعر البيع بالريال") }, singleLine = true)
            OutlinedTextField(qty, { qty = it.filter(Char::isDigit) }, label = { Text("الكمية") }, singleLine = true)
            if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
        } },
        confirmButton = { TextButton(onClick = {
            val b = buy.toLongOrNull(); val s = sell.toLongOrNull(); val q = qty.toIntOrNull()
            error = when {
                name.isBlank() -> "أدخل اسم المنتج"
                b == null || s == null || q == null -> "أدخل أرقامًا صحيحة في السعر والكمية"
                b < 0 || s < 0 || q < 0 -> "لا يسمح بالقيم السالبة"
                else -> ""
            }
            if (error.isEmpty()) onSave(Product(name = name.trim(), code = code.trim(), purchasePrice = b!!, salePrice = s!!, quantity = q!!))
        }) { Text("حفظ") } },
        dismissButton = { TextButton(onClick = onClose) { Text("إلغاء") } }
    )
}
