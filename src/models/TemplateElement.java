package models;

import com.google.gson.annotations.Expose;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import utils.FileWriter;
import utils.InputManager;
import utils.Logger;
import utils.StringTools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.apache.batik.svggen.SVGStylingAttributes.set;

/**
 * Created by Arsen on 15.06.2016.
 */

public class TemplateElement {

    @Expose
    private boolean isDirectory;
    @Expose
    private String name;
    @Expose
    private String templateName;
    @Expose
    private String extension;
    @Expose
    private ArrayList<TemplateElement> listTemplateElement;

    private TemplateElement parent;
    @Expose
    private HashMap<String, String> mapProperties;

    // File
    public TemplateElement(String name, String templateName, String extension, TemplateElement parent) {
        this.isDirectory = false;
        this.parent = parent;
        this.extension = extension;
        this.name = name;
        this.templateName = templateName;
    }

    // Directory
    public TemplateElement(String name, ArrayList<TemplateElement> listTemplateElement, TemplateElement parent) {
        this.isDirectory = true;
        this.parent = parent;
        this.name = name;
        this.listTemplateElement = listTemplateElement;
        mapProperties = new HashMap<>();
    }

    public void add(TemplateElement element) {
        if (getListTemplateElement() != null) {
            getListTemplateElement().add(element);
        }
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public TemplateElement getParent() {
        return parent;
    }

    public void setParent(TemplateElement parent) {
        this.parent = parent;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public void setDirectory(boolean directory) {
        isDirectory = directory;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<TemplateElement> getListTemplateElement() {
        return listTemplateElement;
    }

    public HashMap<String, String> getMapProperties() {
        return mapProperties;
    }

    public void setMapProperties(HashMap<String, String> mapProperties) {
        this.mapProperties = mapProperties;
    }

    public void setListTemplateElement(ArrayList<TemplateElement> listTemplateElement) {
        this.listTemplateElement = listTemplateElement;
    }

    public boolean isNameValid(List<String> listAllTemplates) {
        if (isDirectory()) {
            if (getListTemplateElement() != null) {
                for (TemplateElement element : getListTemplateElement()) {
                    if (!element.isNameValid(listAllTemplates)) {
                        return false;
                    }
                }
            }
        } else {
            if (!listAllTemplates.contains(getName())) {
                Logger.log("Template " + getName() + " doesn't exist!");
                return false;
            }
        }
        return true;
    }

    public void replaceNameVariable(HashMap<String, String> mapProperties) {
        setName(StringTools.replaceGlobalVariables(getName(), mapProperties));
        if (getListTemplateElement() != null) {
            for (TemplateElement element : getListTemplateElement()) {
                element.replaceNameVariable(mapProperties);
            }
        }
    }

    public void makeInputBlock(InputManager inputManager) {
        inputManager.addElement(this);

        if (isDirectory()) {
            if (getListTemplateElement() != null) {
                for (TemplateElement element : getListTemplateElement()) {
                    element.makeInputBlock(inputManager);
                }
            }

            // TODO: 17.06.2016 skip empty packages
            inputManager.onPackageEnds();
        }
    }

    public void writeFile(PsiDirectory currentDir, Project project) {
        if (isDirectory()) {
            PsiDirectory subDirectory = FileWriter.writeDirectory(currentDir, this, project);
            if (subDirectory == null) {
                // TODO: 20.06.2016 error write file
            } else {
                if (getListTemplateElement() != null) {
                    for (TemplateElement element : getListTemplateElement()) {
                        element.writeFile(subDirectory, project);
                    }
                }
            }
        } else {
            PsiElement psiElement = FileWriter.writeFile(currentDir, this);
            if (psiElement == null) {
                // TODO: 20.06.2016 error write file
            }
        }
    }

    public void updateParents(TemplateElement element) {
        setParent(element);
        if (isDirectory()) {
            for (TemplateElement templateElement : getListTemplateElement()){
                templateElement.updateParents(this);
            }
        }
    }
}
