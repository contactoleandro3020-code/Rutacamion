package uy.com.rutacamion

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import uy.com.rutacamion.ui.theme.RutaCamionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RutaCamionTheme { RutaCamionDemo() } }
    }
}

enum class AppTab(val label: String) {
    MAPA("Mapa"), RUTA("Ruta"), REPORTAR("Reportar"), CAMION("Camión")
}

data class TruckProfile(
    val weight: String = "20",
    val height: String = "4.10",
    val width: String = "2.60",
    val length: String = "16.50"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RutaCamionDemo() {
    var tab by remember { mutableStateOf(AppTab.MAPA) }
    var destination by remember { mutableStateOf("Tacuarembó") }
    var truck by remember { mutableStateOf(TruckProfile()) }
    var routeCalculated by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("RutaCamión", fontWeight = FontWeight.Bold)
                        Text("Demo de movilidad para carga", fontSize = 12.sp)
                    }
                },
                navigationIcon = {
                    Icon(Icons.Default.LocalShipping, null, Modifier.padding(start = 14.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF083B7A),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                AppTab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = {
                            Icon(
                                when (item) {
                                    AppTab.MAPA -> Icons.Default.Map
                                    AppTab.RUTA -> Icons.Default.Route
                                    AppTab.REPORTAR -> Icons.Default.Report
                                    AppTab.CAMION -> Icons.Default.LocalShipping
                                },
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF3F7FC))
        ) {
            when (tab) {
                AppTab.MAPA -> MapScreen(
                    destination = destination,
                    routeCalculated = routeCalculated,
                    onCalculate = { routeCalculated = true },
                    onOpenRoute = { tab = AppTab.RUTA }
                )
                AppTab.RUTA -> RouteScreen(
                    destination = destination,
                    onDestinationChange = {
                        destination = it
                        routeCalculated = false
                    },
                    onCalculate = {
                        routeCalculated = true
                        tab = AppTab.MAPA
                    }
                )
                AppTab.REPORTAR -> ReportScreen()
                AppTab.CAMION -> TruckScreen(truck) { truck = it }
            }
        }
    }
}

@Composable
private fun MapScreen(
    destination: String,
    routeCalculated: Boolean,
    onCalculate: () -> Unit,
    onOpenRoute: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(22.dp)
        ) {
            Box(Modifier.fillMaxSize()) {
                OpenStreetMapView(Modifier.fillMaxSize())
                Surface(
                    Modifier.align(Alignment.TopCenter).padding(12.dp),
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 5.dp
                ) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) {
                        Text("Mapa abierto de Uruguay", fontWeight = FontWeight.Bold)
                        Text("Destino: $destination", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Surface(
                    Modifier.align(Alignment.BottomStart).padding(10.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        "© OpenStreetMap contributors",
                        Modifier.padding(7.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        Card(shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(15.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (routeCalculated) Icons.Default.CheckCircle else Icons.Default.Info,
                        null,
                        tint = if (routeCalculated) Color(0xFF188038) else MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (routeCalculated) "Ruta demo preparada" else "Listo para calcular",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (routeCalculated) "Montevideo → $destination · recorrido demostrativo" else "Configura el destino y tu camión",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onOpenRoute, Modifier.weight(1f)) {
                        Icon(Icons.Default.EditLocation, null)
                        Spacer(Modifier.width(6.dp))
                        Text("DESTINO")
                    }
                    Button(onClick = onCalculate, Modifier.weight(1f)) {
                        Icon(Icons.Default.Navigation, null)
                        Spacer(Modifier.width(6.dp))
                        Text("CALCULAR")
                    }
                }
            }
        }

        Surface(color = Color(0xFFFFF4CE), shape = RoundedCornerShape(14.dp)) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, null, tint = Color(0xFF8A5A00))
                Spacer(Modifier.width(9.dp))
                Text(
                    "Demo: no utilizar estas rutas para conducir todavía.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun OpenStreetMapView(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                loadUrl("https://www.openstreetmap.org/export/embed.html?bbox=-58.7%2C-35.2%2C-53.0%2C-30.0&layer=mapnik")
            }
        }
    )
}

