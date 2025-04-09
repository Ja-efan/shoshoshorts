export interface ISpeakerInfo {
  title: string;
  description: string;
  audioBase64: string;
}

export interface ISpeakerInfoGet {
  id: number;
  title: string;
  description: string;
  voiceSampleUrl: string;
  createdAt: string;
  updatedAt: string;
}

export interface ISpeakerInfoDelete {
  speakerId: string;
}
