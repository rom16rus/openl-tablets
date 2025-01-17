package org.openl.rules.webstudio.web.repository;

import static org.openl.rules.security.AccessManager.isGranted;
import static org.openl.rules.security.Privileges.CREATE_DEPLOYMENT;
import static org.openl.rules.security.Privileges.CREATE_PROJECTS;
import static org.openl.rules.security.Privileges.DELETE_DEPLOYMENT;
import static org.openl.rules.security.Privileges.DELETE_PROJECTS;
import static org.openl.rules.security.Privileges.DEPLOY_PROJECTS;
import static org.openl.rules.security.Privileges.EDIT_DEPLOYMENT;
import static org.openl.rules.security.Privileges.EDIT_PROJECTS;
import static org.openl.rules.security.Privileges.ERASE_PROJECTS;
import static org.openl.rules.security.Privileges.VIEW_PROJECTS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.context.FacesContext;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.project.resolving.ProjectDescriptorArtefactResolver;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.security.standalone.persistence.OpenLProject;
import org.openl.rules.security.standalone.persistence.ProjectGrouping;
import org.openl.rules.security.standalone.persistence.Tag;
import org.openl.rules.security.standalone.persistence.TagType;
import org.openl.rules.webstudio.filter.AllFilter;
import org.openl.rules.webstudio.filter.IFilter;
import org.openl.rules.webstudio.security.CurrentUserInfo;
import org.openl.rules.webstudio.service.OpenLProjectService;
import org.openl.rules.webstudio.service.ProjectGroupingService;
import org.openl.rules.webstudio.service.TagService;
import org.openl.rules.webstudio.service.TagTypeService;
import org.openl.rules.webstudio.web.repository.tree.AbstractTreeNode;
import org.openl.rules.webstudio.web.repository.tree.TreeDProject;
import org.openl.rules.webstudio.web.repository.tree.TreeFile;
import org.openl.rules.webstudio.web.repository.tree.TreeFolder;
import org.openl.rules.webstudio.web.repository.tree.TreeNode;
import org.openl.rules.webstudio.web.repository.tree.TreeProject;
import org.openl.rules.webstudio.web.repository.tree.TreeProjectGrouping;
import org.openl.rules.webstudio.web.repository.tree.TreeRepository;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.dtr.DesignTimeRepositoryListener;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.UserWorkspaceListener;
import org.openl.util.StringUtils;
import org.richfaces.component.UITree;
import org.richfaces.event.TreeSelectionChangeEvent;
import org.richfaces.model.SequenceRowKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

/**
 * Used for holding information about rulesRepository tree.
 *
 * @author Andrey Naumenko
 */
@Service
@SessionScope
public class RepositoryTreeState implements DesignTimeRepositoryListener {
    private static final String ROOT_TYPE = "root";

    @Autowired
    private RepositorySelectNodeStateHolder repositorySelectNodeStateHolder;
    @Autowired
    private ProjectDescriptorArtefactResolver projectDescriptorResolver;

    @Autowired
    private CurrentUserInfo currentUserInfo;

    @Autowired
    private TagTypeService tagTypeService;

    @Autowired
    private TagService tagService;

    @Autowired
    private ProjectGroupingService projectGroupingService;

    @Autowired
    private OpenLProjectService projectService;

    private static final String DEFAULT_TAB = "Properties";
    private final Logger log = LoggerFactory.getLogger(RepositoryTreeState.class);
    private static final IFilter<AProjectArtefact> ALL_FILTER = new AllFilter<>();

    private RepositorySelectNodeStateHolder.SelectionHolder selectionHolder;
    /**
     * Root node for RichFaces's tree. It is not displayed.
     */
    private TreeRepository root;
    private TreeRepository previousRoot;
    private TreeRepository rulesRepository;
    private TreeRepository deploymentRepository;

    private UserWorkspace userWorkspace;

    // We can use it for filter on server-side in the future. Currently it's not used.
    private final IFilter<AProjectArtefact> filter = ALL_FILTER;
    private boolean hideDeleted = true;

    private final Object lock = new Object();
    private String errorMessage;
    private final WorkspaceListener workspaceListener = new WorkspaceListener();

    private ProjectGrouping projectGrouping;

