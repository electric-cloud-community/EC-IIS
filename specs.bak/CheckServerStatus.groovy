import spock.lang.*
import com.electriccloud.spec.*
import groovy.json.JsonSlurper

class CheckServerStatus extends PluginTestHelper {
    static def projectName = 'EC-IIS Specs CheckServerStatus'
    static def iisHandler
    static def procName = 'CheckServerStatus'

    def doSetupSpec() {
        dsl 'setProperty(propertyName: "/plugins/EC-IIS/project/ec_debug_logToProperty", value: "/myJob/debug_logs")'
        def resName = createIISResource()
        dslFile 'dsl/RunProcedure.dsl', [
            projName: projectName,
            resName: resName,
            procName: procName,
            params: [
                configname: '',
                usecredentials: '',
                checkUrl: '',
                expectStatus: '',
                unavailable: '',
                checkTimeout: '',
                checkRetries: '',
            ]
        ]
        createHelperProject(resName)
    }

    def doCleanupSpec() {
        if (!System.getenv('NO_CLEANUP')) {
            dsl "deleteProject '$projectName'"
        }
    }

    @Unroll
    def "valid site, #port"() {
        given:
            def siteName = "Site ${port}"
            createLivingSite(siteName, port)
        when: "procedure runs"
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        configname: '',
                        expectStatus: '200',
                        unavailable: '',
                        checkUrl: 'http://localhost:${port}',
                    ]
                )
            """
        then: 'it finishes'
            assert result.outcome == 'success'
            logger.debug(result.logs)
            assert result.logs =~ /URL successful/
        cleanup:
            removeSite(siteName)
        where:
            port << [81]
    }

    def "availability check"() {
        given:
            def port = 9999
            def siteName = "Site ${port}"
            createLivingSite(siteName, port)
        when: "procedure runs"
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        configname: '',
                        expectStatus: '200',
                        unavailable: '1',
                        checkUrl: 'http://localhost:${port}',
                    ]
                )
            """
        then: 'it finishes'
            assert result.outcome == 'error'
            logger.debug(result.logs)
            assert result.logs =~ /Server available at/
        cleanup:
            removeSite(siteName)
    }

    def "availability check, unavailable site"() {
        when: "procedure runs"
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        configname: '',
                        expectStatus: '200',
                        unavailable: '1',
                        checkUrl: 'http://localhost:991',
                    ]
                )
            """
        then: 'it finishes'
            assert result.outcome == 'success'
            logger.debug(result.logs)

    }

    def "timeout, retries"() {
        given:
            def port = 9999
            def siteName = "Site $port"
            createLivingSite(siteName, port)
            def appPoolName = siteName
            createAppPool(appPoolName)
            moveAppToPool(siteName, '', appPoolName)
            stopAppPool(appPoolName)
        when: "procedure runs"
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        configname: '',
                        expectStatus: '200',
                        checkTimeout: '60',
                        checkRetries: '3',
                        checkUrl: 'http://localhost:${port}',
                    ]
                )
            """
        then:
            assert result.outcome == 'error'
            logger.debug(result.logs)
            assert result.logs =~ /Using timeout: 60 seconds/
            assert result.logs =~ /Retries left: 2/
        cleanup:
            removeSite(siteName)
            removeAppPool(appPoolName)
    }

    def "parameters from configuration"() {
        given:
            def port = 9999
            def siteName = "Site $port"
            createLivingSite(siteName, port)
            def config = randomize('config')
            createConfiguration(config, 'http://localhost', port, username, password)
        when: "procedure runs"
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        configname: '$config',
                    ]
                )
            """
        then:
            assert result.outcome == 'success'
            logger.debug(result.logs)
        cleanup:
            removeSite(siteName)
        where:
            username << ['', 'test']
            password << ['', 'test']

    }

    def 'non-existing configuration'() {
        when: "procedure runs"
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        configname: 'no config',
                    ]
                )
            """
        then:
            assert result.outcome == 'error'
            logger.debug(result.logs)
            assert result.logs =~ /does not exist/
    }

    def createLivingSite(siteName, port) {
        createSite(siteName, "http://*:${port}", /C:\\inetpub\\wwwroot/)
    }

    def createConfiguration(configName, url, port, username = '', password = '')  {
        createPluginConfiguration(
            'EC-IIS',
            configName,
            [iis_url: url, iis_port: port],
            username,
            password,
            [:]
        )
    }

}
