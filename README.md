# NBD



## ZADANIE 1

UWAGA! Nie wolno użyć modelu danych wypożyczalni pojazdów, oraz szkieletów aplikacyjnych typu Spring i Quarkus. A w szczególności Spring Data czy też Quarkus Data



Utwórz w pustym repozytorium rozgałęzienie o nazwie ORM i rozwijaj ten projekt właśnie w tym rozgałęzieniu. Pamiętaj, że w projekcie w przyszłości mogą pojawić się inne komponenty, a część implementacji pozostawać niezmienne np. dziedzinowy model danych czy też interfejsy adapterów.



Wykorzystując model danych i logikę biznesową przygotowaną na przedmiot np. Programowanie Obiektowe (bądź inny projekt o podobnych wymaganiach), zbuduj aplikację opartą o relacyjną bazę danych. Możesz wykorzystać do tego celu dowolny język programowania, ale pamiętaj, że kolejne zadania trzeba będzie wykonać także z wykorzystaniem tego języka. Podejmij więc decyzję świadomie.



Wymagania:



1. Przygotować dziedzinowy model danych zgodnie z zasadami DDD. Model ten powinien pozostać niezmienny w trakcie trwania całego semestru. Dla wypozyczalni pojazdów byłyby to klasy Client, Vehicle (z pochodnymi), Rent.
2. Przygotować interfejsy komponentów bazodanowych z metodami realizującymi przypadki użycia dla przykładowej aplikacji. Uwaga! Te interfejsy będą implementowane przez komponenty korzystające z innych baz danych na kolejnych zadaniach. Dla przykładu, dla wypożyczalni pojazdów będą to ClientRepository, VehicleRepository i RentRepository. Interfejsy podobnie jak dziedzinowy model danych powinny być niezmienne w trakcie trwania całego semestru.

3. Przygotować model danych na poziomie repozytorium wykorzystujący dziedziczenie i polimorfizm. Musi on zostać odwzorowany (ORM) na strukturę relacyjnej bazy danych. Dozwolone są oba podejścia, czyli model->baza danych albo baza danych -> model. Dla wypozyczalni pojazdów byłyby to przykładowe klasy ClientEnt, VehicleEnt (z pochodnymi) i RentEnt.

4. W każdym zadaniu będą więc dwa modele danych. Zgodnie z DDD jeden model jest na poziomie dziedziny i to jest to co robiliście na Programowaniu Obiektowym. Drugim modelem jest ten z punktu 3, który mapowany jest na bazę danych. Konieczne jest zaimplementowanie Adaptera np. JpaClientRepositoryAdapter, który implementuje interfejs z 2, konwertuje model dziedzinowy na bazodanowy i wykonuje operacje na Repozytorium np.: JpaClientRepository z wykorzystaniem modelu bazodanowego. Wynik operacji jest konwertowany do modelu dziedzinowego. Takie podejście separuje całkowicie dostęp do bazy danych od reszty aplikacji. Na poziomie implementacji repozytoriów istnieje także wiele różnych podejść i rozwiązań dla tej samej bazy.

5. Dla modelu dziedzinowego wymagane są relacje: do wielu. Mogą być jedno, bądź dwukierunkowe. Przełoży się to na relacje do wielu w modelu bazodanowym

6. Dla relacyjnej bazy danych muszą być zachowane reguły ACID. Należy także dobrać "odpowiedni" poziom izolacji dla operacji bazodanowych

7. Wykorzystaj blokady optymistyczne bądź pesymistyczne, do zachowania określonych reguł biznesowych. Przykładem może być odrzucenie operacji wypożyczenia już wypożyczonego pojazdu. UWAGA!!! Ta operacja może zostać wykonana współbieżnie na tym samym pojeździe.

8. Załącz plik docker-compose.yml w katalogu Docker projektu, pozwalający na uruchomienie skonfigurowanej bazy danych. Można załączyć także skrypty inicjalizujące bazę danych (strukturę i dane początkowe).





|Obiektowy model danych|+3 Dokonano odwzorowania relacyjnego i obiektowego modelu danych. Zademonstrowano powstałą strukturę bazy danych i je powiązanie z modelem obiektowym.<br /><br />+1 Uzasadniono wybór sposobu mapowania dziedziczenia.|
|-|-|
|Relacje do wielu|+3 Zamodelowano przynajmniej jedną relację do wielu (jeden-wiele/wiele-wiele), jedno lub dwukierunkową|
|Zachowane reguł ACID|+4 Wyjaśniono zastosowanie reguł ACID dla implementacji własnej aplikacji.|
|Poziom izolacji operacji bazodanowych|+2 Wybrano i uzasadniono poziom izolacji dla połączenia bazodanowego|
|Blokady optymistyczne bądź pesymistyczne|+4 Nie da się naruszyć reguł biznesowych aplikacji, np. wypożyczenia większej liczby pojazdów niż to jest możliwe, gdy operacje są wykonywane współbieżnie.|
|docker-compose.yml|Zamieszczono w projekcie plik docker-compose do uruchomiania i zainicjalizowania bazy danych.<br /><br />+1 da się uruchomić bazę danych<br /><br />+1 po uruchomieniu aplikacji/testów baza danych jest utworzona i zawiera dane początkowe<br />Maksymalna ocena2|
|Repozytorium|+1 Zdefiniowano dziedzinowy model danych<br /><br />+1 Zdefiniowano interfejsy adapterów.<br /><br />+1 Wykonano implementację repozytoriów i użyto je w adapterach.<br /><br />+1 Wykonano testy na poziomie repozytoriów i adapterów|
|Terminowość i kompletność|Opóźnienie zadania to 20% mniej do oceny finalnej. Zadanie uważa się za opóźnione jeżeli nie jest kompletne bądź nie odpowiedziano na zadane pytania.<br /><br />23.0 pkt - kompletne zadanie oddane w terminie<br /><br />13.2 pkt - oddane tydzień po terminie<br /><br />4.0 pkt - oddane 2 tygodnie po terminie<br /><br />0 - zadanie do poprawki, wymaga dodatkowej odpowiedzi|



