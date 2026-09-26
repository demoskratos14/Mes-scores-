# Mes Scores

Application Android (Kotlin + Jetpack Compose) pour noter les scores de vos jeux de société.

## Un seul fichier manquant : `gradle/wrapper/gradle-wrapper.jar`

Ce fichier est un binaire compilé fourni par Gradle : il ne peut pas être généré comme un fichier texte, donc il n'est pas inclus dans cette archive. Trois façons simples de le récupérer une fois le dépôt sur GitHub :

1. **Le plus simple** : ouvrez le dossier du projet dans Android Studio. Au premier lancement, Android Studio détecte l'absence du wrapper et le télécharge automatiquement pendant la synchronisation Gradle.
2. **En ligne de commande**, si vous avez déjà Gradle installé sur votre machine : lancez `gradle wrapper` à la racine du projet, ce qui régénère le fichier à partir de `gradle-wrapper.properties` (déjà présent ici).
3. **Téléchargement manuel** : récupérez `gradle-wrapper.jar` depuis n'importe quel projet Android récent (ou depuis la documentation officielle de Gradle) et placez-le dans `gradle/wrapper/`.

Tout le reste (code Kotlin, configuration Gradle, icônes, image de fond) est prêt à l'emploi.

## Structure du projet

```
MesScores/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
└── app/
    ├── build.gradle.kts
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/example/scoreboard/
        │   ├── MainActivity.kt
        │   ├── ScoreViewModel.kt
        │   ├── SetupScreen.kt
        │   ├── ScoreScreen.kt
        │   └── AppBackground.kt
        └── res/
            ├── values/strings.xml
            ├── drawable-nodpi/bg_board_games.jpg
            └── mipmap-*/ic_launcher.png, ic_launcher_round.png
```
