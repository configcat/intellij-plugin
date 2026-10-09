import type { AuthorizationModel } from "ng-configcat-publicapi-ui";

export interface ConfigCatWebViewFailData {
  message: string;
  status: number | undefined;
}

export type ConfigCatResponseData =
  | { type: "none"; data: null }
  | { type: "authorization"; data: AuthorizationModel | "unauthorize" }
  | { type: "config-create"; data: string }
  | { type: "ff-create"; data: string }
  | { type: "webview-fail"; data: ConfigCatWebViewFailData };
