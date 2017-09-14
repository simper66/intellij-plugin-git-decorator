package org.jga.intellij.plugin.git.decorator;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.vfs.ex.VirtualFileManagerEx;
import com.intellij.util.messages.MessageBusConnection;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class GitDecoratorProject extends AbstractProjectComponent {

    private static final String COMP_NAME = "GitProjectDecorator";

    private final AtomicBoolean isRefreshing = new AtomicBoolean(false);

    private final Project project;
    private final MessageBusConnection bus;
    private final VirtualFileListener vfListener;

    private ScheduledExecutorService refreshWorker;

    public GitDecoratorProject(Project project) {

        super(project);

        this.project = project;

        this.bus = this.project.getMessageBus().connect();

        this.vfListener = new VirtualFileListener() {
            @Override
            public void propertyChanged(@NotNull VirtualFilePropertyEvent event) {
                askForRefresh();
            }
            @Override
            public void contentsChanged(@NotNull VirtualFileEvent event) {
                askForRefresh();
            }
            @Override
            public void fileCreated(@NotNull VirtualFileEvent event) {
                askForRefresh();
            }
            @Override
            public void fileDeleted(@NotNull VirtualFileEvent event) {
                askForRefresh();
            }
            @Override
            public void fileMoved(@NotNull VirtualFileMoveEvent event) {
                askForRefresh();
            }
            @Override
            public void fileCopied(@NotNull VirtualFileCopyEvent event) {
                askForRefresh();
            }
        };

        this.startRefreshWorker();
    }
    
    private synchronized void startRefreshWorker() {

        if (this.refreshWorker!=null) stopRefreshWorker();
        
        this.refreshWorker = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat(getComponentName()+"-refreshWorker")
                .setPriority(Thread.NORM_PRIORITY)
                .build()
        );
        
        this.refreshWorker.scheduleWithFixedDelay(() -> {
            if (this.isRefreshing.compareAndSet(true, false)) {
                ProgressManager.getInstance().executeNonCancelableSection(this::refresh);
            }
        }, 0, 5L, TimeUnit.SECONDS);
    }

    private synchronized void stopRefreshWorker() {
        if (this.refreshWorker==null) return;
        this.refreshWorker.shutdown();
        try {
            this.refreshWorker.awaitTermination(7L, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        } finally {
            this.refreshWorker=null;
        }
    }

    @Override
    public void initComponent() {
        this.bus.subscribe(GitRepository.GIT_REPO_CHANGE, repo -> this.askForRefresh());
        this.bus.subscribe(GitDecoratorEnableToggleNotifier.TOGGLE_TOPIC, this::toggleGitDecoratorEnabled);
        VirtualFileManagerEx.getInstance().addVirtualFileListener(this.vfListener);
    }

    private synchronized void toggleGitDecoratorEnabled(boolean isGitDecoratorEnabled) {
        if (isGitDecoratorEnabled) {
            this.refresh();
            this.startRefreshWorker();
        } else {
            this.stopRefreshWorker();
            this.refresh();
        }
    }

    private void refresh() {
        ProjectView.getInstance(this.project).refresh();
    }

    private void askForRefresh() {
        this.isRefreshing.compareAndSet(false, true);
    }

    @Override
    public void projectOpened() {
    }

    @Override
    @NotNull
    public String getComponentName() {
        return GitDecoratorProject.COMP_NAME;
    }
    
    @Override
    public void disposeComponent() {
        if (this.bus != null) {
            this.bus.disconnect();
        }
        if (this.vfListener != null) {
            VirtualFileManagerEx.getInstance().removeVirtualFileListener(this.vfListener);
        }
        if (this.refreshWorker != null) {
            this.startRefreshWorker();
        }
    }

    @Override
    public void projectClosed() {
    }
        
}
