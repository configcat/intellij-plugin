import { HttpErrorResponse } from "@angular/common/http";
import { Component, inject, OnInit, ChangeDetectionStrategy } from "@angular/core";
import { EvaluationVersion } from "ng-configcat-publicapi";
import { FeatureFlagItemComponent, SettingItemComponent } from "ng-configcat-publicapi-ui";
import { AppData } from "../app-data";
import type { ConfigCatResponseData } from "../cc-response-data";

@Component({
  selector: "configcat-intellij-feature-flag-setting",
  imports: [SettingItemComponent, FeatureFlagItemComponent],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: "./feature-flag-setting.component.html",
})
export class FeatureFlagSettingComponent implements OnInit {
  ngOnInit(): void {
    console.log("FF ngOnInit");
    console.log(this.appData);
  }
  appData = inject(AppData);
  EvaluationVersion = EvaluationVersion;

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
