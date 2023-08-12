package org.dspace.identifier.darkpid;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.identifier.doi.DOIConnector;
import org.dspace.identifier.doi.DOIIdentifierException;

public class DarkConnector implements DOIConnector {

    @Override
    public boolean isDOIReserved(Context context, String doi) throws DOIIdentifierException {
        return false;
    }

    @Override
    public boolean isDOIRegistered(Context context, String doi) throws DOIIdentifierException {
        return false;
    }

    @Override
    public void deleteDOI(Context context, String doi) throws DOIIdentifierException {

    }

    @Override
    public void reserveDOI(Context context, DSpaceObject dso, String doi) throws DOIIdentifierException {

    }

    @Override
    public void registerDOI(Context context, DSpaceObject dso, String doi) throws DOIIdentifierException {

    }

    @Override
    public void updateMetadata(Context context, DSpaceObject dso, String doi) throws DOIIdentifierException {

    }
}
