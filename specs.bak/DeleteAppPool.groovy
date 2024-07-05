import spock.lang.*
import com.electriccloud.spec.*

class DeleteAppPool extends PluginTestHelper {
    static def projectName = 'EC-IIS Specs DeleteAppPool'
    static def iisHandler
    static def procName = 'DeleteAppPool'

    def doSetupSpec() {
        dsl 'setProperty(propertyName: "/plugins/EC-IIS/project/ec_debug_logToProperty", value: "/myJob/debug_logs")'
        def resName = createIISResource()
        dslFile 'dsl/RunProcedure.dsl', [
            projName: projectName,
            resName: resName,
            procName: procName,
            params: [
                apppoolname: '',
                strictMode: ''
            ]
        ]
        createHelperProject(resName)
    }

    def doCleanupSpec() {
        dsl "deleteProject '$projectName'"
    }

    def "delete existing app pool, strict: #strictMode"() {
        given:
            def appPoolName = randomize('appPool')
            createAppPool(appPoolName)
        when: "procedure runs"
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        apppoolname: '$appPoolName',
                        strictMode: '$strictMode'
                    ]
                )
            """
        then: 'it finishes'
            assert result.outcome == 'success'
            logger.debug(result.logs)
            assert result.logs =~ / APPPOOL object "$appPoolName" deleted/
        where:
            strictMode << ['1', '0']
    }

    def "delete non-existing app pool, strict: #strictMode"() {
        when: "procedure runs"
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        apppoolname: 'deadpool',
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
            logger.debug(result.logs)
        where:
            strictMode << ['1', '0']
    }
}
