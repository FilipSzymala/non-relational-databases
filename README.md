# NBD

## ZADANIE 2

Pamiętaj o zachowaniu dziedzinowego modelu danych jak i interfejsów adapterów

1. Przygotuj model danych który będzie odwzorowany na dokumenty. Dobierz sposób mapowania związków do jednego i do wielu dla dokumentowej bazy danych.
2. Przygotuj repozytoria działające na dokumentowej bazie danych MongoDB, np. MongoClientRepository
3. Jak dla wcześniejszego zadania zaimplementuj Adaptery np. MongoClientRepositoryAdapter, które używają repozytoriów do wykonywania operacji bazodanowych. Adaptery muszą przekonwertować model dziedzinowy do i z modelu repozytoriów.
4. Utwórz klaster bazy danych typu replica-set w docker bądź w chmurze (np. Azure) składający się z minimum 3 węzłów.
5. Zweryfikuj poprawność działania poprzez zaimplementowanie testów na poziomie repozytoriów i adapterów. Testy wykonaj przy poprawnie jak i niepoprawnie działającym klastrze (wyłącz aktualny węzeł primary). Uwaga! Jeżeli korzystasz z testcontainers, to jest możliwe włączanie i wyłączanie kontenerów z poziomu kodu testującego. Nie jest to obowiązkowe, ale dość wygodne. Z tym że trzeba monitorować stan kontenera, który ni zatrzymuje/wyłącza się natychmiast.
6. Operacje na bazie danych powinny pozostawić system w stanie spójnym w przypadku jego normalnego działania. Wykorzystaj do tego celu mechanizmy samej bazy danych jak np. schematy, atomowość operacji na dokumentach. Przykładem może być próba wypożyczenia pojazdu, którego zmiana stanu jest możliwa tylko wtedy, gdy pojazd nie jest wypożyczony.

Kod aplikacji powinien znaleźć się jako archiwum zip w tym zadaniu, oraz w rozgałęzieniu o nazwie MongoDB w repozytorium kodu.