    private void buildTree() {
        try {
            if (root != null) {
                return;
            }
            log.debug("Starting buildTree()");

            root = new TreeRepository("", "", filter, ROOT_TYPE);

            String projectsTreeId = "1st - Projects";
            String rpName = "Projects";
            rulesRepository = new TreeRepository(projectsTreeId, rpName, filter, UiConst.TYPE_REPOSITORY);
            rulesRepository.setData(null);

            String deploymentsTreeId = "2nd - Deploy Configurations";
            String dpName = "Deploy Configurations";
            deploymentRepository = new TreeRepository(deploymentsTreeId,
                dpName,
                filter,
                UiConst.TYPE_DEPLOYMENT_REPOSITORY);
            deploymentRepository.setData(null);

            // Such keys are used for correct order of repositories.
            root.add(rulesRepository);
            root.add(deploymentRepository);

            Collection<RulesProject> rulesProjects = userWorkspace.getProjects();

            IFilter<AProjectArtefact> filter = this.filter;
            final ProjectGrouping grouping = getProjectGrouping();
            final String group1 = grouping.getGroup1();
            if (StringUtils.isBlank(group1)) {
                for (RulesProject project : rulesProjects) {
                    if (!(filter.supports(RulesProject.class) && !filter.select(project))) {
                        addRulesProjectToTree(project);
                    }
                }
            } else {
                final List<Repository> repositories = userWorkspace.getDesignTimeRepository().getRepositories();

                if (TreeProjectGrouping.GROUPING_REPOSITORY.equals(group1)) {
                    List<RulesProject> withoutRepo = new ArrayList<>(rulesProjects);

                    repositories.forEach(repository -> {
                        final String repoId = repository.getId();
                        final String name = "[" + repository.getName() + "]";
                        final String id = RepositoryUtils.getTreeNodeId(repoId);

                        final List<RulesProject> subProjects = rulesProjects.stream()
                            .filter(project -> project.getRepository().getId().equals(repoId))
                            .collect(Collectors.toList());

                        if (!subProjects.isEmpty()) {
                            rulesRepository.add(new TreeProjectGrouping(id,
                                name,
                                subProjects,
                                grouping,
                                1,
                                tagService,
                                hideDeleted,
                                projectDescriptorResolver,
                                projectService,
                                repositories));

                            withoutRepo.removeAll(subProjects);
                        }
                    });

                    // Local projects
                    for (RulesProject project : withoutRepo) {
                        if (!(filter.supports(RulesProject.class) && !filter.select(project))) {
                            addRulesProjectToTree(project);
                        }
                    }
                } else {
                    final List<Tag> tags = tagService.getByTagType(group1);
                    List<RulesProject> withoutTags = new ArrayList<>(rulesProjects);

                    tags.forEach(tag -> {
                        final String name = tag.getName();
                        final String id = RepositoryUtils.getTreeNodeId(name);

                        final List<OpenLProject> projectsForTags = projectService.getProjectsForTag(tag.getId());

                        final List<RulesProject> subProjects = rulesProjects.stream()
                            .filter(project -> projectsForTags.stream()
                                .anyMatch(p -> p.getProjectPath().equals(project.getRealPath())))
                            .collect(Collectors.toList());

                        if (!subProjects.isEmpty()) {
                            rulesRepository.add(new TreeProjectGrouping(id,
                                name,
                                subProjects,
                                grouping,
                                1,
                                tagService,
                                hideDeleted,
                                projectDescriptorResolver,
                                projectService,
                                repositories));

                            withoutTags.removeAll(subProjects);
                        }
                    });

                    for (RulesProject project : withoutTags) {
                        if (!(filter.supports(RulesProject.class) && !filter.select(project))) {
                            addRulesProjectToTree(project);
                        }
                    }
                }
            }
            if (rulesProjects.isEmpty()) {
                // Initialize content of empty node
                rulesRepository.getElements();
            }

            if (previousRoot != null) {
                syncExpandedState(previousRoot, root);

                previousRoot = null;
            }

            try {
                List<ADeploymentProject> deployConfigurations = userWorkspace.getDDProjects();
                for (ADeploymentProject project : deployConfigurations) {
                    addDeploymentProjectToTree(project);
                }
                if (deployConfigurations.isEmpty()) {
                    // Initialize content of empty node
                    deploymentRepository.getElements();
                }
                List<String> exceptions = userWorkspace.getDesignTimeRepository().getExceptions();
                if (!exceptions.isEmpty()) {
                    errorMessage = exceptions.get(0);
                }
            } catch (ProjectException e) {
                log.error("Cannot get deployment projects", e);
            }
            log.debug("Finishing buildTree()");

            if (getSelectedNode() == null || UiConst.TYPE_REPOSITORY.equals(getSelectedNode().getType())) {
                setSelectedNode(rulesRepository);
            } else if (UiConst.TYPE_DEPLOYMENT_REPOSITORY.equals(getSelectedNode().getType())) {
                setSelectedNode(deploymentRepository);
            } else if (UiConst.TYPE_GROUP.equals(getSelectedNode().getType())) {
                setSelectedNode(findNodeById(rulesRepository, getSelectedNode().getId()));
            } else {
                updateSelectedNode();
            }
        } catch (Exception e) {
            // Should never happen
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            String message = "Cannot build repository tree. " + (rootCause == null ? e.getMessage()
                                                                                   : rootCause.getMessage());
            log.error(message, e);
            errorMessage = message;
            setSelectedNode(rulesRepository);
        }
    }

