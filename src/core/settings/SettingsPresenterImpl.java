package core.settings;

import global.utils.Localizer;

/**
 * Created by Arsen on 16.09.2016.
 */
public class SettingsPresenterImpl implements SettingsPresenter {

    private SettingsView view;

    public SettingsPresenterImpl(SettingsView view) {
        this.view = view;
    }

    @Override
    public void onPreShow() {
        view.setTitle(Localizer.get("title.Settings"));
        view.buildView();
    }

}
