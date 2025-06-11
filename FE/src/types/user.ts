import { ISpeakerInfoGet } from "./speakerInfo";

export interface IUserData {
  name: string;
  email: string;
  imgUrl?: string;
  token: number;
  speakerLibrary: ISpeakerInfoGet[];
}

export interface IUserDataUpdate {
  nickname: string;
  imgUrl?: string;
}
