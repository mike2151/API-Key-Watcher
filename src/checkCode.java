import com.intellij.find.FindManager;
import com.intellij.find.FindModel;
import com.intellij.find.FindSettings;
import com.intellij.find.findInProject.FindInProjectManager;
import com.intellij.find.impl.FindInProjectUtil;
import com.intellij.find.impl.FindManagerImpl;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.graph.option.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.*;
import com.intellij.util.Processor;

public class checkCode extends AnAction {

    public static final NotificationGroup NOTIFICATION_GROUP = NotificationGroup.toolWindowGroup("Find in Path",
            ToolWindowId.FIND, false);

    Project project;

    public checkCode() {
        // Set the menu item name.
        super("Check Code");
    }

    public void actionPerformed(AnActionEvent event) {
        /*
        DataContext dataContext = event.getDataContext();
        Project project = event.getData(PlatformDataKeys.PROJECT);

        FindInProjectManager fpm = FindInProjectManager.getInstance(project);

        if (!fpm.isEnabled()) {
            showNotAvailableMessage(event, project);
            return;
        }

        fpm.findInProject(dataContext);
        */
        project = event.getData(PlatformDataKeys.PROJECT);
        DataContext dataContext = event.getDataContext();
        final FindManager findManager = FindManager.getInstance(project);

        final FindModel findModel;

        findModel = findManager.getFindInProjectModel().clone();
        findModel.setReplaceState(false);
        findModel.setOpenInNewTabVisible(true);
        findModel.setOpenInNewTabEnabled(false);
        findModel.setOpenInNewTab(false);
        initModel(findModel, dataContext);

        findManager.showFindDialog(findModel, () -> {
            findModel.setOpenInNewTabVisible(false);
            if (false) {
                FindSettings.getInstance().setShowResultsInSeparateView(findModel.isOpenInNewTab());
            }

            startFindInProject(findModel);

            findModel.setOpenInNewTabVisible(false); //todo check it in both cases: dialog & popup
        });
    }
    /*
    static void showNotAvailableMessage(AnActionEvent e, Project project) {
        final String message = "'" + e.getPresentation().getText() + "' is not available while search is in progress";
        NOTIFICATION_GROUP.createNotification(message, NotificationType.WARNING).notify(project);
    }*/





    public void startFindInProject(FindModel findModel) {
        if (findModel.getDirectoryName() != null && FindInProjectUtil.getDirectory(findModel) == null) {
            return;
        }

        com.intellij.usages.UsageViewManager manager = com.intellij.usages.UsageViewManager.getInstance(project);

        if (manager == null) return;
        final FindManager findManager = FindManager.getInstance(project);
        findManager.getFindInProjectModel().copyFrom(findModel);
        final FindModel findModelCopy = findModel.clone();
        final UsageViewPresentation presentation =
                FindInProjectUtil.setupViewPresentation(FindSettings.getInstance().isShowResultsInSeparateView(), findModelCopy);
        final boolean showPanelIfOnlyOneUsage = !FindSettings.getInstance().isSkipResultsWithOneUsage();

        final FindUsagesProcessPresentation processPresentation =
                FindInProjectUtil.setupProcessPresentation(project, showPanelIfOnlyOneUsage, presentation);
        ConfigurableUsageTarget usageTarget = new FindInProjectUtil.StringUsageTarget(project, findModel);



        ((FindManagerImpl)FindManager.getInstance(project)).getFindUsagesManager().addToHistory(usageTarget);


        manager.searchAndShowUsages(new UsageTarget[]{usageTarget},
                () -> processor -> {

                    try {
                        Processor<UsageInfo> consumer = info -> {

                            Usage usage = UsageInfo2UsageAdapter.CONVERTER.fun(info);
                            usage.getPresentation().getIcon(); // cache icon
                            return processor.process(usage);
                        };


                        FindInProjectUtil.findUsages(findModelCopy, project, consumer, processPresentation);
                    }
                    finally {
                    }
                },
                processPresentation,
                presentation,
                null
        );
    }




    protected void initModel( FindModel findModel, DataContext dataContext) {
        FindInProjectUtil.setDirectoryName(findModel, dataContext);

        String text = PlatformDataKeys.PREDEFINED_TEXT.getData(dataContext);
        if (text != null) {
            FindModel.initStringToFindNoMultiline(findModel, text);
        }
        else {
            FindInProjectUtil.initStringToFindFromDataContext(findModel, dataContext);
        }
    }



}