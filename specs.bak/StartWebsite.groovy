import spock.lang.*
import com.electriccloud.spec.*

class StartWebsite extends PluginTestHelper {
    static def projectName = 'EC-IIS Specs StartWebSite'
    static def iisHandler

    def doSetupSpec() {
        dsl 'setProperty(propertyName: "/plugins/EC-IIS/project/ec_debug_logToProperty", value: "/myJob/debug_logs")'
        def resName = createIISResource()
        dslFile 'dsl/StartSite/StartSite.dsl', [
            projName: projectName,
            resName: resName
        ]
        createHelperProject(resName)
    }

    def doCleanupSpec() {
        dsl "deleteProject '$projectName'"
    }


    def "start stopped site"() {
        given: 'the site is created'
            def siteName = 'TestSite'
            def bindings = 'http://*:9999'
            createSite(siteName, bindings)
            stopSite(siteName)
        when: 'procedure runs'
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: 'Start Site',
                    actualParameter: [
                        sitename: '$siteName',
                    ]
                )
            """
        then: 'procedure succeeds'
            assert result.outcome == 'success'
            logger.debug(result.logs)
        cleanup:
            removeSite(siteName)
    }

    def "start running site"() {
        given: 'the site is created'
            def siteName = 'TestSite'
            def bindings = 'http://*:9999'
            createSite(siteName, bindings)
            startSite(siteName)
        when: 'procedure runs'
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: 'Start Site',
                    actualParameter: [
                        sitename: '$siteName',
                    ]
                )
            """
        then: 'procedure succeeds'
            assert result.outcome == 'success'
            logger.debug(result.logs)
        cleanup:
            removeSite(siteName)
    }

    def "negative: start non-existing site"() {
        when: 'procedure runs'
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: 'Start Site',
                    actualParameter: [
                        sitename: 'SomeSite'
                    ]
                )
            """
        then: 'procedure succeeds'
            assert result.outcome == 'error'
            logger.debug(result.logs)
    }

    def "negative: start site when the port is already used"() {
        given: 'a site'
            def bindings = 'http://*:9090'
            createSite('MySite', bindings)
            createSite('AnotherSite', bindings)
            startSite('MySite')
            stopSite('AnotherSite')

        when: 'procedure runs'
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: 'Start Site',
                    actualParameter: [
                        sitename: 'AnotherSite'
                    ]
                )
            """
        then: 'procedure succeeds'
            assert result.outcome == 'error'
            logger.debug(result.logs)
            assert result.logs =~ /Cannot create a file when that file already exists/
        cleanup:
            removeSite('AnotherSite')
            removeSite('MySite')
    }

}
