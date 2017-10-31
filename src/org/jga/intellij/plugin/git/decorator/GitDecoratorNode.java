package org.jga.intellij.plugin.git.decorator;

import com.intellij.dvcs.repo.Repository;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ProjectViewNodeDecorator;
import com.intellij.ide.projectView.impl.nodes.ClassTreeNode;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.projectView.impl.nodes.PsiFileNode;
import com.intellij.ide.util.treeView.PresentableNodeDescriptor;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packageDependencies.ui.PackageDependenciesNode;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ThreeState;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;

import java.awt.*;

public class GitDecoratorNode implements ProjectViewNodeDecorator {

    private static final String MASTER = "master";

    private static final String OPEN_BRACKET = " [";
    private static final String CLOSE_BRACKET = "]";

    private static final String NO_BRANCH = "NO_BRANCH";

    private static final String DIR_MODIFIED = "<";
    private static final String DIR_MODIFIED_SPACE = " <";

    private static final String FILE_MODIFIED = " *";
    private static final String FILE_NEW = " +";
    private static final String FILE_MOVED = " !";
    
    private static final int SMALL_ITALIC_STYLE = SimpleTextAttributes.STYLE_SMALLER + SimpleTextAttributes.STYLE_ITALIC;
    private static final SimpleTextAttributes NORMAL_ATTR = SimpleTextAttributes.REGULAR_ATTRIBUTES;
    private static final SimpleTextAttributes RED_SMALL_ITALIC_ATTR = new SimpleTextAttributes(SMALL_ITALIC_STYLE, JBColor.RED);
    private static final SimpleTextAttributes SMALL_ITALIC_ATTR = new SimpleTextAttributes(SMALL_ITALIC_STYLE, (Color)null);

    private GitDecoratorConfig gitDecoratorConfig;
    
    private ChangeListManager changeListManager;
    private ModuleManager moduleManager;
    
    @Override
    public void decorate(ProjectViewNode node, PresentationData nodePresentation) {

        VirtualFile nodeVF;
        if (node instanceof PsiDirectoryNode || node instanceof PsiFileNode) {
            nodeVF = node.getVirtualFile();
        } else if (node instanceof ClassTreeNode) {
            nodeVF = ((ClassTreeNode)node).getPsiClass().getContainingFile().getVirtualFile();
        } else {
            return;
        }
        if (nodeVF==null) return; 
            
        if (this.gitDecoratorConfig==null) {
            this.gitDecoratorConfig = GitDecoratorConfig.getInstance(node.getProject());
            if (this.gitDecoratorConfig==null) return;
        }
        if (!this.gitDecoratorConfig.isGitDecoratorEnabled) return;

        if (this.changeListManager==null) {
            this.changeListManager = ChangeListManager.getInstance(node.getProject());
            if (this.changeListManager==null) return;
        }
        if (this.changeListManager.isIgnoredFile(nodeVF)) return;

        GitRepository gitRepository = ServiceManager.getService(node.getProject(), GitRepositoryManager.class).getRepositoryForFile(nodeVF);
        if (gitRepository==null) return;

        if (this.moduleManager==null) {
            this.moduleManager = ModuleManager.getInstance(node.getProject());
            if (this.moduleManager==null) return;
        }

        try {

            if (node instanceof PsiDirectoryNode) {
                this.decorateGitContainer(nodeVF, nodePresentation, gitRepository);
                if (gitRepository.getRoot().equals(nodeVF)) {
                    //GitMergeProvider.detect(node.getProject()).loadRevisions(nodeVF)
                    this.decorateGitRoot(
                            nodePresentation,
                            gitRepository.isOnBranch() ? gitRepository.getCurrentBranch().getName() : NO_BRANCH,
                            gitRepository.getInfo().getState() == Repository.State.NORMAL ? null : gitRepository.getInfo().getState().toString(),
                            gitRepository.getCurrentRevision() != null ? gitRepository.getCurrentRevision().substring(0, 9) : "NO COMMIT"
                    );
                }
            } else {
                this.decorateGitFile(nodeVF, nodePresentation, gitRepository);
            }
            
        } catch (VcsException ignored) {
        }
    }