    private void syncExpandedState(AbstractTreeNode prev, AbstractTreeNode curr) {
        curr.setExpanded(prev.isExpanded());

        List<TreeNode> currChildren = curr.getChildNodes();
        for (TreeNode currChild : currChildren) {
            final TreeNode prevChild = prev.getChild(currChild.getId());
            if (prevChild != null) {
                syncExpandedState(((AbstractTreeNode) prevChild), (AbstractTreeNode) currChild);
            }
        }
    }

    public TreeRepository getDeploymentRepository() {
        synchronized (lock) {
            buildTree();
            return deploymentRepository;
        }
    }

    TreeRepository getRoot() {
        synchronized (lock) {
            buildTree();
            return root;
        }
    }

    public void expandAll() {
        synchronized (lock) {
            expandAll(this.root, true);
        }
    }

    public void collapseAll() {
        synchronized (lock) {
            expandAll(this.root, false);
        }
    }

    private void expandAll(TreeNode node, boolean expand) {
        // Expand only Repository and Grouping (Tag) nodes up to project. Collapse all nodes without exception.
        if (!expand || node instanceof TreeRepository || node instanceof TreeProjectGrouping) {
            ((AbstractTreeNode) node).setExpanded(expand);

            for (TreeNode child : node.getChildNodes()) {
                expandAll(child, expand);
            }
        }
    }

    String getErrorMessage() {
        return errorMessage;
    }

    public TreeRepository getRulesRepository() {
        synchronized (lock) {
            buildTree();
            return rulesRepository;
        }
    }

    public TreeNode getSelectedNode() {
        synchronized (lock) {
            buildTree();
            return this.repositorySelectNodeStateHolder.getSelectedNode();
        }
    }

    public Collection<SequenceRowKey> getSelection() {
        TreeNode node = getSelectedNode();

        List<String> ids = new ArrayList<>();
        while (node != null && !node.getType().equals(ROOT_TYPE)) {
            ids.add(0, node.getId());
            node = node.getParent();
        }

        return new ArrayList<>(Collections.singletonList(new SequenceRowKey(ids.toArray())));
    }

    TreeProject getProjectNodeByBusinessName(String repoId, String businessName) {
        return findProjectNode(getRulesRepository().getChildNodes(),
            project -> ((RulesProject) project.getData()).getBusinessName()
                .equals(businessName) && (repoId == null || repoId.equals(project.getData().getRepository().getId())));
    }

    public TreeProject getProjectNodeByPhysicalName(String repoId, String physicalName) {
        return findProjectNode(getRulesRepository().getChildNodes(),
            project -> project.getData()
                .getName()
                .equals(physicalName) && (repoId == null || repoId.equals(project.getData().getRepository().getId())));
    }

    private TreeProject findProjectNode(List<TreeNode> nodes, Predicate<TreeProject> predicate) {
        for (TreeNode node : nodes) {
            if (node instanceof TreeProject) {
                TreeProject project = (TreeProject) node;
                if (predicate.test(project)) {
                    return project;
                }
            } else if (node instanceof TreeProjectGrouping) {
                final TreeProject childNode = findProjectNode(node.getChildNodes(), predicate);
                if (childNode != null) {
                    return childNode;
                }
            }
        }
        return null;
    }

    public UserWorkspaceProject getSelectedProject() {
        return getProject(getSelectedNode());
    }

