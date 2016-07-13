package reborn.wrappers;

import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.SeparatorComponent;
import com.intellij.util.ui.GridBag;
import models.PackageTemplate;
import reborn.models.BaseElement;
import reborn.models.Directory;
import reborn.models.File;
import reborn.models.GlobalVariable;
import utils.GridBagFactory;
import utils.UIMaker;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/**
 * Created by Arsen on 07.07.2016.
 */
public class PackageTemplateWrapper {

    public static final java.lang.String ATTRIBUTE_BASE_NAME = "BASE_NAME";

    public enum ViewMode {EDIT, CREATE, USAGE}

    private Project project;

    public JPanel panel;
    public EditorTextField etfName;
    public EditorTextField etfDescription;
    public GridBag gridBag;

    private PackageTemplate packageTemplate;
    private DirectoryWrapper rootElement;
    private ArrayList<GlobalVariableWrapper> listGlobalVariableWrapper;
    private ViewMode mode;

    public PackageTemplateWrapper(Project project) {
        this.project = project;
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

    public JPanel buildView() {
        if (panel == null) {
            panel = new JPanel(new GridBagLayout());
            gridBag = GridBagFactory.getBagForPackageTemplate();
        }

        // Header
        JLabel jlName = new JLabel("Name");
        JLabel jlDescription = new JLabel("Description");
        UIMaker.setRightPadding(jlName, UIMaker.PADDING_LABEL);
        UIMaker.setRightPadding(jlDescription, UIMaker.PADDING_LABEL);

        etfName = UIMaker.getEditorTextField(packageTemplate.getName(), project);
        etfDescription = UIMaker.getEditorTextField(packageTemplate.getDescription(), project);

        panel.add(jlName, gridBag.nextLine().next());
        panel.add(etfName, gridBag.next().coverLine(2));
        panel.add(jlDescription, gridBag.nextLine().next());
        panel.add(etfDescription, gridBag.next().coverLine(2));

        // Globals
        JLabel jlGlobals = new JLabel("Global Variables", JLabel.CENTER);
        panel.add(new SeparatorComponent(10), gridBag.nextLine().next().coverLine());
        panel.add(jlGlobals, gridBag.nextLine().next().fillCellHorizontally().coverLine());

        for (GlobalVariableWrapper variableWrapper : getListGlobalVariableWrapper()) {
            variableWrapper.buildView(this, panel, gridBag);
        }

        // Files and Directories
        panel.add(new SeparatorComponent(10), gridBag.nextLine().next().coverLine());
        rootElement.buildView(project, panel, gridBag, mode);

        return panel;
    }

    public void addGlobalVariable(GlobalVariableWrapper gvWrapper) {
        listGlobalVariableWrapper.add(gvWrapper);
        packageTemplate.getListGlobalVariable().add(gvWrapper.getGlobalVariable());
    }

    public void removeGlobalVariable(GlobalVariableWrapper gvWrapper) {
        packageTemplate.getListGlobalVariable().remove(gvWrapper.getGlobalVariable());
        listGlobalVariableWrapper.remove(gvWrapper);
    }

    public void reBuildView() {
        panel.removeAll();
        gridBag = GridBagFactory.getBagForPackageTemplate();
        buildView();
    }

    public void replaceNameVariable() {
        rootElement.replaceNameVariable(packageTemplate.getMapGlobalVars());
    }

    public void collectDataFromFields() {
        packageTemplate.setName(etfName.getText());
        packageTemplate.setDescription(etfDescription.getText());

        for (GlobalVariableWrapper variableWrapper : listGlobalVariableWrapper) {
            variableWrapper.collectDataFromFields();
        }

        rootElement.collectDataFromFields();
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

    public void wrapGlobals() {
        listGlobalVariableWrapper = new ArrayList<>();
        for(GlobalVariable globalVariable : packageTemplate.getListGlobalVariable()){
            listGlobalVariableWrapper.add(new GlobalVariableWrapper(globalVariable));
        }
    }

}
