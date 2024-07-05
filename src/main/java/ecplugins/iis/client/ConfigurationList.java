
// ConfigurationList.java --
//
// ConfigurationList.java is part of ElectricCommander.
//
// Copyright (c) 2005-2012 Electric Cloud, Inc.
// All rights reserved.
//

package ecplugins.iis.client;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Window.Location;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.electriccloud.commander.client.ChainedCallback;
import com.electriccloud.commander.client.requests.RunProcedureRequest;
import com.electriccloud.commander.client.responses.DefaultRunProcedureResponseCallback;
import com.electriccloud.commander.client.responses.RunProcedureResponse;
import com.electriccloud.commander.gwt.client.requests.CgiRequestProxy;
import com.electriccloud.commander.gwt.client.ui.ListTable;
import com.electriccloud.commander.gwt.client.ui.SimpleErrorBox;
import com.electriccloud.commander.gwt.client.util.CommanderUrlBuilder;

import ecinternal.client.DialogClickHandler;
import ecinternal.client.ListBase;
import ecinternal.client.Loader;

import static com.electriccloud.commander.gwt.client.util.CommanderUrlBuilder.createPageUrl;
import static com.electriccloud.commander.gwt.client.util.CommanderUrlBuilder.createRedirectUrl;

import static ecinternal.client.InternalComponentBaseFactory.getPluginName;

/**
 * EC-IIS Configuration List.
 */
public class ConfigurationList
    extends ListBase
{

    //~ Instance fields --------------------------------------------------------

    private final IISConfigList m_configList;

    //~ Constructors -----------------------------------------------------------

    public ConfigurationList()
    {

        // noinspection HardCodedStringLiteral
        super("ecgc", "IIS Configurations", "All Configurations");
        m_configList = new IISConfigList();
    }

    //~ Methods ----------------------------------------------------------------

    @Override protected Anchor constructCreateLink()
    {
        CommanderUrlBuilder urlBuilder = createPageUrl(getPluginName(),
                "newConfiguration");

        urlBuilder.setParameter("redirectTo",
            createRedirectUrl().buildString());

        // noinspection HardCodedStringLiteral
        return new Anchor("Create Configuration", urlBuilder.buildString());
    }

    @Override protected void load()
    {

        // noinspection HardCodedStringLiteral
        setStatus("Loading...");

        Loader loader = new IISConfigListLoader(m_configList, this,
                new ChainedCallback() {
                    @Override public void onComplete()
                    {
                        loadList();
                    }
                });

        loader.load();
    }

    private void deleteConfiguration(String configName)
    {

        // noinspection HardCodedStringLiteral
        setStatus("Deleting...");
        clearErrorMessages();

        // Build runProcedure request
        RunProcedureRequest request = getRequestFactory()
                .createRunProcedureRequest();

        request.setProjectName("/plugins/EC-IIS/project");
        request.setProcedureName("DeleteConfiguration");
        request.addActualParameter("config", configName);
        request.setCallback(new DefaultRunProcedureResponseCallback(this) {
                @Override public void handleResponse(
                        RunProcedureResponse response)
                {

                    if (getLog().isDebugEnabled()) {
                        getLog().debug(
                            "Commander runProcedure request returned job id: "
                                + response.getJobId());
                    }

                    waitForJob(response.getJobId().toString());
                }
            });

        // Launch the procedure
        if (getLog().isDebugEnabled()) {
            getLog().debug("Issuing Commander request: " + request);
        }

        doRequest(request);
    }

    private void loadList()
    {
        ListTable listTable = getListTable();

        if (!m_configList.isEmpty()) {

            // noinspection HardCodedStringLiteral
            listTable.addHeaderRow(true, "Configuration Name", "Url");
        }

        for (String configName : m_configList.getConfigNames()) {

            // Config name
            Label configNameLabel = new Label(configName);

            // Url
            String configUrl      = m_configList.getConfigUrl(configName);
            Label  configUrlLabel = new Label(configUrl);

            // "Edit" link
            CommanderUrlBuilder urlBuilder = createPageUrl(getPluginName(),
                    "editConfiguration");

            urlBuilder.setParameter("configName", configName);
            urlBuilder.setParameter("redirectTo",
                createRedirectUrl().buildString());

            @SuppressWarnings("HardCodedStringLiteral")
            Anchor editConfigLink = new Anchor("Edit",
                    urlBuilder.buildString());

            // "Delete" link
            @SuppressWarnings("HardCodedStringLiteral")
            Anchor       deleteConfigLink = new Anchor("Delete");
            @SuppressWarnings({"HardCodedStringLiteral", "StringConcatenation"})
            ClickHandler dch = new DialogClickHandler(
                    new DeleteConfirmationDialog(configName,
                        "Are you sure you want to delete the IIS configuration '"
                            + configName + "'?") {
                        @Override protected void doDelete()
                        {
                            deleteConfiguration(m_objectId);
                        }
                    });

            deleteConfigLink.addClickHandler(dch);

            // Add the row
            Widget actions = getUIFactory().constructActionList(editConfigLink,
                    deleteConfigLink);

            listTable.addRow(configNameLabel, configUrlLabel, actions);
        }

        clearStatus();
    }

    @SuppressWarnings("OverlyComplexAnonymousInnerClass")
    private void waitForJob(final String jobId)
    {
        CgiRequestProxy     cgiRequestProxy = new CgiRequestProxy(
                getPluginName(), "iisMonitor.cgi");
        Map<String, String> cgiParams       = new HashMap<String, String>();

        cgiParams.put("jobId", jobId);

        // Pass debug flag to CGI, which will use it to determine whether to
        // clean up a successful job
        if ("1".equals(getGetParameter("debug"))) {
            cgiParams.put("debug", "1");
        }

        try {
            cgiRequestProxy.issueGetRequest(cgiParams, new RequestCallback() {
                    @Override public void onError(
                            Request   request,
                            Throwable exception)
                    {

                        // noinspection HardCodedStringLiteral
                        addErrorMessage("CGI request failed: ", exception);
                    }

                    @Override public void onResponseReceived(
                            Request  request,
                            Response response)
                    {
                        String responseString = response.getText();

                        if (getLog().isDebugEnabled()) {
                            getLog().debug(
                                "CGI response received: " + responseString);
                        }

                        if (responseString.startsWith("Success")) {

                            // We're done!
                            Location.reload();
                        }
                        else {
                            @SuppressWarnings(
                                {
                                    "HardCodedStringLiteral",
                                    "StringConcatenation"
                                }
                            )
                            SimpleErrorBox      error      = getUIFactory()
                                    .createSimpleErrorBox(
                                        "Error occurred during configuration deletion: "
                                        + responseString);
                            CommanderUrlBuilder urlBuilder = CommanderUrlBuilder
                                    .createUrl("jobDetails.php")
                                    .setParameter("jobId", jobId);

                            // noinspection HardCodedStringLiteral
                            error.add(
                                new Anchor("(See job for details)",
                                    urlBuilder.buildString()));
                            addErrorMessage(error);
                        }
                    }
                });
        }
        catch (RequestException e) {

            // noinspection HardCodedStringLiteral
            addErrorMessage("CGI request failed:: ", e);
        }
    }
}