    public UserWorkspaceProject getProject(TreeNode node) {
        AProjectArtefact artefact = node.getData();
        if (artefact instanceof UserWorkspaceProject) {
            return (UserWorkspaceProject) artefact;
        } else if (artefact != null && artefact.getProject() instanceof UserWorkspaceProject) {
            return (UserWorkspaceProject) artefact.getProject();
        }

        return null;
    }

    public boolean isSelectedProjectModified() {
        UserWorkspaceProject selectedProject = getSelectedProject();
        return selectedProject != null && selectedProject.isModified();
    }

    public void invalidateSelection() {
        setSelectedNode(rulesRepository);
    }

    /**
     * Refreshes repositoryTreeState.selectedNode after rebuilding tree.
     */
    private void updateSelectedNode() {
        AProjectArtefact artefact = getSelectedNode().getData();
        if (artefact == null) {
            return;
        }

        String branch = null;
        AProject project = artefact.getProject();
        if (project instanceof UserWorkspaceProject) {
            branch = ((UserWorkspaceProject) project).getBranch();
        }

        TreeNode currentNode;

        if (project instanceof ADeploymentProject) {
            currentNode = getDeploymentRepository();
            String id = RepositoryUtils.getTreeNodeId(project);
            currentNode = (TreeNode) currentNode.getChild(id);
        } else {
            String repoId = artefact.getRepository().getId();
            Iterator<String> it = artefact.getArtefactPath().getSegments().iterator();
            currentNode = getRulesRepository();
            while (currentNode != null && it.hasNext()) {
                String id = RepositoryUtils.getTreeNodeId(repoId, it.next());
                TreeNode parentNode = currentNode;
                currentNode = findNodeById(currentNode, id);

                if (currentNode == null) {
                    if (artefact instanceof AProject) {
                        String actualPath = ((AProject) artefact).getRealPath();
                        currentNode = getAllProjectNodes(parentNode).stream()
                            .filter(child -> actualPath.equals(((AProject) child.getData()).getRealPath()))
                            .findFirst()
                            .orElse(null);
                    }
                }

                if (branch != null && currentNode != null) {
                    // If currentNode is a project, update its branch.
                    AProjectArtefact currentArtefact = currentNode.getData();
                    if (currentArtefact instanceof UserWorkspaceProject) {
                        UserWorkspaceProject newProject = (UserWorkspaceProject) currentArtefact;
                        if (!branch.equals(newProject.getBranch())) {
                            try {
                                RulesProject rulesProject = (RulesProject) project;
                                boolean containsBranch = ((BranchRepository) rulesProject.getDesignRepository())
                                    .getBranches(((RulesProject) project).getDesignFolderName())
                                    .contains(branch);
                                if (containsBranch) {
                                    // Update branch for the project
                                    newProject.setBranch(branch);
                                    // Rebuild children for the node
                                    currentNode.refresh();
                                }
                            } catch (ProjectException | IOException e) {
                                log.error("Cannot update selected node: {}", e.getMessage(), e);
                            }
                        }
                    }
                }
            }
        }

        if (currentNode != null) {
            setSelectedNode(currentNode);
        } else {
            invalidateSelection();
        }
    }

    private List<TreeNode> getAllProjectNodes(TreeNode parentNode) {
        List<TreeNode> nodes = new ArrayList<>();
        for (TreeNode child : parentNode.getChildNodes()) {
            if (child instanceof TreeRepository || child instanceof TreeProjectGrouping) {
                nodes.addAll(getAllProjectNodes(child));
            } else if (child.getData() instanceof AProject) {
                nodes.add(child);
            }
        }
        return nodes;
    }

    TreeNode findNodeById(TreeNode currentNode, String id) {
        final TreeNode child = (TreeNode) currentNode.getChild(id);
        if (child != null) {
            return child;
        }
        if (currentNode instanceof TreeRepository || currentNode instanceof TreeProjectGrouping) {
            for (TreeNode childNode : currentNode.getChildNodes()) {
                final TreeNode n = findNodeById(childNode, id);
                if (n != null) {
                    return n;
                }
            }
        }

        return null;
    }

    public void refreshNode(TreeNode node) {
        node.refresh();
    }

    public void deleteNode(TreeNode node) {
        node.getParent().removeChild(node.getId());
    }

