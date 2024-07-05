import spock.lang.*
import com.electriccloud.spec.*

class Deploy extends PluginTestHelper {
    static def projectName = 'EC-IIS Specs Deploy'
    static def netDashUrl = 'https://github.com/electric-cloud/NetDash/archive/master.zip'
    static def procName = 'Deploy'

    def doSetupSpec() {
        dsl 'setProperty(propertyName: "/plugins/EC-IIS/project/ec_debug_logToProperty", value: "/myJob/debug_logs")'
        def resName = createIISResource()
        dslFile 'dsl/RunProcedure.dsl', [
            projName: projectName,
            resName: resName,
            procName: procName,
            params: [
                applicationPath: '',
                applicationPool: '',
                appPoolAdditionalSettings: '',
                autoStart: '',
                enable32BitAppOnWin64: '',
                managedPipelineMode: '',
                managedRuntimeVersion: '',
                msdeployPath: 'msdeploy.exe',
                queueLength: '',
                source: '',
                websiteName: '',
                additionalOptions: ''
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
                        source: 'c:/tmp/NetDash-master/Insya.NetDash',
                        websiteName: '$siteName',
                        msdeployPath: 'msdeploy.exe',
                        applicationPath: '',
                    ]
                )
            """
        then:
            assert result.outcome == 'success'
            logger.debug(result.logs)
        cleanup:
            removeSite(siteName)
    }


    // C259516
    def "deploy application into app"() {
        given: "application is downloaded to the machine"
            createDir('c:/tmp')
            uploadArtifact(netDashUrl, 'c:/tmp/NetDash.zip')
            def siteName = randomize('NetDash')
            def sitePath = "c:/site_${siteName}"
            createDir(sitePath)
            createSite(siteName, 'http://*:8888', sitePath)
        when: 'the deploy runs'
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        source: 'c:/tmp/NetDash-master/Insya.NetDash',
                        websiteName: '$siteName',
                        msdeployPath: 'msdeploy.exe',
                        applicationPath: 'NetDash'
                    ]
                )
            """
        then:
            assert result.outcome == 'success'
            logger.debug(result.logs)
            def app = getApp(siteName, 'NetDash')
            logger.debug(objectToJson(app))
            assert app
            assert app.applicationPool == 'DefaultAppPool'
            // TODO build and check url
        cleanup:
            removeSite(siteName)
    }


