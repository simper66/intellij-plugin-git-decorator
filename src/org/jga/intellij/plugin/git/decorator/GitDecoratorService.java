package org.jga.intellij.plugin.git.decorator;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.ex.VirtualFileManagerEx;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.messages.MessageBusConnection;
import git4idea.repo.GitRepository;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class GitDecoratorService implements Disposable {
    private final static ReentrantLock LOCK = new ReentrantLock();
    private static final String COMP_NAME = "GitProjectDecorator";
    private static final AtomicBoolean isRefreshing = new AtomicBoolean(false);

    static GitDecoratorService getInstance(Project project) {
        return project.getService(GitDecoratorService.class);
    }

    private Project project;
    private MessageBusConnection bus;
    //private VirtualFileListener vfListener;
    private ScheduledExecutorService refreshWorker;

    public void projectOpened(Project project) {
        this.project = project;
        Disposer.register(project, this);

        if (!GitDecoratorConfig.getInstance(this.project).getState().isGitDecoratorEnabled) return;

        this.bus = this.project.getMessageBus().connect();
        this.bus.subscribe(GitRepository.GIT_REPO_CHANGE, repo -> this.askForRefresh());
        //this.bus.subscribe(GitDecoratorEnableToggleNotifier.TOGGLE_TOPIC, this::toggleGitDecoratorEnabled);
        this.bus.subscribe(VirtualFileManagerEx.VFS_CHANGES,
                new BulkFileListener() {
                    @Override
                    public void after(List<? extends VFileEvent> events) {
                        askForRefresh();
                    }
                }
        );

//        this.vfListener = new VirtualFileListener() {
//            @Override
//            public void propertyChanged(VirtualFilePropertyEvent event) {
//                askForRefresh();
//            }
//            @Override
//            public void contentsChanged(VirtualFileEvent event) {
//                askForRefresh();
//            }
//            @Override
//            public void fileCreated(VirtualFileEvent event) {
//                askForRefresh();
//            }
//            @Override
//            public void fileDeleted(VirtualFileEvent event) {
//                askForRefresh();
//            }
//            @Override
//            public void fileMoved(VirtualFileMoveEvent event) {
//                askForRefresh();
//            }
//            @Override
//            public void fileCopied(VirtualFileCopyEvent event) {
//                askForRefresh();
//            }
//        };
//        VirtualFileManagerEx.getInstance().addVirtualFileListener(this.vfListener);

        this.startRefreshWorker();
    }

//    private synchronized void toggleGitDecoratorEnabled(boolean isGitDecoratorEnabled) {
//        if (isGitDecoratorEnabled) {
//            this.startRefreshWorker();
//        } else {
//            this.stopRefreshWorker();
//            this.refresh();
//        }
//    }

    private void askForRefresh() {
        this.isRefreshing.compareAndSet(false, true);
    }

    public void startRefreshWorker() {
        GitDecoratorService.LOCK.lock();
        try {
            stopRefreshWorker();

            this.refreshWorker = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                    .setDaemon(true)
                    .setNameFormat(COMP_NAME + "-refreshWorker")
                    .setPriority(Thread.NORM_PRIORITY)
                    .build()
            );

            this.refreshWorker.scheduleWithFixedDelay(() -> {
                if (this.isRefreshing.compareAndSet(true, false)) {
                    ProgressManager.getInstance().executeNonCancelableSection(this::refresh);
                }
            }, 0, 5L, TimeUnit.SECONDS);

            this.askForRefresh();

        } finally {
            GitDecoratorService.LOCK.unlock();
        }
    }

    private void refresh() {
        ProjectView.getInstance(this.project).refresh();
    }

    public void stopRefreshWorker() {
        GitDecoratorService.LOCK.lock();
        try {
            if (this.refreshWorker==null) return;
            this.refreshWorker.shutdown();
            try {
                this.refreshWorker.awaitTermination(10L, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            } finally {
                this.refreshWorker=null;
            }
        } finally {
            GitDecoratorService.LOCK.unlock();
        }
    }

    public void dispose() {
        if (this.bus != null) {
            this.bus.disconnect();
        }
//        if (this.vfListener != null) {
//            VirtualFileManagerEx.getInstance().removeVirtualFileListener(this.vfListener);
//        }
        this.stopRefreshWorker();
    }

}
