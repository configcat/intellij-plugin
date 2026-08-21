import { HttpErrorResponse } from "@angular/common/http";
import { Component, inject, ChangeDetectionStrategy } from "@angular/core";
import { CreateConfigComponent } from "ng-configcat-publicapi-ui";
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
}
