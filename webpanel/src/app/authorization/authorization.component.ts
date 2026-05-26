import { Component, inject } from "@angular/core";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MatButton } from "@angular/material/button";
import { AuthorizationComponent, AuthorizationModel } from "ng-configcat-publicapi-ui";
import { AppData } from "../app-data";

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
    window["configCatSuccessMethod"].call(this, JSON.stringify(authorizationParameters));
  }

  unauthorize() {
    window["configCatSuccessMethod"].call(this, "unauthorize");
  }

}
