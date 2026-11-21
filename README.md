<h1 align="center">Vitrola</h1>

<p align="center">
  <b>Tu música, en tu celular y nada más.</b><br>
  Reproductor de MP3 para Android sin anuncios, sin suscripción, sin cuenta y sin permiso de internet.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-0b1d2a?style=for-the-badge&logo=kotlin&logoColor=7fd8f0" alt="Kotlin">
  <img src="https://img.shields.io/badge/Jetpack%20Compose-0b1d2a?style=for-the-badge&logo=jetpackcompose&logoColor=7fd8f0" alt="Jetpack Compose">
  <img src="https://img.shields.io/badge/Media3-0b1d2a?style=for-the-badge&logo=android&logoColor=7fd8f0" alt="Media3">
  <img src="https://img.shields.io/badge/Android-8.0%2B-0b1d2a?style=for-the-badge&logo=android&logoColor=c9a45c" alt="Android 8.0+">
  <img src="https://img.shields.io/badge/licencia-GPL--3.0-0b1d2a?style=for-the-badge&logoColor=c9a45c" alt="GPL-3.0">
</p>

<!-- Capturas pendientes: docs/demo.gif y docs/capturas/*.png -->
<!-- <p align="center"><img src="docs/demo.gif" width="300" alt="Vitrola en acción"></p> -->

## Por qué existe

Casi todos los reproductores gratis te llenan de anuncios o te cobran una suscripción para cosas básicas, como un ecualizador o quitar un banner. Vitrola es lo contrario:

- **Sin anuncios y sin suscripción.** Todo está incluido.
- **Sin cuenta.** La abres y suena.
- **Privacidad por diseño.** La app no declara el permiso `INTERNET`, así que no tiene forma de mandar nada a ningún lado. Puedes revisarlo en [`AndroidManifest.xml`](app/src/main/AndroidManifest.xml).

## Funciones

- **Estilos de reproductor analógicos:** vinilo, tocadiscos con brazo, cassette, radio y CD, que giran con inercia.
- **Letras `.lrc` sincronizadas** y velocidad de reproducción ajustable.
- **Widget de inicio** con favoritos y barra de progreso, que toma el color de la portada.
- **Volumen por canción**, manual o nivelado automático.
- **Ecualizador** y temporizador para dormir.
- **Biblioteca completa:** álbumes, artistas, playlists con portada propia, favoritas, recientes y más escuchadas.
- **Cola editable** que se mantiene al cerrar la app.
- **Edición múltiple** de artista y álbum sin modificar tus archivos.
- **Detector de canciones duplicadas.**
- **Respaldo:** exporta e importa playlists, favoritas, historial y metadatos.
- **Configuración** para excluir carpetas, fijar una duración mínima (adiós audios de WhatsApp) y elegir el tema.

## Lo interesante por dentro

- **Media3 con `MediaSessionService`.** La reproducción sigue en segundo plano y se controla desde la notificación, la pantalla de bloqueo y el widget.
- **Rendimiento medido en Compose:** las barras de sonido van a 30 cuadros por segundo, la posición solo recompone la barra de progreso, y la cola se guarda solo cuando cambia.
- **Búsqueda sin tildes** y agrupación de álbumes y artistas, cubiertas con tests unitarios.

## Cómo correrlo

Necesitas Android Studio y un celular o emulador con Android 8.0 o superior.

```bash
git clone https://github.com/SebasZMDev/vitrola.git
cd vitrola
./gradlew installDebug
```

Para correr los tests:

```bash
./gradlew test
```

## Estado

En desarrollo activo (v0.1.0).

---

<p align="center">
  <sub>Hecho por <b>ZM</b> · sin anuncios, sin suscripción.</sub><br>
  <sub><i>"Arquitectura zero-trust con soberanía total del dato del usuario."</i> Traducción: no pide permiso de internet 😄</sub>
</p>
