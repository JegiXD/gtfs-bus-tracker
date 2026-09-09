# GTFS sledilnik avtobusov

Java aplikacija, ki iz voznega reda v obliki GTFS prebere prihode avtobusov
na izbrano postajališče v naslednjih dveh urah. Prihode izpiše po linijah.
Uporablja shranjeni vozni red, ne podatkov o dejanskem položaju avtobusov.

## Hiter zagon

Potrebuješ **JDK 17 ali novejši** in **Maven**. V mapi projekta zaženi:

```bash
mvn package
java -jar target/gtfs-bus-tracker.jar 2 2 relative
```

Primer prikaže največ dva prihoda na linijo za postajališče `2`, čas pa
izpiše v minutah do prihoda. Brez argumentov program uporabi iste nastavitve.

## Uporaba

```text
java -jar target/gtfs-bus-tracker.jar <postajališče> <število_prihodov> <relative|absolute>
```

- `postajališče`: celoštevilski ID postajališča (`stop_id` iz `stops.txt`).
- `število_prihodov`: največ prihodov na linijo; mora biti večje od nič.
- `relative`: čas do prihoda, npr. `10min`; `absolute`: ura prihoda, npr. `12:10`.

Na konec ukaza lahko dodaš:

- `--gtfs <mapa>`: pot do GTFS-podatkov. Sicer se uporabi `GTFS_DATA_DIR`,
  če je nastavljen, ali privzeta mapa `gtfs-data`.
- `--at <yyyy-MM-ddTHH:mm[:ss]>`: čas poizvedbe za preizkušanje.
  Brez te možnosti se uporabi trenutni čas.

Primer z določenim datumom in časom:

```bash
java -jar target/gtfs-bus-tracker.jar 2 2 relative \
  --gtfs gtfs-data --at 2026-09-09T10:00:00
```

Primer vrstice izpisa: `107: 1min, 10min` pomeni, da avtobusa linije 107
po voznem redu prideta čez 1 in 10 minut.

## Podatki in omejitve

GTFS-mapa mora vsebovati `stops.txt`, `stop_times.txt`, `trips.txt` in
`routes.txt`. Datoteka `calendar.txt` določa dneve voženj; če manjka,
program šteje, da vse vožnje veljajo vsak dan. Izjem iz `calendar_dates.txt`
(npr. praznikov) ne upošteva.

V priloženem `gtfs-data/calendar.txt` je veljavnost voznega reda za
preizkušanje prestavljena s 15. 2.–15. 5. 2020 na 1. 1.–31. 12. 2026.
To ni posodobitev dejanskega voznega reda. Testni podatki ohranjajo prvotne datume.

Večji datoteki se bereta po vrsticah, shranijo pa se le podatki za izbrano
postajališče. Program upošteva tudi GTFS-čase nad `24:00:00`, ki označujejo
vožnje po polnoči iz prejšnjega dne.

## Testi

Enotski testi preverijo posamezne dele programa:

```bash
mvn test
```

Vsi testi, vključno z branjem datotek in celotnim zagonom:

```bash
mvn verify
```

Ukaze zaženi v terminalu v mapi projekta. V polje **Run** pri Mavenovi
konfiguraciji v IntelliJ IDEA vpiši samo `test` ali `verify`, brez `mvn`
in brez dodatnega besedila.

## Predlogi za zahtevnejšo nalogo

- Upoštevanje praznikov in izjem iz `calendar_dates.txt`.
- Podpora časovnim pasovom prevoznikov in premiku ure.
- Prikaz zamud in odpovedi s podatki GTFS Realtime.
- Iskanje poti med postajališči s prestopanji.
- Merjenje hitrosti in porabe pomnilnika na velikih GTFS-virih.

## Struktura projekta

```text
src/main/java/com/gtfs/bustracker/
  Main.java     zagon in argumenti
  model/        podatkovni razredi
  gtfs/         branje GTFS-datotek
  service/      iskanje prihodov in izpis
  util/         obdelava časov in CSV-ja
src/test/       testi in testni podatki
gtfs-data/      priloženi vozni red
```

## Avtorstvo

Glavne dele projekta in rešitev sem izdelal sam. Pri pisanju in oblikovanju
kode sem si pomagal z AI-orodjem, končne odločitve in pravilnost pa preveril sam.
