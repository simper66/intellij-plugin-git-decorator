package org.jga.intellij.plugin.git.decorator;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;

public class GitDecoratorStart implements StartupActivity {
    @Override
    public void runActivity(Project project) {
        GitDecoratorService.getInstance(project).projectOpened(project);
    }
}
