package global.wrappers;

import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.SeparatorComponent;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.IconUtil;
import core.actions.custom.CreateDirectoryAction;
import core.actions.custom.DummyDirectoryAction;
import core.actions.custom.base.SimpleAction;
import core.actions.newPackageTemplate.dialogs.configure.ConfigureDialog;
import core.actions.newPackageTemplate.models.ExecutionContext;
import core.textInjection.TextInjection;
import core.textInjection.dialog.TextInjectionDialog;
import core.textInjection.dialog.TextInjectionWrapper;
import global.Const;
import global.listeners.ClickListener;
import global.models.*;
import global.utils.UIHelper;
import global.utils.factories.WrappersFactory;
import global.utils.file.FileWriter;
import global.utils.i18n.Localizer;
import global.visitors.*;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

/**
 * Created by Arsen on 07.07.2016.
 */
public class PackageTemplateWrapper {

    public static final String ATTRIBUTE_BASE_NAME = "BASE_NAME";
    public static final String PATTERN_BASE_NAME = "${" + ATTRIBUTE_BASE_NAME + "}";

    public enum ViewMode {EDIT, CREATE, USAGE}

    private Project project;
    private Properties defaultProperties;
    private PackageTemplate packageTemplate;
    private DirectoryWrapper rootElement;
    private ArrayList<GlobalVariableWrapper> listGlobalVariableWrapper;
    private ArrayList<TextInjectionWrapper> listTextInjectionWrapper;
    private ViewMode mode;
    private ExecutionContext executionContext;

    public PackageTemplateWrapper(Project project) {
        this.project = project;
        this.executionContext = new ExecutionContext();
    }


    //=================================================================
    //  UI
    //=================================================================
    private JPanel panel;
    public EditorTextField etfName;
    public EditorTextField etfDescription;
    public JCheckBox cbShouldRegisterAction;
    public JCheckBox cbSkipDefiningNames;
    public JCheckBox cbSkipRootDirectory;
    public JCheckBox cbShowReportDialog;
    private JPanel panelProperties;
    private JPanel panelElements;
    private JPanel panelTextInjection;
    private JPanel panelGlobals;

