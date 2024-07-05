import spock.lang.*
import com.electriccloud.spec.*

class AddWebSiteBinding extends PluginTestHelper {
    static def projectName = 'EC-IIS Specs AddWebSiteBinding'
    static def iisHandler
    static def procName = 'AddWebSiteBinding'

    def doSetupSpec() {
        dsl 'setProperty(propertyName: "/plugins/EC-IIS/project/ec_debug_logToProperty", value: "/myJob/debug_logs")'
        def resName = createIISResource()
        dslFile 'dsl/RunProcedure.dsl', [
            projName: projectName,
            resName: resName,
            procName: procName,
            params: [
                bindingInformation: '',
                bindingProtocol: '',
                websitename: '',
                hostHeader: ''
            ]
        ]
        createHelperProject(resName)
    }

    def doCleanupSpec() {
        dsl "deleteProject '$projectName'"
    }

    def "normal binding"() {
        given: 'a site exists'
            def siteName = randomize('mysite')
            createSite(siteName, 'http://*:9999')
        when: "procedure runs"
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        bindingInformation: '*:9991',
                        bindingProtocol: 'http',
                        websitename: '$siteName'
                    ]
                )
            """
        then: 'it finishes'
            assert result.outcome == 'success'
            logger.debug(result.logs)
            def site = getSite(siteName)
            logger.debug(objectToJson(site))
            assert site.bindings == ['http/*:9999:', 'http/*:9991:']
        cleanup:
            removeSite(siteName)
    }


    def "duplicate binding"() {
        given: 'a site exists'
            def siteName = randomize('mysite')
            def port = '9999'
            createSite(siteName, "http://*:${port}")
        when: "procedure runs"
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        bindingInformation: '*:$port',
                        bindingProtocol: 'http',
                        websitename: '$siteName'
                    ]
                )
            """
        then: 'it finishes'
            assert result.outcome == 'success'
            logger.debug(result.logs)
            def site = getSite(siteName)
            logger.debug(objectToJson(site))
            assert site.bindings == ['http/*:9999:']
            assert result.logs =~ /already exists/
        cleanup:
            removeSite(siteName)
    }

    def "duplicate binding with host header"() {
        given: 'a site exists'
            def siteName = randomize('mysite')
            def port = '9999'
            createSite(siteName, "http://*:${port}")
        when: "procedure runs"
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        bindingInformation: '*:$port',
                        bindingProtocol: 'http',
                        websitename: '$siteName',
                        hostHeader: 'mysite.com'
                    ]
                )
            """
        then: 'it finishes'
            assert result.outcome == 'success'
            logger.debug(result.logs)
            def site = getSite(siteName)
            logger.debug(objectToJson(site))
            assert site.bindings == ['http/*:9999:', 'http/*:9999:mysite.com']

        cleanup:
            removeSite(siteName)
    }

    def "different protocols two bindings"() {
        given: 'a site exists'
            def siteName = randomize('mysite')
            createSite(siteName, 'http://*:9999')
        when: "procedure runs"
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        bindingInformation: '*:443',
                        bindingProtocol: 'https',
                        websitename: '$siteName'
                    ]
                )
            """
        then: 'it finishes'
            assert result.outcome == 'success'
            logger.debug(result.logs)
            def site = getSite(siteName)
            logger.debug(objectToJson(site))
            assert site.bindings == [
                'http/*:9999:',
                'https/*:443:'
            ]

        cleanup:
            removeSite(siteName)

    }

    def "add bindings with host header"() {
        given: 'a site exists'
            def siteName = randomize('mysite')
            createSite(siteName, 'http://*:9999')
            def hostHeader = 'myhost.com'
        when: "procedure runs"
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        bindingInformation: '*:9911',
                        bindingProtocol: 'http',
                        websitename: '$siteName',
                        hostHeader: '$hostHeader'
                    ]
                )
            """
        then: 'it finishes'
            assert result.outcome == 'success'
            logger.debug(result.logs)
            def site = getSite(siteName)
            logger.debug(objectToJson(site))
            assert "http/*:9911:$hostHeader".toString() in site.bindings

        cleanup:
            removeSite(siteName)

    }

    def "negative: add binding to non-existing site"() {
        when: "procedure runs"
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        bindingInformation: '*:9911',
                        bindingProtocol: 'http',
                        websitename: 'SomeSite',
                    ]
                )
            """
        then: 'it finishes'
            assert result.outcome == 'error'
            assert result.logs =~ /The site "SomeSite" does not exist/
    }

}