    public void deleteSelectedNodeFromTree() {
        synchronized (lock) {
            TreeNode selectedNode = getSelectedNode();
            if (selectedNode != root && selectedNode != rulesRepository && selectedNode != deploymentRepository) {
                deleteNode(selectedNode);
                moveSelectionToParentNode();
            }
        }
    }

    public void addDeploymentProjectToTree(ADeploymentProject project) {
        String name = project.getName();
        String id = RepositoryUtils.getTreeNodeId(project);
        if (!project.isDeleted() || !hideDeleted) {
            TreeDProject prj = new TreeDProject(id, name);
            prj.setData(project);
            deploymentRepository.add(prj);
        }
    }

    void addRulesProjectToTree(RulesProject project) {
        String name = project.getMainBusinessName();
        String id = RepositoryUtils.getTreeNodeId(project);
        if (!project.isDeleted() || !hideDeleted) {
            TreeProject prj = new TreeProject(id, name, filter, projectDescriptorResolver);
            prj.setData(project);
            rulesRepository.add(prj);
        }
    }

    public void addNodeToTree(TreeNode parent, AProjectArtefact childArtefact) {
        String name = childArtefact.getName();
        String id = RepositoryUtils.getTreeNodeId(childArtefact);
        if (childArtefact.isFolder()) {
            TreeFolder treeFolder = new TreeFolder(id, name, filter);
            treeFolder.setData(childArtefact);
            parent.add(treeFolder);
        } else {
            TreeFile treeFile = new TreeFile(id, name);
            treeFile.setData(childArtefact);
            parent.add(treeFile);
        }
    }

    /**
     * Forces tree rebuild during next access.
     */
    public void invalidateTree() {
        synchronized (lock) {
            backupRoot();
            root = null;
            errorMessage = null;
            projectGrouping = null;

            // Clear all ViewScoped beans that could cache some temporary values (for example DeploymentController).
            // Because selection is invalidated too we can assume that view is changed so we can safely clear all
            // views scoped beans.
            FacesContext.getCurrentInstance().getViewRoot().getViewMap().clear();
        }
    }

    /**
     * Moves selection to the parent of the current selected node.
     */
    private void moveSelectionToParentNode() {
        if (getSelectedNode().getParent() != null) {
            setSelectedNode(getSelectedNode().getParent());
        } else {
            invalidateSelection();
        }
    }

    public void processSelection(TreeSelectionChangeEvent event) {
        List<Object> selection = new ArrayList<>(event.getNewSelection());

        /* If there are no selected nodes */
        if (selection.isEmpty()) {
            return;
        }

        Object currentSelectionKey = selection.get(0);
        UITree tree = (UITree) event.getSource();

        Object storedKey = tree.getRowKey();
        tree.setRowKey(currentSelectionKey);
        setSelectedNode((TreeNode) tree.getRowData());
        tree.setRowKey(storedKey);
    }

    public void rulesRepositorySelection() {
        setSelectedNode(rulesRepository);
    }

    /**
     * Refreshes repositoryTreeState.selectedNode.
     */
    public void refreshSelectedNode() {
        refreshNode(getSelectedNode());
    }

    private void onFilterChanged() {
        synchronized (lock) {
            backupRoot();
            root = null;
            errorMessage = null;
        }
    }

    public void filter() {
        onFilterChanged();
        setHideDeleted(hideDeleted);
    }

    public void setSelectedNode(TreeNode selectedNode) {
        selectionHolder.setSelectedNode(selectedNode);
    }

    @PostConstruct
    public void init() {
        selectionHolder = repositorySelectNodeStateHolder.getSelectionHolder();

        this.userWorkspace = WebStudioUtils.getUserWorkspace(WebStudioUtils.getSession());
        userWorkspace.getDesignTimeRepository().addListener(this);
        userWorkspace.addWorkspaceListener(workspaceListener);
    }

    @PreDestroy
    public void destroy() {
        if (userWorkspace != null) {
            userWorkspace.getDesignTimeRepository().removeListener(this);
            userWorkspace.removeWorkspaceListener(workspaceListener);
        }
    }

