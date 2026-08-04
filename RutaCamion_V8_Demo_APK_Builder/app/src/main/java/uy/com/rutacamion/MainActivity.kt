package uy.com.rutacamion

import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import android.os.Bundle
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import uy.com.rutacamion.ui.theme.RutaCamionTheme
import uy.com.rutacamion.work.AlertWorker

class MainActivity : ComponentActivity() {
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= 33) permissions += Manifest.permission.POST_NOTIFICATIONS
        permissionLauncher.launch(permissions.toTypedArray())

        WorkManager.getInstance(this).enqueue(OneTimeWorkRequestBuilder<AlertWorker>().build())
        setContent { RutaCamionTheme { RutaCamionApp() } }
    }
}

enum class Tab(val title: String) {
    MAPA("Mapa"), DESTINO("Destino"), REPORTAR("Reportar"), CAMION("Mi camión"), PERFIL("Perfil")
}

@Composable
fun RutaCamionApp() {
    val context = LocalContext.current
    val storage = remember { TruckStorage(context) }
    var truck by remember { mutableStateOf(storage.load()) }
    var tab by remember { mutableStateOf(Tab.MAPA) }
    var selectedDestination by remember { mutableStateOf(DemoRouteEngine.destinations().first()) }
    var assessments by remember { mutableStateOf(DemoRouteEngine.assess(selectedDestination, truck)) }
    var routeLoading by remember { mutableStateOf(false) }
    var routeMessage by remember { mutableStateOf("Demo listo") }
    val scope = rememberCoroutineScope()

    fun requestRealRoute() {
        val endpoints = DemoRouteEngine.endpoints(selectedDestination) ?: return
        routeLoading = true
        routeMessage = "Calculando ruta abierta…"
        scope.launch {
            ValhallaClient.route(endpoints.first, endpoints.second, truck)
                .onSuccess { live ->
                    assessments = listOf(RouteAssessment(live, true, emptyList()))
                    routeMessage = "Ruta calculada con Valhalla"
                }
                .onFailure {
                    assessments = DemoRouteEngine.assess(selectedDestination, truck)
                    routeMessage = "Modo demostración sin conexión"
                }
            routeLoading = false
        }
    }

    fun recalculate(destination: String = selectedDestination, profile: TruckProfile = truck) {
        selectedDestination = destination
        assessments = DemoRouteEngine.assess(destination, profile)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column { Text("RutaCamión", fontWeight = FontWeight.Bold); Text("Movilidad inteligente para carga", fontSize = 12.sp) } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF083B7A),
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = {
                            Icon(
                                when (item) {
                                    Tab.MAPA -> Icons.Default.Map
                                    Tab.DESTINO -> Icons.Default.Search
                                    Tab.REPORTAR -> Icons.Default.AddCircle
                                    Tab.CAMION -> Icons.Default.LocalShipping
                                    Tab.PERFIL -> Icons.Default.Person
                                },
                                contentDescription = item.title
                            )
                        },
                        label = { Text(item.title) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                Tab.MAPA -> MapScreen(selectedDestination, assessments, routeLoading, routeMessage, { requestRealRoute() }) { tab = Tab.DESTINO }
                Tab.DESTINO -> DestinationScreen(
                    selectedDestination = selectedDestination,
                    assessments = assessments,
                    onDestinationSelected = { recalculate(it) },
                    onOpenMap = { tab = Tab.MAPA }
                )
                Tab.REPORTAR -> ReportScreen()
                Tab.CAMION -> TruckScreen(
                    initialProfile = truck,
                    onSave = {
                        truck = it
                        storage.save(it)
                        recalculate(profile = it)
                    }
                )
                Tab.PERFIL -> ProfileScreen(truck)
            }
        }
    }
}

