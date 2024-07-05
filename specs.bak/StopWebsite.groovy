import spock.lang.*
import com.electriccloud.spec.*

class StopWebsite extends PluginTestHelper {
    static def projectName = 'EC-IIS Specs StopWebsite'
    static def iisHandler

    def doSetupSpec() {
        dsl 'setProperty(propertyName: "/plugins/EC-IIS/project/ec_debug_logToProperty", value: "/myJob/debug_logs")'
        def resName = createIISResource()
        dslFile 'dsl/StopSite/StopSite.dsl', [
            projName: projectName,
            resName: resName
        ]
        createHelperProject(resName)
    }

    def doCleanupSpec() {
        dsl "deleteProject '$projectName'"
    }


    def "stop started site"() {
        given: 'the site is created'
            def siteName = 'TestSite'
            def bindings = 'http://*:9999'
            createSite(siteName, bindings)
            startSite(siteName)
        when: 'procedure runs'
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: 'Stop Site',
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

    def "stop stopped site"() {
        given: 'the site is created'
            def siteName = 'TestSite'
            def bindings = 'http://*:9999'
            createSite(siteName, bindings)
            stopSite(siteName)
        when: 'procedure runs'
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: 'Stop Site',
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

    def "negative: stop non-existing site"() {
        when: 'procedure runs'
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: 'Stop Site',
                    actualParameter: [
                        sitename: 'SomeSite'
                    ]
                )
            """
        then: 'procedure succeeds'
            assert result.outcome == 'error'
            logger.debug(result.logs)
    }


}
