import spock.lang.*
import com.electriccloud.spec.*

class DeployAdvanced extends PluginTestHelper {
    static def projectName = 'EC-IIS Specs DeployAdvanced'
    static def netDashUrl = 'https://github.com/electric-cloud/NetDash/archive/master.zip'
    static def procName = 'DeployAdvanced'

    def doSetupSpec() {
        dsl 'setProperty(propertyName: "/plugins/EC-IIS/project/ec_debug_logToProperty", value: "/myJob/debug_logs")'
        def resName = createIISResource()
        dslFile 'dsl/RunProcedure.dsl', [
            projName: projectName,
            resName: resName,
            procName: procName,
            params: [
                verb: '',
                sourceProvider: '',
                sourceProviderSettings: '',
                destProvider: '',
                destProviderObjectPath: '',
                sourceProviderObjectPath: '',
                destProviderSettings: '',
                allowUntrusted: '',
                msdeployPath: 'msdeploy.exe',
                preSync: '',
                postSync: '',
                additionalOptions: '',
                setParamFile: '',
                declareParamFile: ''
            ]
        ]
        createHelperProject(resName)
        dslFile 'dsl/downloadArtifact.dsl', [
            projName: projectName,
            resName: resName
        ]

    }

    def doCleanupSpec() {
        // dsl "deleteProject '$projectName'"
    }


    // C259515
    def "deploy application into root app"() {
        given: "application is downloaded to the machine"
            createDir('c:/tmp')
            uploadArtifact(netDashUrl, 'c:/tmp/NetDash.zip')
            def siteName = randomize('NetDash')
            def sitePath = "c:/tmp/site_${siteName}"
            createDir(sitePath)
            createSite(siteName, 'http://*:8888', sitePath)
        when: 'the deploy runs'
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        sourceProviderObjectPath: 'c:/tmp/NetDash-master/Insya.NetDash',
                        msdeployPath: 'msdeploy.exe',
                        verb: 'sync',
                        destProvider: 'iisApp',
                        destProviderObjectPath: '$siteName/',
                        sourceProvider: 'iisApp'
                    ]
                )
            """
        then:
            assert result.outcome == 'success'
            logger.debug(result.logs)
        cleanup:
            removeSite(siteName)
    }


    // C259515
    def "delete application"() {
        given: "application is downloaded to the machine"
            createDir('c:/tmp')
            uploadArtifact(netDashUrl, 'c:/tmp/NetDash.zip')
            def siteName = randomize('NetDash')
            def sitePath = "c:/tmp/site_${siteName}"
            createDir(sitePath)
            createSite(siteName, 'http://*:8888', sitePath)
        when: 'the deploy runs'
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        msdeployPath: 'msdeploy.exe',
                        verb: 'delete',
                        destProvider: 'iisApp',
                        destProviderObjectPath: '$siteName/',
                    ]
                )
            """
        then:
            assert result.outcome == 'success'
            logger.debug(result.logs)
        cleanup:
            removeSite(siteName)
    }


    // C259515
    def "dump app pool config"() {
        when: 'the deploy runs'
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        msdeployPath: 'msdeploy.exe',
                        verb: 'dump',
                        sourceProvider: 'appPoolConfig',
                    ]
                )
            """
        then:
            assert result.outcome == 'success'
            logger.debug(result.logs)
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