    // C259517
    def "deploy application into non-default app pool"() {
        given: "application is downloaded to the machine"
            createDir('c:/tmp')
            uploadArtifact(netDashUrl, 'c:/tmp/NetDash.zip')
            def siteName = randomize('NetDash')
            def sitePath = "c:/site_${siteName}"
            createDir(sitePath)
            createSite(siteName, 'http://*:8888', sitePath)
        when: 'the deploy runs'
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        source: 'c:/tmp/NetDash-master/Insya.NetDash',
                        websiteName: '$siteName',
                        msdeployPath: 'msdeploy.exe',
                        applicationPath: 'NetDash',
                        applicationPool: 'NetDashPool'
                    ]
                )
            """
        then:
            assert result.outcome == 'success'
            logger.debug(result.logs)
            def app = getApp(siteName, 'NetDash')
            logger.debug(objectToJson(app))
            assert app
            assert app.applicationPool == 'NetDashPool'
            // TODO build and check url
        cleanup:
            removeSite(siteName)
            removeAppPool('NetDashPool')
    }


    // C259518
    @Unroll
    def "deploy application #appName and change app pool settings"() {
        given: "application is downloaded to the machine"
            createDir('c:/tmp')
            uploadArtifact(netDashUrl, 'c:/tmp/NetDash.zip')
            def siteName = randomize('NetDash')
            def sitePath = "c:/site_${siteName}"
            createDir(sitePath)
            createSite(siteName, 'http://*:8888', sitePath)
            def appPoolName = randomize('NetDashPool')
        when: 'the deploy runs'
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        source: 'c:/tmp/NetDash-master/Insya.NetDash',
                        websiteName: '$siteName',
                        msdeployPath: 'msdeploy.exe',
                        applicationPath: '$appName',
                        applicationPool: '$appPoolName',
                        managedRuntimeVersion: 'v2.0',
                        managedPipelineMode: 'Classic',
                        enable32BitAppOnWin64: 'true',
                        autoStart: 'false',
                        queueLength: '10'

                    ]
                )
            """
        then:
            assert result.outcome == 'success'
            logger.debug(result.logs)
            def app = getApp(siteName, appName)
            logger.debug(objectToJson(app))
            assert app
            assert app.applicationPool == appPoolName
            def appPool = getAppPool(appPoolName, true)
            logger.debug(objectToJson(appPool))
            // TODO build and check url
            assert appPool.mgdVersion == 'v2.0'
            assert appPool.mgdMode == 'Classic'
            logger.debug(appPool.details)
            assert appPool.details =~ /autoStart:"false"/
            assert appPool.details =~ /enable32BitAppOnWin64:"true"/
            assert appPool.details =~ /queueLength:"10"/

        cleanup:
            removeSite(siteName)
            removeAppPool(appPoolName)
        where:
            appName << ['', 'NetDash', '/myApp']
    }


    // C259519
    def "deploy application and change settings of existing app pool"() {
        given: "application is downloaded to the machine"
            createDir('c:/tmp')
            uploadArtifact(netDashUrl, 'c:/tmp/NetDash.zip')
            def siteName = randomize('NetDash')
            def sitePath = "c:/site_${siteName}"
            createDir(sitePath)
            createSite(siteName, 'http://*:8888', sitePath)
            def appPoolName = randomize('NetDashPool')
            createAppPool(appPoolName)
            def appName = 'NetDash'
        when: 'the deploy runs'
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        source: 'c:/tmp/NetDash-master/Insya.NetDash',
                        websiteName: '$siteName',
                        msdeployPath: 'msdeploy.exe',
                        applicationPath: '$appName',
                        applicationPool: '$appPoolName',
                        managedRuntimeVersion: 'v2.0',
                        managedPipelineMode: 'Classic',
                        enable32BitAppOnWin64: 'true',
                        autoStart: 'false',
                        queueLength: '10',
                    ]
                )
            """
        then:
            assert result.outcome == 'success'
            logger.debug(result.logs)
            def app = getApp(siteName, appName)
            logger.debug(objectToJson(app))
            assert app
            assert app.applicationPool == appPoolName
            def appPool = getAppPool(appPoolName, true)
            logger.debug(objectToJson(appPool))
            // TODO build and check url
            assert appPool.mgdVersion == 'v2.0'
            assert appPool.mgdMode == 'Classic'
            logger.debug(appPool.details)
            assert appPool.details =~ /autoStart:"false"/
            assert appPool.details =~ /enable32BitAppOnWin64:"true"/
            assert appPool.details =~ /queueLength:"10"/
        cleanup:
            removeSite(siteName)
            removeAppPool(appPoolName)
    }


    // C259519
    def "provide additional options"() {
        given: "application is downloaded to the machine"
            createDir('c:/tmp')
            uploadArtifact(netDashUrl, 'c:/tmp/NetDash.zip')
            def siteName = randomize('NetDash')
            def sitePath = "c:/site_${siteName}"
            createDir(sitePath)
            createSite(siteName, 'http://*:8888', sitePath)
            def appPoolName = randomize('NetDashPool')
            createAppPool(appPoolName)
            def appName = 'NetDash'
        when: 'the deploy runs'
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        source: 'c:/tmp/NetDash-master/Insya.NetDash',
                        websiteName: '$siteName',
                        msdeployPath: 'msdeploy.exe',
                        applicationPath: '$appName',
                        applicationPool: '$appPoolName',
                        managedRuntimeVersion: 'v2.0',
                        managedPipelineMode: 'Classic',
                        enable32BitAppOnWin64: 'true',
                        autoStart: 'false',
                        queueLength: '10',
                        additionalOptions: '-enableRule:AppOffline'
                    ]
                )
            """
        then:
            assert result.outcome == 'success'
            logger.debug(result.logs)
            assert result.logs =~ /-enableRule:AppOffline/
        cleanup:
            removeSite(siteName)
            removeAppPool(appPoolName)
    }

    // C259520
    def "negative: deploy non-existing dir"() {
        given:
            def siteName = randomize('NetDash')
            def sitePath = "c:/site_${siteName}"
            createDir(sitePath)
            createSite(siteName, 'http://*:8888', sitePath)
            def appPoolName = randomize('NetDashPool')
        when: 'the deploy runs'
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        source: 'c:/tmp/some source',
                        websiteName: '$siteName',
                        msdeployPath: 'msdeploy.exe',
                        applicationPath: 'app',
                        applicationPool: '$appPoolName',
                        managedRuntimeVersion: 'v2.0'
                    ]
                )
            """
        then:
            assert result.outcome == 'error'
            logger.debug(result.logs)
            assert result.logs =~ /Error/

        cleanup:
            removeSite(siteName)
            removeAppPool(appPoolName)

    }


    // C259521
    def "negative: deploy non-existing site"() {
        given:
            def siteName = randomize('NetDash')
        when: 'the deploy runs'
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        source: 'c:/tmp/some source',
                        websiteName: '$siteName',
                        msdeployPath: 'msdeploy.exe',
                        applicationPath: 'app',
                        applicationPool: '',
                        managedRuntimeVersion: 'v2.0'
                    ]
                )
            """
        then:
            assert result.outcome == 'error'
            logger.debug(result.logs)
            assert result.logs =~ /Error/

    }

    // C259522
    def "negative: wrong path to msdeploy"() {
        given:
            def siteName = randomize('NetDash')
        when: 'the deploy runs'
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        source: 'c:/tmp/some source',
                        websiteName: '$siteName',
                        msdeployPath: 'wrong',
                        applicationPath: 'app',
                        applicationPool: '',
                        managedRuntimeVersion: 'v2.0'
                    ]
                )
            """
        then:
            assert result.outcome == 'error'
            logger.debug(result.logs)
            assert result.logs =~ /"wrong"' is not recognized as an internal or external command/

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