    @Override
    public void onRepositoryModified() {
        Collection<RulesProject> projects;
        List<ADeploymentProject> deployConfigs;
        try {
            projects = userWorkspace.getProjects(false);
            deployConfigs = userWorkspace.getDDProjects();
        } catch (ProjectException e) {
            log.error(e.getMessage(), e);
            return;
        }
        synchronized (lock) {
            // We must not refresh the table when getting selected node.
            TreeNode selectedNode = selectionHolder.getSelectedNode();
            AProjectArtefact artefact = selectedNode == null ? null : selectedNode.getData();
            if (artefact != null) {
                AProject project = artefact instanceof UserWorkspaceProject ? (UserWorkspaceProject) artefact
                                                                            : artefact.getProject();

                String name = project.getName();
                if (project instanceof RulesProject) {
                    // We cannot use hasProject() and then getProject(name) in multithreaded environment
                    invalidateSelectionIfDeleted(name, projects);
                } else if (project instanceof ADeploymentProject) {
                    // We cannot use hasDDProject() and then getDDProject(name) in multithreaded environment
                    invalidateSelectionIfDeleted(name, deployConfigs);
                }
            }

            backupRoot();
            root = null;
            errorMessage = null;
        }
    }

    private void invalidateSelectionIfDeleted(String name,
            Collection<? extends UserWorkspaceProject> existingProjects) {
        UserWorkspaceProject existing = null;
        for (UserWorkspaceProject existingProject : existingProjects) {
            if (name.equals(existingProject.getName())) {
                existing = existingProject;
            }
        }
        if (existing == null || existing.isDeleted() && isHideDeleted()) {
            invalidateSelection();
        }
    }

    public boolean isHideDeleted() {
        return hideDeleted;
    }

    public void setHideDeleted(boolean hideDeleted) {
        this.hideDeleted = hideDeleted;
    }

    public boolean getCanCreate() {
        return isGranted(CREATE_PROJECTS);
    }

    // For any project
    public boolean getCanEdit() {
        UserWorkspaceProject selectedProject = getSelectedProject();
        if (selectedProject == null || selectedProject.isLocalOnly() || selectedProject
            .isOpenedForEditing() || selectedProject.isLocked()) {
            return false;
        }

        return isGranted(EDIT_PROJECTS);
    }

    public boolean getCanCreateDeployment() {
        return isGranted(CREATE_DEPLOYMENT) && !isMainBranchProtected(
            userWorkspace.getDesignTimeRepository().getDeployConfigRepository());
    }

    private boolean isMainBranchProtected(Repository repo) {
        if (repo.supports().branches()) {
            BranchRepository branchRepo = (BranchRepository) repo;
            return branchRepo.isBranchProtected(branchRepo.getBranch());
        }
        return false;
    }

    public boolean getCanEditDeployment() {
        UserWorkspaceProject selectedProject = getSelectedProject();
        if (selectedProject.isLocalOnly() || selectedProject.isOpenedForEditing() || selectedProject.isLocked()) {
            return false;
        }

        return isGranted(EDIT_DEPLOYMENT) && !isCurrentBranchProtected(selectedProject);
    }

    public boolean getCanDeleteDeployment() {
        UserWorkspaceProject selectedProject = getSelectedProject();
        if (selectedProject.isLocalOnly()) {
            // any user can delete own local project
            return true;
        }
        return (!selectedProject.isLocked() || selectedProject.isLockedByUser(userWorkspace.getUser())) && isGranted(
            DELETE_DEPLOYMENT) && !isCurrentBranchProtected(selectedProject);
    }

    public boolean getCanSaveDeployment() {
        ADeploymentProject selectedProject = (ADeploymentProject) getSelectedProject();
        return selectedProject.isOpenedForEditing() && selectedProject
            .isModified() && isGranted(EDIT_DEPLOYMENT) && !isCurrentBranchProtected(selectedProject);
    }

