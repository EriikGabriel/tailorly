import { useEffect, useState } from "react";
import { isPdfFile } from "../-utils/pdf-file";

/** URL local (`blob:`) do PDF, usada para saber se há uma prévia a exibir; é liberada quando o arquivo muda. */
export function usePdfPreviewUrl(file: File | null) {
  const [preview, setPreview] = useState<{ file: File; url: string } | null>(
    null,
  );

  useEffect(() => {
    if (!isPdfFile(file)) {
      setPreview(null);
      return;
    }
    const url = URL.createObjectURL(file);
    setPreview({ file, url });
    return () => URL.revokeObjectURL(url);
  }, [file]);

  // Nunca mostra o documento anterior enquanto a URL do novo é criada.
  return preview?.file === file ? preview?.url : undefined;
}
