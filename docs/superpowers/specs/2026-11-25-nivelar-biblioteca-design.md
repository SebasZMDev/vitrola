# Nivelar toda la biblioteca

## Intención
Que ninguna canción suene muy baja y la siguiente muy alta, sin ir una por una.

## Funcionamiento
- En Configuración: "Nivelar toda la biblioteca", con progreso ("120 de 480") y "Cancelar".
- Primero mide todas las canciones; luego toma como objetivo la **mediana** de sus volúmenes y calcula la ganancia de cada una (mismas reglas de hoy: ±6 dB, pasos de 0,5, tope según el pico).
- Las mediciones se guardan: si se cancela o se cierra la app, la próxima vez solo mide lo que falta. Las ganancias se aplican al terminar la medición.
- Volver a ejecutarlo mide las canciones nuevas y recalcula con la mediana actual.
- Un ajuste manual no se toca. "Original" en una canción también cuenta como manual.
- "Quitar nivelado" borra solo las ganancias automáticas de la biblioteca.
- Un archivo que no se puede medir se salta y se informa cuántos fueron.
- El "Nivelar automático" de una canción usa el mismo objetivo si la biblioteca ya fue nivelada; si no, -14 como hoy.

## Fuera
Modo por álbum, nivelado automático de canciones nuevas, modificar archivos.

## Enfoques considerados
1. **Por canción con la mediana** (elegido): ideal para aleatorio y listas mezcladas.
2. Por álbum: conserva la dinámica del disco, pero la biblioteca es sobre todo canciones sueltas.
3. Objetivo fijo -14: más simple, pero más canciones chocan con el límite de ±6 dB.

## Decisiones de construcción (mías)
- Corre dentro de la app, sin dependencias nuevas: sigue al cambiar de pantalla; si Android cierra la app, se retoma después.
- Los volúmenes que ya existen hoy se tratan como manuales.
- Las ganancias van en el respaldo como hasta ahora; al importarlas cuentan como manuales. Las mediciones no se respaldan.
- Base: rama listas-unificadas.
