# Revisiones

| # | Fecha | Skill | Severidad | Qué pasa | Dónde | Estado |
|---|---|---|---|---|---|---|
| 1 | 2026-11-28 | rams-davinci | Importante | El vinilo casi no se veía (poco saliente, etiqueta tapada) | widget/WidgetArt.kt | arreglado 2026-11-28 |
| 2 | 2026-11-28 | rams-davinci | Importante | La carátula tenía ancho fijo y no crecía con el widget | res/layout/widget_player.xml | arreglado 2026-11-28 |
| 3 | 2026-11-28 | rams-davinci | Importante | "Sigue · …" con contraste bajo sobre fondos teñidos | res/layout/widget_player.xml (widget_next_up) | arreglado 2026-11-28 |
| 4 | 2026-11-28 | rams-davinci | Importante | En pausa, tras reinstalar se vio 00:24 con la barra vacía; causa probable: el Chronometer detenido se recalcula al redibujar (sin verificar) | widget/PlayerWidget.kt (bindProgress) | arreglado 2026-11-28 |
| 5 | 2026-11-28 | rams-davinci | Importante | Zonas táctiles bajo 48 dp: el corazón sigue en 32 de alto (era 36x28, ahora 48x32); anterior, siguiente y play ya miden 48 | res/layout/widget_player.xml | abierto |
| 6 | 2026-11-28 | rams-davinci | Menor | Títulos largos se cortan en una línea aunque sobre alto | res/layout/widget_player.xml (widget_title) | abierto |
| 7 | 2026-11-28 | rams-davinci | Menor | Mucho espacio vacío arriba y abajo en el tamaño alto del emulador | res/layout/widget_player.xml | abierto |
