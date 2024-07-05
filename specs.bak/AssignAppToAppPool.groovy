import spock.lang.*
import com.electriccloud.spec.*

class AssignAppToAppPool extends PluginTestHelper {
    static def projectName = 'EC-IIS Specs AssignApp'
    static def iisHandler
    static def procName = 'AssignAppToAppPool'

    def doSetupSpec() {
        dsl 'setProperty(propertyName: "/plugins/EC-IIS/project/ec_debug_logToProperty", value: "/myJob/debug_logs")'
        def resName = createIISResource()
        dslFile 'dsl/RunProcedure.dsl', [
            projName: projectName,
            resName: resName,
            procName: procName,
            params: [
                appname: '',
                apppoolname: '',
                sitename: ''
            ]
        ]
        createHelperProject(resName)
    }

    def doCleanupSpec() {
        dsl "deleteProject '$projectName'"
    }

    def "existing app tp existing app pool"() {
        given:
            def siteName = randomize('mysite')
            def appName = 'app'
            def appPoolName = 'appPool'
            createSite(siteName)
            createApp(siteName, appName)
                createAppPool(appPoolName)
        when: "procedure runs"
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        apppoolname: '$appPoolName',
                        appname: '/$appName',
                        sitename: '$siteName'
                    ]
                )
            """
        then: 'it finishes'
            assert result.outcome == 'success'
            logger.debug(result.logs)
            def app = getApp(siteName, appName)
            logger.debug(objectToJson(app))
            assert app.applicationPool == appPoolName
        cleanup:
            removeSite(siteName)
            removeAppPool(appPoolName)
    }


    def "root app to existing app pool"() {
        given:
            def siteName = randomize('mysite')
            def appName = 'app'
            def appPoolName = 'appPool'
            createSite(siteName)
            createAppPool(appPoolName)
        when: "procedure runs"
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        apppoolname: '$appPoolName',
                        appname: '/',
                        sitename: '$siteName'
                    ]
                )
            """
        then: 'it finishes'
            assert result.outcome == 'success'
            logger.debug(result.logs)
            def app = getApp(siteName, '')
            logger.debug(objectToJson(app))
            assert app.applicationPool == appPoolName
        cleanup:
            removeSite(siteName)
            removeAppPool(appPoolName)
    }

    def "negative: non-existing app to app pool"() {
        given:
            def appPoolName = randomize('appPool')
            createAppPool(appPoolName)
        when: "procedure runs"
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        apppoolname: '$appPoolName',
                        appname: '/some_app',
                        sitename: 'some_site'
                    ]
                )
            """
        then: 'it finishes'
            assert result.outcome == 'error'
        cleanup:
            removeAppPool(appPoolName)
    }

    def "negative: non-existing app pool"() {
        given:
            def siteName = randomize('mysite')
            def appName = 'app'
            createSite(siteName)
            createApp(siteName, appName)
        when: "procedure runs"
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        apppoolname: 'deadpool',
                        appname: '/$appName',
                        sitename: '$siteName'
                    ]
                )
            """
        then: 'it finishes'
            assert result.outcome == 'error'
        cleanup:
            removeSite(siteName)

    }

}
