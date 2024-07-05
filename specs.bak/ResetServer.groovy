import spock.lang.*
import com.electriccloud.spec.*
import groovy.json.JsonSlurper

class ResetServer extends PluginTestHelper {
    static def projectName = 'EC-IIS Specs ResetServer'
    static def iisHandler
    static def procName = 'ResetServer'

    def doSetupSpec() {
        dsl 'setProperty(propertyName: "/plugins/EC-IIS/project/ec_debug_logToProperty", value: "/myJob/debug_logs")'
        def resName = createIISResource()
        dslFile 'dsl/RunProcedure.dsl', [
            projName: projectName,
            resName: resName,
            procName: procName,
            params: [
                additionalParams: '',
                execpath: '',
            ]
        ]
        createHelperProject(resName)
    }

    def doCleanupSpec() {
        restoreIISService()
        dsl "deleteProject '$projectName'"
    }

    @Unroll
    def "reset stopped server"() {
        given:
            stopServer()
        when: "procedure runs"
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        additionalParams: '',
                        execpath: 'iisreset'
                    ]
                )
            """
        then: 'it finishes'
            assert result.outcome == 'success'
            def status = serverStatus()
            assert status =~ /Status for World Wide Web Publishing Service \( W3SVC \) : Running/

    }

    @Unroll
    def "reset started server"() {
        given:
            startServer()
        when: "procedure runs"
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        additionalParams: '',
                        execpath: 'iisreset'
                    ]
                )
            """
        then: 'it finishes'
            assert result.outcome == 'success'
            def status = serverStatus()
            assert status =~ /Status for World Wide Web Publishing Service \( W3SVC \) : Running/

    }

    @Unroll
    def "reset server, timeout"() {
        given:
            stopServer()
        when: "procedure runs"
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        additionalParams: '/TIMEOUT:5',
                        execpath: 'iisreset'
                    ]
                )
            """
        then: 'it finishes'
            assert result.outcome == 'success'

    }

}
