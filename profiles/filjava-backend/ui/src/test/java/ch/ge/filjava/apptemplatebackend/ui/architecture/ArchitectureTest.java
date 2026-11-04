package ch.ge.filjava.apptemplatebackend.ui.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.GeneralCodingRules;

import java.util.Set;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Principes du template rendus vérifiables sur le code de production. Le message de chaque règle
 * est la première chose que lit un développeur ou un agent : il donne la raison et l'exemple à
 * suivre. Une règle qui ne convient plus à ce projet se modifie ou se supprime ici.
 */
@AnalyzeClasses(
        packages = ArchitectureTest.RACINE,
        importOptions = ImportOption.DoNotIncludeTests.class
)
class ArchitectureTest {

    static final String RACINE = "ch.ge.filjava.apptemplatebackend";
    private static final String DOMAINE = RACINE + ".domain";
    private static final String INFRA = RACINE + ".infra";
    private static final String UI = RACINE + ".ui";

    // ---- Dépendances : tout pointe vers le domaine ----

    @ArchTest
    static final ArchRule domainePur = classes()
            .that().resideInAPackage(DOMAINE + "..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("java..", DOMAINE + "..")
            .because("le domaine ne connaît ni JPA, ni REST, ni Spring : "
                    + "tout besoin externe passe par un port (voir ProfilUtilisateurRepository)");

    @ArchTest
    static final ArchRule uiSansInfra = noClasses()
            .that().resideInAPackage(UI + "..")
            .should().dependOnClassesThat().resideInAPackage(INFRA + "..")
            .because("ui parle au domaine, jamais aux adaptateurs : "
                    + "ils sont trouvés par le scan Spring (voir InfrastructureConfiguration)");

    // ---- Organisation par fonctionnalité ----

    @ArchTest
    static final ArchRule pasDePackageParType = noClasses()
            .should().resideInAnyPackage(
                    "..model..", "..repository..", "..service..",
                    "..request..", "..response..", "..controller..")
            .because("le code est regroupé par fonctionnalité, pas par type technique "
                    + "(voir domain/profilutilisateur)");

    @ArchTest
    static final ArchRule toujoursUneFonctionnalite = noClasses()
            .should().resideInAnyPackage(DOMAINE, INFRA, UI + ".rest")
            .because("chaque classe vit dans le package de sa fonctionnalité "
                    + "(voir infra/profilutilisateur)");

    @ArchTest
    static final ArchRule premierNiveauDeUi = classes()
            .that().resideInAPackage(UI + "..")
            .should().resideInAnyPackage(
                    UI + ".rest..", UI + ".messaging..", UI + ".batch..",
                    UI + ".config..", UI + ".security..", UI + ".main..")
            .because("dans ui, le premier niveau indique le type d'interface ; "
                    + "config, security et main portent le démarrage de l'application");

    @ArchTest
    static final ArchRule fonctionnalitesDuDomaineSansCycle = slices()
            .matching(DOMAINE + ".(*)..")
            .should().beFreeOfCycles()
            .because("un cycle empêche de comprendre ou de retirer une fonctionnalité seule");

    @ArchTest
    static final ArchRule fonctionnalitesDeInfraSansCycle = slices()
            .matching(INFRA + ".(*)..")
            .should().beFreeOfCycles()
            .because("un cycle empêche de comprendre ou de retirer une fonctionnalité seule");

    @ArchTest
    static final ArchRule ressourcesSansCycle = slices()
            .matching(UI + ".rest.(*)..")
            .should().beFreeOfCycles()
            .because("un cycle empêche de comprendre ou de retirer une fonctionnalité seule");

    // ---- Persistance et adaptateurs ----

    @ArchTest
    static final ArchRule jpaDansInfra = noClasses()
            .that().resideOutsideOfPackage(INFRA + "..")
            .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..")
            .because("la persistance est un détail d'adaptateur "
                    + "(voir ProfilUtilisateurPersistenceAdapter)");

    @ArchTest
    static final ArchRule entitesNommees = classes()
            .that().areAnnotatedWith("jakarta.persistence.Entity")
            .should().haveSimpleNameEndingWith("Entity")
            .because("le suffixe distingue l'entité JPA de l'objet du domaine "
                    + "(voir ProfilUtilisateurEntity)");

    @ArchTest
    static final ArchRule adaptateursImplementantUnPort = classes()
            .that().resideInAPackage(INFRA + "..")
            .and().haveSimpleNameEndingWith("Adapter")
            .should().implement(resideInAPackage(DOMAINE + ".."))
            .because("un adaptateur implémente un port défini par le domaine "
                    + "(voir ProfilUtilisateurPersistenceAdapter)");

    @ArchTest
    static final ArchRule adaptateursCablesExplicitement = noClasses()
            .that().resideInAPackage(INFRA + "..")
            .and().resideOutsideOfPackage(INFRA + ".config..")
            .should().beMetaAnnotatedWith("org.springframework.stereotype.Component")
            .because("les adaptateurs sont déclarés par @Bean, ce qui garde le choix par profil "
                    + "visible à un seul endroit (voir InfrastructureConfiguration)");

    // ---- Exposition REST ----

    @ArchTest
    static final ArchRule ressourcesIssuesDuContrat = classes()
            .that().areAnnotatedWith("jakarta.ws.rs.Path")
            .and().areNotInterfaces()
            .should().resideInAPackage(UI + ".rest..")
            .andShould().haveSimpleNameEndingWith("Resource")
            .andShould().implement(resideInAPackage("..generated.api.."))
            .because("le contrat OpenAPI fait foi : une ressource implémente l'interface générée "
                    + "(voir ProfilUtilisateurResource)");

    @ArchTest
    static final ArchRule springMvcReserveALaSecurite = classes()
            .that().areMetaAnnotatedWith("org.springframework.stereotype.Controller")
            .should().resideInAPackage(UI + ".security..")
            .because("les ressources métier passent par CXF et JAX-RS ; "
                    + "seul AuthenticationResource utilise Spring MVC");

    // ---- Sécurité et temps ----

    @ArchTest
    static final ArchRule contexteDeSecuriteConfine = noClasses()
            .that().resideOutsideOfPackage(UI + ".security..")
            .should().dependOnClassesThat()
            .haveFullyQualifiedName("org.springframework.security.core.context.SecurityContextHolder")
            .because("l'identifiant utilisateur passe par le port UtilisateurCourantProvider "
                    + "(voir SecurityUtilisateurCourantProvider)");

    @ArchTest
    static final ArchRule tempsInjecte = classes()
            .that().resideOutsideOfPackage(UI + ".config..")
            .should(nePasLireLHorlogeSysteme())
            .because("le temps est injecté par un Clock pour rester testable ; "
                    + "seul ui.config crée l'horloge réelle (voir ProfilUtilisateurService)");

    // ---- Domaine immuable ----

    @ArchTest
    static final ArchRule domaineImmuable = fields()
            .that().areDeclaredInClassesThat().resideInAPackage(DOMAINE + "..")
            .should().beFinal()
            .because("un objet du domaine ne change pas après sa construction, "
                    + "où ses invariants sont vérifiés (voir ProfilUtilisateur)");

    // ---- Hygiène ----

    @ArchTest
    static final ArchRule pasDeSortieStandard = GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

    @ArchTest
    static final ArchRule pasDExceptionGenerique = GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;

    @ArchTest
    static final ArchRule pasDInjectionParChamp = GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

    @ArchTest
    static final ArchRule pasDeJavaUtilLogging = GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

    private static ArchCondition<JavaClass> nePasLireLHorlogeSysteme() {
        Set<String> horlogesSysteme = Set.of("system", "systemDefaultZone", "systemUTC");
        return new ArchCondition<>("ne pas lire l'horloge système") {
            @Override
            public void check(JavaClass classe, ConditionEvents evenements) {
                for (JavaMethodCall appel : classe.getMethodCallsFromSelf()) {
                    var cible = appel.getTarget();
                    boolean nowSansClock = cible.getOwner().getPackageName().equals("java.time")
                            && cible.getName().equals("now")
                            && cible.getRawParameterTypes().isEmpty();
                    boolean horlogeSysteme = cible.getOwner().isEquivalentTo(java.time.Clock.class)
                            && horlogesSysteme.contains(cible.getName());
                    if (nowSansClock || horlogeSysteme) {
                        evenements.add(SimpleConditionEvent.violated(appel, appel.getDescription()));
                    }
                }
            }
        };
    }
}
