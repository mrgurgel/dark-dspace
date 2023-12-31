/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import java.sql.SQLException;

import org.apache.logging.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.web.ContextUtil;

/**
 * This class provides a standard interface to all item counting
 * operations for communities and collections.  It can be run from the
 * command line to prepare the cached data if desired, simply by
 * running:
 *
 * java org.dspace.browse.ItemCounter
 *
 * It can also be invoked via its standard API.  In the event that
 * the data cache is not being used, this class will return direct
 * real time counts of content.
 *
 * @author Richard Jones
 */
public class ItemCounter {
    /**
     * Log4j logger
     */
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(ItemCounter.class);

    /**
     * DAO to use to store and retrieve data
     */
    private ItemCountDAO dao;

    /**
     * DSpace Context
     */
    private Context context;

    /**
     * This field is used to hold singular instance of a class.
     * Singleton pattern is used but this class should be
     * refactored to modern DSpace approach (injectible service).
     */

    private static ItemCounter instance;

    protected ItemService itemService;
    protected ConfigurationService configurationService;

    private boolean showStrengths;
    private boolean useCache;

    /**
     * Construct a new item counter which will use the given DSpace Context
     *
     * @param context current context
     * @throws ItemCountException if count error
     */
    public ItemCounter(Context context) throws ItemCountException {
        this.context = context;
        this.dao = ItemCountDAOFactory.getInstance(this.context);
        this.itemService = ContentServiceFactory.getInstance().getItemService();
        this.configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        this.showStrengths = configurationService.getBooleanProperty("webui.strengths.show", false);
        this.useCache = configurationService.getBooleanProperty("webui.strengths.cache", true);
    }

    /**
     * Get the singular instance of a class.
     * It creates a new instance at the first usage of this method.
     *
     * @return instance af a class
     * @throws ItemCountException when error occurs
     */
    public static ItemCounter getInstance() throws ItemCountException {
        if (instance == null) {
            instance = new ItemCounter(ContextUtil.obtainCurrentRequestContext());
        }
        return instance;
    }

    /**
     * Get the count of the items in the given container. If the configuration
     * value webui.strengths.show is equal to 'true' this method will return all
     * archived items. If the configuration value webui.strengths.show is equal to
     * 'false' this method will return -1.
     * If the configuration value webui.strengths.cache
     * is equal to 'true' this will return the cached value if it exists.
     * If it is equal to 'false' it will count the number of items
     * in the container in real time.
     *
     * @param dso DSpaceObject
     * @return count
     * @throws ItemCountException when error occurs
     */
    public int getCount(DSpaceObject dso) throws ItemCountException {
        if (!showStrengths) {
            return -1;
        }

        if (useCache) {
            return dao.getCount(dso);
        }

        // if we make it this far, we need to manually count
        if (dso instanceof Collection) {
            try {
                return itemService.countItems(context, (Collection) dso);
            } catch (SQLException e) {
                log.error("caught exception: ", e);
                throw new ItemCountException(e);
            }
        }

        if (dso instanceof Community) {
            try {
                return itemService.countItems(context, ((Community) dso));
            } catch (SQLException e) {
                log.error("caught exception: ", e);
                throw new ItemCountException(e);
            }
        }

        return 0;
    }
}
