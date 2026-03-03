import { Component, inject, OnInit } from "@angular/core";
import { EvaluationVersion } from "ng-configcat-publicapi";
import { FeatureFlagItemComponent, SettingItemComponent } from "ng-configcat-publicapi-ui";
import { AppData } from "../app-data";

@Component({
  selector: "configcat-intellij-feature-flag-setting",
  imports: [SettingItemComponent, FeatureFlagItemComponent],
  templateUrl: "./feature-flag-setting.component.html",
})
export class FeatureFlagSettingComponent implements OnInit {
  ngOnInit(): void {
    console.log("FF ngOnInit");
    console.log(this.appData);
  }
  appData = inject(AppData);
  EvaluationVersion = EvaluationVersion;

  saveFailed() {
    console.log("FF save failed");
  }
}
