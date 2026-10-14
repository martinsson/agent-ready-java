package com.example.wallet.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static com.tngtech.archunit.library.freeze.FreezingArchRule.freeze;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.GeneralCodingRules;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * Architecture as executable rules. A violation message is the first thing an agent reads, so every
 * rule says WHY it exists. Copy this file into an existing repo and adjust the packages.
 *
 * <p>Every rule is frozen: violations recorded in config/baseline/archunit are tolerated, any other
 * violation fails. In this template the store is empty, so every violation fails. In a legacy repo
 * run {@code ./verify --baseline} once and commit the store; it may only shrink afterwards.
 */
@AnalyzeClasses(packages = "com.example", importOptions = ImportOption.DoNotIncludeJars.class)
class ArchitectureTest {

  private static final int MAX_CLASSES_PER_PACKAGE = 15;

  // ---- Hexagonal layering: dependencies point inwards (adapter -> application -> domain) ----

  @ArchTest
  static final ArchRule domainDependsOnNothingOutward =
      freeze(
          noClasses()
              .that()
              .resideInAPackage("..wallet.domain..")
              .should()
              .dependOnClassesThat()
              .resideInAnyPackage(
                  "..wallet.application..",
                  "..wallet.adapter..",
                  "java.sql..",
                  "javax.sql..",
                  "org.postgresql..")
              .because(
                  "the domain is pure business logic: no ports, adapters or infrastructure (README)"));

  @ArchTest
  static final ArchRule applicationDoesNotKnowAdapters =
      freeze(
          noClasses()
              .that()
              .resideInAPackage("..wallet.application..")
              .should()
              .dependOnClassesThat()
              .resideInAPackage("..wallet.adapter..")
              .because("the application talks to the outside only through ports (README)"));

  @ArchTest
  static final ArchRule inboundAndOutboundAdaptersAreIndependent =
      freeze(
          noClasses()
              .that()
              .resideInAPackage("..adapter.in..")
              .should()
              .dependOnClassesThat()
              .resideInAPackage("..adapter.out..")
              .because("adapters are wired together by the application, never directly")
              .allowEmptyShould(true));

  @ArchTest
  static final ArchRule portsAreInterfaces =
      freeze(
          classes()
              .that()
              .resideInAPackage("..application.port..")
              .should()
              .beInterfaces()
              .because("ports are contracts; implementations live in adapters or in the service"));

  @ArchTest
  static final ArchRule noCyclesBetweenLayers =
      freeze(
          slices()
              .matching("com.example.wallet.(*)..")
              .should()
              .beFreeOfCycles()
              .because("cycles make the dependency direction ambiguous"));

  // ---- Size ----

  @ArchTest
  static final ArchRule packagesStaySmall =
      freeze(
          classes()
              .that()
              .areTopLevelClasses()
              .and()
              .haveSimpleNameNotEndingWith("Test")
              .and()
              .haveSimpleNameNotEndingWith("IT")
              .should(new PackageSizeCondition(MAX_CLASSES_PER_PACKAGE))
              .because("a package that keeps growing hides several concepts: split it by concept"));

  // ---- Testing principles ----

  @ArchTest
  static final ArchRule noMockito =
      freeze(
          noClasses()
              .should()
              .dependOnClassesThat()
              .resideInAPackage("org.mockito..")
              .because(
                  "we write in-memory fakes of ports instead of mocks (see InMemoryWalletRepository)"));

  // ---- General coding rules ----

  @ArchTest
  static final ArchRule noStandardStreams =
      freeze(GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS);

  @ArchTest
  static final ArchRule noGenericExceptions =
      freeze(GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS);

  @ArchTest
  static final ArchRule noFieldInjection =
      freeze(GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION);

  @ArchTest
  static final ArchRule noJavaUtilLogging =
      freeze(GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING);

  /** Counts the classes per package and reports each package over the limit once. */
  private static final class PackageSizeCondition extends ArchCondition<JavaClass> {
    private final int max;
    private final Map<String, Integer> classesPerPackage = new TreeMap<>();

    PackageSizeCondition(int max) {
      super("be in a package with at most " + max + " classes");
      this.max = max;
    }

    @Override
    public void init(Collection<JavaClass> allClasses) {
      classesPerPackage.clear();
    }

    @Override
    public void check(JavaClass javaClass, ConditionEvents events) {
      classesPerPackage.merge(javaClass.getPackageName(), 1, Integer::sum);
    }

    @Override
    public void finish(ConditionEvents events) {
      classesPerPackage.forEach(
          (packageName, count) -> {
            // No count in the message: a frozen violation is matched by its text.
            String message = "Package <" + packageName + "> has more than " + max + " classes";
            events.add(new SimpleConditionEvent(packageName, count <= max, message));
          });
    }
  }
}
