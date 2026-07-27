export interface Card {
  word: string;
  translationEn: string | null;
  sentenceText: string;
  audioUrl: string;
}

export async function fetchNextCard(language: string): Promise<Card> {
  const response = await fetch(`/api/cards/next?language=${encodeURIComponent(language)}`);
  if (!response.ok) {
    throw new Error(`Failed to fetch next card: ${response.status}`);
  }
  return response.json();
}
