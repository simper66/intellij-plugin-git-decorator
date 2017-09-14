package org.jga.intellij.plugin.git.decorator;

import com.intellij.util.messages.Topic;

public interface GitDecoratorEnableToggleNotifier {
    Topic<GitDecoratorEnableToggleNotifier> TOGGLE_TOPIC = Topic.create("Toggle decorations", GitDecoratorEnableToggleNotifier.class);
    void decorationChanged(Boolean isGitDecoratorEnabled);
}
