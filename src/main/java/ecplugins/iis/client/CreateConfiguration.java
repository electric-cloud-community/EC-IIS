
// CreateConfiguration.java --
//
// CreateConfiguration.java is part of ElectricCommander.
//
// Copyright (c) 2005-2012 Electric Cloud, Inc.
// All rights reserved.
//

package ecplugins.iis.client;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.ui.Anchor;

import com.electriccloud.commander.client.requests.RunProcedureRequest;
import com.electriccloud.commander.client.responses.DefaultRunProcedureResponseCallback;
import com.electriccloud.commander.client.responses.RunProcedureResponse;
import com.electriccloud.commander.gwt.client.requests.CgiRequestProxy;
import com.electriccloud.commander.gwt.client.ui.CredentialEditor;
import com.electriccloud.commander.gwt.client.ui.FormBuilder;
import com.electriccloud.commander.gwt.client.ui.FormTable;
import com.electriccloud.commander.gwt.client.ui.SimpleErrorBox;
import com.electriccloud.commander.gwt.client.util.CommanderUrlBuilder;

import ecinternal.client.InternalFormBase;
import ecinternal.client.ui.CustomEditorLoader;

import static com.electriccloud.commander.gwt.client.util.CommanderUrlBuilder.createPageUrl;
import static com.electriccloud.commander.gwt.client.util.CommanderUrlBuilder.createUrl;

import static ecinternal.client.InternalComponentBaseFactory.getPluginName;

/**
 * Create IIS Configuration.
 */
public class CreateConfiguration
    extends InternalFormBase
{

    //~ Constructors -----------------------------------------------------------

    public CreateConfiguration()
    {

        // noinspection HardCodedStringLiteral
        super("New IIS Configuration", "IIS Configurations");

        CommanderUrlBuilder urlBuilder = createPageUrl(getPluginName(),
                "configurations");

        setDefaultRedirectToUrl(urlBuilder.buildString());
    }

    //~ Methods ----------------------------------------------------------------

    @Override protected FormTable initializeFormTable()
    {
        return getUIFactory().createFormBuilder();
    }

    @Override protected void load()
    {
        FormBuilder fb = (FormBuilder) getFormTable();

        // noinspection HardCodedStringLiteral
        setStatus("Loading...");

        CustomEditorLoader loader = new CustomEditorLoader(fb, this);

        loader.setCustomEditorPath("/plugins/EC-IIS"
                + "/project/ui_forms/IISCreateConfigForm");
        loader.load();
        clearStatus();
    }

    @Override protected void submit()
    {

        // noinspection HardCodedStringLiteral
        setStatus("Saving...");
        clearAllErrors();

        FormBuilder fb = (FormBuilder) getFormTable();

        if (!fb.validate()) {
            clearStatus();

            return;
        }

        // Build runProcedure request
        RunProcedureRequest request = getRequestFactory()
                .createRunProcedureRequest();

        request.setProjectName("/plugins/EC-IIS/project");
        request.setProcedureName("CreateConfiguration");

        Map<String, String> params           = fb.getValues();
        Collection<String>  credentialParams = fb.getCredentialIds();

        for (Entry<String, String> stringStringEntry : params.entrySet()) {

            if (credentialParams.contains(stringStringEntry.getKey())) {
                CredentialEditor credential = fb.getCredential(
                        stringStringEntry.getKey());

                request.addCredentialParameter(stringStringEntry.getKey(),
                    credential.getUsername(), credential.getPassword());
            }
            else {
                request.addActualParameter(stringStringEntry.getKey(),
                    stringStringEntry.getValue());
            }
        }

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

        if (getLog().isDebugEnabled()) {
            getLog().debug("Issuing Commander request: " + request);
        }

        doRequest(request);
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
                            cancel();
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
                                        "Error occurred during configuration creation: "
                                        + responseString);
                            CommanderUrlBuilder urlBuilder = createUrl(
                                    "jobDetails.php").setParameter("jobId",
                                    jobId);

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
            addErrorMessage("CGI request failed: ", e);
        }
    }
}