@Composable
fun MapScreen(
    destination: String,
    assessments: List<RouteAssessment>,
    routeLoading: Boolean,
    routeMessage: String,
    onCalculateRoute: () -> Unit,
    onChooseDestination: () -> Unit
) {
    val recommended = assessments.firstOrNull { it.isCompatible }
    Column(
        Modifier.fillMaxSize().background(Color(0xFFEAF1F8)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(22.dp)
        ) {
            Box(Modifier.fillMaxSize()) {
                UruguayOpenMap(recommended?.route, Modifier.fillMaxSize())
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(12.dp),
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 5.dp
                ) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) {
                        Text("Mapa abierto de Uruguay", fontWeight = FontWeight.Bold)
                        Text(destination, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Surface(
                    modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 4.dp
                ) {
                    Text(
                        "© OpenStreetMap · OpenFreeMap · MapLibre",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                if (routeLoading) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
                else Icon(Icons.Default.Route, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(routeMessage, fontWeight = FontWeight.Bold)
                    Text("OpenStreetMap + MapLibre + Valhalla", style = MaterialTheme.typography.bodySmall)
                }
                FilledTonalButton(onClick = onCalculateRoute, enabled = !routeLoading) { Text("CALCULAR") }
            }
        }
        if (recommended != null) {
            Card {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF16803A))
                        Spacer(Modifier.width(10.dp))
                        Text("Ruta compatible", fontWeight = FontWeight.Bold)
                    }
                    Text(recommended.route.name)
                    Text("${recommended.route.distanceKm} km · ${recommended.route.estimatedMinutes / 60} h ${recommended.route.estimatedMinutes % 60} min")
                }
            }
        } else {
            Card {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = Color(0xFFD32F2F))
                    Spacer(Modifier.width(12.dp))
                    Text("No hay una ruta compatible con el camión configurado.")
                }
            }
        }
        Button(onClick = onChooseDestination, Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Default.Navigation, null)
            Spacer(Modifier.width(8.dp))
            Text("ELEGIR DESTINO")
        }
    }
}

@Composable
private fun UruguayOpenMap(route: RouteOption?, modifier: Modifier = Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var mapController by remember { mutableStateOf<MapLibreMap?>(null) }
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(Bundle())
            getMapAsync { map ->
                mapController = map
                map.setStyle(OPEN_MAP_STYLE) { style ->
                    drawRoute(style, route)
                }
                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(-32.75, -56.05))
                    .zoom(5.2)
                    .build()
            }
        }
    }

    LaunchedEffect(route, mapController) {
        val map = mapController ?: return@LaunchedEffect
        map.getStyle { style -> drawRoute(style, route) }
        route?.geometry?.let { points ->
            if (points.isNotEmpty()) {
                val center = points[points.size / 2]
                map.animateCamera(
                    org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(
                        CameraPosition.Builder()
                            .target(LatLng(center.latitude, center.longitude))
                            .zoom(6.0)
                            .build()
                    ),
                    900
                )
            }
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    Box(modifier) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        Column(
            modifier = Modifier.align(Alignment.CenterEnd).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SmallFloatingActionButton(onClick = {
                mapController?.animateCamera(
                    org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(
                        CameraPosition.Builder().target(LatLng(-32.75, -56.05)).zoom(5.2).build()
                    )
                )
            }) { Icon(Icons.Default.Public, contentDescription = "Ver Uruguay") }

            SmallFloatingActionButton(onClick = {
                centerOnCurrentLocation(context, mapController)
            }) { Icon(Icons.Default.MyLocation, contentDescription = "Mi ubicación") }
        }
    }
}

private fun drawRoute(style: org.maplibre.android.maps.Style, route: RouteOption?) {
    listOf(ROUTE_LAYER_ID, ROUTE_POINTS_LAYER_ID).forEach { style.removeLayer(it) }
    listOf(ROUTE_SOURCE_ID, ROUTE_POINTS_SOURCE_ID).forEach { style.removeSource(it) }
    if (route == null || route.geometry.size < 2) return

    val linePoints = route.geometry.map { Point.fromLngLat(it.longitude, it.latitude) }
    style.addSource(GeoJsonSource(ROUTE_SOURCE_ID, Feature.fromGeometry(LineString.fromLngLats(linePoints))))
    style.addLayer(
        LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
            lineColor("#0B63CE"),
            lineWidth(6f)
        )
    )

    val endpointFeatures = listOf(linePoints.first(), linePoints.last()).map { Feature.fromGeometry(it) }
    style.addSource(GeoJsonSource(ROUTE_POINTS_SOURCE_ID, FeatureCollection.fromFeatures(endpointFeatures)))
    style.addLayer(
        CircleLayer(ROUTE_POINTS_LAYER_ID, ROUTE_POINTS_SOURCE_ID).withProperties(
            circleColor("#FFFFFF"),
            circleRadius(7f)
        )
    )
}

private fun centerOnCurrentLocation(context: android.content.Context, map: MapLibreMap?) {
    if (map == null) return
    val fineGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarseGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    if (!fineGranted && !coarseGranted) return

    val manager = context.getSystemService(LocationManager::class.java)
    val providers = manager.getProviders(true)
    val lastLocation: Location? = providers
        .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
        .maxByOrNull { it.time }

    lastLocation?.let { location ->
        map.animateCamera(
            org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(LatLng(location.latitude, location.longitude))
                    .zoom(13.5)
                    .build()
            ),
            900
        )
    }
}

private const val ROUTE_SOURCE_ID = "recommended-route-source"
private const val ROUTE_LAYER_ID = "recommended-route-layer"
private const val ROUTE_POINTS_SOURCE_ID = "route-endpoints-source"
private const val ROUTE_POINTS_LAYER_ID = "route-endpoints-layer"

