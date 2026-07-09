import { Component, inject } from "@angular/core";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MatButton } from "@angular/material/button";
import { AuthorizationComponent, AuthorizationModel } from "ng-configcat-publicapi-ui";
import { AppData } from "../app-data";
import type { ConfigCatResponseData } from "../cc-response-data";

@Component({
  selector: "configcat-intellij-authorization",
  templateUrl: "./authorization.component.html",
  styleUrls: ["./authorization.component.scss"],
  imports: [
    FormsModule,
    ReactiveFormsModule,
    MatButton,
    AuthorizationComponent,
  ],
})
export class AuthComponent {
  appData = inject(AppData);
  loading = true;

  login(authorizationParameters: AuthorizationModel) {
    const responseData: ConfigCatResponseData = { type: "authorization", data: authorizationParameters };
    console.log("authorization: " + responseData);
    window["configCatResponseMethod"].call(this, JSON.stringify(responseData));
  }

  unauthorize() {
    const responseData: ConfigCatResponseData = { type: "authorization", data: "unauthorize" };
    window["configCatResponseMethod"].call(this, JSON.stringify(responseData));
  }

}
