package org.jga.intellij.plugin.git.decorator;

import com.intellij.lifecycle.PeriodicalTasksCloser;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State( 
        name = "GitDecoratorConfig",
        storages = { @Storage("gitDecoratorConfig.xml") }, 
        reloadable = false
)
@Storage()
public class GitDecoratorConfig implements PersistentStateComponent<GitDecoratorConfig> {

    static GitDecoratorConfig getInstance(@NotNull Project project) {
        return PeriodicalTasksCloser.getInstance().safeGetService(project, GitDecoratorConfig.class);
    }
    
    boolean isGitDecoratorEnabled = true;
    
    public GitDecoratorConfig() {
        this.isGitDecoratorEnabled = true;
    }

    @Nullable
    @Override
    public GitDecoratorConfig getState() {
        return this;
    }

    @Override
    public void loadState(GitDecoratorConfig state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
