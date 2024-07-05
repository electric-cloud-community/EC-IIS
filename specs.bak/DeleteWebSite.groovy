import spock.lang.*
import com.electriccloud.spec.*

class DeleteWebSite extends PluginTestHelper {
    static def projectName = 'EC-IIS Specs DeleteWebSite'
    static def iisHandler
    static def procName = 'DeleteWebSite'

    def doSetupSpec() {
        dsl 'setProperty(propertyName: "/plugins/EC-IIS/project/ec_debug_logToProperty", value: "/myJob/debug_logs")'
        def resName = createIISResource()
        dslFile 'dsl/RunProcedure.dsl', [
            projName: projectName,
            resName: resName,
            procName: procName,
            params: [
                websitename: '',
                strictMode: ''
            ]
        ]
        createHelperProject(resName)
    }

    def doCleanupSpec() {
        dsl "deleteProject '$projectName'"
    }

    def "delete existing website, strict: #strictMode"() {
        given:
            def siteName = randomize('site')
            createSite(siteName)
        when: "procedure runs"
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        websitename: '$siteName',
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

    def "delete non-existing website, strict: #strictMode"() {
        when: "procedure runs"
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        websitename: 'some_site',
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
