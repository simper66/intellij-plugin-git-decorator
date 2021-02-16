//package org.jga.intellij.plugin.git.decorator;
//
//import com.intellij.openapi.components.PersistentStateComponent;
//import com.intellij.openapi.components.State;
//import com.intellij.openapi.components.Storage;
//import com.intellij.openapi.project.Project;
//import com.intellij.util.xmlb.XmlSerializerUtil;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//@State(
//        name = "GitDecoratorConfig",
//        storages = { @Storage("gitDecoratorConfig.xml") },
//        reloadable = false
//)
//@Storage()
//public class GitDecoratorConfig implements PersistentStateComponent<GitDecoratorConfig> {
//
//    static GitDecoratorConfig getInstance(@NotNull Project project) {
//        return project.getService(GitDecoratorConfig.class);
//    }
//
//    boolean isGitDecoratorEnabled = true;
//
//    public GitDecoratorConfig() {
//        this.isGitDecoratorEnabled = true;
//    }
//
//    @Nullable
//    @Override
//    public GitDecoratorConfig getState() {
//        return this;
//    }
//
//    @Override
//    public void loadState(GitDecoratorConfig state) {
//        XmlSerializerUtil.copyBean(state, this);
//    }
//}


package org.jga.intellij.plugin.git.decorator;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;

@State(
        name = "GitDecoratorConfig",
        storages = { @Storage("gitDecoratorConfig.xml") },
        reloadable = false
)
@Storage()
public class GitDecoratorConfig implements PersistentStateComponent<GitDecoratorConfig.State> {
    static class State {
        public boolean isGitDecoratorEnabled = true;
    }

    static GitDecoratorConfig getInstance(Project project) {
        return project.getService(GitDecoratorConfig.class);
    }

    private State state = new State();

    @Override
    public State getState() {
        return state;
    }

    @Override
    public void loadState(State state) {
        this.state = state;
    }

}
