package com.jurisfacil.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.jurisfacil")
class ArchitectureTest {

        @ArchTest
        static final ArchRule domainMustNotDependOnFrameworks = noClasses()
                        .that().resideInAnyPackage("com.jurisfacil..model.domain..")
                        .should().dependOnClassesThat().resideInAnyPackage(
                                        "org.springframework..",
                                        "jakarta.persistence..",
                                        "com.bucket4j..")
                        .allowEmptyShould(true);
}