    public boolean getCanSaveProject() {
        try {
            UserWorkspaceProject selectedProject = getSelectedProject();
            return selectedProject != null && selectedProject.isModified() && isGranted(
                EDIT_PROJECTS) && !isCurrentBranchProtected(selectedProject);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    public boolean getCanClose() {
        UserWorkspaceProject selectedProject = getSelectedProject();

        if (selectedProject != null) {
            return !selectedProject.isLocalOnly() && selectedProject.isOpened();
        } else {
            return false;
        }
    }

    public boolean getCanDelete() {
        try {
            UserWorkspaceProject selectedProject = getSelectedProject();
            if (selectedProject.isLocalOnly()) {
                // any user can delete own local project
                return true;
            }
            boolean unlocked = !selectedProject.isLocked() || selectedProject.isLockedByUser(userWorkspace.getUser());
            boolean mainBranch = isMainBranch(selectedProject);
            boolean branchProtected = isCurrentBranchProtected(selectedProject);
            return unlocked && isGranted(DELETE_PROJECTS) && mainBranch && !branchProtected;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    public boolean getCanDeleteBranch() {
        try {
            UserWorkspaceProject selectedProject = getSelectedProject();
            if (selectedProject.isLocalOnly()) {
                return false;
            }
            boolean unlocked = !selectedProject.isLocked() || selectedProject.isLockedByUser(userWorkspace.getUser());
            boolean mainBranch = isMainBranch(selectedProject);
            boolean branchProtected = isCurrentBranchProtected(selectedProject);
            return unlocked && isGranted(DELETE_PROJECTS) && !mainBranch && !branchProtected;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    private boolean isMainBranch(UserWorkspaceProject selectedProject) {
        boolean mainBranch = true;
        Repository designRepository = selectedProject.getDesignRepository();
        if (designRepository.supports().branches()) {
            String branch = selectedProject.getBranch();
            if (!((BranchRepository) designRepository).getBaseBranch().equals(branch)) {
                mainBranch = false;
            }
        }
        return mainBranch;
    }

    private boolean isCurrentBranchProtected(UserWorkspaceProject selectedProject) {
        Repository repo = selectedProject.getDesignRepository();
        if (repo != null && repo.supports().branches()) {
            return ((BranchRepository) repo).isBranchProtected(selectedProject.getBranch());
        }
        return false;
    }

    public boolean getCanErase() {
        UserWorkspaceProject project = getSelectedProject();
        boolean branchProtected = isCurrentBranchProtected(project);
        return project.isDeleted() && isGranted(ERASE_PROJECTS) && isMainBranch(project) && !branchProtected;
    }

    public boolean getCanOpen() {
        try {
            UserWorkspaceProject selectedProject = getSelectedProject();
            if (selectedProject == null || selectedProject.isLocalOnly() || selectedProject
                .isOpenedForEditing() || selectedProject.isOpened()) {
                return false;
            }

            return isGranted(VIEW_PROJECTS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    public boolean getCanOpenOtherVersion() {
        UserWorkspaceProject selectedProject = getSelectedProject();

        if (selectedProject == null) {
            return false;
        }

        if (!selectedProject.isLocalOnly()) {
            return isGranted(VIEW_PROJECTS);
        }

        return false;
    }

    public boolean getCanExport() {
        return !getSelectedProject().isLocalOnly();
    }

    public boolean getCanCompare() {
        if (getSelectedProject().isLocalOnly()) {
            return false;
        }
        return isGranted(VIEW_PROJECTS);
    }

    public boolean getCanRedeploy() {
        try {
            UserWorkspaceProject selectedProject = getSelectedProject();
            if (selectedProject == null || selectedProject.isLocalOnly() || selectedProject.isModified()) {
                return false;
            }

            return isGranted(DEPLOY_PROJECTS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    public boolean getCanUndelete() {
        UserWorkspaceProject project = getSelectedProject();
        boolean branchProtected = isCurrentBranchProtected(project);
        return project.isDeleted() && isGranted(EDIT_PROJECTS) && isMainBranch(project) && !branchProtected;
    }

    // for any project artefact
    public boolean getCanModify() {
        UserWorkspaceProject project = getSelectedProject();
        if (project != null) {
            boolean branchProtected = isCurrentBranchProtected(project);
            return project.isOpenedForEditing() && isGranted(EDIT_PROJECTS) && !branchProtected;
        } else {
            return false;
        }
    }

    public boolean getCanModifyTags() {
        return isGranted(EDIT_PROJECTS);
    }

    // for deployment project
    public boolean getCanDeploy() {
        return !getSelectedProject().isModified() && isGranted(DEPLOY_PROJECTS);
    }

    public boolean getCanMerge() {
        if (!isSupportsBranches() || isLocalOnly()) {
            return false;
        }

        try {
            UserWorkspaceProject project = getSelectedProject();
            if (project.isModified() || !(project instanceof RulesProject)) {
                return false;
            }
            List<String> branches = ((BranchRepository) project.getDesignRepository())
                .getBranches(((RulesProject) project).getDesignFolderName());
            if (branches.size() < 2) {
                return false;
            }

            return isGranted(EDIT_PROJECTS);
        } catch (IOException e) {
            return false;
        }
    }

    public String getDefSelectTab() {
        return DEFAULT_TAB;
    }

    public boolean isLocalOnly() {
        return getSelectedProject().isLocalOnly();
    }

    public String clearSelectPrj() {
        synchronized (lock) {
            buildTree();
            invalidateSelection();
        }

        return "";
    }

    public void setRepositorySelectNodeStateHolder(RepositorySelectNodeStateHolder repositorySelectNodeStateHolder) {
        this.repositorySelectNodeStateHolder = repositorySelectNodeStateHolder;
    }

    public void setProjectDescriptorResolver(ProjectDescriptorArtefactResolver projectDescriptorResolver) {
        this.projectDescriptorResolver = projectDescriptorResolver;
    }

    /**
     * Returns true if both are true: 1) Old project version is opened and 2) project is not modified yet.
     *
     * Otherwise return false
     */
    public boolean isConfirmOverwriteNewerRevision() {
        UserWorkspaceProject project = getSelectedProject();
        return project != null && project.isOpenedOtherVersion() && !project.isModified();
    }

    /**
     * Checks if selected project supports branches
     */
    public boolean isSupportsBranches() {
        try {
            UserWorkspaceProject project = getSelectedProject();
            return project != null && project.isSupportsBranches();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    public List<TagType> getTagTypes() {
        return tagTypeService.getAllTagTypes();
    }

    public List<String> getGroupingTypes() {
        final List<String> types = new ArrayList<>();
        types.add(TreeProjectGrouping.GROUPING_NONE);
        types.add(TreeProjectGrouping.GROUPING_REPOSITORY);
        types.addAll(tagTypeService.getAllTagTypes().stream().map(TagType::getName).collect(Collectors.toList()));
        return types;
    }

    public ProjectGrouping getProjectGrouping() {
        if (projectGrouping == null) {
            final String userName = currentUserInfo.getUserName();
            projectGrouping = projectGroupingService.getProjectGrouping(userName);
            if (projectGrouping == null) {
                projectGrouping = new ProjectGrouping();
                projectGrouping.setLoginName(userName);
            }
        }
        final List<String> groupings = tagTypeService.getAllTagTypes()
            .stream()
            .map(TagType::getName)
            .collect(Collectors.toList());
        groupings.add(TreeProjectGrouping.GROUPING_REPOSITORY);
        groupings.add(TreeProjectGrouping.GROUPING_NONE);
        groupings.add(null);
        groupings.add("");
        boolean changed = false;
        if (!groupings.contains(projectGrouping.getGroup3())) {
            projectGrouping.setGroup3(null);
            changed = true;
        }
        if (!groupings.contains(projectGrouping.getGroup2())) {
            projectGrouping.setGroup2(projectGrouping.getGroup3());
            projectGrouping.setGroup3(null);
            changed = true;
        }
        if (!groupings.contains(projectGrouping.getGroup1())) {
            projectGrouping.setGroup1(projectGrouping.getGroup2());
            projectGrouping.setGroup2(projectGrouping.getGroup3());
            projectGrouping.setGroup3(null);
            changed = true;
        }
        if (changed) {
            projectGroupingService.save(projectGrouping);
        }
        return projectGrouping;
    }

    public void group() {
        try {
            if (TreeProjectGrouping.GROUPING_NONE.equals(projectGrouping.getGroup1())) {
                projectGrouping.setGroup1(null);
            }
            if (TreeProjectGrouping.GROUPING_NONE.equals(projectGrouping.getGroup2())) {
                projectGrouping.setGroup2(null);
            }
            if (TreeProjectGrouping.GROUPING_NONE.equals(projectGrouping.getGroup3())) {
                projectGrouping.setGroup3(null);
            }

            projectGroupingService.save(projectGrouping);

            invalidateTree();
        } catch (Exception e) {
            String msg = e.getMessage();
            log.error(msg, e);
            WebStudioUtils.addErrorMessage(msg);
        }
    }

    private class WorkspaceListener implements UserWorkspaceListener {
        @Override
        public void workspaceReleased(UserWorkspace workspace) {
            synchronized (lock) {
                backupRoot();
                root = null;
            }
        }

        @Override
        public void workspaceRefreshed() {
            synchronized (lock) {
                backupRoot();
                root = null;
            }
        }
    }

    private void backupRoot() {
        if (root != null) {
            previousRoot = root;
        }
    }
}
