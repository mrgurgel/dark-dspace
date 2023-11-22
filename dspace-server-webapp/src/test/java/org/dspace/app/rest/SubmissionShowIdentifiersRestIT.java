/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.eperson.EPerson;
import org.dspace.handle.service.HandleService;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.services.ConfigurationService;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test suite for testing the Show Identifiers submission step
 *
 * @author Kim Shepherd
 *
 */
public class SubmissionShowIdentifiersRestIT extends AbstractControllerIntegrationTest {

    @Autowired
    private WorkspaceItemService workspaceItemService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private HandleService handleService;

    private Collection collection;
    private EPerson submitter;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Root community").build();

        submitter = EPersonBuilder.createEPerson(context)
                                .withEmail("submitter.em@test.com")
                                .withPassword(password)
                                .build();

        collection = CollectionBuilder.createCollection(context, parentCommunity)
                                .withName("Collection")
                                .withEntityType("Publication")
                                .withSubmitterGroup(submitter).build();

        // Manually set configuration to allow registration handles, DOIs at workspace item creation
        configurationService.setProperty("identifiers.submission.register", true);

        context.restoreAuthSystemState();
    }

    @After
    public void after() throws SQLException, IOException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        workspaceItemService.findAll(context).forEach(this::deleteWorkspaceItem);
        // Manually restore identifiers configuration
        configurationService.setProperty("identifiers.submission.register", false);
        context.restoreAuthSystemState();
    }

    private void deleteWorkspaceItem(WorkspaceItem workspaceItem) {
        try {
            workspaceItemService.deleteAll(context, workspaceItem);
        } catch (SQLException | AuthorizeException | IOException e) {
            throw new RuntimeException();
        }
    }

    @Test
    public void testItemHandleReservation() throws Exception {
        // Test publication that should get Handle and DOI
        context.turnOffAuthorisationSystem();
        WorkspaceItem workspaceItem = createWorkspaceItem("Test publication", collection);
        context.restoreAuthSystemState();
        // Expected handle
        String expectedHandle = handleService.resolveToURL(context, workspaceItem.getItem().getHandle());
        String submitterToken = getAuthToken(submitter.getEmail(), password);
        getClient(submitterToken).perform(get("/api/submission/workspaceitems/" + workspaceItem.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections.identifiers.identifiers[1].type").value("identifier"))
                .andExpect(jsonPath("$.sections.identifiers.identifiers[1].value").value(expectedHandle))
                .andExpect(jsonPath("$.sections.identifiers.identifiers[1].identifierType").value("handle"));
    }

    @Test
    public void testItemDoiReservation() throws Exception {
        // Test publication that should get Handle and DOI
        context.turnOffAuthorisationSystem();
        WorkspaceItem workspaceItem = createWorkspaceItem("Test publication", collection);
        context.restoreAuthSystemState();

        String submitterToken = getAuthToken(submitter.getEmail(), password);
        getClient(submitterToken).perform(get("/api/submission/workspaceitems/" + workspaceItem.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections.identifiers.identifiers[0].type").value("identifier"))
                .andExpect(jsonPath("$.sections.identifiers.identifiers[0].identifierType").value("doi"))
                .andExpect(jsonPath("$.sections.identifiers.identifiers[0].identifierStatus")
                        .value(DOIIdentifierProvider.statusText[PENDING]));
    }

    private WorkspaceItem createWorkspaceItem(String title, Collection collection) {
        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                .withTitle(title)
                .withSubmitter(submitter)
                .build();
        return workspaceItem;
    }

    private static final Integer TO_BE_REGISTERED = 1;
    // The DOI is queued for reservation with the service provider
    private static final Integer TO_BE_RESERVED = 2;
    // The DOI has been registered online
    private static final Integer IS_REGISTERED = 3;
    // The DOI has been reserved online
    private static final Integer IS_RESERVED = 4;
    // The DOI is reserved and requires an updated metadata record to be sent to the service provider
    private static final Integer UPDATE_RESERVED = 5;
    // The DOI is registered and requires an updated metadata record to be sent to the service provider
    private static final Integer UPDATE_REGISTERED = 6;
    // The DOI metadata record should be updated before performing online registration
    private static final Integer UPDATE_BEFORE_REGISTRATION = 7;
    // The DOI will be deleted locally and marked as deleted in the DOI service provider
    private static final Integer TO_BE_DELETED = 8;
    // The DOI has been deleted and is no longer associated with an item
    private static final Integer DELETED = 9;
    // The DOI is created in the database and is waiting for either successful filter check on item install or
    // manual intervention by an administrator to proceed to reservation or registration
    private static final Integer PENDING = 10;
    // The DOI is created in the database, but no more context is known
    private static final Integer MINTED = 11;

}