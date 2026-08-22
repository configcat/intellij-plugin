import { HttpErrorResponse } from "@angular/common/http";
import { ChangeDetectionStrategy, Component, inject } from "@angular/core";
import {
  CreateConfigComponent,
  DEFAULT_CUSTOMIZE_CREATE_CONFIG,
  ICustomizeCreateConfig
} from "ng-configcat-publicapi-ui";
import { AppData } from "../app-data";
import type { ConfigCatResponseData } from "../cc-response-data";

@Component({
  selector: "configcat-intellij-create-config",
  imports: [CreateConfigComponent],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: "./create-config.component.html",
})
export class ConfigCreateComponent {
  appData = inject(AppData);

  createConfig(configId: string) {
    const responseData: ConfigCatResponseData = { type: "config-create", data: configId };
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

  getCustomize(): ICustomizeCreateConfig {
    return { ...DEFAULT_CUSTOMIZE_CREATE_CONFIG, hideCancelButton: false, targetSectionHeader: "Product", targetSectionDescription: "The config will be created under the following product in ConfigCat." };
  }
}
