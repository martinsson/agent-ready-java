# Profil `filjava-backend`

Règles déterministes pour le template backend Java de la filière FILJAVA et les projets qui en
sont issus (`core`, `infra`, `ui`, CXF, OpenAPI, Cucumber). Elles décrivent le template **tel qu'il
est aujourd'hui** : Mockito reste permis dans `infra`, `ui/config` et `ui/security` restent admis.
Le template le contient à partir de sa branche `feature/outillage-agents` ; ce dossier en est la
source pour les autres projets.

Ce profil reste volontairement plus léger que la racine de ce dépôt : ni Spotless (il reformaterait
tout), ni PIT, Selfie, jqwik ou Testcontainers (minimalisme du template), ni ratchet.

## Installer dans un projet

1. Copier les fichiers en gardant leurs chemins, sauf ce `README.md` et `pom.patch`. Fusionner à la
   main `.gitattributes` et `.mvn/jvm.config` s'ils existent déjà. Appliquer `pom.patch` (`git apply`,
   ou le reporter à la main dans les trois `pom.xml`).
2. Remplacer `ch.ge.filjava.apptemplatebackend` par le package racine du projet, dans les fichiers
   et dans le chemin de `ui/src/test/java/...`.
3. Dans `pom.xml`, adapter `-XepOpt:NullAway:UnannotatedSubPackages`, qui nomme le code généré de
   `profilutilisateur`, aux fonctionnalités du projet.
4. Lancer `./verify`. Si des violations existent, décider pour chacune : corriger le code, adapter
   la règle ou l'écarter (`config/checkstyle/suppressions.xml`).

Les règles appartiennent ensuite au projet, qui reste libre de les changer. Pour récupérer des
nouveautés, demander à l'agent de comparer ce dossier au projet et d'appliquer ce que le développeur
retient.

## Règles

| Outil | Règle |
|---|---|
| Enforcer | Java 25 minimum ; `core` sans aucune dépendance hors du scope test |
| ArchUnit | Domaine limité à `java..` et à lui-même ; champs du domaine `final` |
| ArchUnit | `ui` n'utilise pas `infra` |
| ArchUnit | Pas de package `model`, `repository`, `service`, `request`, `response`, `controller` |
| ArchUnit | Toute classe sous une fonctionnalité dans `domain`, `infra` et `ui/rest` |
| ArchUnit | Premier niveau de `ui` : `rest`, `messaging`, `batch`, `config`, `security`, `main` |
| ArchUnit | Pas de cycle entre fonctionnalités (`domain`, `infra`, `ui/rest`) |
| ArchUnit | JPA uniquement dans `infra` ; `@Entity` nommé `*Entity` |
| ArchUnit | `*Adapter` de `infra` implémente une interface du domaine ; pas de `@Component` dans `infra` hors `infra.config` |
| ArchUnit | `@Path` : dans `ui/rest`, nommé `*Resource`, implémente une interface générée |
| ArchUnit | Contrôleurs Spring MVC uniquement dans `ui/security` |
| ArchUnit | `SecurityContextHolder` uniquement dans `ui/security` |
| ArchUnit | Pas de `now()` de `java.time` sans argument ni d'horloge système, sauf dans `ui/config` |
| ArchUnit | Cucumber uniquement dans `ui/bdd` ; pas de Mockito dans les tests de `ui` |
| ArchUnit | Pas de `System.out`, de `java.util.logging`, d'exception générique, d'injection par champ |
| JUnit | Aucun contrat OpenAPI ne porte d'identifiant utilisateur (paramètre ou propriété) |
| JUnit | `application*.yml` : valeurs sensibles en `${VARIABLE}` sans valeur par défaut, hôte d'URL compris, et `ddl-auto: validate` ; `application.yml` : Actuator limité à `health,info,prometheus` |
| Checkstyle | Tailles, complexité, couplage, imports inutilisés (`config/checkstyle/`) |
| PMD | Code mort et défauts de conception (`config/pmd/`) |
| Error Prone | Bloque en sévérité ERROR ; les avertissements restent affichés. `UnicodeInCode` coupé : le vocabulaire est français |
| NullAway | Code de production uniquement, hors code généré et entités JPA |
| JaCoCo | `core` : 90 % des lignes, 85 % des branches |

Chaque règle a été vérifiée en plaçant une violation volontaire, sauf les modules Checkstyle ou PMD
non visés par une sonde.

## Version de Java

Le profil vise Java 25, mais ses règles n'en dépendent pas. Le seul outil qui ne tourne pas sur un
JDK 17 est Error Prone 2.50 (JDK 21 minimum), et NullAway s'exécute comme son extension. Sur un
JDK 21, Error Prone exige aussi `-XDaddTypeAnnotationsToSymbol=true`. Le README du template détaille
les adaptations essayées pour une cible 21 et une cible 17.
