import { Component, inject } from "@angular/core";
import { CreateConfigComponent } from "ng-configcat-publicapi-ui";
import { AppData } from "../app-data";

@Component({
  selector: "configcat-intellij-create-config",
  imports: [CreateConfigComponent],
  templateUrl: "./create-config.component.html",
})
export class ConfigCreateComponent {

  appData = inject(AppData);

  createConfig(configId: string) {
    window["CONFIGCAT_SUCCESS_METHOD"].call(this, configId);
  }
}
