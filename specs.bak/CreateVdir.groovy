import spock.lang.*
import com.electriccloud.spec.*

class CreateVirtualDirectory extends PluginTestHelper {
    static def projectName = 'EC-IIS Specs CreateVirtualDirectory'
    static def iisHandler
    static def procName = 'CreateVirtualDirectory'

    def doSetupSpec() {
        dsl 'setProperty(propertyName: "/plugins/EC-IIS/project/ec_debug_logToProperty", value: "/myJob/debug_logs")'
        def resName = createIISResource()
        dslFile 'dsl/RunProcedure.dsl', [
            projName: projectName,
            resName: resName,
            procName: procName,
            params: [
                appname: '',
                path: '',
                physicalpath: '',
                createDirectory: '',
                credential: ''
            ]
        ]
        createHelperProject(resName)
    }

    def doCleanupSpec() {
        // dsl "deleteProject '$projectName'"
    }

    @Unroll
    def "create vdir #appName"() {
        given:
            def siteName = randomize('site')
            createSite(siteName)
        when: "procedure runs"
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        appname: '${siteName}/',
                        path: '$appName/',
                        physicalpath: 'c:/tmp/path'
                    ]
                )
            """
        then:
            assert result.outcome == 'success'
        cleanup:
            removeSite(siteName)
        where:
            appName << ['app', 'my app']
    }

    @Unroll
    def "update dir"() {
        given:
            def siteName = randomize('site')
            createSite(siteName)
            def appName = 'app'
            createApp(siteName, appName)
        when:
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        appname: '${siteName}/',
                        path: '$appName/',
                        physicalpath: 'c:/tmp/another path'
                    ]
                )
            """
        then:
            assert result.outcome == 'success'
            def vdir = getVdir(siteName, appName)
            assert vdir.path == 'C:\\tmp\\another path'
    }

    @Unroll
    def "negative: non-existing site"() {
        given:
            def siteName = 'some site'
        when:
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        appname: '${siteName}/',
                        path: 'some path/',
                        physicalpath: 'c:/tmp/another path'
                    ]
                )
            """
        then:
            assert result.outcome == 'error'
    }

    @Unroll
    def "create directory #createDirectory"() {
        given:
            def appName = 'app'
            def siteName = randomize('my site')
            createSite(siteName)
            def physicalPath = "c:/tmp/$siteName/$appName"
        when:
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        appname: '${siteName}/',
                        path: '$appName/',
                        physicalpath: '$physicalPath',
                        createDirectory: '$createDirectory'
                    ]
                )
            """
        then:
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
    def "with credentials"() {
        given:
            def appName = 'app'
            def siteName = randomize('my site')
            createSite(siteName)
            def physicalPath = "c:/tmp/$siteName/$appName"
        when:
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    credential: [
                        credentialName: 'credential',
                        userName: '$userName',
                        password: '$password'
                    ],
                    actualParameter: [
                        appname: '${siteName}/',
                        path: '$appName/',
                        physicalpath: '$physicalPath',
                        createDirectory: '1',
                        credential: 'credential'
                    ]
                )
            """
        then:
            assert result.outcome == 'success'
            def vdir = runAppCmdLogs("list vdir /vdir.name:\"$siteName/$appName/\" /text:*")
            logger.debug(vdir)
            assert vdir =~ /$userName/
            assert vdir =~ /"\Q$password"/

        cleanup:
            removeSite(siteName)
        where:
            userName << ['test', 'test1']
            password << ['test', 'ID&!*&!***&^']
    }
}
