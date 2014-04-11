package com.clario.swift.code

import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer
import org.codehaus.groovy.control.customizers.SecureASTCustomizer
import org.codehaus.groovy.control.customizers.builder.CompilerCustomizationBuilder

import static org.codehaus.groovy.syntax.Types.*

/**
 * @author George Coller
 */
public class GroovyNode {

    public static void main(String[] args) {
        def code = '''
            row.apple = 'apple'
            row['bacon and beans'] = 'bacon'
            row['balls'] = sin(33) * abs(52) + XXX
       '''

        def config = new CompilerConfiguration()
        config.addCompilationCustomizers(createImportCustomizer(), createSecureAstCustomizer())
        def shell = new GroovyShell(config)
        def row = [:]
        shell.setVariable('row', row)
        shell.setVariable('XXX', 666)
        shell.evaluate(code)
        println row
    }

    def static ImportCustomizer createImportCustomizer() {
        def icz = new ImportCustomizer()
        icz.addStaticStars('java.lang.Math')
        return icz
    }

    def static SecureASTCustomizer createSecureAstCustomizer() {
        def scz = new SecureASTCustomizer()
        scz.with {
            closuresAllowed = false // user will not be able to write closures
            methodDefinitionAllowed = false // user will not be able to define methods
            importsWhitelist = [] // empty whitelist means imports are disallowed
            staticImportsWhitelist = [] // same for static imports
            staticStarImportsWhitelist = ['java.lang.Math'] // only java.lang.Math is allowed
            // the list of tokens the user can find
            // constants are defined in org.codehaus.groovy.syntax.Types
            tokensWhitelist = [
                    LEFT_SQUARE_BRACKET,
                    RIGHT_SQUARE_BRACKET,
                    PLUS,
                    MINUS,
                    MULTIPLY,
                    DIVIDE,
                    MOD,
                    POWER,
                    EQUALS,
                    PLUS_PLUS,
                    MINUS_MINUS,
                    COMPARE_EQUAL,
                    COMPARE_NOT_EQUAL,
                    COMPARE_LESS_THAN,
                    COMPARE_LESS_THAN_EQUAL,
                    COMPARE_GREATER_THAN,
                    COMPARE_GREATER_THAN_EQUAL,
            ].asImmutable()
            // limit the types of constants that a user can define to number types only
            constantTypesClassesWhiteList = [
                    Object,
                    Date,
                    String,
                    Integer,
                    Float,
                    Long,
                    Double,
                    BigDecimal,
                    Integer.TYPE,
                    Long.TYPE,
                    Float.TYPE,
                    Double.TYPE
            ].asImmutable()
            // method calls are only allowed if the receiver is of one of those types
            // be careful, it's not a runtime type!
            receiversClassesWhiteList = [
                    Object,
                    Math,
//                    Integer,
//                    Float,
                    Double,
//                    Long,
//                    BigDecimal
            ].asImmutable()
        }
        return scz
    }

}