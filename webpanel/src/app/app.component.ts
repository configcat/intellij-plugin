import { Component, inject, OnDestroy, OnInit } from "@angular/core";
import { Theme, ThemeService } from "ng-configcat-publicapi-ui";
import { ViewData } from "./app.config";
import { ConfigCreateComponent } from "./create-config/create-config.component";
import { CreateFeatureFlagSettingComponent } from "./create-feature-flag/create-feature-flag-setting.component";
import { FeatureFlagSettingComponent } from "./feature-flag-setting/feature-flag-setting.component";

@Component({
  selector: "configcat-intellij-root",
  templateUrl: "./app.component.html",
  styles: [],
  imports: [CreateFeatureFlagSettingComponent, FeatureFlagSettingComponent, ConfigCreateComponent],

})
export class AppComponent implements OnInit, OnDestroy {

  private readonly themeService = inject(ThemeService);
  viewData = inject(ViewData);

  postThemeChange = (event: MessageEvent<({ command: string; value: string })>) => {
    const message = event.data;
    if (message.command === "themeChange") {
      const turnOn = message.value === "dark";
      this.themeService.setTheme(turnOn ? Theme.Dark : Theme.Light);
    }
  }

  ngOnInit(): void {
    if (this.viewData.initialTheme === "dark") {
      this.themeService.setTheme(Theme.Dark);
    }

    window.addEventListener("message", this.postThemeChange);
  }

  ngOnDestroy(): void {
    window.removeEventListener("message", this.postThemeChange);
  }

}
