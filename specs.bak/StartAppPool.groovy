import spock.lang.*
import com.electriccloud.spec.*

class StartAppPool extends PluginTestHelper {
    static def projectName = 'EC-IIS Specs StartAppPool'
    static def iisHandler
    static def procName = 'StartAppPool'

    def doSetupSpec() {
        dsl 'setProperty(propertyName: "/plugins/EC-IIS/project/ec_debug_logToProperty", value: "/myJob/debug_logs")'
        def resName = createIISResource()
        dslFile 'dsl/RunProcedure.dsl', [
            projName: projectName,
            resName: resName,
            procName: procName,
            params: [
                apppoolname: '',
            ]
        ]
        createHelperProject(resName)
    }

    def doCleanupSpec() {
        dsl "deleteProject '$projectName'"
    }

    def "start app pool"() {
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
                    ]
                )
            """
        then: 'it finishes'
            assert result.outcome == 'success'
            def appPool = getAppPool(appPoolName)
            assert appPool.state == 'Started'
        cleanup:
            removeAppPool(appPoolName)
    }

    def "start running app pool"() {
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
            assert appPool.state == 'Started'
        cleanup:
            removeAppPool(appPoolName)
    }


    def "negative: start non-existing app pool"() {
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
