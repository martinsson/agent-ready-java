package ch.ge.filjava.apptemplatebackend.ui.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/** Règles qui portent aussi sur le code de test, contrairement à {@link ArchitectureTest}. */
@AnalyzeClasses(packages = ArchitectureTest.RACINE)
class ArchitectureDesTestsTest {

    private static final String RACINE = ArchitectureTest.RACINE;

    @ArchTest
    static final ArchRule cucumberDansBdd = noClasses()
            .that().resideOutsideOfPackage(RACINE + ".ui.bdd..")
            .should().dependOnClassesThat().resideInAPackage("io.cucumber..")
            .because("les étapes Cucumber vivent dans ui/bdd/<fonctionnalité> "
                    + "(voir ProfilUtilisateurSteps)");

    @ArchTest
    static final ArchRule mockitoLimiteAInfra = noClasses()
            .that().resideOutsideOfPackage(RACINE + ".infra..")
            .should().dependOnClassesThat().resideInAPackage("org.mockito..")
            .because("le domaine et les scénarios se testent avec un faux dépôt derrière le port "
                    + "(voir ProfilUtilisateurServiceTest)");
}
