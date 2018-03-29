import com.intellij.find.FindManager;
import com.intellij.find.FindModel;
import com.intellij.find.FindSettings;
import com.intellij.find.impl.FindInProjectUtil;
import com.intellij.find.impl.FindManagerImpl;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.*;
import com.intellij.util.Processor;

import java.util.ArrayList;
import java.util.List;

public class checkCode extends AnAction {
    Project project;

    //list of likely phrases that involve API keys
    ArrayList<String> apiKeyPhrases = new ArrayList<String>();

    String searchString = "";


    public checkCode() {
        // Set the menu item name.
        super("Check For API Key Usages");
    }

    public void actionPerformed(AnActionEvent event) {

        project = event.getData(PlatformDataKeys.PROJECT);
        DataContext dataContext = event.getDataContext();
        final FindManager findManager = FindManager.getInstance(project);

        final FindModel findModel;

        findModel = findManager.getFindInProjectModel().clone();
        findModel.setReplaceState(false);
        findModel.setOpenInNewTabVisible(true);
        findModel.setOpenInNewTabEnabled(false);
        findModel.setOpenInNewTab(false);

        //set up strings to find that will likely be API Keys.
        populateWords();

        searchString = setUpString(apiKeyPhrases);

        findModel.setStringToFind(searchString);


        initModel(findModel, dataContext);

        findManager.showFindDialog(findModel, () -> {
            findModel.setOpenInNewTabVisible(false);
            if (false) {
                FindSettings.getInstance().setShowResultsInSeparateView(findModel.isOpenInNewTab());
            }

            startFindInProject(findModel);

            findModel.setOpenInNewTabVisible(false);
        });
    }


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

    private String setUpString( List<String> words) {
        String barSeparatedWords = "";
        for (String word : words ) {
            barSeparatedWords = barSeparatedWords + (word + "|");
        }
        //remove last bar in regex search phrase
        barSeparatedWords = barSeparatedWords.substring(0, barSeparatedWords.length() - 1);
        return "\\b(?:" + barSeparatedWords + ")\\b";
    }

    private void populateWords() {
        apiKeyPhrases.add("key");
        apiKeyPhrases.add("API");
    }



}