private const val OPEN_MAP_STYLE = "https://tiles.openfreemap.org/styles/liberty"

@Composable
fun DestinationScreen(
    selectedDestination: String,
    assessments: List<RouteAssessment>,
    onDestinationSelected: (String) -> Unit,
    onOpenMap: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Planificar ruta", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("Elige un recorrido de demostración. Luego incorporaremos destinos y datos reales de Uruguay.")
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selectedDestination, modifier = Modifier.weight(1f))
                Icon(Icons.Default.ArrowDropDown, null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DemoRouteEngine.destinations().forEach { destination ->
                    DropdownMenuItem(
                        text = { Text(destination) },
                        onClick = {
                            expanded = false
                            onDestinationSelected(destination)
                        }
                    )
                }
            }
        }
        assessments.forEach { assessment -> RouteCard(assessment) }
        Button(onClick = onOpenMap, modifier = Modifier.fillMaxWidth()) {
            Text("VER RUTA RECOMENDADA")
        }
        Text(
            "Aviso: las restricciones actuales son datos de demostración y no deben utilizarse para conducir.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun RouteCard(assessment: RouteAssessment) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (assessment.isCompatible) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    null,
                    tint = if (assessment.isCompatible) Color(0xFF16803A) else Color(0xFFC62828)
                )
                Spacer(Modifier.width(10.dp))
                Text(assessment.route.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
            Text("${assessment.route.distanceKm} km · ${assessment.route.estimatedMinutes} minutos")
            if (assessment.isCompatible) {
                Text("Compatible con el vehículo configurado.", color = Color(0xFF16803A))
            } else {
                assessment.warnings.forEach { warning -> Text("• $warning", color = Color(0xFFC62828)) }
            }
        }
    }
}

@Composable
fun ReportScreen() {
    var selected by remember { mutableStateOf("Calle bloqueada") }
    var details by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Reportar incidente", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        listOf("Calle bloqueada", "Obra", "Accidente", "Restricción de peso", "Puente bajo", "Congestión").forEach { item ->
            FilterChip(selected = selected == item, onClick = { selected = item }, label = { Text(item) })
        }
        OutlinedTextField(
            value = details,
            onValueChange = { details = it },
            label = { Text("Detalles") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
        Button(onClick = { sent = true }, Modifier.fillMaxWidth()) { Text("GUARDAR REPORTE DE PRUEBA") }
        if (sent) Text("Reporte guardado localmente como demostración.", color = Color(0xFF16803A))
    }
}

@Composable
fun TruckScreen(initialProfile: TruckProfile, onSave: (TruckProfile) -> Unit) {
    var weight by remember(initialProfile) { mutableStateOf(toInput(initialProfile.weightTons)) }
    var height by remember(initialProfile) { mutableStateOf(toInput(initialProfile.heightMeters)) }
    var width by remember(initialProfile) { mutableStateOf(toInput(initialProfile.widthMeters)) }
    var length by remember(initialProfile) { mutableStateOf(toInput(initialProfile.lengthMeters)) }
    var message by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Mi camión", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("RutaCamión comparará estas medidas con cada restricción vial.")
        NumberField(weight, { weight = it }, "Peso total (toneladas)")
        NumberField(height, { height = it }, "Altura (metros)")
        NumberField(width, { width = it }, "Ancho (metros)")
        NumberField(length, { length = it }, "Largo (metros)")
        Button(
            onClick = {
                val profile = TruckProfile(
                    weightTons = parseNumber(weight),
                    heightMeters = parseNumber(height),
                    widthMeters = parseNumber(width),
                    lengthMeters = parseNumber(length)
                )
                if (listOf(profile.weightTons, profile.heightMeters, profile.widthMeters, profile.lengthMeters).all { it > 0 }) {
                    onSave(profile)
                    message = "Vehículo guardado y rutas recalculadas."
                } else {
                    message = "Revisa los valores: todos deben ser mayores que cero."
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("GUARDAR VEHÍCULO") }
        message?.let { Text(it, color = if (it.startsWith("Vehículo")) Color(0xFF16803A) else MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun NumberField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true
    )
}

@Composable
fun ProfileScreen(truck: TruckProfile) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Person, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("Perfil", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("Conductor invitado")
        Text("País: Uruguay")
        Text("Alertas en segundo plano: activadas")
        Spacer(Modifier.height(16.dp))
        Text("Camión: ${toInput(truck.weightTons)} t · ${toInput(truck.heightMeters)} m de altura")
    }
}

private fun parseNumber(value: String): Double = value.replace(',', '.').toDoubleOrNull() ?: 0.0
private fun toInput(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString().replace('.', ',')
