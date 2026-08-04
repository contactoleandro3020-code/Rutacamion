# RutaCamión — demo Android

Proyecto limpio para generar un APK de demostración mediante GitHub Actions.

## Importante al subirlo

Sube **los archivos y carpetas que están dentro de este ZIP directamente a la raíz del repositorio**. En la pantalla principal de GitHub deben verse juntos:

- `.github`
- `app`
- `build.gradle.kts`
- `gradle.properties`
- `settings.gradle.kts`

No subas una carpeta exterior llamada `RutaCamion_Demo_Limpio`.

## Generar el APK

1. Abre la pestaña **Actions / Comportamiento**.
2. Selecciona **Construir APK RutaCamion**.
3. Pulsa **Run workflow / Ejecutar flujo de trabajo**.
4. Cuando aparezca el visto verde, abre la ejecución.
5. En **Artifacts / Artefactos**, descarga `RutaCamion-Demo-APK`.
6. Descomprime el archivo descargado e instala `app-debug.apk`.

## Alcance

Este APK es una demostración visual. El mapa usa OpenStreetMap. Las rutas, restricciones y reportes todavía no están validados para conducción real.
