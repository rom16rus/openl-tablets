package org.openl.rules.webstudio.web.repository.merge;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.webstudio.util.WebTool;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.workspace.MultiUserWorkspaceManager;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.IOUtils;
import org.openl.util.StringTool;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@Path("/conflict/")
public class ConflictService {
    private static final Logger LOG = LoggerFactory.getLogger(ConflictService.class);

    private final MultiUserWorkspaceManager workspaceManager;
    private final UserManagementService userManagementService;

    public ConflictService(MultiUserWorkspaceManager workspaceManager, UserManagementService userManagementService) {
        this.workspaceManager = workspaceManager;
        this.userManagementService = userManagementService;
    }

    @GET
    @Path("repository")
    @Produces("application/octet-stream")
    public Response repository(@QueryParam(Constants.REQUEST_PARAM_REPO_ID) final String repoId,
            @QueryParam(Constants.REQUEST_PARAM_NAME) final String name,
            @QueryParam(Constants.REQUEST_PARAM_VERSION) final String version,
            @QueryParam(Constants.RESPONSE_MONITOR_COOKIE) String cookieId,
            @Context HttpServletRequest request) {

        String cookieName = Constants.RESPONSE_MONITOR_COOKIE + "_" + cookieId;
        StreamingOutput streamingOutput = output -> {
            InputStream stream = null;
            try {
                FileItem file = workspaceManager.getUserWorkspace(getUser())
                    .getDesignTimeRepository()
                    .getRepository(repoId)
                    .readHistory(name, version);
                if (file == null) {
                    throw new FileNotFoundException(String.format("File '%s' is not found.", name));
                }

                stream = file.getStream();
                IOUtils.copy(stream, output);
                output.flush();
            } finally {
                IOUtils.closeQuietly(stream);
            }
        };

        return prepareResponse(request, cookieName, name, streamingOutput);
    }

    @GET
    @Path("local")
    @Produces("application/octet-stream")
    public Response local(@QueryParam(Constants.REQUEST_PARAM_REPO_ID) final String repoId,
            @QueryParam(Constants.REQUEST_PARAM_NAME) final String name,
            @QueryParam(Constants.RESPONSE_MONITOR_COOKIE) String cookieId,
            @Context HttpServletRequest request) {

        String cookieName = Constants.RESPONSE_MONITOR_COOKIE + "_" + cookieId;
        StreamingOutput streamingOutput = output -> {
            InputStream stream = null;
            try {
                UserWorkspace userWorkspace = workspaceManager.getUserWorkspace(getUser());
                Optional<RulesProject> projectByPath = userWorkspace.getProjectByPath(repoId, name);
                if (projectByPath.isPresent()) {
                    RulesProject project = projectByPath.get();
                    String artefactPath = name.substring(project.getRealPath().length() + 1);
                    if (project.hasArtefact(artefactPath)) {
                        stream = ((AProjectResource) project.getArtefact(artefactPath)).getContent();
                        IOUtils.copy(stream, output);
                        output.flush();
                        return;
                    }
                }
                throw new FileNotFoundException(String.format("File %s is not found.", name));
            } catch (ProjectException e) {
                LOG.warn(e.getMessage(), e);
                throw new IOException(e.getMessage(), e);
            } finally {
                IOUtils.closeQuietly(stream);
            }
        };

        return prepareResponse(request, cookieName, name, streamingOutput);
    }

    @GET
    @Path("merged")
    @Produces("application/octet-stream")
    public Response merged(@QueryParam(Constants.REQUEST_PARAM_NAME) final String name,
            @QueryParam(Constants.RESPONSE_MONITOR_COOKIE) String cookieId,
            @Context HttpServletRequest request) {

        StreamingOutput streamingOutput = output -> {
            Map<String, ConflictResolution> conflictResolutions = ConflictUtils
                .getResolutionsFromSession(request.getSession());
            ConflictResolution conflictResolution = conflictResolutions.get(name);
            try (InputStream input = conflictResolution.getCustomResolutionFile().getInput()) {
                IOUtils.copy(input, output);
            } finally {
                output.flush();
            }
        };

        String cookieName = Constants.RESPONSE_MONITOR_COOKIE + "_" + cookieId;
        return prepareResponse(request, cookieName, name, streamingOutput);
    }

    private Response prepareResponse(HttpServletRequest request,
            String cookieName,
            String filePath,
            StreamingOutput streamingOutput) {
        try {
            String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
            return Response.ok(streamingOutput)
                .cookie(newCookie(cookieName, "success", request.getContextPath()))
                .header("Content-Disposition", WebTool.getContentDispositionValue(fileName))
                .build();
        } catch (Exception e) {
            String message = "Failed to download file.";
            LOG.error(message, e);

            return Response.status(Response.Status.NOT_FOUND)
                .entity(e.getMessage())
                .cookie(newCookie(cookieName, message, request.getContextPath()))
                .build();
        }
    }

    private static NewCookie newCookie(String cookieName, String value, String contextPath) {
        if (StringUtils.isEmpty(contextPath)) {
            contextPath = "/"; // //EPBDS-7613
        }

        return new NewCookie(cookieName,
            StringTool.encodeURL(value),
            contextPath,
            null,
            1,
            null,
            -1,
            null,
            false,
            false); // Has to be visible from client scripting
    }

    private WorkspaceUserImpl getUser() {
        return new WorkspaceUserImpl(getUserName(),
            (username) -> Optional.ofNullable(userManagementService.getUser(username))
                .map(usr -> new UserInfo(usr.getUsername(), usr.getEmail(), usr.getDisplayName()))
                .orElse(null));
    }

    private static String getUserName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

}
