/**
 * Shrinks a photo to at most `maxEdge` pixels on its long side, as a JPEG.
 * Phone photos run to several megabytes; a sheet stays legible at 2000px and
 * uploads in a moment. A format the browser cannot decode goes up as it is.
 */
export async function downscaleImage(file: Blob, maxEdge = 2000, quality = 0.85): Promise<Blob> {
  let bitmap: ImageBitmap;
  try {
    bitmap = await createImageBitmap(file, { imageOrientation: 'from-image' });
  } catch {
    return file;
  }
  const scale = Math.min(1, maxEdge / Math.max(bitmap.width, bitmap.height));
  const canvas = document.createElement('canvas');
  canvas.width = Math.round(bitmap.width * scale);
  canvas.height = Math.round(bitmap.height * scale);
  canvas.getContext('2d')?.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
  bitmap.close();
  return new Promise((resolve) => canvas.toBlob((blob) => resolve(blob ?? file), 'image/jpeg', quality));
}
