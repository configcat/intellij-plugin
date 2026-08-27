import { HttpErrorResponse } from "@angular/common/http";
import { ChangeDetectionStrategy, Component, inject } from "@angular/core";
import {
  CreateFeatureFlagComponent, DEFAULT_CUSTOMIZE_CREATE_FEATURE_FLAG,
  ICustomizeCreateFeatureFlag,
  LinkFeatureFlagParameters
} from "ng-configcat-publicapi-ui";
import { AppData } from "../app-data";
import type { ConfigCatResponseData } from "../cc-response-data";

@Component({
  selector: "configcat-intellij-create-feature-flag",
  imports: [CreateFeatureFlagComponent],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: "./create-feature-flag-setting.component.html",
})
export class CreateFeatureFlagSettingComponent {
  appData = inject(AppData);

  createFeatureFlag(linkFeatureFlagParameters: LinkFeatureFlagParameters) {
    const responseData: ConfigCatResponseData = {
      type: "ff-create",
      data: linkFeatureFlagParameters.settingId.toString(),
    };
    window["configCatResponseMethod"].call(this, JSON.stringify(responseData));
  }

  componentFailed(error: Error) {
    const errorMessage = error.message;
    let errorStatus: number | undefined;
    if (error instanceof HttpErrorResponse) {
      errorStatus = error.status;
    }

    const responseData: ConfigCatResponseData = {
      type: "webview-fail",
      data: { message: errorMessage, status: errorStatus },
    };
    window["configCatResponseMethod"].call(this, JSON.stringify(responseData));
  }

  getCustomize(): ICustomizeCreateFeatureFlag {
    return {
      ...DEFAULT_CUSTOMIZE_CREATE_FEATURE_FLAG,
      hideLinkSection: true,
      targetSectionHeader: "Product and Config",
      targetSectionDescription: "The feature flag will be created under the following product and config in ConfigCat.",
    };
  }
}
