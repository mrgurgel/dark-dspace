package org.dspace.app.rest.submit.step;

import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.step.DataIdentifiers;
import org.dspace.app.rest.submit.AbstractProcessingStep;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.InProgressSubmission;
import org.dspace.core.Context;
import org.dspace.identifier.Dark;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.services.model.Request;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

public class RequestDarkPidStep extends AbstractProcessingStep {


    public static final String PREFIX_HYPERDRIVE = "dark:/";
    private static final Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger(RequestDarkPidStep.class);


    @Override
    public DataIdentifiers getData(SubmissionService submissionService, InProgressSubmission inProgressSubmission, SubmissionStepConfig config) throws Exception {

        DataIdentifiers dataIdentifiers = new DataIdentifiers();
        dataIdentifiers.setDisplayTypes(Arrays.asList("otherIdentifiers"));

        itemService.getMetadata(inProgressSubmission.getItem(), "dc", "identifier", "uri", null).stream()
                .filter(metadataValue -> metadataValue.getValue().startsWith(PREFIX_HYPERDRIVE)).findFirst()
                .ifPresentOrElse(metadataValue -> dataIdentifiers.addIdentifier("otherIdentifiers",
                                "dArk PID " + metadataValue.getValue(), null),
                        () -> dataIdentifiers.addIdentifier("otherIdentifiers", "dArk PID " +
                                Dark.createNewDarkPid(getContext(), inProgressSubmission).getDoi(), null));


        return dataIdentifiers;
    }


    @Override
    public void doPatchProcessing(Context context, HttpServletRequest currentRequest, InProgressSubmission source, Operation op, SubmissionStepConfig stepConf) throws Exception {

    }

    private Context getContext() {
        Context context;
        Request currentRequest = DSpaceServicesFactory.getInstance().getRequestService().getCurrentRequest();
        if (currentRequest != null) {
            HttpServletRequest request = currentRequest.getHttpServletRequest();
            context = ContextUtil.obtainContext(request);
        } else {
            context = new Context();
        }

        return context;
    }


}
