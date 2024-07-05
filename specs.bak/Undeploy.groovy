import spock.lang.*
import com.electriccloud.spec.*

class Undeploy extends PluginTestHelper {
    static def projectName = 'EC-IIS Specs Undeploy'
    static def netDashUrl = 'https://github.com/electric-cloud/NetDash/archive/master.zip'
    static def procName = 'Undeploy'

    def doSetupSpec() {
        dsl 'setProperty(propertyName: "/plugins/EC-IIS/project/ec_debug_logToProperty", value: "/myJob/debug_logs")'
        def resName = createIISResource()
        dslFile 'dsl/RunProcedure.dsl', [
            projName: projectName,
            resName: resName,
            procName: procName,
            params: [
                applicationName: '',
                websiteName: '',
                deleteVirtualDirectories: '',
                msdeployPath: 'msdeploy.exe',
                strictMode: '',
            ]
        ]
        createHelperProject(resName)
        dslFile 'dsl/downloadArtifact.dsl', [
            projName: projectName,
            resName: resName
        ]

    }

    def doCleanupSpec() {
        dsl "deleteProject '$projectName'"
    }

    def "undeploy site"() {
        given:
            def siteName = randomize('SiteName')
            createSite(siteName)
        when:
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        websiteName: '$siteName',
                        msdeployPath: 'msdeploy.exe',
                    ]
                )
            """
        then:
            assert result.outcome == 'success'
            logger.debug(result.logs)
            def site = getSite(siteName)
            assert site
        cleanup:
            removeSite(siteName)
    }

    def "undeploy application"() {
        given:
            def siteName = randomize('SiteName')
            createSite(siteName)
            def appName = 'app'
            createApp(siteName, appName)
        when:
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        websiteName: '$siteName',
                        msdeployPath: 'msdeploy.exe',
                        applicationName: '$appName'
                    ]
                )
            """
        then:
            assert result.outcome == 'success'
            logger.debug(result.logs)
            def site = getSite(siteName)
            assert site
        cleanup:
            removeSite(siteName)
    }

    def "undeploy application, delete virtual directories"() {
        given:
            def siteName = randomize('SiteName')
            createSite(siteName)
            def appName = 'app'
            createApp(siteName, appName)
        when:
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        websiteName: '$siteName',
                        msdeployPath: 'msdeploy.exe',
                        applicationName: '$appName',
                        deleteVirtualDirectories: '1'
                    ]
                )
            """
        then:
            assert result.outcome == 'success'
            logger.debug(result.logs)
            def site = getSite(siteName)
            assert result.logs =~ /Deleting virtualDirectory/
            assert site
        cleanup:
            removeSite(siteName)
    }

    def "strict mode #strictMode"() {
        given:
            def siteName = 'No Such Site'
        when:
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        websiteName: '$siteName',
                        msdeployPath: 'msdeploy.exe',
                        strictMode: '$strictMode'
                    ]
                )
            """
        then:
            if (strictMode == '1') {
                assert result.outcome == 'error'
            }
            else {
                assert result.outcome == 'warning'
            }
        where:
            strictMode << ['1', '0']
    }

    def "strict mode #strictMode, website exists, application does not"() {
        given:
            def siteName = randomize('siteName')
            createSite(siteName)
        when:
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        websiteName: '$siteName',
                        msdeployPath: 'msdeploy.exe',
                        strictMode: '$strictMode',
                        applicationName: 'no such app'
                    ]
                )
            """
        then:
            if (strictMode == '1') {
                assert result.outcome == 'error'
            }
            else {
                assert result.outcome == 'warning'
            }
        where:
            strictMode << ['1', '0']
    }

    def uploadArtifact(url, artifactPath) {
        def res = dsl """
            runProcedure(
                projectName: '$projectName',
                procedureName: 'Download And Unpack',
                actualParameter: [
                    url: '$url',
                    artifactPath: '$artifactPath'
                ]
            )
        """
        assert res.jobId
        waitUntil {
            jobCompleted res.jobId
        }

        jobSucceeded(res.jobId)
    }

}
