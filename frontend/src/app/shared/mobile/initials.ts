/** "Player A" → "PA", "Ana" → "AN" — for the round avatars on mobile. */
export function initials(name: string): string {
  const words = name.trim().split(/\s+/).filter(Boolean);
  if (words.length > 1) {
    return (words[0][0] + words[words.length - 1][0]).toUpperCase();
  }
  return (words[0] ?? '?').slice(0, 2).toUpperCase();
}
