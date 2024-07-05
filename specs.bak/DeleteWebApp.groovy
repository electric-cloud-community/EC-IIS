import spock.lang.*
import com.electriccloud.spec.*

class DeleteWebApplication extends PluginTestHelper {
    static def projectName = 'EC-IIS Specs DeleteWebApplication'
    static def iisHandler
    static def procName = 'DeleteWebApplication'

    def doSetupSpec() {
        dsl 'setProperty(propertyName: "/plugins/EC-IIS/project/ec_debug_logToProperty", value: "/myJob/debug_logs")'
        def resName = createIISResource()
        dslFile 'dsl/RunProcedure.dsl', [
            projName: projectName,
            resName: resName,
            procName: procName,
            params: [
                appname: '',
                strictMode: ''
            ]
        ]
        createHelperProject(resName)
    }

    def doCleanupSpec() {
        dsl "deleteProject '$projectName'"
    }

    def "delete existing app, strict: #strictMode"() {
        given:
            def siteName = randomize('site')
            def appName = 'app'
            createSite(siteName)
            createApp(siteName, appName)
        when: "procedure runs"
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        appname: '$siteName/$appName',
                        strictMode: '$strictMode'
                    ]
                )
            """
        then:
            assert result.outcome == 'success'
            logger.debug(result.logs)
        cleanup:
            removeSite(siteName)
        where:
            strictMode << ['1', '0']
    }

    def "delete non-existing app, strict: #strictMode"() {
        when: "procedure runs"
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        appname: 'some_app',
                        strictMode: '$strictMode'
                    ]
                )
            """
        then: 'it finishes'
            if (strictMode == '1') {
                assert result.outcome == 'error'
            }
            else {
                assert result.outcome == 'warning'
            }
        where:
            strictMode << ['1', '0']
    }
}
