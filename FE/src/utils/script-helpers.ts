import { Speaker, ScriptLineType, defaultEmotions } from "@/types/script-editor";

export function getLineStyles(type: Speaker): string {
  switch (type) {
    case "Narrator":
      return "border-l-4 border-blue-500 bg-blue-50";
    case "Speaker A":
      return "border-l-4 border-green-500 bg-green-50";
    case "Speaker B":
      return "border-l-4 border-purple-500 bg-purple-50";
    case "Situation":
      return "border-l-4 border-amber-500 bg-amber-50";
    default:
      return "bg-gray-50";
  }
}

export function getAvatarColor(type: Speaker): string {
  switch (type) {
    case "Narrator":
      return "bg-blue-500";
    case "Speaker A":
      return "bg-green-500";
    case "Speaker B":
      return "bg-purple-500";
    case "Situation":
      return "bg-amber-500";
    default:
      return "bg-gray-500";
  }
}

export function getAvatarInitial(type: Speaker): string {
  switch (type) {
    case "Narrator":
      return "N";
    case "Speaker A":
      return "A";
    case "Speaker B":
      return "B";
    case "Situation":
      return "S";
    default:
      return (type as string).charAt(0);
  }
}

export function createNewScriptLine(type: Speaker = "Speaker A", content: string = "New dialogue line"): ScriptLineType {
  return {
    id: `line-${Date.now()}`,
    type,
    content,
    emotions: defaultEmotions,
  };
}

export function exportScript(scriptLines: ScriptLineType[]): void {
  const jsonData = JSON.stringify(scriptLines, null, 2);
  const blob = new Blob([jsonData], { type: "application/json" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = "script-data.json";
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}