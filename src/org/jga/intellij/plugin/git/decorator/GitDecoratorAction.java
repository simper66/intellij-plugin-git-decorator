package org.jga.intellij.plugin.git.decorator;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import org.jetbrains.annotations.NotNull;

public class GitDecoratorAction extends ToggleAction {

    public GitDecoratorAction() {
        super();
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
        return e.getProject() != null && GitDecoratorConfig.getInstance(e.getProject()).isGitDecoratorEnabled;
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
        if (e.getProject() != null) {
            GitDecoratorConfig.getInstance(e.getProject()).isGitDecoratorEnabled = state;
            e.getProject().getMessageBus().syncPublisher(GitDecoratorEnableToggleNotifier.TOGGLE_TOPIC).decorationChanged(state);
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(e.getProject()!=null);
        super.update(e);
    }
}
