# NBD

## ZADANIE 3

Zadanie to jest modyfikacją zadania 2 polegającą na dodaniu funkcjonalności cache dla wybranych danych.

1. Zaproponuj jeden ze zbiorów danych (kolekcji w MongoDB), które należałoby trzymać w cache’u i jak długo - uzasadnij
2. Zaimplementuj metody do cache’owania danych i do pobierania danych z cache’a.
3. Zaproponuj kiedy i gdzie powinny być odświeżane dane w cache’u (inwalidacja cache’a). Pamiętaj, aby zadbać o to, aby dane w Redisie i MongoDB były aktualne.
4. Zaimplementuj komponent adaptera dla Redis wykorzystujący jeden z interfejsów (dla wybranego zbioru danych). Zastosuj wzorzec dekoratora.
    1. Dodaj w niem metody do usuwania danych z cache’a (unieważnienie danych cache).
    2. Implementacja metod musi przewidywać failover, tzn. jeżeli nie ma połączenia z Redis, to operacje są wykonywane na MongoDB.
5. Zaimplementuj testy jednostkowe dla metody: dodającej dane do Redisa oraz odczytującej dane z Redisa.
6. Wykonaj testy wydajnościowe czytania danych z bazy danych i cache. Porównaj i omów uzyskane wyniki, w przypadku trafienia i nie (dana była w cache i gdy jej nie było - do tego celu użyj metody unieważniającej cache). Jakie były dodatkowe opóźnienia. Dla Java testy wykonaj poprzez JMH (Java Microbenchmark Harness). Dla innych języków programowania wybierz bibliotekę o podobnych możliwściach jak JMH.

Pamiętaj o zachowaniu dziedzinowego modelu danych jak i interfejsów adapterów

1. Przygotuj model danych który będzie odwzorowany na dokumenty. Dobierz sposób mapowania związków do jednego i do wielu dla dokumentowej bazy danych.
2. Przygotuj repozytoria działające na dokumentowej bazie danych MongoDB, np. MongoClientRepository
3. Jak dla wcześniejszego zadania zaimplementuj Adaptery np. MongoClientRepositoryAdapter, które używają repozytoriów do wykonywania operacji bazodanowych. Adaptery muszą przekonwertować model dziedzinowy do i z modelu repozytoriów.
4. Utwórz klaster bazy danych typu replica-set w docker bądź w chmurze (np. Azure) składający się z minimum 3 węzłów.
5. Zweryfikuj poprawność działania poprzez zaimplementowanie testów na poziomie repozytoriów i adapterów. Testy wykonaj przy poprawnie jak i niepoprawnie działającym klastrze (wyłącz aktualny węzeł primary). Uwaga! Jeżeli korzystasz z testcontainers, to jest możliwe włączanie i wyłączanie kontenerów z poziomu kodu testującego. Nie jest to obowiązkowe, ale dość wygodne. Z tym że trzeba monitorować stan kontenera, który ni zatrzymuje/wyłącza się natychmiast.
6. Operacje na bazie danych powinny pozostawić system w stanie spójnym w przypadku jego normalnego działania. Wykorzystaj do tego celu mechanizmy samej bazy danych jak np. schematy, atomowość operacji na dokumentach. Przykładem może być próba wypożyczenia pojazdu, którego zmiana stanu jest możliwa tylko wtedy, gdy pojazd nie jest wypożyczony.

Kod aplikacji powinien znaleźć się jako archiwum zip w tym zadaniu, oraz w rozgałęzieniu o nazwie MongoDB w repozytorium kodu.