@Composable
private fun RouteScreen(
    destination: String,
    onDestinationChange: (String) -> Unit,
    onCalculate: () -> Unit
) {
    val options = listOf("Tacuarembó", "Rivera", "Durazno", "Salto", "Paysandú")
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Planificar recorrido", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Origen", fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = "Montevideo",
            onValueChange = {},
            readOnly = true,
            leadingIcon = { Icon(Icons.Default.TripOrigin, null) },
            modifier = Modifier.fillMaxWidth()
        )
        Text("Destino", fontWeight = FontWeight.SemiBold)
        options.forEach { city ->
            Card(
                onClick = { onDestinationChange(city) },
                colors = CardDefaults.cardColors(
                    containerColor = if (destination == city) Color(0xFFE3F0FF) else Color.White
                )
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = destination == city, onClick = { onDestinationChange(city) })
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(city, fontWeight = FontWeight.Bold)
                        Text("Ruta nacional disponible en el demo", style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Default.ChevronRight, null)
                }
            }
        }
        Button(onClick = onCalculate, Modifier.fillMaxWidth().height(54.dp)) {
            Icon(Icons.Default.Route, null)
            Spacer(Modifier.width(8.dp))
            Text("CALCULAR RUTA DEMO")
        }
    }
}

@Composable
private fun ReportScreen() {
    var type by remember { mutableStateOf("Camión atascado") }
    var details by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }
    val types = listOf("Camión atascado", "Calle angosta", "Puente bajo", "Corte de ruta", "Accidente")

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Reportar problema", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Ayuda a otros transportistas a evitar zonas problemáticas.")
        types.forEach { item ->
            FilterChip(
                selected = type == item,
                onClick = { type = item },
                label = { Text(item) },
                leadingIcon = if (type == item) { { Icon(Icons.Default.Check, null) } } else null
            )
        }
        OutlinedTextField(
            value = details,
            onValueChange = { details = it },
            label = { Text("Detalles") },
            placeholder = { Text("Ej.: acceso bloqueado para vehículos largos") },
            modifier = Modifier.fillMaxWidth().height(130.dp)
        )
        Button(
            onClick = { sent = true },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Icon(Icons.Default.Send, null)
            Spacer(Modifier.width(8.dp))
            Text("ENVIAR REPORTE DEMO")
        }
        if (sent) {
            Surface(color = Color(0xFFE3F6E8), shape = RoundedCornerShape(14.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF188038))
                    Spacer(Modifier.width(9.dp))
                    Text("Reporte guardado en el demo: $type")
                }
            }
        }
    }
}

@Composable
private fun TruckScreen(profile: TruckProfile, onSave: (TruckProfile) -> Unit) {
    var weight by remember(profile) { mutableStateOf(profile.weight) }
    var height by remember(profile) { mutableStateOf(profile.height) }
    var width by remember(profile) { mutableStateOf(profile.width) }
    var length by remember(profile) { mutableStateOf(profile.length) }
    var saved by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Perfil del camión", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Estas medidas se usarán para evitar rutas incompatibles en futuras versiones.")
        NumberField("Peso total", weight, "t") { weight = it }
        NumberField("Altura", height, "m") { height = it }
        NumberField("Ancho", width, "m") { width = it }
        NumberField("Largo", length, "m") { length = it }
        Button(
            onClick = {
                onSave(TruckProfile(weight, height, width, length))
                saved = true
            },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Icon(Icons.Default.Save, null)
            Spacer(Modifier.width(8.dp))
            Text("GUARDAR PERFIL")
        }
        if (saved) Text("Perfil guardado durante esta sesión.", color = Color(0xFF188038))
    }
}

@Composable
private fun NumberField(label: String, value: String, suffix: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { new ->
            if (new.all { it.isDigit() || it == '.' || it == ',' }) onChange(new)
        },
        label = { Text(label) },
        trailingIcon = { Text(suffix, Modifier.padding(end = 12.dp), fontWeight = FontWeight.Bold) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}