    public JPanel buildView() {
        if (panel == null) {
            panel = new JPanel(new MigLayout(new LC().fillX().gridGapY("0")));
        }

        // Properties
         panelProperties = new JPanel(new MigLayout(new LC()));
        buildProperties();
        panel.add(panelProperties, new CC().spanX().wrap().pushX().growX());


        // Globals
        panel.add(new SeparatorComponent(10), new CC().pushX().growX().wrap().spanX());
        JLabel jlGlobals = new JLabel(Localizer.get("GlobalVariables"), JLabel.CENTER);
        panel.add(jlGlobals, new CC().wrap().growX().pushX().spanX());

        panelGlobals = new JPanel(new MigLayout());
        buildGlobals();
        panel.add(panelGlobals, new CC().spanX().pushX().growX().wrap());


        // Files and Directories | Elements
        panel.add(new SeparatorComponent(10), new CC().pushX().growX().wrap().spanX());
        JLabel jlFilesAndDirs = new JLabel(Localizer.get("FilesAndDirs"), JLabel.CENTER);
        panel.add(jlFilesAndDirs, new CC().wrap().growX().pushX().spanX());

        panelElements = new JPanel(new MigLayout());
        buildElements();
        panel.add(panelElements, new CC().spanX().pushX().growX().wrap());

        // Text Injection
        panel.add(new SeparatorComponent(10), new CC().pushX().growX().wrap().spanX());
        JLabel jlTextInjection = new JLabel(Localizer.get("TextInjection"), JLabel.CENTER);
        panel.add(jlTextInjection, new CC().wrap().growX().pushX().spanX());

        panelTextInjection = new JPanel(new MigLayout());
        buildTextInjections();
        panel.add(panelTextInjection, new CC().spanX().pushX().growX().wrap());

        JButton btnAdd = new JButton(Localizer.get("action.AddTextInjection"), IconUtil.getAddIcon());
        btnAdd.addMouseListener(new ClickListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                createTextInjection();
            }
        });
        panel.add(btnAdd, new CC().wrap());

        return panel;
    }

    private void buildProperties() {
        if (mode != ViewMode.USAGE) {
            // Header
            JLabel jlName = new JLabel(Localizer.get("Name"));
            JLabel jlDescription = new JLabel(Localizer.get("Description"));

            etfName = UIHelper.getEditorTextField(packageTemplate.getName(), project);
            etfDescription = UIHelper.getEditorTextField(packageTemplate.getDescription(), project);

            panelProperties.add(jlName, new CC().wrap().spanX().pad(0, 0, 0, 8).gapY("0", "8"));
            panelProperties.add(etfName, new CC().spanX().growX().pushX().wrap());
            panelProperties.add(jlDescription, new CC().wrap().spanX().pad(0, 0, 0, 8).gapY("8", "8"));
            panelProperties.add(etfDescription, new CC().spanX().growX().pushX().wrap());

            // Properties
            cbShouldRegisterAction = new JBCheckBox(Localizer.get("property.ShouldRegisterAction"), packageTemplate.isShouldRegisterAction());
            cbSkipDefiningNames = new JBCheckBox(Localizer.get("property.SkipPresettings"), packageTemplate.isSkipDefiningNames());
            panelProperties.add(cbShouldRegisterAction, new CC().wrap().spanX());
            panelProperties.add(cbSkipDefiningNames, new CC().wrap().spanX());
        }

        // Properties
        cbSkipRootDirectory = new JBCheckBox(Localizer.get("property.SkipRootDirectory"), packageTemplate.isSkipRootDirectory());
        cbSkipRootDirectory.addItemListener(e -> {
            collectDataFromFields();
            reBuildProperties();
            reBuildElements();
        });
        cbShowReportDialog = new JBCheckBox(Localizer.get("property.ShowReportDialog"), packageTemplate.shouldShowReport());
        cbShowReportDialog.addItemListener(e -> {
            collectDataFromFields();
            reBuildProperties();
        });

        panelProperties.add(cbSkipRootDirectory, new CC().spanX().wrap());
        panelProperties.add(cbShowReportDialog, new CC().spanX().wrap());
    }

    private void buildElements() {
        rootElement.buildView(project, panelElements);
    }

    private void buildGlobals() {
        for (GlobalVariableWrapper variableWrapper : getListGlobalVariableWrapper()) {
            variableWrapper.buildView(this, panelGlobals);
        }
    }

    private void buildTextInjections() {
        for (TextInjectionWrapper wrapper : getListTextInjectionWrapper()) {
            wrapper.buildView(this, panelTextInjection);
        }
    }


    //=================================================================
    //  UI Actions
    //=================================================================
    private void createTextInjection() {
        new TextInjectionDialog(project, null) {
            @Override
            public void onSuccess(TextInjection textInjection) {
                addTextInjection(WrappersFactory.wrapTextInjection(textInjection));
                reBuildTextInjections();
            }
        };
    }

    public void addGlobalVariable(GlobalVariableWrapper gvWrapper) {
        listGlobalVariableWrapper.add(gvWrapper);
        packageTemplate.getListGlobalVariable().add(gvWrapper.getGlobalVariable());
    }

    public void removeGlobalVariable(GlobalVariableWrapper gvWrapper) {
        packageTemplate.getListGlobalVariable().remove(gvWrapper.getGlobalVariable());
        listGlobalVariableWrapper.remove(gvWrapper);
    }

    public void addTextInjection(TextInjectionWrapper wrapper) {
        listTextInjectionWrapper.add(wrapper);
        packageTemplate.getListTextInjection().add(wrapper.getTextInjection());
    }

    public void removeTextInjection(TextInjectionWrapper wrapper) {
        packageTemplate.getListTextInjection().remove(wrapper.getTextInjection());
        listTextInjectionWrapper.remove(wrapper);
    }


    //=================================================================
    //  UI ReBuild
    //=================================================================
    ConfigureDialog.UpdateUICallback updateUICallback;

    public void setUpdateUICallback(ConfigureDialog.UpdateUICallback updateUICallback) {
        this.updateUICallback = updateUICallback;
    }

    public void fullReBuild() {
        reBuildProperties();
        reBuildGlobals();
        reBuildElements();
        reBuildTextInjections();
    }

    public void reBuildProperties() {
        panelProperties.removeAll();
        buildProperties();
    }

    public void reBuildGlobals() {
        panelGlobals.removeAll();
        buildTextInjections();
        packParentContainer();
    }

    public void reBuildElements() {
        panelElements.removeAll();
        buildElements();
        packParentContainer();
    }

    public void reBuildTextInjections() {
        panelTextInjection.removeAll();
        buildTextInjections();
        packParentContainer();
    }

    private void packParentContainer() {
        if(updateUICallback != null){
            updateUICallback.pack();
        }
    }


    //=================================================================
    //  Utils
    //=================================================================
    public void replaceNameVariable() {
        rootElement.accept(new ReplaceNameVariableVisitor(packageTemplate.getMapGlobalVars()));
    }

    public void collectDataFromFields() {
        if (getMode() != ViewMode.USAGE) {
            packageTemplate.setName(etfName.getText());
            packageTemplate.setDescription(etfDescription.getText());
            packageTemplate.setShouldRegisterAction(cbShouldRegisterAction.isSelected());
            packageTemplate.setSkipDefiningNames(cbSkipDefiningNames.isSelected());
        }
        packageTemplate.setSkipRootDirectory(cbSkipRootDirectory.isSelected());
        packageTemplate.setShouldShowReport(cbShowReportDialog.isSelected());

        for (GlobalVariableWrapper variableWrapper : listGlobalVariableWrapper) {
            variableWrapper.collectDataFromFields();
        }

        for (TextInjectionWrapper variableWrapper : listTextInjectionWrapper) {
            variableWrapper.collectDataFromFields();
        }

        rootElement.accept(new СollectDataFromFieldsVisitor());
    }

    /**
     * Replace BASE_NAME and Run SCRIPT
     */
    public void prepareGlobals() {
        packageTemplate.setMapGlobalVars(new HashMap<>());
        // Context Vars
        packageTemplate.getMapGlobalVars().put(Const.Key.CTX_FULL_PATH, executionContext.ctxFullPath);
        packageTemplate.getMapGlobalVars().put(Const.Key.CTX_DIR_PATH, executionContext.ctxDirPath);

        for (GlobalVariableWrapper variableWrapper : listGlobalVariableWrapper) {
            if (getMode() == ViewMode.USAGE) {
                // Replace ${BASE_NAME}
                if (!variableWrapper.getGlobalVariable().getName().equals(ATTRIBUTE_BASE_NAME)) {
                    //todo global Smart replace ${var}
                    variableWrapper.replaceBaseName(packageTemplate.getMapGlobalVars().get(ATTRIBUTE_BASE_NAME));
                }
                // SCRIPT
                variableWrapper.runScript();
            }
            packageTemplate.getMapGlobalVars().put(variableWrapper.getGlobalVariable().getName(), variableWrapper.getGlobalVariable().getValue());
        }
    }

    public void addGlobalVariablesToFileTemplates() {
        rootElement.accept(new AddGlobalVariablesVisitor());
    }

    public DirectoryWrapper wrapDirectory(Directory directory, DirectoryWrapper parent) {
        DirectoryWrapper result = new DirectoryWrapper();
        result.setDirectory(directory);
        result.setParent(parent);
        result.setPackageTemplateWrapper(PackageTemplateWrapper.this);

        ArrayList<ElementWrapper> list = new ArrayList<>();

        for (BaseElement baseElement : directory.getListBaseElement()) {
            if (baseElement.isDirectory()) {
                list.add(wrapDirectory(((Directory) baseElement), result));
            } else {
                list.add(wrapFile(((File) baseElement), result));
            }
        }

        result.setListElementWrapper(list);
        return result;
    }

    private FileWrapper wrapFile(File file, DirectoryWrapper parent) {
        FileWrapper result = new FileWrapper();
        result.setPackageTemplateWrapper(PackageTemplateWrapper.this);
        result.setParent(parent);
        result.setFile(file);

        return result;
    }

    public void initCollections() {
        listGlobalVariableWrapper = new ArrayList<>();
        for (GlobalVariable item : packageTemplate.getListGlobalVariable()) {
            listGlobalVariableWrapper.add(new GlobalVariableWrapper(item));
        }

        listTextInjectionWrapper = new ArrayList<>();
        for (TextInjection item : packageTemplate.getListTextInjection()) {
            listTextInjectionWrapper.add(new TextInjectionWrapper(item));
        }
    }

    public void runElementsScript() {
        rootElement.accept(new RunScriptVisitor());
    }

    public void collectSimpleActions(Project project, VirtualFile virtualFile, List<SimpleAction> listSimpleAction) {
        ApplicationManager.getApplication().runReadAction(() -> {
            PsiDirectory currentDir = FileWriter.findCurrentDirectory(project, virtualFile);
            if (currentDir == null) {
                return;
            }

            initDefaultProperties();

            SimpleAction rootAction;
            if (packageTemplate.isSkipRootDirectory()) {
                // Without root
                rootAction = new DummyDirectoryAction(project, currentDir);
                listSimpleAction.add(rootAction);
            } else {
                // With root
                rootAction = new CreateDirectoryAction(packageTemplate.getDirectory(), project);
                listSimpleAction.add(wrapInDummyDirAction(rootAction, currentDir));
            }

            CollectSimpleActionVisitor visitor = new CollectSimpleActionVisitor(rootAction, project);

            for (ElementWrapper elementWrapper : rootElement.getListElementWrapper()) {
                elementWrapper.accept(visitor);
            }
        });
    }

    private SimpleAction wrapInDummyDirAction(SimpleAction simpleAction, PsiDirectory currentDir) {
        DummyDirectoryAction dummyAction = new DummyDirectoryAction(project, currentDir);
        dummyAction.addAction(simpleAction);
        return dummyAction;
    }

    public void initDefaultProperties() {
        defaultProperties = FileTemplateManager.getInstance(getProject()).getDefaultProperties();
    }


    //=================================================================
    //  Getter | Setter
    //=================================================================
    public ExecutionContext getExecutionContext() {
        return executionContext;
    }

    public void setExecutionContext(ExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public PackageTemplate getPackageTemplate() {
        return packageTemplate;
    }

    public void setPackageTemplate(PackageTemplate packageTemplate) {
        this.packageTemplate = packageTemplate;
    }

    public DirectoryWrapper getRootElement() {
        return rootElement;
    }

    public void setRootElement(DirectoryWrapper rootElement) {
        this.rootElement = rootElement;
    }

    public ArrayList<GlobalVariableWrapper> getListGlobalVariableWrapper() {
        return listGlobalVariableWrapper;
    }

    public void setListGlobalVariableWrapper(ArrayList<GlobalVariableWrapper> listGlobalVariableWrapper) {
        this.listGlobalVariableWrapper = listGlobalVariableWrapper;
    }

    public ViewMode getMode() {
        return mode;
    }

    public void setMode(ViewMode mode) {
        this.mode = mode;
    }

    public Properties getDefaultProperties() {
        return defaultProperties;
    }

    public ArrayList<TextInjectionWrapper> getListTextInjectionWrapper() {
        return listTextInjectionWrapper;
    }

    public void setListTextInjectionWrapper(ArrayList<TextInjectionWrapper> listTextInjectionWrapper) {
        this.listTextInjectionWrapper = listTextInjectionWrapper;
    }
}