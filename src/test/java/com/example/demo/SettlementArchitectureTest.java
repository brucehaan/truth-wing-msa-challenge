package com.example.demo;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.example.demo")
public class SettlementArchitectureTest {

    @ArchTest
    static final ArchRule 도메인은_자바_표준만_안다 =
            classes().that().resideInAPackage("..settlement.domain..")
                    .should().onlyDependOnClassesThat()
                    .resideInAnyPackage("..settlement.domain..", "java..");

    @ArchTest
    static final ArchRule 애플리케이션은_도메인과_자바_표준만_안다 =
            classes().that().resideInAPackage("..settlement.application..")
                    .should().onlyDependOnClassesThat()
                    .resideInAnyPackage("..settlement.domain..", "..settlement.application..", "java..");

    @ArchTest
    static final ArchRule 정산은_상류_내부를_모른다 =
            noClasses().that().resideInAPackage("..settlement..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..order.domain..", "..order.infrastructure..", "..payment.domain..", "..payment.infrastructure..");
}
