import spock.lang.*
import com.electriccloud.spec.*

class DeleteVirtualDirectory extends PluginTestHelper {
    static def projectName = 'EC-IIS Specs DeleteVirtualDirectory'
    static def iisHandler
    static def procName = 'DeleteVirtualDirectory'

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

    def "delete existing vdir, strict: #strictMode"() {
        given:
            def appName = 'app'
            def siteName = randomize('site')
            createSite(siteName)
            createApp(siteName, appName)
        when: "procedure runs"
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        appname: '$siteName/$appName/',
                        strictMode: '$strictMode'
                    ]
                )
            """
        then: 'it finishes'
            assert result.outcome == 'success'
            logger.debug(result.logs)
        cleanup:
            removeSite(siteName)
        where:
            strictMode << ['1', '0']
    }

    def "delete non-existing vdir, strict: #strictMode"() {
        when: "procedure runs"
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        appname: 'some_dir',
                        strictMode: '$strictMode'
                    ]
                )
            """
        then: 'it finishes'
            assert result.logs =~ /does not exist/
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