    private void decorateGitContainer(VirtualFile nodeVF, PresentationData nodePresentation, GitRepository gitRepository) throws VcsException {

        for(Module module: this.moduleManager.getModules()) {
            for(VirtualFile moduleRoot: ModuleRootManager.getInstance(module).getContentRoots()) {
                if (moduleRoot.equals(nodeVF)) {
                    if (this.gitContainerHasChangesUnder(nodeVF, gitRepository)) {
                        this.decorateNode(nodePresentation, DIR_MODIFIED, RED_SMALL_ITALIC_ATTR);
                    }
                    return;
                }
            }
        }

        this.decorateNode(nodePresentation, nodeVF.getName(), NORMAL_ATTR);
        if (this.gitContainerHasChangesUnder(nodeVF, gitRepository)) {
            this.decorateNode(nodePresentation, DIR_MODIFIED_SPACE, RED_SMALL_ITALIC_ATTR);
        }

    }
    
    private boolean gitContainerHasChangesUnder(VirtualFile nodeVF, GitRepository gitRepository) throws VcsException {
        if (this.changeListManager.haveChangesUnder(nodeVF) != ThreeState.NO) {
            return true;
        } else {
            for (VirtualFile newFile : gitRepository.getUntrackedFilesHolder().retrieveUntrackedFiles()) {
                if (!this.changeListManager.isIgnoredFile(newFile)) {
                    if (FileUtil.isAncestor(nodeVF.getPath(), newFile.getPath(), true)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void decorateGitFile(VirtualFile nodeVF, PresentationData nodePresentation, GitRepository gitRepository) throws VcsException {
        
        if (this.changeListManager.getChange(nodeVF)!=null) {
            Change.Type changeType = this.changeListManager.getChange(nodeVF).getType();
            
            if (changeType==Change.Type.MODIFICATION) {
                this.decorateNode(nodePresentation, nodeVF.getName()+FILE_MODIFIED, NORMAL_ATTR);

            } else if (changeType==Change.Type.MOVED) {
                this.decorateNode(
                        nodePresentation, 
                        //nodeVF.getName()+FILE_MOVED+this.changeListManager.getChange(nodeVF).getBeforeRevision().getFile(), 
                        nodeVF.getName()+FILE_MOVED,
                        NORMAL_ATTR
                );
            }

        } else {
            for (VirtualFile newFile : gitRepository.getUntrackedFilesHolder().retrieveUntrackedFiles()) {
                if (!this.changeListManager.isIgnoredFile(newFile)) {
                    if (nodeVF.getPath().equals(newFile.getPath())) {
                        this.decorateNode(nodePresentation, nodeVF.getName()+FILE_NEW, NORMAL_ATTR);
                    }
                }
            }
        }
    }
    
    private void decorateGitRoot(PresentationData nodePresentation, String branchName, String repoState, String repoRev) {
        this.decorateNode(nodePresentation, OPEN_BRACKET + branchName + CLOSE_BRACKET, SMALL_ITALIC_STYLE, branchName.equals(MASTER) || branchName.equals(NO_BRANCH) ? JBColor.RED : JBColor.CYAN);
        if (repoState != null) {
            this.decorateNode(nodePresentation, OPEN_BRACKET + repoState + CLOSE_BRACKET, RED_SMALL_ITALIC_ATTR);
        }
        this.decorateNode(nodePresentation, OPEN_BRACKET + repoRev + CLOSE_BRACKET, SMALL_ITALIC_ATTR);
    }

    private void decorateNode(PresentationData nodePresentation, String text, int style, Color color) {
        nodePresentation.addText(new PresentableNodeDescriptor.ColoredFragment(text, new SimpleTextAttributes(style, color)));
    }
    
    private void decorateNode(PresentationData nodePresentation, String text, SimpleTextAttributes attr) {
        nodePresentation.addText(new PresentableNodeDescriptor.ColoredFragment(text, attr));
    }

    @Override
    public void decorate(PackageDependenciesNode node, ColoredTreeCellRenderer cellRenderer) {
    }
    
}