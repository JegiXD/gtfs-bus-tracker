# GTFS sledilnik avtobusov

Majhna Java aplikacija, ki prebere statični GTFS-vir in izpiše naslednje
prihode avtobusov na izbrano postajališče v naslednjih dveh urah, združene
po linijah.

## Uporaba

```text
busTrips <številka_postajališča> <število_avtobusov_na_linijo> <relative|absolute>
```

- `station_id` (int) - GTFS `stop_id` postajališča.
- `num_buses_per_line` (int) - največje število prihodov na linijo.
- `relative|absolute` - `absolute` izpiše uro (`12:10`), `relative` pa čas
  do prihoda (`10min`).

Primer po gradnji:

```bash
GTFS_DATA_DIR=gtfs-data java -jar target/gtfs-bus-tracker.jar 2 2 relative
```

Program podpira tudi dve dodatni zastavici, ki omogočata lažji zagon in
preizkušanje brez spreminjanja kode:

- `--gtfs <mapa>` - mapa z datotekami `stops.txt`, `stop_times.txt`,
  `trips.txt`, `routes.txt` in po želji `calendar.txt`. Privzeto je
  uporabljena mapa `./gtfs-data`, oziroma vrednost spremenljivke okolja
  `GTFS_DATA_DIR`.
- `--at <yyyy-MM-ddTHH:mm[:ss]>` - določitev časa poizvedbe namesto
  trenutnega sistemskega časa.

Če program zaženemo brez argumentov, uporabi privzeto postajališče `2`,
prikaže dva prihoda na linijo in uporabi relativni zapis časa.

Primer za postajališče 2:

```bash
java -jar target/gtfs-bus-tracker.jar 2 2 relative \
  --gtfs gtfs-data --at 2026-09-09T10:00:00
```

```text
Upcoming buses at AL Masjid Al-nabawi (Clock Roundabout) (stop 2)
Query time: 2026-09-09 10:00 | next 2h
107: 1min, 10min
101: 8min, 10min
106: 10min, 11min
```

## Gradnja in zagon

Potrebujemo JDK 17 ali novejši in Maven.

```bash
mvn package
java -jar target/gtfs-bus-tracker.jar 2 2 relative \
  --gtfs gtfs-data --at 2026-09-09T10:00:00
```

## Testi

```bash
mvn test          # enotski testi
mvn verify        # enotski in integracijski testi
```

- **Enotski testi** (`*Test.java`) preverjajo razčlenjevanje GTFS-časov,
  CSV-datotek, algoritem prihodov in oblikovanje izpisa brez dostopa do
  datotek.
- **Integracijski testi** (`*IT.java`) preverjajo delo z datotekami in celoten
  zagon CLI-programa nad testnim GTFS-virom v
  `src/test/resources/gtfs-fixture`.

## Opomba o zasnovi

Uporabljene so datoteke `stops.txt`, `stop_times.txt`, `trips.txt`,
`routes.txt` in po želji `calendar.txt`. Datoteka `calendar_dates.txt` ni
uporabljena, ker so GTFS-izjeme in prazniki izven obsega projekta. Če
`calendar.txt` manjka, se vse storitve obravnavajo kot aktivne.

Največji datoteki, `stop_times.txt` in `trips.txt`, se bereta vrstico po
vrstici. V pomnilniku se obdržijo samo podatki, povezani z izbranim
postajališčem, zato poraba pomnilnika ni odvisna od celotne velikosti vira.

GTFS dovoljuje čase, večje od `24:00:00`, za vožnje po polnoči, ki še vedno
pripadajo storitvi prejšnjega dne. `NextArrivalsService` zato pri iskanju
prihodov preveri današnji in včerajšnji dan.

## Avtorsko delo in pomoč pri programiranju

Glavne elemente projekta sem izdelal sam in samostojno prišel do rešitve.
Pri pisanju in oblikovanju kode sem si pomagal z AI-orodjem, pri čemer sem
končne odločitve, strukturo projekta in pravilnost rešitve preveril sam.

## Struktura projekta

```text
src/main/java/com/gtfs/bustracker/
  Main.java                     vstopna točka CLI-programa
  model/                        podatkovni modeli
  gtfs/                         nalaganje GTFS-datotek
  service/                      glavna logika in oblikovanje izpisa
  util/                         razčlenjevanje GTFS-časov in CSV-ja
src/test/java/com/gtfs/bustracker/   enotski in integracijski testi
src/test/resources/gtfs-fixture/     testni GTFS-vir
gtfs-data/                            privzeti GTFS-vir za zagon
```
