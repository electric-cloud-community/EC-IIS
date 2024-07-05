import spock.lang.*
import com.electriccloud.spec.*

class RecycleAppPool extends PluginTestHelper {
    static def projectName = 'EC-IIS Specs RecycleAppPool'
    static def iisHandler
    static def procName = 'RecycleAppPool'

    def doSetupSpec() {
        dsl 'setProperty(propertyName: "/plugins/EC-IIS/project/ec_debug_logToProperty", value: "/myJob/debug_logs")'
        def resName = createIISResource()
        dslFile 'dsl/RunProcedure.dsl', [
            projName: projectName,
            resName: resName,
            procName: procName,
            params: [
                applicationPool: '',
            ]
        ]
        createHelperProject(resName)
    }

    def doCleanupSpec() {
        dsl "deleteProject '$projectName'"
    }

    def "recycle app pool"() {
        given:
            def appPoolName = randomize('appPool')
            createAppPool(appPoolName)
        when: "procedure runs"
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        applicationPool: '$appPoolName',
                    ]
                )
            """
        then: 'it finishes'
            assert result.outcome == 'success'
        cleanup:
            removeAppPool(appPoolName)
    }

    def "recycle non-existing app pool"() {
        when: "procedure runs"
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        applicationPool: 'deadpool',
                    ]
                )
            """
        then: 'it finishes'
            assert result.outcome == 'error'

    }
}
