import spock.lang.*
import com.electriccloud.spec.*

class CreateWebsite extends PluginTestHelper {
    static def projectName = 'EC-IIS Specs CreateWebSite'
    static def iisHandler

    def doSetupSpec() {
        dsl 'setProperty(propertyName: "/plugins/EC-IIS/project/ec_debug_logToProperty", value: "/myJob/debug_logs")'
        def resName = createIISResource()
        dslFile 'dsl/CreateSite/CreateSite.dsl', [
            projName: projectName,
            resName: resName
        ]
        createHelperProject(resName)
    }

    def doCleanupSpec() {
        dsl "deleteProject '$projectName'"
    }

    @Unroll
    def "normal params siteName: #siteName, siteId: #siteId, sitePath: #sitePath , bindings: #bindings"() {
        given: 'the site is removed'
            removeSite(siteName)
        when: 'procedure runs'
            def result = runProcedureDsl("""
                runProcedure(
                    projectName: "$projectName",
                    procedureName: 'Create Site',
                    actualParameter: [
                        websitename: '$siteName',
                        websiteid: '$siteId',
                        websitepath: '$sitePath',
                        bindings: '''$bindings''',
                    ]
                )
            """)
        then: 'procedure succeeds'
            assert result.outcome == 'success'
            logger.debug(result.logs)
            assert result.logs =~ /SITE object "\Q$siteName\E" added/
            assert result.logs =~ /APP object "\Q$siteName\E\/" added/

            def validPath = sitePath.replaceAll('/', "\\\\").replace('c', 'C')
            def vdir = getVdir(siteName)
            assert vdir.path == validPath
        cleanup:
            removeSite(siteName)
        where:
            siteName << ['mysite', 'Some Site()%$#&', 'Multiple Bindings']
            siteId << ['', 56, '']
            sitePath << ['c:/tmp/path', 'c:/tmp/somepath', 'c:/tmp/path']
            bindings << ['http://*:80', 'http://localhost:9080', "http://*:9991,http://*:1112"]
    }

    @Unroll
    def "site already exists #siteName"() {
        given: 'a site'
            def bindings = 'http://*:80'
            def sitePath = 'c:/tmp/Test'
            createSite(siteName)
        when: 'procedure runs'
            def result = runProcedureDsl("""
                runProcedure(
                    projectName: "$projectName",
                    procedureName: 'Create Site',
                    actualParameter: [
                        websitename: '$siteName',
                        websitepath: 'c:/tmp/newPath',
                        bindings: '''http://*:8080'''
                    ]
                )
            """)
        then: 'procedure succeeds'
            assert result.outcome == 'success'
            logger.debug(result.logs)
            assert result.logs =~ /SITE object "\Q$siteName\E" changed/
            def updatedSite = getSite(siteName)
            logger.debug(objectToJson(updatedSite))
            assert updatedSite.bindings =~ /8080/
            def updatedVdir = getVdir(siteName)
            assert updatedVdir.path =~ /newPath/
        cleanup:
            removeSite(siteName)
        where:
            siteName << ['Test Site', '!@#$%^&*()']
    }

    def "port & path already taken"() {
        given: 'a site'
            def siteName = 'Test'
            def bindings = 'http://*:80'
            def sitePath = 'c:/tmp/test'
            createSite(siteName, bindings, sitePath)
        when: 'procedure runs'
            def result = runProcedureDsl("""
                runProcedure(
                    projectName: "$projectName",
                    procedureName: 'Create Site',
                    actualParameter: [
                        websitename: '$siteName',
                        websitepath: '$sitePath',
                        bindings: '''$bindings'''
                    ]
                )
            """)
        then: 'procedure succeeds'
            assert result.outcome == 'success'
        cleanup:
            removeSite(siteName)
    }

    def "create site with id"() {
        given:
            def siteName = 'MySite'
            def siteId = 99
        when: 'procedure runs'
             def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: 'Create Site',
                    actualParameter: [
                        websitename: '$siteName',
                        websitepath: 'c:/tmp/site',
                        bindings: '''http://*:9999''',
                        websiteid: '$siteId'
                    ]
                )
            """
        then:
            assert result.outcome == 'success'
            def site = getSite(siteName)
            assert site.id == "${siteId}"
        cleanup:
            removeSite(siteName)

    }

    def "negative: invalid bindings"() {
        when: 'procedure runs'
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: 'Create Site',
                    actualParameter: [
                        websitename: 'MySite',
                        websitepath: 'c:/tmp/site',
                        bindings: '''http'''
                    ]
                )
            """
        then: 'it fails'
            assert result.outcome == 'error'
    }

    @Ignore
    def "negative: id already taken"() {
        given:
            def siteId = 99
            def siteName = 'TestSite'
            def path = 'c:/tmp/test_site'
            def bindings = 'http://*:80'
            createSite(siteName, bindings, path, siteId)
        when: 'procedure runs'
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: 'Create Site',
                    actualParameter: [
                        websitename: 'AnotherSite',
                        websitepath: '$path',
                        bindings: '''$bindings''',
                        websiteid: '$siteId'
                    ]
                )
            """
        then:
            assert result.outcome == 'error'
        cleanup:
            removeSite(siteName)
    }

    @Unroll
    def "create directory #createDirectory, dir #dir"() {
        given: 'no site'
            def siteName = randomize('site')
            removeSite(siteName)
            def port = 9912
            def bindings = "http://*:$port"
        when:
            def result = runProcedureDsl("""
                runProcedure(
                    projectName: "$projectName",
                    procedureName: 'Create Site',
                    actualParameter: [
                        websitename: '$siteName',
                        websitepath: '$dir',
                        bindings: '''$bindings''',
                        createDirectory: '$createDirectory'
                    ]
                )
            """)
        then:
            assert result.outcome == 'success'
            def exists = dirExists(dir)
            logger.debug(exists)
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
            dir << ["c:/tmp/site/" + randomize('site'), 'c:/tmp/dir1/' + randomize('dir') + '/dir2']
    }

    @Unroll
    def "new site with credentials #userName"() {
        given: 'no site'
            def siteName = randomize('site_with_creds')
            removeSite(siteName)
            def dir = 'c:/tmp/vdir'
            def bindings = 'http://*:9999'
        when:
            def result = runProcedureDsl("""
                runProcedure(
                    projectName: "$projectName",
                    procedureName: 'Create Site',
                    credential: [
                        credentialName: 'credential',
                        userName: '$userName',
                        password: '$password'
                    ],
                    actualParameter: [
                        websitename: '$siteName',
                        websitepath: '$dir',
                        bindings: '''$bindings''',
                        createDirectory: '1',
                        credential: 'credential',
                    ]
                )
            """)
        then:
            assert result.outcome == 'success'
            def vdir = runAppCmdLogs("list vdir /vdir.name:$siteName/ /text:*")
            logger.debug(vdir)
            assert !(result.logs =~ /\Q$password/)
            assert vdir =~ /$userName/
            assert vdir =~ /"\Q$password"/
        cleanup:
            removeSite(siteName)
        where:
            userName << ['build', 'test&!']
            password << ['test', "test!*&#"]

    }
}
