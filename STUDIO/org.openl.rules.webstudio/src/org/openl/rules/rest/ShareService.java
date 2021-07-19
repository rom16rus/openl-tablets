package org.openl.rules.rest;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.openl.rules.rest.dto.AccessEntryDTO;
import org.openl.rules.rest.dto.GroupAccessDTO;
import org.openl.rules.rest.dto.SecurityObjectDTO;
import org.openl.rules.rest.dto.UserAccessDTO;
import org.openl.rules.security.standalone.persistence.OpenLAccessEntry;
import org.openl.rules.security.standalone.persistence.OpenLGroupAccessEntry;
import org.openl.rules.security.standalone.persistence.OpenLUserAccessEntry;
import org.openl.rules.security.standalone.persistence.SecurityObjectType;
import org.openl.rules.webstudio.service.AccessManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Path("/access/")
public class ShareService {

    private final AccessManagementService accessManagementService;

    @Autowired
    public ShareService(AccessManagementService accessManagementService) {
        this.accessManagementService = accessManagementService;
    }

    @GET
    @Produces("application/json")
    public Response findObjectAccessList(@QueryParam("objectType") String objectType,
            @QueryParam("objectName") String objectName) {
        // TODO: fixme
        final List<OpenLAccessEntry> objectAccessRights = accessManagementService
            .getObjectAccessRights(SecurityObjectType.valueOf(objectType), objectName);
        List<AccessEntryDTO> resultList = new ArrayList<>();
        for (OpenLAccessEntry objectAccessRight : objectAccessRights) {
            if (objectAccessRight instanceof OpenLUserAccessEntry) {
                final OpenLUserAccessEntry userAccessEntry = (OpenLUserAccessEntry) objectAccessRight;
                UserAccessDTO userAccessDto = new UserAccessDTO(objectAccessRight.getId(),
                    objectAccessRight.getAccessLevel().name(),
                    userAccessEntry.getUser().getLoginName(),
                    null);
                resultList.add(userAccessDto);
            } else if (objectAccessRight instanceof OpenLGroupAccessEntry) {
                final OpenLGroupAccessEntry groupAccessEntry = (OpenLGroupAccessEntry) objectAccessRight;
                GroupAccessDTO groupAccessDTO = new GroupAccessDTO(groupAccessEntry.getId(),
                    groupAccessEntry.getAccessLevel().name(),
                    groupAccessEntry.getGroup().getName(),
                    null);
                resultList.add(groupAccessDTO);
            }
        }
        return Response.ok(resultList).build();
    }

    @POST
    public void editAccess(SecurityObjectDTO securityObjectDTO) {

    }

}
