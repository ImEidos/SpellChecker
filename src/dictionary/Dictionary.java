package dictionary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;

import levenshtein.Levenshtein;

public class Dictionary {
  private Set<String> words = new HashSet<String>(100000);
  private Map<String, List<String>> trigramMap = new HashMap<>(100000);

  private Levenshtein levenshtein;

  public Dictionary(Scanner scanner, Levenshtein levenshtein) {
    this.levenshtein = levenshtein;
    while (scanner.hasNext()) {
      String word = scanner.next();
      words.add(word);
      var trigramWord = transformForTrigrams(word);

      for (int i = 0; i < trigramWord.length() - 2; i++) {
        String trigram = trigramWord.substring(i, i + 3);
        if (trigramMap.containsKey(trigram)) {
          trigramMap.get(trigram).add(word);
        } else {
          ArrayList<String> ws = new ArrayList<>(16);
          ws.ensureCapacity(16);
          ws.add(word);
          trigramMap.put(trigram, ws);
        }
      }
    }
  }

  /**
   * @param str The word to check
   * @return true if the words exists in the dictionary, false otherwise
   */
  public boolean exists(String word) {
    return words.contains(word);
  }

  /**
   * @param str A misspelled word to correct
   * @return The closests words to word, sorted by ascending levenshtein distance
   */
  public List<String> closestWords(String word) {
    var trigramWord = transformForTrigrams(word);

    // We count how many common trigrams there are between the word and those in the
    // dictionary
    HashMap<String, Integer> trigramOccs = new HashMap<>();
    for (int i = 0; i < trigramWord.length() - 2; i++) {
      String trigram = trigramWord.substring(i, i + 3);
      List<String> matchingWithTrigram = trigramMap.get(trigram);
      if (matchingWithTrigram == null)
        continue;
      for (String w : matchingWithTrigram)
        trigramOccs.compute(w, (s, n) -> n == null ? 1 : n + 1);
    }

    // We select the words that have the most trigrams in common
    Queue<String> withMostCommonTrigrams = new PriorityQueue<>(100,
        (a, b) -> trigramOccs.get(a).compareTo(trigramOccs.get(b)));

    for (String w : trigramOccs.keySet()) {
      if (withMostCommonTrigrams.size() < 100) {
        withMostCommonTrigrams.add(w);
      } else {
        String min = withMostCommonTrigrams.peek();
        if (trigramOccs.get(w) > trigramOccs.get(min)) {
          withMostCommonTrigrams.poll();
          withMostCommonTrigrams.add(w);
        }
      }
    }

    List<String> closestWords = new ArrayList<>(withMostCommonTrigrams);

    // We compute the levenshtein distance for each selected word
    Map<String, Integer> levDists = new HashMap<>(100);
    for (String w : closestWords)
      levDists.put(w, levenshtein.distance(word, w));

    closestWords.sort((a, b) -> levDists.get(a).compareTo(levDists.get(b)));
    return closestWords;
  }

  private String transformForTrigrams(String word) {
    word = word.toLowerCase();
    word = word.replace("é", "e");
    word = word.replace("è", "e");
    word = word.replace("û", "u");
    word = word.replace("œ", "oe");
    word = word.replace("ô", "o");
    return "<" + word + ">";
  }
}
