package com.github.itisokey.githelper.gitlab.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurableProvider;
import org.jetbrains.annotations.Nullable;


/**
 * @author Lv LiFeng
 * @date 2022/1/23 10:16
 */
public class SettingsConfigurableProvider extends ConfigurableProvider {

    @Nullable
    @Override
    public Configurable createConfigurable() {
        return new SettingsView(null);
    }

}
