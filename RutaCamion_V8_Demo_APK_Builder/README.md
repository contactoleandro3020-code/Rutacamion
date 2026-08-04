# RutaCamión V8 — Demo de rutas abiertas

Prototipo Android con MapLibre, OpenStreetMap/OpenFreeMap y cálculo de rutas para camiones mediante Valhalla.

## Incluye
- Interfaz demo azul y blanca.
- Mapa real de Uruguay.
- Perfil del camión: peso, altura, ancho y largo.
- Solicitud de ruta con perfil `truck`.
- Trazado GeoJSON sobre MapLibre.
- Modo de respaldo con rutas locales si el servicio público no responde.

## Aviso
El endpoint público de Valhalla se usa únicamente para desarrollo. Para producción se debe desplegar una instancia propia, validar datos oficiales de restricciones y cumplir las políticas de los proveedores cartográficos. No usar este prototipo para conducción real.
