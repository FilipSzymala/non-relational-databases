# Wyniki

JMH 1.37, tryb `AverageTime`, jednostka `µs/op` (mniej oznacza krótszy odczyt). Błąd to margines przedziału ufności 99,9% raportowany przez JMH dla 10 iteracji pomiarowych (2 forki × 5 iteracji).

| Wariant | Średnia [µs/op] | Błąd JMH [µs/op] |
|---|---:|---:|
| MongoDB bez cache | 481.806 | ± 279.122 |
| Trafienie w cache | 70.678 | ± 36.310 |
| Chybienie i odbudowa wpisu | 500.438 | ± 259.215 |

Dla zmierzonych średnich trafienie skróciło odczyt o **411.128 µs (85.3%)**, czyli było około **6.82 raza** szybsze.

[Surowe wyniki JSON](jmh.json), [log JMH](jmh.log).
