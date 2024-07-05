import spock.lang.*
import com.electriccloud.spec.*

class CreateWebApp extends PluginTestHelper {
    static def projectName = 'EC-IIS Specs CreateWebApp'
    static def iisHandler

    def doSetupSpec() {
        dsl 'setProperty(propertyName: "/plugins/EC-IIS/project/ec_debug_logToProperty", value: "/myJob/debug_logs")'
        def resName = createIISResource()
        dslFile 'dsl/CreateApp/CreateApp.dsl', [
            projName: projectName,
            resName: resName
        ]
        createHelperProject(resName)
    }

    def doCleanupSpec() {
        dsl "deleteProject '$projectName'"
    }

    @Unroll
    def "normal params physicalPath #physicalPath, path #path"() {
        given: 'a site exists'
            def siteName = 'MySite'
            createSite(siteName)
        when: 'procedure runs'
            def result = runProcedureDsl("""
                runProcedure(
                    projectName: "$projectName",
                    procedureName: 'Create App',
                    actualParameter: [
                        appname: '$siteName',
                        physicalpath: '$physicalPath',
                        path: '$path',
                    ]
                )
            """)
        then: 'procedure succeeds'
            assert result.outcome == 'success'
            logger.debug(result.logs)
            assert result.logs =~ /APP object "${siteName}\/${path}" added/
            def app = getApp(siteName, path)
            logger.debug(objectToJson(app))
        cleanup:
            removeSite(siteName)
        where:
            physicalPath << ['c:/tmp/myPath', 'c:/tmp/some path']
            path << ['myApp', 'app/app2']
    }

    def "update application"() {
        given: 'a site and an app exist'
            def siteName = 'MySite'
            def appName = 'myapp'
            createSite(siteName)
            createApp(siteName, appName)
        when: 'procedure runs'
            def result = runProcedureDsl("""
                runProcedure(
                    projectName: "$projectName",
                    procedureName: 'Create App',
                    actualParameter: [
                        appname: '$siteName',
                        physicalpath: 'c:/tmp/myPath',
                        path: '$appName',
                    ]
                )
            """)
        then: 'procedure succeeds'
            assert result.outcome == 'success'
            logger.debug(result.logs)
            assert result.logs =~ /VDIR object "$siteName\/$appName\/" changed/
            def vdir = getVdir(siteName, appName)
            assert vdir.path == 'C:\\tmp\\myPath'
        cleanup:
            removeSite(siteName)
    }


    def "negative: add app to non-existing site"() {
        when:
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: 'Create App',
                    actualParameter: [
                        appname: 'SomeSite',
                        physicalpath: 'c:/tmp/path',
                        path: 'app',
                    ]
                )
            """
        then:
            assert result.outcome == 'error'
            logger.debug(result.logs)
            assert result.logs =~ /message:Cannot find SITE object with identifier "SomeSite"/
    }

    def "Create directory"() {
        given: 'a site exists'
            def siteName = 'MySite'
            createSite(siteName)
            def path = randomize('app')
            def physicalPath = "c:/tmp/$path"
        when: 'procedure runs'
            def result = runProcedureDsl("""
                runProcedure(
                    projectName: "$projectName",
                    procedureName: 'Create App',
                    actualParameter: [
                        appname: '$siteName',
                        physicalpath: '$physicalPath',
                        path: '$path',
                        createDirectory: '$createDirectory'
                    ]
                )
            """)
        then: 'procedure succeeds'
            assert result.outcome == 'success'
            def exists = dirExists(physicalPath)
            if (createDirectory == '1') {
                assert exists =~ /Exists/
            }
            else {
                assert exists =~ /Does not exist/
            }
        cleanup:
            removeSite(siteName)
        where:
            createDirectory << ['1', '0']

    }

    @Unroll
    def "Credentials #userName, #password"() {
        given: 'a site exists'
            def siteName = 'MySite'
            createSite(siteName)
            def path = randomize('app')
            def physicalPath = "c:/tmp/$path"
        when: 'procedure runs'
            def result = runProcedureDsl("""
                runProcedure(
                    projectName: "$projectName",
                    procedureName: 'Create App',
                    credential: [
                        credentialName: 'credential',
                        userName: "$userName",
                        password: '$password'
                    ],
                    actualParameter: [
                        appname: '$siteName',
                        physicalpath: '$physicalPath',
                        path: '$path',
                        createDirectory: '1',
                        credential: 'credential'
                    ]
                )
            """)
        then: 'procedure succeeds'
            assert result.outcome == 'success'
            assert !(result.logs =~ /\Q$password/)
        cleanup:
            removeSite(siteName)
        where:
            userName << ['test', 'test']
            password << ['test', "71#&^&^%!#"]

    }


}
