import type { EditorState } from "@codemirror/state";
import type { CursorInfo } from "../-types/typst-editor";

export function cursorInfoFromState(state: EditorState): CursorInfo {
  const pos = state.selection.main.head;
  const line = state.doc.lineAt(pos);
  const text = state.doc.toString();
  const words = text.trim().length === 0 ? 0 : text.trim().split(/\s+/).length;
  return {
    line: line.number,
    column: pos - line.from + 1,
    characters: text.length,
    words,
  };
}
