import spock.lang.*
import com.electriccloud.spec.*

class StopAppPool extends PluginTestHelper {
    static def projectName = 'EC-IIS Specs StopAppPool'
    static def iisHandler
    static def procName = 'StopAppPool'

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
        // dsl "deleteProject '$projectName'"
    }

    def "stop app pool"() {
        given:
            def appPoolName = randomize('appPool')
            createAppPool(appPoolName)
            startAppPool(appPoolName)
        when: "procedure runs"
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        apppoolname: '$appPoolName',
                    ]
                )
            """
        then: 'it finishes'
            assert result.outcome == 'success'
            def appPool = getAppPool(appPoolName)
            assert appPool.state == 'Stopped'
        cleanup:
            removeAppPool(appPoolName)
    }

    @Unroll
    def "stop stopped app pool #strictMode"() {
        given:
            def appPoolName = randomize('appPool')
            createAppPool(appPoolName)
            stopAppPool(appPoolName)
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
            if (strictMode == '1') {
                assert result.outcome == 'error'
            }
            else {
                assert result.outcome == 'warning'
            }
            def appPool = getAppPool(appPoolName)
            assert appPool.state == 'Stopped'
        cleanup:
            removeAppPool(appPoolName)
        where:
            strictMode << ['1', '0']
    }


    def "negative: stop non-existing app pool"() {
        when: "procedure runs"
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        apppoolname: 'deadpool',
                    ]
                )
            """
        then: 'it finishes'
            assert result.outcome == 'error'
    }

}
