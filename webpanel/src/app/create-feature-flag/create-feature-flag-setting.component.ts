import { Component, inject } from "@angular/core";
import { CreateFeatureFlagComponent, LinkFeatureFlagParameters } from "ng-configcat-publicapi-ui";
import { AppData } from "../app-data";

@Component({
  selector: "configcat-intellij-create-feature-flag",
  imports: [CreateFeatureFlagComponent],
  templateUrl: "./create-feature-flag-setting.component.html",
})
export class CreateFeatureFlagSettingComponent {
  appData = inject(AppData);

  createFeatureFlag(linkFeatureFlagParameters: LinkFeatureFlagParameters) {
    console.log("FF was created: " + linkFeatureFlagParameters.settingId);
  }
}
