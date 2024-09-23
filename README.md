# Aan de slag met JDBC Template

Om gegevens uit een database te kunnen gebruiken in een Spring-applicatie gebruiken we JDBC Template.

In dit document wordt uitgelegd hoe je een met JDBC Template werkt. Als database gebruiken we H2: dat is een database die in het geheugen draait ("in memory"). Wat je erin opslaat, wordt niet bewaard als je je applicatie afsluit. Voor productie-doeleinden is zo'n database uiteraard niet geschikt, maar om te leren hoe JDBC werkt, voldoet H2 prima.

Om de werking van je code te testen, maak je een controller aan met methoden waarin je database-code wordt uitgevoerd. Tip: gebruik streams om het resultaat van je code in de browser te zien te krijgen.

## 1: Configuratie

1. Maak een nieuwe spring boot project met de [Spring Initializr](https://start.spring.io/).
   1. Kies voor Maven
   1. Kies voor Java
   1. Spring Boot versie: de laatste zonder iets tussen haakjes zoals (SNAPSHOT) of (M3)
   1. Packaging: JAR 
   1. Java versie 17, 21 of 22, afhankelijk van welke je al hebt op je laptop.
   1. Voeg de volgende dependencies toe: Spring Web, Spring Data JPA, H2 Database
2. Open het project met IntelliJ IDEA.
3. Maak een package "config" en plaats daarin een klasse met configuratie voor je database. Fix alle imports en let op de annotaties:
```java
@Configuration
public class DBConfig {
    @Bean
    public DataSource dataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .build();
    }
}
```

4. Voeg een @RestController class toe. Geef deze class een private variabele van het type JdbcTemplate. Het JdbcTemplate object laat je injecteren met @Autowired:
```java
@RestController
public class MyController {
   @Autowired
   private JdbcTemplate jdbcTemplate;
}

```
5. Voeg aan de controller een endpoint to met@GetMapping en test of je JDBC verbinding met de H2 database werkt: maak een tabel (met jdbcTemplate.execute()), stop er data in en haal er data uit. 

```java
 @GetMapping("/testdb")
 public String testDB() {
     jdbcTemplate.execute("CREATE TABLE box (id INT NOT NULL)");
     jdbcTemplate.execute("INSERT INTO box VALUES (1)");
     jdbcTemplate.execute("INSERT INTO box VALUES (2)");
     int value = jdbcTemplate.queryForObject("SELECT MIN(id) FROM box", Integer.class);
     return String.valueOf(value);
 }
```
6. Start je applicatie en roep het endpoint aan met de browser of postman.
Als je een servererror krijgt (statuscode 500) lukt het nog niet om een databaseverbinding op te zetten. Check de stacktrace in de output van de server om te troubleshooten.

7. Om je database gevuld te krijgen met basisgegevens zet je de sqlcode in twee aparte bestanden. schema.sql voor het database schema (de tabellen, welke kolommen er zijn, datatypes etc. Ook wel de DDL) en de test data, of test vulling voor je database in testdata.sql. Plaats deze bestanden in de /main/resources/db map. Vertel Spring om deze scripts bij het opstarten uit te voeren in de dbConfig class die je eerder hebt aangemaakt:

```java
@Bean
public DataSource dataSource() {
    return new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .addScript("classpath:db/schema.sql")
            .addScript("classpath:db/testdata.sql")
            .build();
}
```

In dit voorbeeld gebruiken we de volgende testdata:

### schema.sql

```sql
CREATE TABLE Snack (
    snacknr INT PRIMARY KEY ,
    snacknaam VARCHAR(255) NOT NULL,
    calorieen INT NOT NULL
);
```

### testdata.sql

```sql
INSERT INTO Snack (snacknr, snacknaam, calorieen) VALUES (1, 'Snack1', 100);
INSERT INTO Snack (snacknr, snacknaam, calorieen) VALUES (2, 'Snack2', 200);
INSERT INTO Snack (snacknr, snacknaam, calorieen) VALUES (3, 'Snack3', 300);
INSERT INTO Snack (snacknr, snacknaam, calorieen) VALUES (4, 'Snack4', 100);
INSERT INTO Snack (snacknr, snacknaam, calorieen) VALUES (5, 'Snack5', 200);
```

8. Test of je testdata wordt ingelezen en beschikbaar is. Merk op dat het nog niet mogelijk is meerdere records of meerdere kolommen uit de database op te halen. Hiervoor heb je extra klassen nodig. Voor nu vinden we het genoeg als we één enkel veld uit één enkel record kunnen ophalen:

```java
 @GetMapping("/testdata")
 public String testData() {
     return jdbcTemplate.queryForObject("SELECT snacknaam FROM Snack WHERE snacknr = 1", String.class);
 }
```

## 2: Gegevens uit de database omzetten naar objecten

De volgende stap is gegevens uit de database omzetten naar objecten die je in je java-code kunt gebruiken. Hiervoor hebben we twee dingen nodig: een klasse voor de objecten zelf én een klasse die weet hoe records uit de database omgezet moeten worden naar deze objecten. Een klasse die een verzameling ("set") records ("results") omzet naar objecten noemen we een ResultSetExtractor. Zo'n ResultSetExtractor maken en gebruiken doen we als volgt:

1. Maak een klasse aan voor één rij uit je voorbeeldtabel. Maak attributen voor elke kolom:

```java
public class Snack {
    public int snacknr;
    public String snacknaam;
    public int calorieen;
   
   // Uiteraard is het handig om een toString-methode te implementeren.
}
```

2. Om de resultaten van een query te verwerken, heb je een zogenoemde ResultSetExtractor nodig. Een ResultSetExtractor is een object dat de interface ResultSetExtractor implementeert voor een List van de objecten die je uit de database wil halen. In dit voorbeeld maken we dus een `ResultSetExtractor<List<Snack>>`. Een ResultSetExtractor loopt door alle gevonden records heen, haalt daar kolomdata uit op en plaatst deze in objecten. Deze objecten worden toegevoegd aan een lijst en de lijst met objecten wordt teruggegeven aan de aanroeper.

```java
public class SnackResultSetExtractor implements ResultSetExtractor<List<Snack>> {
   @Override
   public List<Snack> extractData(ResultSet rs) throws SQLException, DataAccessException {
      List<Snack> snacks = new ArrayList<Snack>();
      while (rs.next()) {
         Snack s = new Snack();
         s.snacknr = rs.getInt("snacknr");
         s.snacknaam = rs.getString("snacknaam");
         s.calorieen= rs.getInt("calorieen");
         snacks.add(s);
      }
      return snacks;
   }
}
```

3. Aanroepen van je ResultSetExtractor doe je dan als volgt:

```java
List<Snack> snacks = jdbcTemplate.query("SELECT * FROM Snack", new SnackResultSetExtractor());
```

4. Het is ook mogelijk om parameters mee te geven aan je query (vgl geparametriseerde queries in PHP):

```java
List<Snack> snacks = jdbcTemplate.query("SELECT * FROM Snack WHERE snacknr < ? OR snacknr > ?", new SnackResultSetExtractor(), Integer.valueOf(3), Integer.valueOf(4));
```

## 3: RowMapper

Een ResultSetExtractor maakt het dus mogelijk om gegevens uit meerdere records om te zetten naar een lijst met objecten.  Het nadeel aan een ResultSetExtractor is echter dat hij je **dwingt** om gegevens om te zetten naar een lijst met objecten. Als je al weet dat je eigenlijk maar één object (record) wil hebben, is dat onhandig.

De oplossing is gebruikmaken van de interface RowMapper. Met deze interface beschrijf je hoe je één enkel record omzet naar één enkel object. De klasse die deze interface implementeert kun je dan gebruiken zowel in gevallen waar je maar één enkel object wil hebben als in gevallen waarin je meerdere objecten opvraagt uit de database.

De interface heet RowMapper omdat hij ons in staat stelt om records ("rows") om te zetten ("map") naar objecten.

De RowMapper voor dit voorbeeld ziet eruit als volgt:

```java
public class SnackRowMapper implements RowMapper<Snack> {
    public Snack mapRow(ResultSet rs, int rowNum) throws SQLException {
        Snack s = new Snack();
        s.snacknr = rs.getInt("snacknr");
        s.snacknaam = rs.getString("snacknaam");
        s.calorieen = rs.getInt("calorieen");
        return d;
    }
}
```

Als je een lijst met objecten nodig hebt, roep je een RowMapper aan op dezelfde manier als een ResultSetExtractor:

```java
List<Snack> snacks = jdbcTemplate.query("SELECT * FROM Snack WHERE snacknr < ? OR snacknr > ?", new SnackRowMapper(), Integer.valueOf(3), Integer.valueOf(4));
```

Wil je daarentegen één enkel object hebben, dan gebruik je de methode jdbcTemplate.queryForObject:

```java
Snack s = jdbcTemplate.queryForObject("SELECT * FROM Snack WHERE snacknr = ?", new SnackRowMapper(), Integer.valueOf(2));
```

## Gegevens opslaan en bijwerken: INSERT en UPDATE

Om gegevens op te slaan en bij te werken gebruik je de methode jdbcTemplate.update.

INSERT:

```java
jdbcTemplate.update(
    "INSERT INTO Snack (snacknr, snacknaam, calorieen) VALUES (?, ?, ?)",
    Integer.valueOf(91), "Snack91", Integer.valueOf(123));
```

UPDATE:

```java
// s is een bestaand object van de klasse Snack.
s.snacknaam = "Gewijzigd";
jdbcTemplate.update("UPDATE Snack SET snacknaam = ? WHERE snacknr = ?",
    s.snacknaam, s.snacknr);
```


