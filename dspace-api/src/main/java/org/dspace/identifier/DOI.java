/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.identifier;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.core.ReloadableEntity;

/**
 * DOI identifiers.
 *
 * @author Pascal-Nicolas Becker
 */
@Entity
@Table(name = "doi")
public class DOI
    implements Identifier, ReloadableEntity<Integer> {

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


    public static final String SCHEME = "doi:";

    @Id
    @Column(name = "doi_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "doi_seq")
    @SequenceGenerator(name = "doi_seq", sequenceName = "doi_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "doi", unique = true, length = 256)
    private String doi;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dspace_object")
    private DSpaceObject dSpaceObject;

    @Column(name = "resource_type_id")
    private Integer resourceTypeId;

    @Column(name = "status")
    private Integer status;

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.identifier.service.DOIService#create(Context)}
     */
    protected DOI() {
    }

    @Override
    public Integer getID() {
        return id;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public DSpaceObject getDSpaceObject() {
        return dSpaceObject;
    }

    public void setDSpaceObject(DSpaceObject dSpaceObject) {
        this.dSpaceObject = dSpaceObject;

        // set the Resource Type if the Object is not null
        // don't set object type null, we want to know to which resource type
        // the DOI was assigned to if the Object is deleted.
        if (dSpaceObject != null) {
            this.resourceTypeId = dSpaceObject.getType();
        }
    }

    /**
     * returns the resource type of the DSpaceObject the DOI is or was assigned
     * to. The resource type is set automatically when a DOI is assigned to a
     * DSpaceObject, using {@link #setDSpaceObject(org.dspace.content.DSpaceObject) }.
     *
     * @return the integer constant of the DSO, see {@link org.dspace.core.Constants#Constants Constants}
     */
    public Integer getResourceTypeId() {
        return this.resourceTypeId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public void setToBeRegistered() {
        setStatus(DOI.TO_BE_REGISTERED);
    }


    public void setToBeDeleted() {
        setStatus(DOI.TO_BE_DELETED);
    }



    public boolean isRegistered() {
        return DOI.IS_REGISTERED.equals(getStatus());
    }

    public boolean isDeleted() {
        return DOI.DELETED.equals(getStatus());
    }

    public boolean isToBeDeleted() {
        return DOI.TO_BE_DELETED.equals(getStatus());
    }

    public void setRegistered() {
        setStatus(DOI.IS_REGISTERED);
    }

    public void setUpdateRegistered() {
        setStatus(DOI.UPDATE_REGISTERED);
    }

    public void setUpdateBeforeRegistration() {
        setStatus(DOI.UPDATE_BEFORE_REGISTRATION);
    }
    public void isUpdateBeforeRegistration() {
        setStatus(DOI.UPDATE_BEFORE_REGISTRATION);
    }

    public boolean isIsUpdateBeforeRegistration() {
        return getStatus().equals(DOI.UPDATE_BEFORE_REGISTRATION);
    }

    public void setUpdateReserved() {
        setStatus(DOI.UPDATE_RESERVED);
    }
    public boolean isUpdateReserved() {
        return getStatus().equals(DOI.UPDATE_RESERVED);
    }
    public boolean isUpdateRegistered() {
        return getStatus().equals(DOI.UPDATE_REGISTERED);
    }



    public void setIsRegistered() {
        setStatus(DOI.IS_REGISTERED);
    }

    public void setIsReserved() {
        setStatus(DOI.IS_RESERVED);
    }

    public void setDeleted() {
        setStatus(DOI.DELETED);
    }

    public void setToBeReserved() {
        setStatus(DOI.TO_BE_RESERVED);
    }

    public boolean isToBeReserved() {
        return getStatus().equals(DOI.TO_BE_RESERVED);
    }

    public boolean isToBeRegesitered() {
        return getStatus().equals(DOI.TO_BE_REGISTERED);
    }

    public void setMinted() {
        setStatus(DOI.MINTED);
    }

    public boolean isMinted() {
        return getStatus().equals(DOI.MINTED);
    }

    public void setPending() {
        setStatus(DOI.PENDING);
    }
}
