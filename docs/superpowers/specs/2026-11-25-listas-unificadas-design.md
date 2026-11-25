# Listas de canciones con las mismas funciones

## Intención
Cualquier lista de canciones se comporta como la pestaña Canciones: lo que se puede hacer en una, se puede en todas.

## Alcance
Canciones, detalle de playlist, búsqueda, detalle de álbum (también géneros), detalle de artista y listas automáticas (Favoritas, Recientes, Más escuchadas).

Fuera: Duplicados y la cola de reproducción (otro propósito). "Tirar para actualizar" sigue solo en Canciones.

## Selección múltiple (en las 6)
- Mantener presionado inicia la selección; tocar marca o desmarca; Atrás o ✕ la cancela.
- Barra: cantidad, seleccionar todas, eliminar del teléfono, reproducir a continuación, agregar a la cola, agregar a playlist, editar artista/álbum/género (con separar del título y restaurar).
- En selección se oculta el ⋮ de cada fila.
- Solo playlist: además "Quitar de la playlist". Se selecciona por posición, porque una canción puede repetirse.
- Búsqueda: solo canciones, no álbumes ni artistas. Al cambiar el texto se cancela la selección.

## Ordenar (en las 6)
- Mismo botón que en Canciones: título, artista, álbum, duración; tocar el activo invierte el sentido.
- Las otras 5 suman "Orden original" (por defecto): el de la playlist, las pistas, lo reciente, lo más escuchado o los resultados.
- Reproducir, aleatorio y tocar una canción usan el orden visible.
- Se recuerda por lista (cada playlist, álbum, artista y lista automática; una sola preferencia para búsqueda). Canciones conserva la suya, sin "Orden original".
- Ordenar nunca reescribe la playlist.
- En álbum, el número de pista acompaña a la canción aunque cambie el orden.

## Enfoques considerados
1. **Componente compartido** (estado de selección, barra, diálogos, botón de orden y filas) que cada pantalla usa dentro de su propio diseño. Elegido: una sola implementación y cada pantalla conserva su cabecera.
2. Copiar el código en cada pantalla: rápido al inicio, pero las listas volverían a desalinearse.
3. Una única pantalla genérica de lista: las cabeceras son muy distintas y obligaría a reescribirlas.

## Decisiones de construcción (mías)
- La lógica de selección y orden, sin Compose, con tests unitarios.
- La preferencia de orden por lista se guarda en los ajustes de la app; no entra en el respaldo.
- Base: rama `generos-explorar-menu` (commit db19887).
