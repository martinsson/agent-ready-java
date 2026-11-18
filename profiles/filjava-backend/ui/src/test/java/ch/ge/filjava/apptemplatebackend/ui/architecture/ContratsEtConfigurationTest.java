package ch.ge.filjava.apptemplatebackend.ui.architecture;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Principes portés par des fichiers que ArchUnit ne voit pas : contrats OpenAPI et configuration. */
class ContratsEtConfigurationTest {

    private static final Path RESSOURCES = Path.of("src/main/resources");
    private static final Path APPLICATION_YML = RESSOURCES.resolve("application.yml");
    private static final Pattern IDENTIFIANT_UTILISATEUR =
            Pattern.compile("(?i).*(identifiant|user.?id|user.?name|utilisateur.?id|id.?utilisateur).*");
    private static final Pattern CLE_SENSIBLE = Pattern.compile(
            "(?i).*(password|secret|token|username|client-id|host|[.-](key|uri|url|urls))$");
    /** Une variable sans valeur par défaut, seule ou comme hôte d'une URL, suivie d'un port et d'un chemin. */
    private static final Pattern VALEUR_INJECTEE = Pattern.compile(
            "^([a-z:]+//)?\\$\\{[A-Z0-9_]+\\}((:[0-9]+)?/[^$]*(\\$\\{[A-Z0-9_]+\\}[^$]*)*)?$");

    @Test
    void aucunContratNAccepteDIdentifiantUtilisateur() throws IOException {
        List<String> trouves = new ArrayList<>();
        try (var fichiers = Files.list(RESSOURCES.resolve("openapi"))) {
            for (Path contrat : fichiers.filter(ContratsEtConfigurationTest::estYaml).toList()) {
                for (Object document : documents(contrat)) {
                    collecterNomsDeChamps(document, contrat.getFileName() + ":", trouves);
                }
            }
        }
        assertEquals(List.of(), trouves,
                "L'identifiant utilisateur vient du contexte de sécurité, jamais du contrat "
                        + "(voir UtilisateurCourantProvider)");
    }

    @Test
    void lesValeursSensiblesViennentDeLEnvironnement() throws IOException {
        List<Map.Entry<String, String>> configuration = toutesLesValeurs();
        List<String> enDur = configuration.stream()
                .filter(cle -> CLE_SENSIBLE.matcher(cle.getKey()).matches())
                .filter(cle -> !VALEUR_INJECTEE.matcher(cle.getValue()).matches())
                .map(Map.Entry::getKey)
                .toList();
        assertFalse(configuration.isEmpty());
        assertEquals(List.of(), enDur,
                "Hostnames, identifiants et secrets sont injectés par variables d'environnement, sans valeur par défaut");
    }

    @Test
    void actuatorNExposeQueLesEndpointsPrevus() throws IOException {
        assertEquals("health,info,prometheus",
                configuration().get("management.endpoints.web.exposure.include"),
                "Tout autre endpoint Actuator s'active explicitement selon l'environnement");
    }

    @Test
    void hibernateNeModifieJamaisLeSchema() throws IOException {
        List<String> valeurs = toutesLesValeurs().stream()
                .filter(cle -> "spring.jpa.hibernate.ddl-auto".equals(cle.getKey()))
                .map(Map.Entry::getValue)
                .distinct()
                .toList();
        assertEquals(List.of("validate"), valeurs,
                "Le schéma vient des scripts de db/migration, Hibernate le vérifie seulement");
    }

    private static boolean estConfiguration(Path fichier) {
        return fichier.getFileName().toString().startsWith("application") && estYaml(fichier);
    }

    private static boolean estYaml(Path fichier) {
        String nom = fichier.getFileName().toString();
        return nom.endsWith(".yaml") || nom.endsWith(".yml");
    }

    private static List<Object> documents(Path fichier) throws IOException {
        try (Reader lecteur = Files.newBufferedReader(fichier)) {
            List<Object> documents = new ArrayList<>();
            new Yaml().loadAll(lecteur).forEach(documents::add);
            return documents;
        }
    }

    private static void collecterNomsDeChamps(Object noeud, String chemin, List<String> trouves) {
        if (noeud instanceof Map<?, ?> table) {
            table.forEach((cle, valeur) -> {
                if ("properties".equals(cle) && valeur instanceof Map<?, ?> proprietes) {
                    proprietes.keySet().forEach(nom -> signaler(nom, chemin, trouves));
                }
                if ("parameters".equals(cle)) {
                    parametres(valeur).stream()
                            .filter(Map.class::isInstance)
                            .forEach(parametre -> signaler(((Map<?, ?>) parametre).get("name"), chemin, trouves));
                }
                collecterNomsDeChamps(valeur, chemin + "/" + cle, trouves);
            });
        } else if (noeud instanceof List<?> liste) {
            liste.forEach(element -> collecterNomsDeChamps(element, chemin, trouves));
        }
    }

    /** Une liste sous un chemin, une table sous {@code components}. */
    private static Collection<?> parametres(Object valeur) {
        if (valeur instanceof List<?> liste) {
            return liste;
        }
        return valeur instanceof Map<?, ?> table ? table.values() : List.of();
    }

    private static void signaler(Object nom, String chemin, List<String> trouves) {
        if (nom != null && IDENTIFIANT_UTILISATEUR.matcher(nom.toString()).matches()) {
            trouves.add(chemin + " -> " + nom);
        }
    }

    private static Map<String, String> configuration() throws IOException {
        Map<String, String> aplatie = new LinkedHashMap<>();
        for (Object document : documents(APPLICATION_YML)) {
            aplatir(document, "", aplatie);
        }
        return aplatie;
    }

    /** Chaque document de chaque fichier à part : un document suivant ne masque pas une valeur. */
    private static List<Map.Entry<String, String>> toutesLesValeurs() throws IOException {
        List<Map.Entry<String, String>> valeurs = new ArrayList<>();
        try (var fichiers = Files.list(RESSOURCES)) {
            for (Path fichier : fichiers.filter(ContratsEtConfigurationTest::estConfiguration).toList()) {
                for (Object document : documents(fichier)) {
                    Map<String, String> aplatie = new LinkedHashMap<>();
                    aplatir(document, "", aplatie);
                    valeurs.addAll(aplatie.entrySet());
                }
            }
        }
        return valeurs;
    }

    private static void aplatir(Object noeud, String prefixe, Map<String, String> aplatie) {
        if (noeud instanceof Map<?, ?> table) {
            table.forEach((cle, valeur) ->
                    aplatir(valeur, prefixe.isEmpty() ? cle.toString() : prefixe + "." + cle, aplatie));
        } else if (noeud != null) {
            aplatie.put(prefixe, noeud.toString());
        }
    }
}
