import { Speaker } from "@/types/script-editor"

export function getAvatarColor(type: Speaker): string {
  switch (type) {
    case "Narrator":
      return "bg-blue-500"
    case "Speaker A":
      return "bg-green-500"
    case "Speaker B":
      return "bg-purple-500"
    case "Situation":
      return "bg-amber-500"
    default:
      return "bg-gray-500"
  }
}

export function getAvatarInitial(type: Speaker): string {
  switch (type) {
    case "Narrator":
      return "N"
    case "Speaker A":
      return "A"
    case "Speaker B":
      return "B"
    case "Situation":
      return "S"
    default:
      return "?"
  }
}