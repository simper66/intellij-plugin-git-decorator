package org.jga.intellij.plugin.git.decorator;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;

public class GitDecoratorAction extends ToggleAction {

    public GitDecoratorAction() {
        super();
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
        return e.getProject() != null && GitDecoratorConfig.getInstance(e.getProject()).getState().isGitDecoratorEnabled;
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
        GitDecoratorConfig.getInstance(e.getProject()).getState().isGitDecoratorEnabled = state;
        if (e.getProject() != null && state) {
            GitDecoratorService.getInstance(e.getProject()).startRefreshWorker();
        }
        GitDecoratorService.getInstance(e.getProject()).stopRefreshWorker();
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabled(e.getProject()!=null);
        super.update(e);
    }
}
