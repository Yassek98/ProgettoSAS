# 🍽️ CatERing - Sistema di Gestione Catering

Sistema per la gestione di eventi catering, progettato per il corso di **Sviluppo Applicazioni Software (SAS)**.

## 📋 Indice

- [Requisiti](#-requisiti)
- [Setup Rapido](#-setup-rapido)
- [Struttura Progetto](#-struttura-progetto)
- [Comandi Utili](#-comandi-utili)
- [Test](#-test)
- [Documentazione](#-documentazione)

## 🔧 Requisiti

- **Java JDK 11+** (testato con JDK 24)
- **Maven 3.9+** ([Download](https://maven.apache.org/download.cgi))
- (Opzionale) IDE: VS Code, IntelliJ IDEA, Eclipse

## 🚀 Setup Rapido

```bash
# 1. Clona il repository
git clone <url-repo>
cd catering

# 2. Compila il progetto
mvn clean compile

# 3. Esegui i test
mvn test

# 4. Esegui l'applicazione
mvn exec:java
```

## 📁 Struttura Progetto

```
catering/
├── src/
│   ├── main/java/catering/
│   │   ├── businesslogic/          # Logica di business
│   │   │   ├── event/              # Gestione eventi e servizi
│   │   │   ├── kitchen/            # Gestione cucina e compiti
│   │   │   ├── menu/               # Gestione menu
│   │   │   ├── recipe/             # Gestione ricette
│   │   │   ├── shift/              # Gestione turni
│   │   │   └── user/               # Gestione utenti
│   │   ├── persistence/            # Layer di persistenza (DB)
│   │   └── util/                   # Utilities (logging, ecc.)
│   └── test/java/                  # Test JUnit 5
├── database/
│   └── catering_init_sqlite.sql    # Script inizializzazione DB
├── docs/                           # Documentazione LaTeX
├── pom.xml                         # Configurazione Maven
└── README.md
```

## ⚡ Comandi Utili

| Comando | Descrizione |
|---------|-------------|
| `mvn clean compile` | Pulisce e compila il progetto |
| `mvn test` | Esegue tutti i test |
| `mvn verify` | Compila, testa e crea il JAR |
| `mvn exec:java` | Esegue l'applicazione principale |
| `mvn clean` | Rimuove la cartella target/ |

## 🧪 Test

Il progetto usa **JUnit 5** per i test. Attualmente sono implementati:

- `SummarySheetTest` - Test per la creazione e gestione dei fogli riepilogativi

```bash
# Esegui tutti i test
mvn test

# Esegui un test specifico
mvn test -Dtest=SummarySheetTest
```

## 📖 Documentazione

La documentazione LaTeX si trova in `docs/`. Per compilarla:

```bash
cd docs
pdflatex main.tex
```

## 👥 Contribuire

1. Crea un branch per la tua feature: `git checkout -b feature/nome-feature`
2. Sviluppa e testa le modifiche
3. Committa: `git commit -m "Aggiunta feature X"`
4. Pusha: `git push origin feature/nome-feature`
5. Apri una Pull Request

## 📝 Note Importanti

- Il database SQLite viene rigenerato dai test usando `database/catering_init_sqlite.sql`
- I file `.db` sono ignorati da git (ogni sviluppatore ha il suo locale)
- Le configurazioni IDE (`.idea/`, `.vscode/`) sono personali e ignorate

---

*Progetto SAS - Università degli Studi di Torino*
